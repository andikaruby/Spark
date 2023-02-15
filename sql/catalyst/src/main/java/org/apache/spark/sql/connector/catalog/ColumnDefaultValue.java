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

package org.apache.spark.sql.connector.catalog;

import java.util.Objects;
import javax.annotation.Nonnull;

import org.apache.spark.sql.connector.expressions.Literal;

/**
 * A class representing the default value of a column. It contains both the SQL string and literal
 * value of the user-specified default value expression. The SQL string should be re-evaluated for
 * each table writing, which may produce different values if the default value expression is
 * something like {@code CURRENT_DATE()}. The literal value is used to back-fill existing data if
 * new columns with default value are added. Note: the back-fill can be lazy. The data sources can
 * remember the column default value and let the reader fill the column value when reading existing
 * data that do not have these new columns.
 */
public class ColumnDefaultValue {
  private String sql;
  private Literal<?> value;

  public ColumnDefaultValue(String sql, Literal<?> value) {
    this.sql = sql;
    this.value = value;
  }

  /**
   * Returns the SQL string of the default value expression.
   */
  @Nonnull
  public String getSql() {
    return sql;
  }

  /**
   * Returns the default value literal.
   */
  @Nonnull
  public Literal<?> getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ColumnDefaultValue)) return false;
    ColumnDefaultValue that = (ColumnDefaultValue) o;
    return sql.equals(that.sql) && value.equals(that.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sql, value);
  }

  @Override
  public String toString() {
    return "ColumnDefaultValue{sql='" + sql + "\', value=" + value + '}';
  }
}
