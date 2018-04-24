package ria.lang.compiler.code;

import ria.lang.compiler.Binder;
import ria.lang.compiler.CType;
import ria.lang.compiler.CompileException;
import ria.lang.compiler.Context;
import ria.lang.compiler.code.Apply;
import ria.lang.compiler.code.BindRef;

public abstract class CaptureRef extends BindRef {
    Function capturer;
    BindRef ref;
    private Binder[] args;
    private Capture[] argCaptures;
    private boolean hasArgCaptures;

    Capture[] argCaptures() {
        if(hasArgCaptures && argCaptures == null) {
            argCaptures = new Capture[args.length];
            for(Capture c = capturer.captures; c != null; c = c.next) {
                for(int i = args.length; --i >= 0; ) {
                    if(c.binder == args[i]) {
                        argCaptures[i] = c;
                        break;
                    }
                }
            }
        }
        return argCaptures;
    }

    @Override
    public Code apply(Code arg, CType res, int line) {
        if(args != null) {
            return new SelfApply(res, this, arg, line, args.length);
        }

        int n = 0;
        for(Function f = capturer; f != null; ++n, f = f.outer) {
            if(f.selfBind == ref.binder) {
                if(ref.flagop(ASSIGN)) {
                    break; // no tail recursion for vars
                }
                args = new Binder[n];
                f = capturer.outer;
                for(int i = n; --i >= 0; f = f.outer) {
                    args[i] = f;
                }
                return new SelfApply(res, this, arg, line, n);
            }
        }
        return super.apply(arg, res, line);
    }

    final class SelfApply extends Apply {
        boolean tail;
        int depth;

        SelfApply(CType type, Code f, Code arg, int line, int depth) {
            super(type, f, arg, line);
            this.depth = depth;
            if(origin != null) {
                this.arity = origin.arity = args.length - depth + 1;
                this.ref = origin;
            }
            if(depth == 0 && capturer.argCaptures == null) {
                if(hasArgCaptures) {
                    throw new CompileException(line, 0, "Internal error - already has argCaptures");
                }
                hasArgCaptures = true;
                // we have to resolve the captures lazily later,
                // as here all might not yet be referenced
                capturer.argCaptures = CaptureRef.this;
            }
        }

        // evaluates call arguments and pushes values into stack
        void genArg(Context context, int i) {
            if(i > 0) {
                ((SelfApply)fun).genArg(context, i - 1);
            }
            arg.gen(context);
        }

        @Override
        public void gen(Context context) {
            if(!tail || depth != 0 || capturer.argCaptures != CaptureRef.this
                || capturer.restart == null) {
                // regular apply, if tail call optimisation can't be done
                super.gen(context);
                return;
            }
            // push all argument values into stack - they must be evaluated
            // BEFORE modifying any of the arguments for tail-"call"-jump.
            genArg(context, argCaptures() == null ? 0 : argCaptures.length);
            context.varInsn(ASTORE, capturer.argVar);
            // Now assign the call argument values into argument registers.
            if(argCaptures != null) {
                int i = argCaptures.length;
                if(capturer.outer != null && capturer.outer.merged &&
                    i > 0 && argCaptures[i - 1] == null) {
                    --i;
                    context.varInsn(ASTORE, 1); // HACK - fixes merged argument
                }
                while(--i >= 0) {
                    if(argCaptures[i] != null) {
                        context.varInsn(ASTORE, argCaptures[i].localVar);
                    } else {
                        context.insn(POP);
                    }
                }
            }
            // And just jump into the start of the function...
            context.jumpInsn(GOTO, capturer.restart);
        }

        @Override
        public void markTail() {
            tail = true;
        }

        @Override
        public Code apply(Code arg, CType res, int line) {
            if(depth < 0) {
                return super.apply(arg, res, line);
            }
            return new SelfApply(res, this, arg, line, depth - 1);
        }
    }
}
