/*
 * Decompiled with CFR 0.152.
 */
package com.ibm.icu.text;

public class BidiClassifier {
    protected Object context;

    public BidiClassifier(Object context) {
        this.context = context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public Object getContext() {
        return this.context;
    }

    public int classify(int c2) {
        return 23;
    }
}

