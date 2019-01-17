package com.coherentchaos.gdb.debugadapter.utils;

import java.util.Arrays;

/**
 * In mathematics, a pairing function is a process to uniquely encode two
 * natural numbers into a single natural number.
 * <p>
 * https://en.wikipedia.org/wiki/Pairing_function
 */
public class Cantor {
    public static long pair(long x, long y) {
        if (x < 0 || y < 0)
            return -1;

        long[] input = {x, y};
        long result = (long) (0.5 * (x + y) * (x + y + 1) + y);
        if (Arrays.equals(depair(result), input))
            return result;
        else
            return -1;
    }

    public static long[] depair(long z) {
        long w = (long) (Math.floor((Math.sqrt(8 * z + 1) - 1) / 2));
        long t = (long) ((Math.pow(w, 2) + w) / 2);
        long y = z - t;
        long x = w - y;
        return new long[]{x, y};
    }
}
