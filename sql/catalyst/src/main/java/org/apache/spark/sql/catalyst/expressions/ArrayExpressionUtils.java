/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.sql.catalyst.expressions;

import java.util.Arrays;
import java.util.Comparator;

import scala.reflect.ClassTag$;

import org.apache.spark.sql.catalyst.util.ArrayData;
import org.apache.spark.sql.catalyst.util.SQLOrderingUtil;
import org.apache.spark.sql.types.DataType;

import static org.apache.spark.sql.types.DataTypes.BooleanType;
import static org.apache.spark.sql.types.DataTypes.ByteType;
import static org.apache.spark.sql.types.DataTypes.DoubleType;
import static org.apache.spark.sql.types.DataTypes.FloatType;
import static org.apache.spark.sql.types.DataTypes.IntegerType;
import static org.apache.spark.sql.types.DataTypes.LongType;
import static org.apache.spark.sql.types.DataTypes.ShortType;

public class ArrayExpressionUtils {

  // comparator
  // Boolean ascending nullable comparator
  private static final Comparator<Boolean> booleanComp = (o1, o2) -> {
    if (o1 == null && o2 == null) {
      return 0;
    } else if (o1 == null) {
      return -1;
    } else if (o2 == null) {
      return 1;
    }
    return o1.equals(o2) ? 0 : (o1 ? 1 : -1);
  };

  // Byte ascending nullable comparator
  private static final Comparator<Byte> byteComp = (o1, o2) -> {
    if (o1 == null && o2 == null) {
      return 0;
    } else if (o1 == null) {
      return -1;
    } else if (o2 == null) {
      return 1;
    }
    return Byte.compare(o1, o2);
  };

  // Short ascending nullable comparator
  private static final Comparator<Short> shortComp = (o1, o2) -> {
    if (o1 == null && o2 == null) {
      return 0;
    } else if (o1 == null) {
      return -1;
    } else if (o2 == null) {
      return 1;
    }
    return Short.compare(o1, o2);
  };

  // Integer ascending nullable comparator
  private static final Comparator<Integer> integerComp = (o1, o2) -> {
    if (o1 == null && o2 == null) {
      return 0;
    } else if (o1 == null) {
      return -1;
    } else if (o2 == null) {
      return 1;
    }
    return Integer.compare(o1, o2);
  };

  // Long ascending nullable comparator
  private static final Comparator<Long> longComp = (o1, o2) -> {
    if (o1 == null && o2 == null) {
      return 0;
    } else if (o1 == null) {
      return -1;
    } else if (o2 == null) {
      return 1;
    }
    return Long.compare(o1, o2);
  };

  // Float ascending nullable comparator
  private static final Comparator<Float> floatComp = (o1, o2) -> {
    if (o1 == null && o2 == null) {
      return 0;
    } else if (o1 == null) {
      return -1;
    } else if (o2 == null) {
      return 1;
    }
    return SQLOrderingUtil.compareFloats(o1, o2);
  };

  // Double ascending nullable comparator
  private static final Comparator<Double> doubleComp = (o1, o2) -> {
    if (o1 == null && o2 == null) {
      return 0;
    } else if (o1 == null) {
      return -1;
    } else if (o2 == null) {
      return 1;
    }
    return SQLOrderingUtil.compareDoubles(o1, o2);
  };

  // boolean
  // boolean non-nullable
  public static int binarySearch(boolean[] data, boolean value) {
    int low = 0;
    int high = data.length - 1;

    while (low <= high) {
      int mid = (low + high) >>> 1;
      boolean midVal = data[mid];

      int cmp = booleanComp.compare(midVal, value);
      if (cmp < 0) {
        low = mid + 1;
      } else if (cmp > 0) {
        high = mid - 1;
      } else {
        return mid; // key found
      }
    }

    return -(low + 1);  // key not found.
  }

  // Boolean nullable
  public static int binarySearch(Boolean[] data, Boolean value) {
    return Arrays.binarySearch(data, value, booleanComp);
  }

  // byte
  // byte non-nullable
  public static int binarySearch(byte[] data, byte value) {
    return Arrays.binarySearch(data, value);
  }

  // Byte nullable
  public static int binarySearch(Byte[] data, Byte value) {
    return Arrays.binarySearch(data, value, byteComp);
  }

  // short
  // short non-nullable
  public static int binarySearch(short[] data, short value) {
    return Arrays.binarySearch(data, value);
  }

  // Short nullable
  public static int binarySearch(Short[] data, Short value) {
    return Arrays.binarySearch(data, value, shortComp);
  }

  // int
  // int non-nullable
  public static int binarySearch(int[] data, int value) {
    return Arrays.binarySearch(data, value);
  }

  // Integer nullable
  public static int binarySearch(Integer[] data, Integer value) {
    return Arrays.binarySearch(data, value, integerComp);
  }

  // long
  // long non-nullable
  public static int binarySearch(long[] data, long value) {
    return Arrays.binarySearch(data, value);
  }

  // Long nullable
  public static int binarySearch(Long[] data, Long value) {
    return Arrays.binarySearch(data, value, longComp);
  }

  // float
  // float non-nullable
  public static int binarySearch(float[] data, float value) {
    return Arrays.binarySearch(data, value);
  }

  // Float nullable
  public static int binarySearch(Float[] data, Float value) {
    return Arrays.binarySearch(data, value, floatComp);
  }

  // double
  // double non-nullable
  public static int binarySearch(double[] data, double value) {
    return Arrays.binarySearch(data, value);
  }

  // Double nullable
  public static int binarySearch(Double[] data, Double value) {
    return Arrays.binarySearch(data, value, doubleComp);
  }

  // Object
  public static int binarySearch(Object[] data, Object value, Comparator<Object> comp) {
    return Arrays.binarySearch(data, value, comp);
  }

  // boolean
  // boolean non-nullable
  public static boolean[] toBooleanArray(ArrayData arrayData) {
    return arrayData.toBooleanArray();
  }

  // Boolean nullable
  public static Boolean[] toBoxedBooleanArray(ArrayData arrayData) {
    return (Boolean[]) arrayData.toArray(BooleanType,
      ClassTag$.MODULE$.apply(java.lang.Boolean.class));
  }

  // byte
  // byte non-nullable
  public static byte[] toByteArray(ArrayData arrayData) {
    return arrayData.toByteArray();
  }

  // Byte nullable
  public static Byte[] toBoxedByteArray(ArrayData arrayData) {
    return (Byte[]) arrayData.toArray(ByteType, ClassTag$.MODULE$.apply(java.lang.Byte.class));
  }

  // short
  // short non-nullable
  public static short[] toShortArray(ArrayData arrayData) {
    return arrayData.toShortArray();
  }

  // Short nullable
  public static Short[] toBoxedShortArray(ArrayData arrayData) {
    return (Short[]) arrayData.toArray(ShortType, ClassTag$.MODULE$.apply(java.lang.Short.class));
  }

  // int
  // int non-nullable
  public static int[] toIntArray(ArrayData arrayData) {
    return arrayData.toIntArray();
  }

  // Integer nullable
  public static Integer[] toBoxedIntArray(ArrayData arrayData) {
    return (Integer[]) arrayData.toArray(IntegerType,
      ClassTag$.MODULE$.apply(java.lang.Integer.class));
  }

  // long
  // long non-nullable
  public static long[] toLongArray(ArrayData arrayData) {
    return arrayData.toLongArray();
  }

  // Long nullable
  public static Long[] toBoxedLongArray(ArrayData arrayData) {
    return (Long[]) arrayData.toArray(LongType, ClassTag$.MODULE$.apply(java.lang.Long.class));
  }

  // float
  // float non-nullable
  public static float[] toFloatArray(ArrayData arrayData) {
    return arrayData.toFloatArray();
  }

  // Float nullable
  public static Float[] toBoxedFloatArray(ArrayData arrayData) {
    return (Float[]) arrayData.toArray(FloatType, ClassTag$.MODULE$.apply(java.lang.Float.class));
  }

  // double
  // double non-nullable
  public static double[] toDoubleArray(ArrayData arrayData) {
    return arrayData.toDoubleArray();
  }

  // Double nullable
  public static Double[] toBoxedDoubleArray(ArrayData arrayData) {
    return (Double[]) arrayData.toArray(DoubleType,
      ClassTag$.MODULE$.apply(java.lang.Double.class));
  }

  // Object
  public static Object[] toObjectArray(ArrayData arrayData, DataType elementType) {
    return arrayData.toObjectArray(elementType);
  }
}
