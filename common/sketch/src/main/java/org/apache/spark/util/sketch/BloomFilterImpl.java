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

package org.apache.spark.util.sketch;

import java.io.*;

public class BloomFilterImpl extends BloomFilter implements Serializable {

  private int numHashFunctions;
  private BitArray bits;

  BloomFilterImpl(int numHashFunctions, long numBits) {
    this(new BitArray(numBits), numHashFunctions);
  }

  private BloomFilterImpl(BitArray bits, int numHashFunctions) {
    this.bits = bits;
    this.numHashFunctions = numHashFunctions;
  }

  private BloomFilterImpl() {}

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }

    if (other == null || !(other instanceof BloomFilterImpl)) {
      return false;
    }

    BloomFilterImpl that = (BloomFilterImpl) other;

    return this.numHashFunctions == that.numHashFunctions && this.bits.equals(that.bits);
  }

  @Override
  public int hashCode() {
    return bits.hashCode() * 31 + numHashFunctions;
  }

  @Override
  public double expectedFpp() {
    return Math.pow((double) bits.cardinality() / bits.bitSize(), numHashFunctions);
  }

  @Override
  public long bitSize() {
    return bits.bitSize();
  }

  private byte[] getBytesFromUTF8String(Object s) {
    try {
      return ((String) s).getBytes("utf-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Only support utf-8 string", e);
    }
  }

  private long integralToLong(Object i) {
    long longValue;

    if (i instanceof Long) {
      longValue = (Long) i;
    } else if (i instanceof Integer) {
      longValue = ((Integer) i).longValue();
    } else if (i instanceof Short) {
      longValue = ((Short) i).longValue();
    } else if (i instanceof Byte) {
      longValue = ((Byte) i).longValue();
    } else {
      throw new IllegalArgumentException(
        "Support for " + i.getClass().getName() + " not implemented"
      );
    }

    return longValue;
  }

  @Override
  public boolean put(Object item) {
    if (item instanceof String) {
      return putBinary(getBytesFromUTF8String(item));
    } else {
      return putLong(integralToLong(item));
    }
  }

  @Override
  public boolean putBinary(byte[] bytes) {
    int h1 = Murmur3_x86_32.hashUnsafeBytes(bytes, Platform.BYTE_ARRAY_OFFSET, bytes.length, 0);
    int h2 = Murmur3_x86_32.hashUnsafeBytes(bytes, Platform.BYTE_ARRAY_OFFSET, bytes.length, h1);

    long bitSize = bits.bitSize();
    boolean bitsChanged = false;
    for (int i = 1; i <= numHashFunctions; i++) {
      int combinedHash = h1 + (i * h2);
      // Flip all the bits if it's negative (guaranteed positive number)
      if (combinedHash < 0) {
        combinedHash = ~combinedHash;
      }
      bitsChanged |= bits.set(combinedHash % bitSize);
    }
    return bitsChanged;
  }

  private boolean mightContainBinary(byte[] bytes) {
    int h1 = Murmur3_x86_32.hashUnsafeBytes(bytes, Platform.BYTE_ARRAY_OFFSET, bytes.length, 0);
    int h2 = Murmur3_x86_32.hashUnsafeBytes(bytes, Platform.BYTE_ARRAY_OFFSET, bytes.length, h1);

    long bitSize = bits.bitSize();
    for (int i = 1; i <= numHashFunctions; i++) {
      int combinedHash = h1 + (i * h2);
      // Flip all the bits if it's negative (guaranteed positive number)
      if (combinedHash < 0) {
        combinedHash = ~combinedHash;
      }
      if (!bits.get(combinedHash % bitSize)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean putLong(long l) {
    // Here we first hash the input long element into 2 int hash values, h1 and h2, then produce n
    // hash values by `h1 + i * h2` with 1 <= i <= numHashFunctions.
    // Note that `CountMinSketch` use a different strategy, it hash the input long element with
    // every i to produce n hash values.
    // TODO: the strategy of `CountMinSketch` looks more advanced, should we follow it here?
    int h1 = Murmur3_x86_32.hashLong(l, 0);
    int h2 = Murmur3_x86_32.hashLong(l, h1);

    long bitSize = bits.bitSize();
    boolean bitsChanged = false;
    for (int i = 1; i <= numHashFunctions; i++) {
      int combinedHash = h1 + (i * h2);
      // Flip all the bits if it's negative (guaranteed positive number)
      if (combinedHash < 0) {
        combinedHash = ~combinedHash;
      }
      bitsChanged |= bits.set(combinedHash % bitSize);
    }
    return bitsChanged;
  }

  private boolean mightContainLong(long l) {
    int h1 = Murmur3_x86_32.hashLong(l, 0);
    int h2 = Murmur3_x86_32.hashLong(l, h1);

    long bitSize = bits.bitSize();
    for (int i = 1; i <= numHashFunctions; i++) {
      int combinedHash = h1 + (i * h2);
      // Flip all the bits if it's negative (guaranteed positive number)
      if (combinedHash < 0) {
        combinedHash = ~combinedHash;
      }
      if (!bits.get(combinedHash % bitSize)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean mightContain(Object item) {
    if (item instanceof String) {
      return mightContainBinary(getBytesFromUTF8String(item));
    } else {
      return mightContainLong(integralToLong(item));
    }
  }

  @Override
  public boolean isCompatible(BloomFilter other) {
    if (other == null) {
      return false;
    }

    if (!(other instanceof BloomFilterImpl)) {
      return false;
    }

    BloomFilterImpl that = (BloomFilterImpl) other;
    return this.bitSize() == that.bitSize() && this.numHashFunctions == that.numHashFunctions;
  }

  @Override
  public BloomFilter mergeInPlace(BloomFilter other) throws IncompatibleMergeException {
    // Duplicates the logic of `isCompatible` here to provide better error message.
    if (other == null) {
      throw new IncompatibleMergeException("Cannot merge null bloom filter");
    }

    if (!(other instanceof BloomFilter)) {
      throw new IncompatibleMergeException(
        "Cannot merge bloom filter of class " + other.getClass().getName()
      );
    }

    BloomFilterImpl that = (BloomFilterImpl) other;

    if (this.bitSize() != that.bitSize()) {
      throw new IncompatibleMergeException("Cannot merge bloom filters with different bit size");
    }

    if (this.numHashFunctions != that.numHashFunctions) {
      throw new IncompatibleMergeException(
        "Cannot merge bloom filters with different number of hash functions");
    }

    this.bits.putAll(that.bits);
    return this;
  }

  @Override
  public void writeTo(OutputStream out) throws IOException {
    DataOutputStream dos = new DataOutputStream(out);

    dos.writeInt(Version.V1.getVersionNumber());
    dos.writeInt(numHashFunctions);
    bits.writeTo(dos);
  }

  private void readFrom0(InputStream in) throws IOException {
    DataInputStream dis = new DataInputStream(in);

    int version = dis.readInt();
    if (version != Version.V1.getVersionNumber()) {
      throw new IOException("Unexpected Bloom filter version number (" + version + ")");
    }

    this.numHashFunctions = dis.readInt();
    this.bits = BitArray.readFrom(dis);
  }

  public static BloomFilterImpl readFrom(InputStream in) throws IOException {
    BloomFilterImpl filter = new BloomFilterImpl();
    filter.readFrom0(in);
    return filter;
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    writeTo(out);
  }

  private void readObject(ObjectInputStream in) throws IOException {
    readFrom0(in);
  }
}
