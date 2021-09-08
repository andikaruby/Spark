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

package org.apache.spark.sql.connector.expressions.filter;

import java.util.Objects;

import org.apache.spark.annotation.Evolving;
import org.apache.spark.sql.connector.expressions.Literal;
import org.apache.spark.sql.connector.expressions.NamedReference;

/**
 * Performs equality comparison, similar to {@link EqualTo}. However, this differs from
 * {@link EqualTo} in that it returns {@code true} (rather than NULL) if both inputs are NULL,
 * and {@code false} (rather than NULL) if one of the input is NULL and the other is not NULL.
 *
 * @since 3.3.0
 */
@Evolving
public final class EqualNullSafe extends Filter {
  private final NamedReference column;
  private final Literal<?> value;

  public EqualNullSafe(NamedReference column, Literal<?> value) {
    this.column = column;
    this.value = value;
  }

  public NamedReference column() { return column; }
  public Literal<?> value() { return value; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    EqualNullSafe that = (EqualNullSafe) o;
    return Objects.equals(column, that.column) && Objects.equals(value, that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(column, value);
  }

  @Override
  public String toString() { return column.describe() + " <=> " + value.describe(); }

  @Override
  public NamedReference[] references() { return new NamedReference[]{column}; }
}
