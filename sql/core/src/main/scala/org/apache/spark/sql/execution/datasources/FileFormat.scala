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

package org.apache.spark.sql.execution.datasources

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import org.apache.hadoop.io.compress.{CompressionCodecFactory, SplittableCompressionCodec}
import org.apache.hadoop.mapreduce.Job

import org.apache.spark.sql._
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.codegen.GenerateUnsafeProjection
import org.apache.spark.sql.errors.QueryExecutionErrors
import org.apache.spark.sql.internal.SQLConf
import org.apache.spark.sql.sources.Filter
import org.apache.spark.sql.types._
import org.apache.spark.unsafe.types.UTF8String


/**
 * Used to read and write data stored in files to/from the [[InternalRow]] format.
 */
trait FileFormat {
  /**
   * When possible, this method should return the schema of the given `files`.  When the format
   * does not support inference, or no valid files are given should return None.  In these cases
   * Spark will require that user specify the schema manually.
   */
  def inferSchema(
      sparkSession: SparkSession,
      options: Map[String, String],
      files: Seq[FileStatus]): Option[StructType]

  /**
   * Prepares a write job and returns an [[OutputWriterFactory]].  Client side job preparation can
   * be put here.  For example, user defined output committer can be configured here
   * by setting the output committer class in the conf of spark.sql.sources.outputCommitterClass.
   */
  def prepareWrite(
      sparkSession: SparkSession,
      job: Job,
      options: Map[String, String],
      dataSchema: StructType): OutputWriterFactory

  /**
   * Returns whether this format supports returning columnar batch or not.
   * If columnar batch output is requested, users shall supply
   * FileFormat.OPTION_RETURNING_BATCH -> true
   * in relation options when calling buildReaderWithPartitionValues.
   * This should only be passed as true if it can actually be supported.
   * For ParquetFileFormat and OrcFileFormat, passing this option is required.
   *
   * TODO: we should just have different traits for the different formats.
   */
  def supportBatch(sparkSession: SparkSession, dataSchema: StructType): Boolean = {
    false
  }

  /**
   * Returns concrete column vector class names for each column to be used in a columnar batch
   * if this format supports returning columnar batch.
   */
  def vectorTypes(
      requiredSchema: StructType,
      partitionSchema: StructType,
      sqlConf: SQLConf): Option[Seq[String]] = {
    None
  }

  /**
   * Returns whether a file with `path` could be split or not.
   */
  def isSplitable(
      sparkSession: SparkSession,
      options: Map[String, String],
      path: Path): Boolean = {
    false
  }

  /**
   * Returns a function that can be used to read a single file in as an Iterator of InternalRow.
   *
   * @param dataSchema The global data schema. It can be either specified by the user, or
   *                   reconciled/merged from all underlying data files. If any partition columns
   *                   are contained in the files, they are preserved in this schema.
   * @param partitionSchema The schema of the partition column row that will be present in each
   *                        PartitionedFile. These columns should be appended to the rows that
   *                        are produced by the iterator.
   * @param requiredSchema The schema of the data that should be output for each row.  This may be a
   *                       subset of the columns that are present in the file if column pruning has
   *                       occurred.
   * @param filters A set of filters than can optionally be used to reduce the number of rows output
   * @param options A set of string -> string configuration options.
   * @return
   */
  protected def buildReader(
      sparkSession: SparkSession,
      dataSchema: StructType,
      partitionSchema: StructType,
      requiredSchema: StructType,
      filters: Seq[Filter],
      options: Map[String, String],
      hadoopConf: Configuration): PartitionedFile => Iterator[InternalRow] = {
    throw QueryExecutionErrors.buildReaderUnsupportedForFileFormatError(this.toString)
  }

  /**
   * Exactly the same as [[buildReader]] except that the reader function returned by this method
   * appends partition values to [[InternalRow]]s produced by the reader function [[buildReader]]
   * returns.
   */
  def buildReaderWithPartitionValues(
      sparkSession: SparkSession,
      dataSchema: StructType,
      partitionSchema: StructType,
      requiredSchema: StructType,
      filters: Seq[Filter],
      options: Map[String, String],
      hadoopConf: Configuration): PartitionedFile => Iterator[InternalRow] = {
    val dataReader = buildReader(
      sparkSession, dataSchema, partitionSchema, requiredSchema, filters, options, hadoopConf)

    new (PartitionedFile => Iterator[InternalRow]) with Serializable {
      private val fullSchema = requiredSchema.toAttributes ++ partitionSchema.toAttributes

      // Using lazy val to avoid serialization
      private lazy val appendPartitionColumns =
        GenerateUnsafeProjection.generate(fullSchema, fullSchema)

      override def apply(file: PartitionedFile): Iterator[InternalRow] = {
        // Using local val to avoid per-row lazy val check (pre-mature optimization?...)
        val converter = appendPartitionColumns

        // Note that we have to apply the converter even though `file.partitionValues` is empty.
        // This is because the converter is also responsible for converting safe `InternalRow`s into
        // `UnsafeRow`s.
        if (partitionSchema.isEmpty) {
          dataReader(file).map { dataRow =>
            converter(dataRow)
          }
        } else {
          val joinedRow = new JoinedRow()
          dataReader(file).map { dataRow =>
            converter(joinedRow(dataRow, file.partitionValues))
          }
        }
      }
    }
  }

  /**
   * Create a file metadata struct column containing fields supported by the given file format.
   */
  def createFileMetadataCol(): AttributeReference = {
    // Strip out the fields' metadata to avoid exposing it to the user. [[FileSourceStrategy]]
    // avoids confusion by mapping back to [[metadataSchemaFields]].
    val fields = metadataSchemaFields
      .map(FileSourceMetadataAttribute.cleanupFileSourceMetadataInformation)
    FileSourceMetadataAttribute(FileFormat.METADATA_NAME, StructType(fields), nullable = false)
  }

  /**
   * Returns whether this format supports the given [[DataType]] in read/write path.
   * By default all data types are supported.
   */
  def supportDataType(dataType: DataType): Boolean = true

  /**
   * Returns whether this format supports the given filed name in read/write path.
   * By default all field name is supported.
   */
  def supportFieldName(name: String): Boolean = true

  /**
   * All fields the file format's _metadata struct defines.
   *
   * Each field's metadata should define [[METADATA_COL_ATTR_KEY]],
   * [[FILE_SOURCE_METADATA_COL_ATTR_KEY]], and either
   * [[FILE_SOURCE_CONSTANT_METADATA_COL_ATTR_KEY]] or
   * [[FILE_SOURCE_GENERATED_METADATA_COL_ATTR_KEY]] as appropriate.
   *
   * Constant attributes will be extracted automatically from
   * [[PartitionedFile.extraConstantMetadataColumnValues]], while generated metadata columns always
   * map to some hidden/internal column the underslying reader provides.
   *
   * NOTE: It is not possible to change the semantics of the base metadata fields by overriding this
   * method. Technically, a file format could choose suppress them, but that is not recommended.
   */
  def metadataSchemaFields: Seq[StructField] = FileFormat.BASE_METADATA_FIELDS
}

object FileFormat {

  val FILE_PATH = "file_path"

  val FILE_NAME = "file_name"

  val FILE_BLOCK_START = "file_block_start"

  val FILE_BLOCK_LENGTH = "file_block_length"

  val FILE_SIZE = "file_size"

  val FILE_MODIFICATION_TIME = "file_modification_time"

  val METADATA_NAME = "_metadata"

  /**
   * Option to pass to buildReaderWithPartitionValues to return columnar batch output or not.
   * For ParquetFileFormat and OrcFileFormat, passing this option is required.
   * This should only be passed as true if it can actually be supported, which can be checked
   * by calling supportBatch.
   */
  val OPTION_RETURNING_BATCH = "returning_batch"

  /**
   * Schema of metadata struct that can be produced by every file format,
   * metadata fields for every file format must be *not* nullable.
   */
  val BASE_METADATA_FIELDS: Seq[StructField] = Seq(
    FileSourceConstantMetadataStructField(FILE_PATH, StringType, nullable = false),
    FileSourceConstantMetadataStructField(FILE_NAME, StringType, nullable = false),
    FileSourceConstantMetadataStructField(FILE_SIZE, LongType, nullable = false),
    FileSourceConstantMetadataStructField(FILE_BLOCK_START, LongType, nullable = false),
    FileSourceConstantMetadataStructField(FILE_BLOCK_LENGTH, LongType, nullable = false),
    FileSourceConstantMetadataStructField(FILE_MODIFICATION_TIME, TimestampType, nullable = false))

  // create an internal row given required metadata fields and file information
  def createMetadataInternalRow(
      fieldNames: Seq[String],
      filePath: Path,
      fileSize: Long,
      fileModificationTime: Long): InternalRow = {
    // We are not aware of `FILE_BLOCK_START` and `FILE_BLOCK_LENGTH` before splitting files
    assert(!fieldNames.contains(FILE_BLOCK_START) && !fieldNames.contains(FILE_BLOCK_LENGTH))
    updateMetadataInternalRow(new GenericInternalRow(fieldNames.length), fieldNames,
      filePath, fileSize, 0L, fileSize, fileModificationTime, Map.empty)
  }

  // update an internal row given required metadata fields and file information
  def updateMetadataInternalRow(
      row: InternalRow,
      fieldNames: Seq[String],
      filePath: Path,
      fileSize: Long,
      fileBlockStart: Long,
      fileBlockLength: Long,
      fileModificationTime: Long,
      otherConstantMetadataColumnValues: Map[String, Any]): InternalRow = {
    fieldNames.zipWithIndex.foreach { case (name, i) =>
      name match {
        // NOTE: The base metadata fields are hard-wired here and cannot be overridden.
        case FILE_PATH => row.update(i, UTF8String.fromString(filePath.toString))
        case FILE_NAME => row.update(i, UTF8String.fromString(filePath.getName))
        case FILE_SIZE => row.update(i, fileSize)
        case FILE_BLOCK_START => row.update(i, fileBlockStart)
        case FILE_BLOCK_LENGTH => row.update(i, fileBlockLength)
        case FILE_MODIFICATION_TIME =>
          // the modificationTime from the file is in millisecond,
          // while internally, the TimestampType `file_modification_time` is stored in microsecond
          row.update(i, fileModificationTime * 1000L)
        case other =>
          // Other metadata columns use the file-provided value (if any). Automatically convert raw
          // values (including nulls) to literals as a courtesy.
          Literal(otherConstantMetadataColumnValues.get(other).orNull) match {
            case Literal(null, _) => row.setNullAt(i)
            case literal => row.update(i, literal.value)
          }
      }
    }
    row
  }
}

/**
 * The base class file format that is based on text file.
 */
abstract class TextBasedFileFormat extends FileFormat {
  private var codecFactory: CompressionCodecFactory = _

  override def isSplitable(
      sparkSession: SparkSession,
      options: Map[String, String],
      path: Path): Boolean = {
    if (codecFactory == null) {
      codecFactory = new CompressionCodecFactory(
        sparkSession.sessionState.newHadoopConfWithOptions(options))
    }
    val codec = codecFactory.getCodec(path)
    codec == null || codec.isInstanceOf[SplittableCompressionCodec]
  }
}
