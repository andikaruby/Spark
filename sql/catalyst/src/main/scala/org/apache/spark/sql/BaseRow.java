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

package org.apache.spark.sql;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;

import scala.collection.Seq;
import scala.collection.mutable.ArraySeq;

import org.apache.spark.sql.catalyst.expressions.GenericRow;
import org.apache.spark.sql.types.StructType;

public abstract class BaseRow implements Row {

  @Override
  public int length() {
    return size();
  }

  @Override
  public boolean anyNull() {
    for (int i=0; i< size(); i++) {
      if (isNullAt(i)) {
        return true;
      }
    }
    float a = 0;
    byte b = 0;
    return false;
  }

  @Override
  public StructType schema() { throw new UnsupportedOperationException(); }

  @Override
  public Object apply(int i) {
    return get(i);
  }

  @Override
  public int getInt(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLong(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public float getFloat(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public double getDouble(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte getByte(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public short getShort(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean getBoolean(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getString(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public BigDecimal getDecimal(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Date getDate(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> Seq<T> getSeq(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> List<T> getList(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <K, V> scala.collection.Map<K, V> getMap(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> scala.collection.immutable.Map<String, T> getValuesMap(Seq<String> fieldNames) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <K, V> java.util.Map<K, V> getJavaMap(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Row getStruct(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getAs(int i) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getAs(String fieldName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int fieldIndex(String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Row copy() {
    Object[] arr = new Object[size()];
    for (int i = 0; i < size(); i++) {
      arr[i] = get(i);
    }
    return new GenericRow(arr);
  }

  @Override
  public Seq<Object> toSeq() {
    final ArraySeq<Object> values = new ArraySeq<Object>(size());
    for (int i = 0; i < size(); i++) {
      values.update(i, get(i));
    }
    return values;
  }

  @Override
  public String toString() {
    return mkString("[", ",", "]");
  }

  @Override
  public String mkString() {
    return toSeq().mkString();
  }

  @Override
  public String mkString(String sep) {
    return toSeq().mkString(sep);
  }

  @Override
  public String mkString(String start, String sep, String end) {
    return toSeq().mkString(start, sep, end);
  }
}
