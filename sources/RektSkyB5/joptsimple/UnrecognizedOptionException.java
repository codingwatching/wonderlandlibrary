/*
 * Decompiled with CFR 0.152.
 */
package joptsimple;

import java.util.Collections;
import joptsimple.OptionException;

class UnrecognizedOptionException
extends OptionException {
    private static final long serialVersionUID = -1L;

    UnrecognizedOptionException(String option) {
        super(Collections.singletonList(option));
    }

    Object[] messageArguments() {
        return new Object[]{this.singleOptionString()};
    }
}

