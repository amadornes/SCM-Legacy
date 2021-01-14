package com.amadornes.rscircuits.util;

import java.util.Collection;
import java.util.EnumSet;

public class MiscUtils {

    public static <E extends Enum<E>> EnumSet<E> asEnumSet(Collection<E> collection, Class<E> type) {

        if (collection == null || collection instanceof EnumSet) {
            return (EnumSet<E>) collection;
        }
        EnumSet<E> set = EnumSet.noneOf(type);
        set.addAll(collection);
        return set;
    }

}
