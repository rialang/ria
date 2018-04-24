package ria.lang.compiler;

import ria.lang.compiler.code.BindRef;
import ria.lang.compiler.code.BooleanConstant;
import ria.lang.compiler.code.Compose;
import ria.lang.compiler.code.Cons;
import ria.lang.compiler.code.Escape;
import ria.lang.compiler.code.For;
import ria.lang.compiler.code.Foreach;
import ria.lang.compiler.code.Head;
import ria.lang.compiler.code.InOpFun;
import ria.lang.compiler.code.IsDefined;
import ria.lang.compiler.code.IsEmpty;
import ria.lang.compiler.code.IsNullPtr;
import ria.lang.compiler.code.LazyCons;
import ria.lang.compiler.code.Length;
import ria.lang.compiler.code.MatchOpFun;
import ria.lang.compiler.code.Negate;
import ria.lang.compiler.code.NotOp;
import ria.lang.compiler.code.Same;
import ria.lang.compiler.code.StaticRef;
import ria.lang.compiler.code.StrChar;
import ria.lang.compiler.code.Latch;
import ria.lang.compiler.code.Tail;
import ria.lang.compiler.code.Throw;
import ria.lang.compiler.code.UnitConstant;

public final class Intrinsic implements Binder {
    private int op;

    public Intrinsic(int op) {
        this.op = op;
    }

    public static BindRef undef_str(Binder binder, int line) {
        return new StaticRef("ria/lang/Core", "UNDEF_STR",
            RiaType.STR_TYPE, binder, true, line);
    }

    @Override
    public BindRef getRef(int line) {
        BindRef r = null;
        switch(op) {
            //case 1: obsolete
            // Used to be argv
            //break
            case 2:
                r = new InOpFun(line);
                break;
            case 3:
                r = new Cons(line);
                break;
            case 4:
                r = new LazyCons(line);
                break;
            case 5:
                r = new For(line);
                break;
            case 6:
                r = new Compose(line);
                break;
            case 7:
                r = new Latch(line);
                break;
            case 8:
                r = new IsNullPtr(RiaType.A_TO_BOOL, "nullptr$q", line);
                break;
            case 9:
                r = new IsDefined(line);
                break;
            case 10:
                r = new IsEmpty(line);
                break;
            case 11:
                r = new Head(line);
                break;
            case 12:
                r = new Tail(line);
                break;
            case 13:
                r = new MatchOpFun(line, true);
                break;
            case 14:
                r = new MatchOpFun(line, false);
                break;
            case 15:
                r = new NotOp(line);
                break;
            case 16:
                r = new StrChar(line);
                break;
            case 17:
                r = new UnitConstant(RiaType.BOOL_TYPE);
                break;
            case 18:
                r = new BooleanConstant(false);
                break;
            case 19:
                r = new BooleanConstant(true);
                break;
            case 20:
                r = new Negate();
                break;
            case 21:
                r = new Same();
                break;
            case 23:
                r = undef_str(this, line);
                break;
            case 24:
                r = new Escape(line);
                break;
            case 25:
                r = new Length();
                break;
            case 26:
                r = new Throw(line);
                break;
            case 27:
                r = new Foreach(line);
        }
        if(r != null) {
            r.binder = this;
        }
        return r;
    }
}
