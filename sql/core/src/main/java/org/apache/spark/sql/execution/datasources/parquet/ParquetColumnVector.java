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

package org.apache.spark.sql.execution.datasources.parquet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;

import org.apache.spark.memory.MemoryMode;
import org.apache.spark.sql.execution.vectorized.OffHeapColumnVector;
import org.apache.spark.sql.execution.vectorized.OnHeapColumnVector;
import org.apache.spark.sql.execution.vectorized.WritableColumnVector;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.MapType;
import org.apache.spark.sql.types.StructType;

/**
 * Contains necessary information representing a Parquet column, either of primitive or nested type.
 */
final class ParquetColumnVector {
  private final ParquetColumn column;
  private final List<ParquetColumnVector> children;
  private final WritableColumnVector vector;

  /**
   * Repetition & Definition levels
   * These are allocated only for leaf columns; for non-leaf columns, they simply maintain
   * references to that of the former.
   */
  private WritableColumnVector repetitionLevels;
  private WritableColumnVector definitionLevels;

  /** Whether this column is primitive (i.e., leaf column) */
  private final boolean isPrimitive;

  /** Reader for this column - only set if 'isPrimitive' is true */
  private VectorizedColumnReader columnReader;

  ParquetColumnVector(
      ParquetColumn column,
      WritableColumnVector vector,
      int capacity,
      MemoryMode memoryMode,
      Set<ParquetColumn> missingColumns,
      boolean isTopLevel,
      Object defaultValue) {
    DataType sparkType = column.sparkType();
    if (!sparkType.sameType(vector.dataType())) {
      throw new IllegalArgumentException("Spark type: " + sparkType +
        " doesn't match the type: " + vector.dataType() + " in column vector");
    }

    this.column = column;
    this.vector = vector;
    this.children = new ArrayList<>();
    this.isPrimitive = column.isPrimitive();

    if (missingColumns.contains(column)) {
      if (defaultValue == null) {
        vector.setAllNull();
        return;
      }
      // For Parquet tables whose columns have associated DEFAULT values, this reader must return
      // those values instead of NULL when the corresponding columns are not present in storage.
      // Here we write the 'defaultValue' to each element in the new WritableColumnVector using
      // the appendObjects method. This delegates to some specific append* method depending on the
      // type of 'defaultValue'; for example, if 'defaultValue' is a Float, then we call the
      // appendFloats method.
      if (!vector.appendObjects(capacity, defaultValue).isPresent()) {
        throw new IllegalArgumentException("Cannot assign default column value to result " +
          "column batch in vectorized Parquet reader because the data type is not supported: " +
          defaultValue);
      }
    }

    if (isPrimitive) {
      if (column.repetitionLevel() > 0) {
        repetitionLevels = allocateLevelsVector(capacity, memoryMode);
      }
      // We don't need to create and store definition levels if the column is top-level.
      if (!isTopLevel) {
        definitionLevels = allocateLevelsVector(capacity, memoryMode);
      }
    } else {
      Preconditions.checkArgument(column.children().size() == vector.getNumChildren());
      boolean allChildrenAreMissing = true;

      for (int i = 0; i < column.children().size(); i++) {
        ParquetColumnVector childCv = new ParquetColumnVector(column.children().apply(i),
          vector.getChild(i), capacity, memoryMode, missingColumns, false, null);
        children.add(childCv);


        // Only use levels from non-missing child, this can happen if only some but not all
        // fields of a struct are missing.
        if (!childCv.vector.isAllNull()) {
          allChildrenAreMissing = false;
          this.repetitionLevels = childCv.repetitionLevels;
          this.definitionLevels = childCv.definitionLevels;
        }
      }

      // This can happen if all the fields of a struct are missing, in which case we should mark
      // the struct itself as a missing column
      if (allChildrenAreMissing) {
        vector.setAllNull();
      }
    }
  }

  /**
   * Returns all the children of this column.
   */
  List<ParquetColumnVector> getChildren() {
    return children;
  }

  /**
   * Returns all the leaf columns in depth-first order.
   */
  List<ParquetColumnVector> getLeaves() {
    List<ParquetColumnVector> result = new ArrayList<>();
    getLeavesHelper(this, result);
    return result;
  }

  private static void getLeavesHelper(ParquetColumnVector vector, List<ParquetColumnVector> coll) {
    if (vector.isPrimitive) {
      coll.add(vector);
    } else {
      for (ParquetColumnVector child : vector.children) {
        getLeavesHelper(child, coll);
      }
    }
  }

  /**
   * Assembles this column and calculate collection offsets recursively.
   * This is a no-op for primitive columns.
   */
  void assemble() {
    // nothing to do if the column itself is missing
    if (vector.isAllNull()) return;

    DataType type = column.sparkType();
    if (type instanceof ArrayType || type instanceof MapType) {
      for (ParquetColumnVector child : children) {
        child.assemble();
      }
      assembleCollection();
    } else if (type instanceof StructType) {
      for (ParquetColumnVector child : children) {
        child.assemble();
      }
      assembleStruct();
    }
  }

  /**
   * Resets this Parquet column vector, which includes resetting all the writable column vectors
   * (used to store values, definition levels, and repetition levels) for this and all its children.
   */
  void reset() {
    // nothing to do if the column itself is missing
    if (vector.isAllNull()) return;

    vector.reset();
    if (repetitionLevels != null) {
      repetitionLevels.reset();
    }
    if (definitionLevels != null) {
      definitionLevels.reset();
    }
    for (ParquetColumnVector child : children) {
      child.reset();
    }
  }

  /**
   * Returns the {@link ParquetColumn} of this column vector.
   */
  ParquetColumn getColumn() {
    return this.column;
  }

  /**
   * Returns the writable column vector used to store values.
   */
  WritableColumnVector getValueVector() {
    return this.vector;
  }

  /**
   * Returns the writable column vector used to store repetition levels.
   */
  WritableColumnVector getRepetitionLevelVector() {
    return this.repetitionLevels;
  }

  /**
   * Returns the writable column vector used to store definition levels.
   */
  WritableColumnVector getDefinitionLevelVector() {
    return this.definitionLevels;
  }

  /**
   * Returns the column reader for reading a Parquet column.
   */
  VectorizedColumnReader getColumnReader() {
    return this.columnReader;
  }

  /**
   * Sets the column vector to 'reader'. Note this can only be called on a primitive Parquet
   * column.
   */
  void setColumnReader(VectorizedColumnReader reader) {
    if (!isPrimitive) {
      throw new IllegalStateException("Can't set reader for non-primitive column");
    }
    this.columnReader = reader;
  }

  /**
   * Assemble collections, e.g., array, map.
   */
  private void assembleCollection() {
    int maxDefinitionLevel = column.definitionLevel();
    int maxElementRepetitionLevel = column.repetitionLevel();

    // There are 4 cases when calculating definition levels:
    //   1. definitionLevel == maxDefinitionLevel
    //     ==> value is defined and not null
    //   2. definitionLevel == maxDefinitionLevel - 1
    //     ==> value is null
    //   3. definitionLevel < maxDefinitionLevel - 1
    //     ==> value doesn't exist since one of its optional parents is null
    //   4. definitionLevel > maxDefinitionLevel
    //     ==> value is a nested element within an array or map
    //
    // `i` is the index over all leaf elements of this array, while `offset` is the index over
    // all top-level elements of this array.
    int rowId = 0;
    for (int i = 0, offset = 0; i < definitionLevels.getElementsAppended();
         i = getNextCollectionStart(maxElementRepetitionLevel, i)) {
      vector.reserve(rowId + 1);
      int definitionLevel = definitionLevels.getInt(i);
      if (definitionLevel <= maxDefinitionLevel) {
        // This means the value is not an array element, but a collection that is either null or
        // empty. In this case, we should increase offset to skip it when returning an array
        // starting from the offset.
        //
        // For instance, considering an array of strings with 3 elements like the following:
        //  null, [], [a, b, c]
        // the child array (which is of String type) in this case will be:
        //  null:   1 1 0 0 0
        //  length: 0 0 1 1 1
        //  offset: 0 0 0 1 2
        // and the array itself will be:
        //  null:   1 0 0
        //  length: 0 0 3
        //  offset: 0 1 2
        //
        // It's important that for the third element `[a, b, c]`, the offset in the array
        // (not the elements) starts from 2 since otherwise we'd include the first & second null
        // element from child array in the result.
        offset += 1;
      }
      if (definitionLevel <= maxDefinitionLevel - 1) {
        // Collection is null or one of its optional parents is null
        vector.putNull(rowId++);
      } else if (definitionLevel == maxDefinitionLevel) {
        // Collection is defined but empty
        vector.putNotNull(rowId);
        vector.putArray(rowId, offset, 0);
        rowId++;
      } else if (definitionLevel > maxDefinitionLevel) {
        // Collection is defined and non-empty: find out how many top elements are there until the
        // start of the next array.
        vector.putNotNull(rowId);
        int length = getCollectionSize(maxElementRepetitionLevel, i);
        vector.putArray(rowId, offset, length);
        offset += length;
        rowId++;
      }
    }
    vector.addElementsAppended(rowId);
  }

  private void assembleStruct() {
    int maxRepetitionLevel = column.repetitionLevel();
    int maxDefinitionLevel = column.definitionLevel();

    vector.reserve(definitionLevels.getElementsAppended());

    int rowId = 0;
    boolean hasRepetitionLevels =
      repetitionLevels != null && repetitionLevels.getElementsAppended() > 0;
    for (int i = 0; i < definitionLevels.getElementsAppended(); i++) {
      // If repetition level > maxRepetitionLevel, the value is a nested element (e.g., an array
      // element in struct<array<int>>), and we should skip the definition level since it doesn't
      // represent with the struct.
      if (!hasRepetitionLevels || repetitionLevels.getInt(i) <= maxRepetitionLevel) {
        if (definitionLevels.getInt(i) <= maxDefinitionLevel - 1) {
          // Struct is null
          vector.putNull(rowId);
          rowId++;
        } else if (definitionLevels.getInt(i) >= maxDefinitionLevel) {
          vector.putNotNull(rowId);
          rowId++;
        }
      }
    }
    vector.addElementsAppended(rowId);
  }

  private static WritableColumnVector allocateLevelsVector(int capacity, MemoryMode memoryMode) {
    switch (memoryMode) {
      case ON_HEAP:
        return new OnHeapColumnVector(capacity, DataTypes.IntegerType);
      case OFF_HEAP:
        return new OffHeapColumnVector(capacity, DataTypes.IntegerType);
      default:
        throw new IllegalArgumentException("Unknown memory mode: " + memoryMode);
    }
  }

  /**
   * For a collection (i.e., array or map) element at index 'idx', returns the starting index of
   * the next collection after it.
   *
   * @param maxRepetitionLevel the maximum repetition level for the elements in this collection
   * @param idx the index of this collection in the Parquet column
   * @return the starting index of the next collection
   */
  private int getNextCollectionStart(int maxRepetitionLevel, int idx) {
    idx += 1;
    for (; idx < repetitionLevels.getElementsAppended(); idx++) {
      if (repetitionLevels.getInt(idx) <= maxRepetitionLevel) {
        break;
      }
    }
    return idx;
  }

  /**
   * Gets the size of a collection (i.e., array or map) element, starting at 'idx'.
   *
   * @param maxRepetitionLevel the maximum repetition level for the elements in this collection
   * @param idx the index of this collection in the Parquet column
   * @return the size of this collection
   */
  private int getCollectionSize(int maxRepetitionLevel, int idx) {
    int size = 1;
    for (idx += 1; idx < repetitionLevels.getElementsAppended(); idx++) {
      if (repetitionLevels.getInt(idx) <= maxRepetitionLevel) {
        break;
      } else if (repetitionLevels.getInt(idx) <= maxRepetitionLevel + 1) {
        // Only count elements which belong to the current collection
        // For instance, suppose we have the following Parquet schema:
        //
        // message schema {                        max rl   max dl
        //   optional group col (LIST) {              0        1
        //     repeated group list {                  1        2
        //       optional group element (LIST) {      1        3
        //         repeated group list {              2        4
        //           required int32 element;          2        4
        //         }
        //       }
        //     }
        //   }
        // }
        //
        // For a list such as: [[[0, 1], [2, 3]], [[4, 5], [6, 7]]], the repetition & definition
        // levels would be:
        //
        // repetition levels: [0, 2, 1, 2, 0, 2, 1, 2]
        // definition levels: [2, 2, 2, 2, 2, 2, 2, 2]
        //
        // When calculating collection size for the outer array, we should only count repetition
        // levels whose value is <= 1 (which is the max repetition level for the inner array)
        size++;
      }
    }
    return size;
  }
}
