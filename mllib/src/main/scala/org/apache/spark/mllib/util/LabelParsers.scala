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

package org.apache.spark.mllib.util

/** Trait for label parsers. */
trait LabelParser extends Serializable {
  /** Parses a string label into a double label. */
  def apply(labelString: String): Double
}

/**
 * Label parser for binary labels, which outputs 1.0 (positive) if the value is greater than 0.5,
 * or 0.0 (negative) otherwise. So it works with +1/-1 labeling and +1/0 labeling.
 */
class BinaryLabelParser extends LabelParser {
  /**
   * Parses the input label into positive (1.0) if the value is greater than 0.5,
   * or negative (0.0) otherwise.
   */
  override def apply(labelString: String): Double = if (labelString.toDouble > 0.5) 1.0 else 0.0
}

object BinaryLabelParser {
  private lazy val instance = new BinaryLabelParser()
  /** Gets the default instance of BinaryLabelParser. */
  def apply(): BinaryLabelParser = instance
}

/**
 * Label parser for multiclass labels, which converts the input label to double.
 */
class MulticlassLabelParser extends LabelParser {
  override def apply(labelString: String): Double =  labelString.toDouble
}

object MulticlassLabelParser {
  private lazy val instance = new MulticlassLabelParser()
  /** Gets the default instance of MulticlassLabelParser. */
  def apply(): MulticlassLabelParser = instance
}
