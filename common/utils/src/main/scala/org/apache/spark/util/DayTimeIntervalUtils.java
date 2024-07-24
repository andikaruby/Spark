package org.apache.spark.util;

import org.apache.spark.SparkException;

import java.math.BigDecimal;
import java.util.ArrayList;

// Replicating code from SparkIntervalUtils so code in the 'common' space can work with
// year-month intervals.
public class DayTimeIntervalUtils {
  private static byte DAY = 0;
  private static byte HOUR = 1;
  private static byte MINUTE = 2;
  private static byte SECOND = 3;
  private static long HOURS_PER_DAY = 24;
  private static long MINUTES_PER_HOUR = 60;
  private static long SECONDS_PER_MINUTE = 60;
  private static long MILLIS_PER_SECOND = 1000;
  private static long MICROS_PER_MILLIS = 1000;
  private static long MICROS_PER_SECOND = MICROS_PER_MILLIS * MILLIS_PER_SECOND;
  private static long MICROS_PER_MINUTE = SECONDS_PER_MINUTE * MICROS_PER_SECOND;
  private static long MICROS_PER_HOUR = MINUTES_PER_HOUR * MICROS_PER_MINUTE;
  private static long MICROS_PER_DAY = HOURS_PER_DAY * MICROS_PER_HOUR;
  private static long MAX_DAY = Long.MAX_VALUE / MICROS_PER_DAY;
  private static long MAX_HOUR = Long.MAX_VALUE / MICROS_PER_HOUR;
  private static long MAX_MINUTE = Long.MAX_VALUE / MICROS_PER_MINUTE;
  private static long MAX_SECOND = Long.MAX_VALUE / MICROS_PER_SECOND;

  public static String fieldToString(byte field) throws SparkException {
    if (field == DAY) {
      return "DAY";
    } else if (field == HOUR) {
      return "HOUR";
    } else if (field == MINUTE) {
      return "MINUTE";
    } else if (field == SECOND) {
      return "SECOND";
    } else {
      throw new SparkException("Invalid field in day-time interval: " + field +
              ". Supported fields are: DAY, HOUR, MINUTE, SECOND");
    }
  }

  public static String toDayTimeIntervalANSIString(long micros, byte startField, byte endField)
          throws SparkException {
    String sign = "";
    long rest = micros;
    try {
      String from = fieldToString(startField).toUpperCase();
      String to = fieldToString(endField).toUpperCase();
      String prefix = "INTERVAL '";
      String postfix = startField == endField ? "' " + from : "' " + from + " TO " + to;
      if (micros < 0) {
        if (micros == Long.MIN_VALUE) {
          // Especial handling of minimum `Long` value because negate op overflows `Long`.
          // seconds = 106751991 * (24 * 60 * 60) + 4 * 60 * 60 + 54 = 9223372036854
          // microseconds = -9223372036854000000L-775808 == Long.MinValue
          String baseStr = "-106751991 04:00:54.775808000";
          String firstStr = "-" + (startField == DAY ? Long.toString(MAX_DAY) :
                  (startField == HOUR ? Long.toString(MAX_HOUR) :
                          (startField == MINUTE ? Long.toString(MAX_MINUTE) :
                                  Long.toString(MAX_SECOND) + ".775808")));
          if (startField == endField) {
            return prefix + firstStr + postfix;
          } else {
            int substrStart = startField == DAY ? 10 : (startField == HOUR ? 13 : 16);
            int substrEnd = endField == HOUR ? 13 : (endField == MINUTE ? 16 : 26);
            return prefix + firstStr + baseStr.substring(substrStart, substrEnd) + postfix;
          }
        } else {
          sign = "-";
          rest = -rest;
        }
      }
      StringBuilder formatBuilder = new StringBuilder(sign);
      ArrayList<Long> formatArgs = new ArrayList<>();
      if (startField == DAY) {
        formatBuilder.append(rest / MICROS_PER_DAY);
        rest %= MICROS_PER_DAY;
      } else if (startField == HOUR) {
        formatBuilder.append("%02d");
        formatArgs.add(rest / MICROS_PER_HOUR);
        rest %= MICROS_PER_HOUR;
      } else if (startField == MINUTE) {
        formatBuilder.append("%02d");
        formatArgs.add(rest / MICROS_PER_MINUTE);
        rest %= MICROS_PER_MINUTE;
      } else if (startField == SECOND) {
        String leadZero = rest < 10 * MICROS_PER_SECOND ? "0" : "";
        formatBuilder.append(leadZero + BigDecimal.valueOf(rest, 6)
                .stripTrailingZeros().toPlainString());
      }

      if (startField < HOUR && HOUR <= endField) {
        formatBuilder.append(" %02d");
        formatArgs.add(rest / MICROS_PER_HOUR);
        rest %= MICROS_PER_HOUR;
      }
      if (startField < MINUTE && MINUTE <= endField) {
        formatBuilder.append(":%02d");
        formatArgs.add(rest / MICROS_PER_MINUTE);
        rest %= MICROS_PER_MINUTE;
      }
      if (startField < SECOND && SECOND <= endField) {
        String leadZero = rest < 10 * MICROS_PER_SECOND ? "0" : "";
        formatBuilder.append(":" + leadZero + BigDecimal.valueOf(rest, 6)
                .stripTrailingZeros().toPlainString());
      }
      return prefix + String.format(formatBuilder.toString(), formatArgs.toArray()) + postfix;
    } catch (SparkException e) {
      throw e;
    }
  }
}
