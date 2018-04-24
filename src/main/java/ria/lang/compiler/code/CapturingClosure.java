package ria.lang.compiler.code;

import ria.lang.compiler.Context;

public abstract class CapturingClosure extends AbstractClosure {
    Capture captures;

    Capture captureRef(BindRef code) {
        for(Capture c = captures; c != null; c = c.next) {
            if(c.binder == code.binder) {
                // evil hack... ref sharing broke fun-method
                // optimisation accounting of ref usage
                c.origin = code.origin;
                return c;
            }
        }
        Capture c = new Capture();
        c.binder = code.binder;
        c.type = code.type;
        c.polymorph = code.polymorph;
        c.ref = code;
        c.wrapper = code.capture();
        c.origin = code.origin;
        c.next = captures;
        captures = c;
        return c;
    }

    @Override
    public BindRef refProxy(BindRef code) {
        return code.flagop(DIRECT_BIND) ? code : captureRef(code);
    }

    // Called by mergeCaptures to initialize a capture.
    // It must be ok to copy capture after that.
    abstract void captureInit(Context fun, Capture c, int n);

    // mergeCaptures seems to drop only some uncaptured ones
    // (looks like because so is easy to do, currently
    // this seems to cause extra check only in Function.finishGen).
    int mergeCaptures(Context context, boolean cleanup) {
        int counter = 0;
        Capture prev = null, next;

        next_capture:
        for(Capture c = captures; c != null; c = next) {
            next = c.next;
            Object identity = c.identity = c.captureIdentity();
            if(cleanup && (c.uncaptured || c.ref.flagop(DIRECT_BIND))) {
                c.uncaptured = true;
                if(prev == null) {
                    captures = next;
                } else {
                    prev.next = next;
                }
            }
            if(c.uncaptured) {
                continue;
            }
            // remove shared captures
            for(Capture i = captures; i != c; i = i.next) {
                if(i.identity == identity) {
                    c.id = i.id; // copy old one's id
                    c.localVar = i.localVar;
                    prev.next = next;
                    onMerge(c);
                    continue next_capture;
                }
            }
            captureInit(context, c, counter++);
            prev = c;
        }
        return counter;
    }

    void onMerge(Capture removed) {
    }
}
