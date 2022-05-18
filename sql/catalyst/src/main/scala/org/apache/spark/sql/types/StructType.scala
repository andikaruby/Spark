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

package org.apache.spark.sql.types

import scala.collection.{mutable, Map}
import scala.util.Try
import scala.util.control.NonFatal

import org.json4s.JsonDSL._

import org.apache.spark.annotation.Stable
import org.apache.spark.sql.catalyst.analysis.Resolver
import org.apache.spark.sql.catalyst.expressions.{AnsiCast, Attribute, AttributeReference, Cast, InterpretedOrdering, Literal => ExprLiteral}
import org.apache.spark.sql.catalyst.parser.{CatalystSqlParser, LegacyTypeStringParser, ParseException}
import org.apache.spark.sql.catalyst.trees.Origin
import org.apache.spark.sql.catalyst.util.{truncatedString, StringUtils}
import org.apache.spark.sql.catalyst.util.StringUtils.StringConcat
import org.apache.spark.sql.errors.{QueryCompilationErrors, QueryExecutionErrors}
import org.apache.spark.sql.internal.SQLConf

/**
 * A [[StructType]] object can be constructed by
 * {{{
 * StructType(fields: Seq[StructField])
 * }}}
 * For a [[StructType]] object, one or multiple [[StructField]]s can be extracted by names.
 * If multiple [[StructField]]s are extracted, a [[StructType]] object will be returned.
 * If a provided name does not have a matching field, it will be ignored. For the case
 * of extracting a single [[StructField]], a `null` will be returned.
 *
 * Scala Example:
 * {{{
 * import org.apache.spark.sql._
 * import org.apache.spark.sql.types._
 *
 * val struct =
 *   StructType(
 *     StructField("a", IntegerType, true) ::
 *     StructField("b", LongType, false) ::
 *     StructField("c", BooleanType, false) :: Nil)
 *
 * // Extract a single StructField.
 * val singleField = struct("b")
 * // singleField: StructField = StructField(b,LongType,false)
 *
 * // If this struct does not have a field called "d", it throws an exception.
 * struct("d")
 * // java.lang.IllegalArgumentException: d does not exist.
 * //   ...
 *
 * // Extract multiple StructFields. Field names are provided in a set.
 * // A StructType object will be returned.
 * val twoFields = struct(Set("b", "c"))
 * // twoFields: StructType =
 * //   StructType(StructField(b,LongType,false), StructField(c,BooleanType,false))
 *
 * // Any names without matching fields will throw an exception.
 * // For the case shown below, an exception is thrown due to "d".
 * struct(Set("b", "c", "d"))
 * // java.lang.IllegalArgumentException: d does not exist.
 * //    ...
 * }}}
 *
 * A [[org.apache.spark.sql.Row]] object is used as a value of the [[StructType]].
 *
 * Scala Example:
 * {{{
 * import org.apache.spark.sql._
 * import org.apache.spark.sql.types._
 *
 * val innerStruct =
 *   StructType(
 *     StructField("f1", IntegerType, true) ::
 *     StructField("f2", LongType, false) ::
 *     StructField("f3", BooleanType, false) :: Nil)
 *
 * val struct = StructType(
 *   StructField("a", innerStruct, true) :: Nil)
 *
 * // Create a Row with the schema defined by struct
 * val row = Row(Row(1, 2, true))
 * }}}
 *
 * @since 1.3.0
 */
@Stable
case class StructType(fields: Array[StructField]) extends DataType with Seq[StructField] {

  /** No-arg constructor for kryo. */
  def this() = this(Array.empty[StructField])

  /** Returns all field names in an array. */
  def fieldNames: Array[String] = fields.map(_.name)

  /**
   * Returns all field names in an array. This is an alias of `fieldNames`.
   *
   * @since 2.4.0
   */
  def names: Array[String] = fieldNames

  private lazy val fieldNamesSet: Set[String] = fieldNames.toSet
  private lazy val nameToField: Map[String, StructField] = fields.map(f => f.name -> f).toMap
  private lazy val nameToIndex: Map[String, Int] = fieldNames.zipWithIndex.toMap

  override def equals(that: Any): Boolean = {
    that match {
      case StructType(otherFields) =>
        java.util.Arrays.equals(
          fields.asInstanceOf[Array[AnyRef]], otherFields.asInstanceOf[Array[AnyRef]])
      case _ => false
    }
  }

  override def toString(): String = {
    s"${getClass.getSimpleName}${fields.map(_.toString).mkString("(", ",", ")")}"
  }

  private lazy val _hashCode: Int = java.util.Arrays.hashCode(fields.asInstanceOf[Array[AnyRef]])
  override def hashCode(): Int = _hashCode

  /**
   * Creates a new [[StructType]] by adding a new field.
   * {{{
   * val struct = (new StructType)
   *   .add(StructField("a", IntegerType, true))
   *   .add(StructField("b", LongType, false))
   *   .add(StructField("c", StringType, true))
   *}}}
   */
  def add(field: StructField): StructType = {
    StructType(fields :+ field)
  }

  /**
   * Creates a new [[StructType]] by adding a new nullable field with no metadata.
   *
   * val struct = (new StructType)
   *   .add("a", IntegerType)
   *   .add("b", LongType)
   *   .add("c", StringType)
   */
  def add(name: String, dataType: DataType): StructType = {
    StructType(fields :+ StructField(name, dataType, nullable = true, Metadata.empty))
  }

  /**
   * Creates a new [[StructType]] by adding a new field with no metadata.
   *
   * val struct = (new StructType)
   *   .add("a", IntegerType, true)
   *   .add("b", LongType, false)
   *   .add("c", StringType, true)
   */
  def add(name: String, dataType: DataType, nullable: Boolean): StructType = {
    StructType(fields :+ StructField(name, dataType, nullable, Metadata.empty))
  }

  /**
   * Creates a new [[StructType]] by adding a new field and specifying metadata.
   * {{{
   * val struct = (new StructType)
   *   .add("a", IntegerType, true, Metadata.empty)
   *   .add("b", LongType, false, Metadata.empty)
   *   .add("c", StringType, true, Metadata.empty)
   * }}}
   */
  def add(
      name: String,
      dataType: DataType,
      nullable: Boolean,
      metadata: Metadata): StructType = {
    StructType(fields :+ StructField(name, dataType, nullable, metadata))
  }

  /**
   * Creates a new [[StructType]] by adding a new field and specifying metadata.
   * {{{
   * val struct = (new StructType)
   *   .add("a", IntegerType, true, "comment1")
   *   .add("b", LongType, false, "comment2")
   *   .add("c", StringType, true, "comment3")
   * }}}
   */
  def add(
      name: String,
      dataType: DataType,
      nullable: Boolean,
      comment: String): StructType = {
    StructType(fields :+ StructField(name, dataType, nullable).withComment(comment))
  }

  /**
   * Creates a new [[StructType]] by adding a new nullable field with no metadata where the
   * dataType is specified as a String.
   *
   * {{{
   * val struct = (new StructType)
   *   .add("a", "int")
   *   .add("b", "long")
   *   .add("c", "string")
   * }}}
   */
  def add(name: String, dataType: String): StructType = {
    add(name, CatalystSqlParser.parseDataType(dataType), nullable = true, Metadata.empty)
  }

  /**
   * Creates a new [[StructType]] by adding a new field with no metadata where the
   * dataType is specified as a String.
   *
   * {{{
   * val struct = (new StructType)
   *   .add("a", "int", true)
   *   .add("b", "long", false)
   *   .add("c", "string", true)
   * }}}
   */
  def add(name: String, dataType: String, nullable: Boolean): StructType = {
    add(name, CatalystSqlParser.parseDataType(dataType), nullable, Metadata.empty)
  }

  /**
   * Creates a new [[StructType]] by adding a new field and specifying metadata where the
   * dataType is specified as a String.
   * {{{
   * val struct = (new StructType)
   *   .add("a", "int", true, Metadata.empty)
   *   .add("b", "long", false, Metadata.empty)
   *   .add("c", "string", true, Metadata.empty)
   * }}}
   */
  def add(
      name: String,
      dataType: String,
      nullable: Boolean,
      metadata: Metadata): StructType = {
    add(name, CatalystSqlParser.parseDataType(dataType), nullable, metadata)
  }

  /**
   * Creates a new [[StructType]] by adding a new field and specifying metadata where the
   * dataType is specified as a String.
   * {{{
   * val struct = (new StructType)
   *   .add("a", "int", true, "comment1")
   *   .add("b", "long", false, "comment2")
   *   .add("c", "string", true, "comment3")
   * }}}
   */
  def add(
      name: String,
      dataType: String,
      nullable: Boolean,
      comment: String): StructType = {
    add(name, CatalystSqlParser.parseDataType(dataType), nullable, comment)
  }

  /**
   * Extracts the [[StructField]] with the given name.
   *
   * @throws IllegalArgumentException if a field with the given name does not exist
   */
  def apply(name: String): StructField = {
    nameToField.getOrElse(name,
      throw new IllegalArgumentException(
        s"$name does not exist. Available: ${fieldNames.mkString(", ")}"))
  }

  /**
   * Returns a [[StructType]] containing [[StructField]]s of the given names, preserving the
   * original order of fields.
   *
   * @throws IllegalArgumentException if at least one given field name does not exist
   */
  def apply(names: Set[String]): StructType = {
    val nonExistFields = names -- fieldNamesSet
    if (nonExistFields.nonEmpty) {
      throw new IllegalArgumentException(
        s"${nonExistFields.mkString(", ")} do(es) not exist. " +
          s"Available: ${fieldNames.mkString(", ")}")
    }
    // Preserve the original order of fields.
    StructType(fields.filter(f => names.contains(f.name)))
  }

  /**
   * Returns the index of a given field.
   *
   * @throws IllegalArgumentException if a field with the given name does not exist
   */
  def fieldIndex(name: String): Int = {
    nameToIndex.getOrElse(name,
      throw new IllegalArgumentException(
        s"$name does not exist. Available: ${fieldNames.mkString(", ")}"))
  }

  private[sql] def getFieldIndex(name: String): Option[Int] = {
    nameToIndex.get(name)
  }

  /**
   * Returns the normalized path to a field and the field in this struct and its child structs.
   *
   * If includeCollections is true, this will return fields that are nested in maps and arrays.
   */
  private[sql] def findNestedField(
      fieldNames: Seq[String],
      includeCollections: Boolean = false,
      resolver: Resolver = _ == _,
      context: Origin = Origin()): Option[(Seq[String], StructField)] = {

    @scala.annotation.tailrec
    def findField(
        struct: StructType,
        searchPath: Seq[String],
        normalizedPath: Seq[String]): Option[(Seq[String], StructField)] = {
      assert(searchPath.nonEmpty)
      val searchName = searchPath.head
      val found = struct.fields.filter(f => resolver(searchName, f.name))
      if (found.length > 1) {
        throw QueryCompilationErrors.ambiguousFieldNameError(fieldNames, found.length, context)
      } else if (found.isEmpty) {
        None
      } else {
        val field = found.head
        val currentPath = normalizedPath :+ field.name
        val newSearchPath = searchPath.tail
        if (newSearchPath.isEmpty) {
          Some(normalizedPath -> field)
        } else {
          (newSearchPath, field.dataType) match {
            case (_, s: StructType) =>
              findField(s, newSearchPath, currentPath)

            case _ if !includeCollections =>
              throw QueryCompilationErrors.invalidFieldName(fieldNames, currentPath, context)

            case (Seq("key", rest @ _*), MapType(keyType, _, _)) =>
              findFieldInCollection(keyType, false, rest, currentPath, "key")

            case (Seq("value", rest @ _*), MapType(_, valueType, isNullable)) =>
              findFieldInCollection(valueType, isNullable, rest, currentPath, "value")

            case (Seq("element", rest @ _*), ArrayType(elementType, isNullable)) =>
              findFieldInCollection(elementType, isNullable, rest, currentPath, "element")

            case _ =>
              throw QueryCompilationErrors.invalidFieldName(fieldNames, currentPath, context)
          }
        }
      }
    }

    def findFieldInCollection(
        dt: DataType,
        nullable: Boolean,
        searchPath: Seq[String],
        normalizedPath: Seq[String],
        collectionFieldName: String): Option[(Seq[String], StructField)] = {
      if (searchPath.isEmpty) {
        Some(normalizedPath -> StructField(collectionFieldName, dt, nullable))
      } else {
        val newPath = normalizedPath :+ collectionFieldName
        dt match {
          case s: StructType =>
            findField(s, searchPath, newPath)
          case _ =>
            throw QueryCompilationErrors.invalidFieldName(fieldNames, newPath, context)
        }
      }
    }

    findField(this, fieldNames, Nil)
  }

  protected[sql] def toAttributes: Seq[AttributeReference] =
    map(f => AttributeReference(f.name, f.dataType, f.nullable, f.metadata)())

  def treeString: String = treeString(Int.MaxValue)

  def treeString(maxDepth: Int): String = {
    val stringConcat = new StringUtils.StringConcat()
    stringConcat.append("root\n")
    val prefix = " |"
    val depth = if (maxDepth > 0) maxDepth else Int.MaxValue
    fields.foreach(field => field.buildFormattedString(prefix, stringConcat, depth))
    stringConcat.toString()
  }

  // scalastyle:off println
  def printTreeString(): Unit = println(treeString)
  // scalastyle:on println

  private[sql] def buildFormattedString(
      prefix: String,
      stringConcat: StringConcat,
      maxDepth: Int): Unit = {
    fields.foreach(field => field.buildFormattedString(prefix, stringConcat, maxDepth))
  }

  override private[sql] def jsonValue =
    ("type" -> typeName) ~
      ("fields" -> map(_.jsonValue))

  override def apply(fieldIndex: Int): StructField = fields(fieldIndex)

  override def length: Int = fields.length

  override def iterator: Iterator[StructField] = fields.iterator

  /**
   * The default size of a value of the StructType is the total default sizes of all field types.
   */
  override def defaultSize: Int = fields.map(_.dataType.defaultSize).sum

  override def simpleString: String = {
    val fieldTypes = fields.view.map(field => s"${field.name}:${field.dataType.simpleString}").toSeq
    truncatedString(
      fieldTypes,
      "struct<", ",", ">",
      SQLConf.get.maxToStringFields)
  }

  override def catalogString: String = {
    // in catalogString, we should not truncate
    val stringConcat = new StringUtils.StringConcat()
    val len = fields.length
    stringConcat.append("struct<")
    var i = 0
    while (i < len) {
      stringConcat.append(s"${fields(i).name}:${fields(i).dataType.catalogString}")
      i += 1
      if (i < len) stringConcat.append(",")
    }
    stringConcat.append(">")
    stringConcat.toString
  }

  override def sql: String = s"STRUCT<${fields.map(_.sql).mkString(", ")}>"

  /**
   * Returns a string containing a schema in DDL format. For example, the following value:
   * `StructType(Seq(StructField("eventId", IntegerType), StructField("s", StringType)))`
   * will be converted to `eventId` INT, `s` STRING.
   * The returned DDL schema can be used in a table creation.
   *
   * @since 2.4.0
   */
  def toDDL: String = fields.map(_.toDDL).mkString(",")

  private[sql] override def simpleString(maxNumberFields: Int): String = {
    val builder = new StringBuilder
    val fieldTypes = fields.take(maxNumberFields).map {
      f => s"${f.name}: ${f.dataType.simpleString(maxNumberFields)}"
    }
    builder.append("struct<")
    builder.append(fieldTypes.mkString(", "))
    if (fields.length > 2) {
      if (fields.length - fieldTypes.length == 1) {
        builder.append(" ... 1 more field")
      } else {
        builder.append(" ... " + (fields.length - 2) + " more fields")
      }
    }
    builder.append(">").toString()
  }

  /**
   * Merges with another schema (`StructType`).  For a struct field A from `this` and a struct field
   * B from `that`,
   *
   * 1. If A and B have the same name and data type, they are merged to a field C with the same name
   *    and data type.  C is nullable if and only if either A or B is nullable.
   * 2. If A doesn't exist in `that`, it's included in the result schema.
   * 3. If B doesn't exist in `this`, it's also included in the result schema.
   * 4. Otherwise, `this` and `that` are considered as conflicting schemas and an exception would be
   *    thrown.
   */
  private[sql] def merge(that: StructType): StructType =
    StructType.merge(this, that).asInstanceOf[StructType]

  override private[spark] def asNullable: StructType = {
    val newFields = fields.map {
      case StructField(name, dataType, nullable, metadata) =>
        StructField(name, dataType.asNullable, nullable = true, metadata)
    }

    StructType(newFields)
  }

  override private[spark] def existsRecursively(f: (DataType) => Boolean): Boolean = {
    f(this) || fields.exists(field => field.dataType.existsRecursively(f))
  }

  @transient
  private[sql] lazy val interpretedOrdering =
    InterpretedOrdering.forSchema(this.fields.map(_.dataType))

  /**
   * Parses the text representing constant-folded default column literal values.
   * @return a sequence of either (1) NULL, if the column had no default value, or (2) an object of
   *         Any type suitable for assigning into a row using the InternalRow.update method.
   */
  private [sql] lazy val existenceDefaultValues: Array[Any] =
    fields.map { field: StructField =>
      val defaultValue: Option[String] = field.getExistenceDefaultValue()
      defaultValue.map { text: String =>
        val expr = try {
          val expr = CatalystSqlParser.parseExpression(text)
          expr match {
            case _: ExprLiteral | _: AnsiCast | _: Cast => expr
          }
        } catch {
          case _: ParseException | _: MatchError =>
            throw QueryCompilationErrors.failedToParseExistenceDefaultAsLiteral(field.name, text)
        }
        // The expression should be a literal value by this point, possibly wrapped in a cast
        // function. This is enforced by the execution of commands that assign default values.
        expr.eval()
      }.getOrElse(null)
    }
}

/**
 * @since 1.3.0
 */
@Stable
object StructType extends AbstractDataType {

  override private[sql] def defaultConcreteType: DataType = new StructType

  override private[sql] def acceptsType(other: DataType): Boolean = {
    other.isInstanceOf[StructType]
  }

  override private[sql] def simpleString: String = "struct"

  private[sql] def fromString(raw: String): StructType = {
    Try(DataType.fromJson(raw)).getOrElse(LegacyTypeStringParser.parseString(raw)) match {
      case t: StructType => t
      case _ => throw QueryExecutionErrors.failedParsingStructTypeError(raw)
    }
  }

  /**
   * Creates StructType for a given DDL-formatted string, which is a comma separated list of field
   * definitions, e.g., a INT, b STRING.
   *
   * @since 2.2.0
   */
  def fromDDL(ddl: String): StructType = CatalystSqlParser.parseTableSchema(ddl)

  def apply(fields: Seq[StructField]): StructType = StructType(fields.toArray)

  def apply(fields: java.util.List[StructField]): StructType = {
    import scala.collection.JavaConverters._
    StructType(fields.asScala.toSeq)
  }

  private[sql] def fromAttributes(attributes: Seq[Attribute]): StructType =
    StructType(attributes.map(a => StructField(a.name, a.dataType, a.nullable, a.metadata)))

  private[sql] def removeMetadata(key: String, dt: DataType): DataType =
    dt match {
      case StructType(fields) =>
        val newFields = fields.map { f =>
          val mb = new MetadataBuilder()
          f.copy(dataType = removeMetadata(key, f.dataType),
            metadata = mb.withMetadata(f.metadata).remove(key).build())
        }
        StructType(newFields)
      case _ => dt
    }

  /**
   * This leverages `merge` to merge data types for UNION operator by specializing
   * the handling of struct types to follow UNION semantics.
   */
  private[sql] def unionLikeMerge(left: DataType, right: DataType): DataType =
    mergeInternal(left, right, (s1: StructType, s2: StructType) => {
      val leftFields = s1.fields
      val rightFields = s2.fields
      require(leftFields.size == rightFields.size, "To merge nullability, " +
        "two structs must have same number of fields.")

      val newFields = leftFields.zip(rightFields).map {
        case (leftField, rightField) =>
          leftField.copy(
            dataType = unionLikeMerge(leftField.dataType, rightField.dataType),
            nullable = leftField.nullable || rightField.nullable)
      }.toSeq
      StructType(newFields)
    })

  private[sql] def merge(left: DataType, right: DataType): DataType =
    mergeInternal(left, right, (s1: StructType, s2: StructType) => {
      val leftFields = s1.fields
      val rightFields = s2.fields
      val newFields = mutable.ArrayBuffer.empty[StructField]

      val rightMapped = fieldsMap(rightFields)
      leftFields.foreach {
        case leftField @ StructField(leftName, leftType, leftNullable, _) =>
          rightMapped.get(leftName)
            .map { case rightField @ StructField(rightName, rightType, rightNullable, _) =>
              try {
                leftField.copy(
                  dataType = merge(leftType, rightType),
                  nullable = leftNullable || rightNullable)
              } catch {
                case NonFatal(e) =>
                  throw QueryExecutionErrors.failedMergingFieldsError(leftName, rightName, e)
              }
            }
            .orElse {
              Some(leftField)
            }
            .foreach(newFields += _)
      }

      val leftMapped = fieldsMap(leftFields)
      rightFields
        .filterNot(f => leftMapped.get(f.name).nonEmpty)
        .foreach { f =>
          newFields += f
        }

      StructType(newFields.toSeq)
    })

  private def mergeInternal(
      left: DataType,
      right: DataType,
      mergeStruct: (StructType, StructType) => StructType): DataType =
    (left, right) match {
      case (ArrayType(leftElementType, leftContainsNull),
      ArrayType(rightElementType, rightContainsNull)) =>
        ArrayType(
          mergeInternal(leftElementType, rightElementType, mergeStruct),
          leftContainsNull || rightContainsNull)

      case (MapType(leftKeyType, leftValueType, leftContainsNull),
      MapType(rightKeyType, rightValueType, rightContainsNull)) =>
        MapType(
          mergeInternal(leftKeyType, rightKeyType, mergeStruct),
          mergeInternal(leftValueType, rightValueType, mergeStruct),
          leftContainsNull || rightContainsNull)

      case (s1: StructType, s2: StructType) => mergeStruct(s1, s2)

      case (DecimalType.Fixed(leftPrecision, leftScale),
        DecimalType.Fixed(rightPrecision, rightScale)) =>
        if (leftScale == rightScale) {
          DecimalType(leftPrecision.max(rightPrecision), leftScale)
        } else {
          throw QueryExecutionErrors.cannotMergeDecimalTypesWithIncompatibleScaleError(
            leftScale, rightScale)
        }

      case (leftUdt: UserDefinedType[_], rightUdt: UserDefinedType[_])
        if leftUdt.userClass == rightUdt.userClass => leftUdt

      case (YearMonthIntervalType(lstart, lend), YearMonthIntervalType(rstart, rend)) =>
        YearMonthIntervalType(Math.min(lstart, rstart).toByte, Math.max(lend, rend).toByte)

      case (DayTimeIntervalType(lstart, lend), DayTimeIntervalType(rstart, rend)) =>
        DayTimeIntervalType(Math.min(lstart, rstart).toByte, Math.max(lend, rend).toByte)

      case (leftType, rightType) if leftType == rightType =>
        leftType

      case _ =>
        throw QueryExecutionErrors.cannotMergeIncompatibleDataTypesError(left, right)
    }

  private[sql] def fieldsMap(fields: Array[StructField]): Map[String, StructField] = {
    // Mimics the optimization of breakOut, not present in Scala 2.13, while working in 2.12
    val map = mutable.Map[String, StructField]()
    map.sizeHint(fields.length)
    fields.foreach(s => map.put(s.name, s))
    map
  }

  /**
   * Returns a `StructType` that contains missing fields recursively from `source` to `target`.
   * Note that this doesn't support looking into array type and map type recursively.
   */
  def findMissingFields(
      source: StructType,
      target: StructType,
      resolver: Resolver): Option[StructType] = {
    def bothStructType(dt1: DataType, dt2: DataType): Boolean =
      dt1.isInstanceOf[StructType] && dt2.isInstanceOf[StructType]

    val newFields = mutable.ArrayBuffer.empty[StructField]

    target.fields.foreach { field =>
      val found = source.fields.find(f => resolver(field.name, f.name))
      if (found.isEmpty) {
        // Found a missing field in `source`.
        newFields += field
      } else if (bothStructType(found.get.dataType, field.dataType) &&
          !found.get.dataType.sameType(field.dataType)) {
        // Found a field with same name, but different data type.
        findMissingFields(found.get.dataType.asInstanceOf[StructType],
          field.dataType.asInstanceOf[StructType], resolver).map { missingType =>
          newFields += found.get.copy(dataType = missingType)
        }
      }
    }

    if (newFields.isEmpty) {
      None
    } else {
      Some(StructType(newFields.toSeq))
    }
  }
}
