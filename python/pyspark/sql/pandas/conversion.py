#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import sys
import warnings
from collections import Counter

from pyspark import since
from pyspark.rdd import _load_from_socket
from pyspark.sql.pandas.serializers import ArrowCollectSerializer
from pyspark.sql.types import IntegralType
from pyspark.sql.types import ByteType, ShortType, IntegerType, LongType, FloatType, \
    DoubleType, BooleanType, TimestampType, StructType, DataType
from pyspark.traceback_utils import SCCallSiteSync


class PandasConversionMixin(object):
    """
    Min-in for the conversion from Spark to pandas. Currently, only :class:`DataFrame`
    can use this class.
    """

    @since(1.3)
    def toPandas(self):
        """
        Returns the contents of this :class:`DataFrame` as Pandas ``pandas.DataFrame``.

        This is only available if Pandas is installed and available.

        .. note:: This method should only be used if the resulting Pandas's :class:`DataFrame` is
            expected to be small, as all the data is loaded into the driver's memory.

        .. note:: Usage with spark.sql.execution.arrow.pyspark.enabled=True is experimental.

        >>> df.toPandas()  # doctest: +SKIP
           age   name
        0    2  Alice
        1    5    Bob
        """
        from pyspark.sql.dataframe import DataFrame

        assert isinstance(self, DataFrame)

        from pyspark.sql.pandas.utils import require_minimum_pandas_version
        require_minimum_pandas_version()

        import numpy as np
        import pandas as pd

        timezone = self.sql_ctx._conf.sessionLocalTimeZone()

        if self.sql_ctx._conf.arrowPySparkEnabled():
            use_arrow = True
            try:
                from pyspark.sql.pandas.types import to_arrow_schema
                from pyspark.sql.pandas.utils import require_minimum_pyarrow_version

                require_minimum_pyarrow_version()
                to_arrow_schema(self.schema)
            except Exception as e:

                if self.sql_ctx._conf.arrowPySparkFallbackEnabled():
                    msg = (
                        "toPandas attempted Arrow optimization because "
                        "'spark.sql.execution.arrow.pyspark.enabled' is set to true; however, "
                        "failed by the reason below:\n  %s\n"
                        "Attempting non-optimization as "
                        "'spark.sql.execution.arrow.pyspark.fallback.enabled' is set to "
                        "true." % str(e))
                    warnings.warn(msg)
                    use_arrow = False
                else:
                    msg = (
                        "toPandas attempted Arrow optimization because "
                        "'spark.sql.execution.arrow.pyspark.enabled' is set to true, but has "
                        "reached the error below and will not continue because automatic fallback "
                        "with 'spark.sql.execution.arrow.pyspark.fallback.enabled' has been set to "
                        "false.\n  %s" % str(e))
                    warnings.warn(msg)
                    raise

            # Try to use Arrow optimization when the schema is supported and the required version
            # of PyArrow is found, if 'spark.sql.execution.arrow.pyspark.enabled' is enabled.
            if use_arrow:
                try:
                    from pyspark.sql.pandas.types import _check_series_localize_timestamps
                    import pyarrow
                    # Rename columns to avoid duplicated column names.
                    tmp_column_names = ['col_{}'.format(i) for i in range(len(self.columns))]
                    batches = self.toDF(*tmp_column_names)._collect_as_arrow()
                    if len(batches) > 0:
                        table = pyarrow.Table.from_batches(batches)
                        # Pandas DataFrame created from PyArrow uses datetime64[ns] for date type
                        # values, but we should use datetime.date to match the behavior with when
                        # Arrow optimization is disabled.
                        pdf = table.to_pandas(date_as_object=True)
                        # Rename back to the original column names.
                        pdf.columns = self.columns
                        for field in self.schema:
                            if isinstance(field.dataType, TimestampType):
                                pdf[field.name] = \
                                    _check_series_localize_timestamps(pdf[field.name], timezone)
                        return pdf
                    else:
                        return pd.DataFrame.from_records([], columns=self.columns)
                except Exception as e:
                    # We might have to allow fallback here as well but multiple Spark jobs can
                    # be executed. So, simply fail in this case for now.
                    msg = (
                        "toPandas attempted Arrow optimization because "
                        "'spark.sql.execution.arrow.pyspark.enabled' is set to true, but has "
                        "reached the error below and can not continue. Note that "
                        "'spark.sql.execution.arrow.pyspark.fallback.enabled' does not have an "
                        "effect on failures in the middle of "
                        "computation.\n  %s" % str(e))
                    warnings.warn(msg)
                    raise

        # Below is toPandas without Arrow optimization.
        pdf = pd.DataFrame.from_records(self.collect(), columns=self.columns)
        column_counter = Counter(self.columns)

        dtype = [None] * len(self.schema)
        for fieldIdx, field in enumerate(self.schema):
            # For duplicate column name, we use `iloc` to access it.
            if column_counter[field.name] > 1:
                pandas_col = pdf.iloc[:, fieldIdx]
            else:
                pandas_col = pdf[field.name]

            pandas_type = PandasConversionMixin._to_corrected_pandas_type(field.dataType)
            # SPARK-21766: if an integer field is nullable and has null values, it can be
            # inferred by pandas as float column. Once we convert the column with NaN back
            # to integer type e.g., np.int16, we will hit exception. So we use the inferred
            # float type, not the corrected type from the schema in this case.
            if pandas_type is not None and \
                not(isinstance(field.dataType, IntegralType) and field.nullable and
                    pandas_col.isnull().any()):
                dtype[fieldIdx] = pandas_type
            # Ensure we fall back to nullable numpy types, even when whole column is null:
            if isinstance(field.dataType, IntegralType) and pandas_col.isnull().any():
                dtype[fieldIdx] = np.float64
            if isinstance(field.dataType, BooleanType) and pandas_col.isnull().any():
                dtype[fieldIdx] = np.object

        df = pd.DataFrame()
        for index, t in enumerate(dtype):
            column_name = self.schema[index].name

            # For duplicate column name, we use `iloc` to access it.
            if column_counter[column_name] > 1:
                series = pdf.iloc[:, index]
            else:
                series = pdf[column_name]

            if t is not None:
                series = series.astype(t, copy=False)

            # `insert` API makes copy of data, we only do it for Series of duplicate column names.
            # `pdf.iloc[:, index] = pdf.iloc[:, index]...` doesn't always work because `iloc` could
            # return a view or a copy depending by context.
            if column_counter[column_name] > 1:
                df.insert(index, column_name, series, allow_duplicates=True)
            else:
                df[column_name] = series

        pdf = df

        if timezone is None:
            return pdf
        else:
            from pyspark.sql.pandas.types import _check_series_convert_timestamps_local_tz
            for field in self.schema:
                # TODO: handle nested timestamps, such as ArrayType(TimestampType())?
                if isinstance(field.dataType, TimestampType):
                    pdf[field.name] = \
                        _check_series_convert_timestamps_local_tz(pdf[field.name], timezone)
            return pdf

    @staticmethod
    def _to_corrected_pandas_type(dt):
        """
        When converting Spark SQL records to Pandas :class:`DataFrame`, the inferred data type
        may be wrong. This method gets the corrected data type for Pandas if that type may be
        inferred incorrectly.
        """
        import numpy as np
        if type(dt) == ByteType:
            return np.int8
        elif type(dt) == ShortType:
            return np.int16
        elif type(dt) == IntegerType:
            return np.int32
        elif type(dt) == LongType:
            return np.int64
        elif type(dt) == FloatType:
            return np.float32
        elif type(dt) == DoubleType:
            return np.float64
        elif type(dt) == BooleanType:
            return np.bool
        elif type(dt) == TimestampType:
            return np.datetime64
        else:
            return None

    def _collect_as_arrow(self):
        """
        Returns all records as a list of ArrowRecordBatches, pyarrow must be installed
        and available on driver and worker Python environments.

        .. note:: Experimental.
        """
        from pyspark.sql.dataframe import DataFrame

        assert isinstance(self, DataFrame)

        with SCCallSiteSync(self._sc):
            port, auth_secret, jsocket_auth_server = self._jdf.collectAsArrowToPython()

        # Collect list of un-ordered batches where last element is a list of correct order indices
        try:
            results = list(_load_from_socket((port, auth_secret), ArrowCollectSerializer()))
        finally:
            # Join serving thread and raise any exceptions from collectAsArrowToPython
            jsocket_auth_server.getResult()

        # Separate RecordBatches from batch order indices in results
        batches = results[:-1]
        batch_order = results[-1]

        # Re-order the batch list using the correct order
        return [batches[i] for i in batch_order]


class SparkConversionMixin(object):
    """
    Min-in for the conversion from pandas to Spark. Currently, only :class:`SparkSession`
    can use this class.
    pandasRDD=True creates a DataFrame from an RDD of pandas dataframes
    (currently only supported using arrow)
    """
    def createDataFrame(self, data, schema=None, samplingRatio=None, verifySchema=True,
                        pandasRDD=False):
        from pyspark.sql import SparkSession

        assert isinstance(self, SparkSession)

        from pyspark.sql.pandas.utils import require_minimum_pandas_version
        require_minimum_pandas_version()

        timezone = self._wrapped._conf.sessionLocalTimeZone()

        if self._wrapped._conf.arrowPySparkEnabled() and pandasRDD:
            from pyspark.rdd import RDD
            if not isinstance(data, RDD):
                raise ValueError('pandasRDD is set but data is of type %s, expected RDD type.'
                                 % type(data))
            # TODO: Support non-arrow conversion? might be *very* slow
            return self._create_from_pandas_rdd_with_arrow(data, schema, timezone)

        # If no schema supplied by user then get the names of columns only
        if schema is None:
            schema = [str(x) if not isinstance(x, str) else
                      (x.encode('utf-8') if not isinstance(x, str) else x)
                      for x in data.columns]

        if self._wrapped._conf.arrowPySparkEnabled() and len(data) > 0:
            try:
                return self._create_from_pandas_with_arrow(data, schema, timezone)
            except Exception as e:
                if self._wrapped._conf.arrowPySparkFallbackEnabled():
                    msg = (
                        "createDataFrame attempted Arrow optimization because "
                        "'spark.sql.execution.arrow.pyspark.enabled' is set to true; however, "
                        "failed by the reason below:\n  %s\n"
                        "Attempting non-optimization as "
                        "'spark.sql.execution.arrow.pyspark.fallback.enabled' is set to "
                        "true." % str(e))
                    warnings.warn(msg)
                else:
                    msg = (
                        "createDataFrame attempted Arrow optimization because "
                        "'spark.sql.execution.arrow.pyspark.enabled' is set to true, but has "
                        "reached the error below and will not continue because automatic "
                        "fallback with 'spark.sql.execution.arrow.pyspark.fallback.enabled' "
                        "has been set to false.\n  %s" % str(e))
                    warnings.warn(msg)
                    raise
        data = self._convert_from_pandas(data, schema, timezone)
        return self._create_dataframe(data, schema, samplingRatio, verifySchema)

    def _convert_from_pandas(self, pdf, schema, timezone):
        """
         Convert a pandas.DataFrame to list of records that can be used to make a DataFrame
         :return list of records
        """
        from pyspark.sql import SparkSession

        assert isinstance(self, SparkSession)

        if timezone is not None:
            from pyspark.sql.pandas.types import _check_dataframe_covert_timestamps_tz_local
            pdf = _check_dataframe_covert_timestamps_tz_local(pdf, timezone, schema)

        # Convert pandas.DataFrame to list of numpy records
        np_records = pdf.to_records(index=False)

        # Check if any columns need to be fixed for Spark to infer properly
        if len(np_records) > 0:
            record_dtype = self._get_numpy_record_dtype(np_records[0])
            if record_dtype is not None:
                return [r.astype(record_dtype).tolist() for r in np_records]

        # Convert list of numpy records to python lists
        return [r.tolist() for r in np_records]

    def _get_numpy_record_dtype(self, rec):
        """
        Used when converting a pandas.DataFrame to Spark using to_records(), this will correct
        the dtypes of fields in a record so they can be properly loaded into Spark.
        :param rec: a numpy record to check field dtypes
        :return corrected dtype for a numpy.record or None if no correction needed
        """
        import numpy as np
        cur_dtypes = rec.dtype
        col_names = cur_dtypes.names
        record_type_list = []
        has_rec_fix = False
        for i in range(len(cur_dtypes)):
            curr_type = cur_dtypes[i]
            # If type is a datetime64 timestamp, convert to microseconds
            # NOTE: if dtype is datetime[ns] then np.record.tolist() will output values as longs,
            # conversion from [us] or lower will lead to py datetime objects, see SPARK-22417
            if curr_type == np.dtype('datetime64[ns]'):
                curr_type = 'datetime64[us]'
                has_rec_fix = True
            record_type_list.append((str(col_names[i]), curr_type))
        return np.dtype(record_type_list) if has_rec_fix else None

    def _create_from_pandas_rdd_with_arrow(self, prdd, schema, timezone):
        """
        Create a DataFrame from an RDD of pandas.DataFrames by converting each DF to one or more
        Arrow RecordBatches which are then sent to the JVM.
        If a schema is passed in, the data types will be used to coerce the data in
        Pandas to Arrow conversion.
        """
        import pandas as pd
        import pyarrow as pa

        safecheck = self._wrapped._conf.arrowSafeTypeConversion()

        # In case no schema is passed, extract inferred schema from the first record batch
        from pyspark.sql.pandas.types import from_arrow_schema
        if schema is None:
            schema = from_arrow_schema(pa.Schema.from_pandas(prdd.first()))

        # Convert to an RDD of arrow record batches
        rb_rdd = (prdd.
                  filter(lambda x: isinstance(x, pd.DataFrame)).
                  map(lambda x: _dataframe_to_arrow_record_batch(x,
                                                                 timezone=timezone,
                                                                 schema=schema,
                                                                 safecheck=safecheck)))

        # Create Spark DataFrame from Arrow record batches RDD
        from pyspark.sql.dataframe import DataFrame
        jrdd = rb_rdd._to_java_object_rdd()
        jdf = self._jvm.PythonSQLUtils.toDataFrame(jrdd, schema.json(), self._wrapped._jsqlContext)
        df = DataFrame(jdf, self._wrapped)
        df._schema = schema
        return df

    def _create_from_pandas_with_arrow(self, pdf, schema, timezone):
        """
        Create a DataFrame from a given pandas.DataFrame by slicing it into partitions, converting
        to Arrow data, then sending to the JVM to parallelize. If a schema is passed in, the
        data types will be used to coerce the data in Pandas to Arrow conversion.
        """
        from pyspark.sql import SparkSession
        from pyspark.sql.dataframe import DataFrame

        assert isinstance(self, SparkSession)

        from pyspark.sql.pandas.serializers import ArrowStreamPandasSerializer
        from pyspark.sql.types import TimestampType
        from pyspark.sql.pandas.types import from_arrow_type, to_arrow_type
        from pyspark.sql.pandas.utils import require_minimum_pandas_version, \
            require_minimum_pyarrow_version

        require_minimum_pandas_version()
        require_minimum_pyarrow_version()

        from pandas.api.types import is_datetime64_dtype, is_datetime64tz_dtype
        import pyarrow as pa

        # Create the Spark schema from list of names passed in with Arrow types
        if isinstance(schema, (list, tuple)):
            arrow_schema = pa.Schema.from_pandas(pdf, preserve_index=False)
            struct = StructType()
            for name, field in zip(schema, arrow_schema):
                struct.add(name, from_arrow_type(field.type), nullable=field.nullable)
            schema = struct

        # Determine arrow types to coerce data when creating batches
        if isinstance(schema, StructType):
            arrow_types = [to_arrow_type(f.dataType) for f in schema.fields]
        elif isinstance(schema, DataType):
            raise ValueError("Single data type %s is not supported with Arrow" % str(schema))
        else:
            # Any timestamps must be coerced to be compatible with Spark
            arrow_types = [to_arrow_type(TimestampType())
                           if is_datetime64_dtype(t) or is_datetime64tz_dtype(t) else None
                           for t in pdf.dtypes]

        # Slice the DataFrame to be batched
        step = -(-len(pdf) // self.sparkContext.defaultParallelism)  # round int up
        pdf_slices = (pdf.iloc[start:start + step] for start in range(0, len(pdf), step))

        # Create list of Arrow (columns, type) for serializer dump_stream
        arrow_data = [[(c, t) for (_, c), t in zip(pdf_slice.iteritems(), arrow_types)]
                      for pdf_slice in pdf_slices]

        jsqlContext = self._wrapped._jsqlContext

        safecheck = self._wrapped._conf.arrowSafeTypeConversion()
        col_by_name = True  # col by name only applies to StructType columns, can't happen here
        ser = ArrowStreamPandasSerializer(timezone, safecheck, col_by_name)

        def reader_func(temp_filename):
            return self._jvm.PythonSQLUtils.readArrowStreamFromFile(jsqlContext, temp_filename)

        def create_RDD_server():
            return self._jvm.ArrowRDDServer(jsqlContext)

        # Create Spark DataFrame from Arrow stream file, using one batch per partition
        jrdd = self._sc._serialize_to_jvm(arrow_data, ser, reader_func, create_RDD_server)
        jdf = self._jvm.PythonSQLUtils.toDataFrame(jrdd, schema.json(), jsqlContext)
        df = DataFrame(jdf, self._wrapped)
        df._schema = schema
        return df


def _sanitize_arrow_schema(schema):
    import pyarrow as pa
    import re
    sanitized_fields = []

    # Convert pyarrow schema to a spark compatible one
    _SPARK_DISALLOWED_CHARS = re.compile('[ ,;{}()\n\t=]')

    def _sanitized_spark_field_name(name):
        return _SPARK_DISALLOWED_CHARS.sub('_', name)

    for field in schema:
        name = field.name
        sanitized_name = _sanitized_spark_field_name(name)

        if sanitized_name != name:
            sanitized_field = pa.field(sanitized_name, field.type,
                                       field.nullable, field.metadata)
            sanitized_fields.append(sanitized_field)
        else:
            sanitized_fields.append(field)

    new_schema = pa.schema(sanitized_fields, metadata=schema.metadata)
    return new_schema


def _dataframe_to_arrow_record_batch(pdf, schema=None, timezone=None, safecheck=False):
    """
    Create a DataFrame from a given pandas.DataFrame by slicing it into partitions, converting
    to Arrow data, then sending to the JVM to parallelize. If a schema is passed in, the
    data types will be used to coerce the data in Pandas to Arrow conversion.
    """
    import re
    import pyarrow as pa
    from pyspark.sql.pandas.types import to_arrow_schema, from_arrow_schema
    from pyspark.sql.pandas.utils import require_minimum_pandas_version, \
        require_minimum_pyarrow_version

    require_minimum_pandas_version()
    require_minimum_pyarrow_version()

    # Determine arrow types to coerce data when creating batches
    if schema is not None:
        arrow_schema = to_arrow_schema(schema)
    else:
        # Any timestamps must be coerced to be compatible with Spark
        arrow_schema = to_arrow_schema(from_arrow_schema(pa.Schema.from_pandas(pdf)))

    # Sanitize arrow schema for spark compatibility
    arrow_schema = _sanitize_arrow_schema(arrow_schema)

    # Create an Arrow record batch, one batch per DF
    from pyspark.sql.pandas.serializers import ArrowStreamPandasSerializer
    arrow_data = [(pdf[col_name], arrow_type) for col_name, arrow_type
                  in zip(arrow_schema.names, arrow_schema.types)]

    col_by_name = True  # col by name only applies to StructType columns, can't happen here
    ser = ArrowStreamPandasSerializer(timezone, safecheck, col_by_name)

    return bytearray(ser._create_batch(arrow_data).serialize())


def _test():
    import doctest
    from pyspark.sql import SparkSession
    import pyspark.sql.pandas.conversion
    globs = pyspark.sql.pandas.conversion.__dict__.copy()
    spark = SparkSession.builder\
        .master("local[4]")\
        .appName("sql.pandas.conversion tests")\
        .getOrCreate()
    globs['spark'] = spark
    (failure_count, test_count) = doctest.testmod(
        pyspark.sql.pandas.conversion, globs=globs,
        optionflags=doctest.ELLIPSIS | doctest.NORMALIZE_WHITESPACE | doctest.REPORT_NDIFF)
    spark.stop()
    if failure_count:
        sys.exit(-1)


if __name__ == "__main__":
    _test()
