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

package org.apache.spark.sql.catalyst.expressions

import org.apache.spark.sql.catalyst.ScalaReflection
import org.apache.spark.sql.types.DataType

/**
 * User-defined function.
 * @param dataType  Return type of function.
 */
case class ScalaUdf(function: AnyRef, dataType: DataType, children: Seq[Expression])
  extends Expression {

  type EvaluatedType = Any

  def nullable = true

  override def toString = s"scalaUDF(${children.mkString(",")})"

  // scalastyle:off

  /** This method has been generated by this script

    (1 to 22).map { x =>
      val anys = (1 to x).map(x => "Any").reduce(_ + ", " + _)
      val evals = (0 to x - 1).map(x => s"    ScalaReflection.convertToScala(children($x).eval(input), children($x).dataType)").reduce(_ + ",\n    " + _)

    s"""
    case $x =>
      function.asInstanceOf[($anys) => Any](
    $evals)
    """
    }.foreach(println)

  */

  override def eval(input: Row): Any = {
    val result = children.size match {
      case 0 => function.asInstanceOf[() => Any]()
      case 1 =>
        function.asInstanceOf[(Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType))


      case 2 =>
        function.asInstanceOf[(Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType))


      case 3 =>
        function.asInstanceOf[(Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType))


      case 4 =>
        function.asInstanceOf[(Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType))


      case 5 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType))


      case 6 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType))


      case 7 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType))


      case 8 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType))


      case 9 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType))


      case 10 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType))


      case 11 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType))


      case 12 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType))


      case 13 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType),
          ScalaReflection.convertToScala(children(12).eval(input), children(12).dataType))


      case 14 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType),
          ScalaReflection.convertToScala(children(12).eval(input), children(12).dataType),
          ScalaReflection.convertToScala(children(13).eval(input), children(13).dataType))


      case 15 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType),
          ScalaReflection.convertToScala(children(12).eval(input), children(12).dataType),
          ScalaReflection.convertToScala(children(13).eval(input), children(13).dataType),
          ScalaReflection.convertToScala(children(14).eval(input), children(14).dataType))


      case 16 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType),
          ScalaReflection.convertToScala(children(12).eval(input), children(12).dataType),
          ScalaReflection.convertToScala(children(13).eval(input), children(13).dataType),
          ScalaReflection.convertToScala(children(14).eval(input), children(14).dataType),
          ScalaReflection.convertToScala(children(15).eval(input), children(15).dataType))


      case 17 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType),
          ScalaReflection.convertToScala(children(12).eval(input), children(12).dataType),
          ScalaReflection.convertToScala(children(13).eval(input), children(13).dataType),
          ScalaReflection.convertToScala(children(14).eval(input), children(14).dataType),
          ScalaReflection.convertToScala(children(15).eval(input), children(15).dataType),
          ScalaReflection.convertToScala(children(16).eval(input), children(16).dataType))


      case 18 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType),
          ScalaReflection.convertToScala(children(12).eval(input), children(12).dataType),
          ScalaReflection.convertToScala(children(13).eval(input), children(13).dataType),
          ScalaReflection.convertToScala(children(14).eval(input), children(14).dataType),
          ScalaReflection.convertToScala(children(15).eval(input), children(15).dataType),
          ScalaReflection.convertToScala(children(16).eval(input), children(16).dataType),
          ScalaReflection.convertToScala(children(17).eval(input), children(17).dataType))


      case 19 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType),
          ScalaReflection.convertToScala(children(12).eval(input), children(12).dataType),
          ScalaReflection.convertToScala(children(13).eval(input), children(13).dataType),
          ScalaReflection.convertToScala(children(14).eval(input), children(14).dataType),
          ScalaReflection.convertToScala(children(15).eval(input), children(15).dataType),
          ScalaReflection.convertToScala(children(16).eval(input), children(16).dataType),
          ScalaReflection.convertToScala(children(17).eval(input), children(17).dataType),
          ScalaReflection.convertToScala(children(18).eval(input), children(18).dataType))


      case 20 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType),
          ScalaReflection.convertToScala(children(12).eval(input), children(12).dataType),
          ScalaReflection.convertToScala(children(13).eval(input), children(13).dataType),
          ScalaReflection.convertToScala(children(14).eval(input), children(14).dataType),
          ScalaReflection.convertToScala(children(15).eval(input), children(15).dataType),
          ScalaReflection.convertToScala(children(16).eval(input), children(16).dataType),
          ScalaReflection.convertToScala(children(17).eval(input), children(17).dataType),
          ScalaReflection.convertToScala(children(18).eval(input), children(18).dataType),
          ScalaReflection.convertToScala(children(19).eval(input), children(19).dataType))


      case 21 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType),
          ScalaReflection.convertToScala(children(12).eval(input), children(12).dataType),
          ScalaReflection.convertToScala(children(13).eval(input), children(13).dataType),
          ScalaReflection.convertToScala(children(14).eval(input), children(14).dataType),
          ScalaReflection.convertToScala(children(15).eval(input), children(15).dataType),
          ScalaReflection.convertToScala(children(16).eval(input), children(16).dataType),
          ScalaReflection.convertToScala(children(17).eval(input), children(17).dataType),
          ScalaReflection.convertToScala(children(18).eval(input), children(18).dataType),
          ScalaReflection.convertToScala(children(19).eval(input), children(19).dataType),
          ScalaReflection.convertToScala(children(20).eval(input), children(20).dataType))


      case 22 =>
        function.asInstanceOf[(Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) => Any](
          ScalaReflection.convertToScala(children(0).eval(input), children(0).dataType),
          ScalaReflection.convertToScala(children(1).eval(input), children(1).dataType),
          ScalaReflection.convertToScala(children(2).eval(input), children(2).dataType),
          ScalaReflection.convertToScala(children(3).eval(input), children(3).dataType),
          ScalaReflection.convertToScala(children(4).eval(input), children(4).dataType),
          ScalaReflection.convertToScala(children(5).eval(input), children(5).dataType),
          ScalaReflection.convertToScala(children(6).eval(input), children(6).dataType),
          ScalaReflection.convertToScala(children(7).eval(input), children(7).dataType),
          ScalaReflection.convertToScala(children(8).eval(input), children(8).dataType),
          ScalaReflection.convertToScala(children(9).eval(input), children(9).dataType),
          ScalaReflection.convertToScala(children(10).eval(input), children(10).dataType),
          ScalaReflection.convertToScala(children(11).eval(input), children(11).dataType),
          ScalaReflection.convertToScala(children(12).eval(input), children(12).dataType),
          ScalaReflection.convertToScala(children(13).eval(input), children(13).dataType),
          ScalaReflection.convertToScala(children(14).eval(input), children(14).dataType),
          ScalaReflection.convertToScala(children(15).eval(input), children(15).dataType),
          ScalaReflection.convertToScala(children(16).eval(input), children(16).dataType),
          ScalaReflection.convertToScala(children(17).eval(input), children(17).dataType),
          ScalaReflection.convertToScala(children(18).eval(input), children(18).dataType),
          ScalaReflection.convertToScala(children(19).eval(input), children(19).dataType),
          ScalaReflection.convertToScala(children(20).eval(input), children(20).dataType),
          ScalaReflection.convertToScala(children(21).eval(input), children(21).dataType))

    }
    // scalastyle:on

    ScalaReflection.convertToCatalyst(result, dataType)
  }
}
