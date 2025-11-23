package com.rayvinchen.async.event.core.util;


import com.rayvinchen.async.event.core.exception.AsyncEventException;

/**
 * Asserts
 *
 * @author rayvinchen
 * @since 2025/11/8 17:11
 */
public class Asserts {

    private Asserts() {}

    public static void notNull(Object object, String errMsg) {
        if (object == null) {
            throw new AsyncEventException(errMsg);
        }
    }

    public static void check(boolean condition, String errMsg) {
        if (!condition) {
            throw new AsyncEventException(errMsg);
        }
    }

}
