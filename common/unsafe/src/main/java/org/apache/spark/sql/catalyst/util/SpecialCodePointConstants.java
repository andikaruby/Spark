package org.apache.spark.sql.catalyst.util;

public class SpecialCodePointConstants {

    public static final int COMBINING_DOT = 0x0307;
    public static final int ASCII_SMALL_I = 0x0069;
    /**
     * `COMBINED_ASCII_SMALL_I_COMBINING_DOT` is an internal representation of the combined lowercase
     * code point for ASCII lowercase letter i with an additional combining dot character (U+0307).
     * This integer value is not a valid code point itself, but rather an artificial code point
     * marker used to represent the two lowercase characters that are the result of converting the
     * uppercase Turkish dotted letter I with a combining dot character (U+0130) to lowercase.
     */
    public static final int COMBINED_ASCII_SMALL_I_COMBINING_DOT =
      ASCII_SMALL_I << 16 | COMBINING_DOT;
    public static final int ASCII_SPACE = 0x0020;
    public static final int GREEK_CAPITAL_SIGMA = 0x03A3;
    public static final int GREEK_SMALL_SIGMA = 0x03C3;
    public static final int GREEK_FINAL_SIGMA = 0x03C2;
    public static final int CAPITAL_I_WITH_DOT_ABOVE = 0x0130;
}
