package ria.lang.compiler;

import ria.lang.compiler.code.Code;

public interface CaptureWrapper {
    void genPreGet(Context context);

    void genGet(Context context);

    void genSet(Context context, Code value);

    Object captureIdentity();

    String captureType();
}
