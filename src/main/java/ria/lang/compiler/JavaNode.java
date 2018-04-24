package ria.lang.compiler;

// Java source tree node, can be class or method or field.
public class JavaNode {
    int modifier;
    String type; // extends for classes
    String name; // full name for classes
    JavaNode field;  // field/method list
    String[] argv;   // implements for classes
    JavaSource source;
}
