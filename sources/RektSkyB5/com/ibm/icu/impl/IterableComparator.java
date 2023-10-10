/*
 * Decompiled with CFR 0.152.
 */
package com.ibm.icu.impl;

import java.util.Comparator;
import java.util.Iterator;

public class IterableComparator<T>
implements Comparator<Iterable<T>> {
    private final Comparator<T> comparator;
    private final int shorterFirst;
    private static final IterableComparator NOCOMPARATOR = new IterableComparator();

    public IterableComparator() {
        this(null, true);
    }

    public IterableComparator(Comparator<T> comparator) {
        this(comparator, true);
    }

    public IterableComparator(Comparator<T> comparator, boolean shorterFirst) {
        this.comparator = comparator;
        this.shorterFirst = shorterFirst ? 1 : -1;
    }

    @Override
    public int compare(Iterable<T> a2, Iterable<T> b2) {
        T bItem;
        T aItem;
        int result;
        if (a2 == null) {
            return b2 == null ? 0 : -this.shorterFirst;
        }
        if (b2 == null) {
            return this.shorterFirst;
        }
        Iterator<T> ai = a2.iterator();
        Iterator<T> bi = b2.iterator();
        do {
            if (!ai.hasNext()) {
                return bi.hasNext() ? -this.shorterFirst : 0;
            }
            if (!bi.hasNext()) {
                return this.shorterFirst;
            }
            aItem = ai.next();
            bItem = bi.next();
        } while ((result = this.comparator != null ? this.comparator.compare(aItem, bItem) : ((Comparable)aItem).compareTo(bItem)) == 0);
        return result;
    }

    public static <T> int compareIterables(Iterable<T> a2, Iterable<T> b2) {
        return NOCOMPARATOR.compare(a2, b2);
    }
}

