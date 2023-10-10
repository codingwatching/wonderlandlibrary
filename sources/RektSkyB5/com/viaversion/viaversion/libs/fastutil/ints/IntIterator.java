/*
 * Decompiled with CFR 0.152.
 */
package com.viaversion.viaversion.libs.fastutil.ints;

import com.viaversion.viaversion.libs.fastutil.ints.IntConsumer;
import java.util.PrimitiveIterator;
import java.util.function.Consumer;

public interface IntIterator
extends PrimitiveIterator.OfInt {
    @Override
    public int nextInt();

    @Override
    @Deprecated
    default public Integer next() {
        return this.nextInt();
    }

    @Override
    default public void forEachRemaining(IntConsumer action) {
        this.forEachRemaining((java.util.function.IntConsumer)action);
    }

    @Override
    @Deprecated
    default public void forEachRemaining(Consumer<? super Integer> action) {
        this.forEachRemaining(action instanceof java.util.function.IntConsumer ? (java.util.function.IntConsumer)((Object)action) : action::accept);
    }

    default public int skip(int n2) {
        if (n2 < 0) {
            throw new IllegalArgumentException("Argument must be nonnegative: " + n2);
        }
        int i2 = n2;
        while (i2-- != 0 && this.hasNext()) {
            this.nextInt();
        }
        return n2 - i2 - 1;
    }
}

