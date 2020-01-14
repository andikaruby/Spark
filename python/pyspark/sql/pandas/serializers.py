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

"""
Serializers for PyArrow and pandas conversions. See `pyspark.serializers` for more details.
"""

import sys
if sys.version < '3':
    from itertools import izip as zip
else:
    basestring = unicode = str
    xrange = range

from pyspark.serializers import Serializer, read_int, write_int, UTF8Deserializer


class SpecialLengths(object):
    END_OF_DATA_SECTION = -1
    PYTHON_EXCEPTION_THROWN = -2
    TIMING_DATA = -3
    END_OF_STREAM = -4
    NULL = -5
    START_ARROW_STREAM = -6


class ArrowCollectSerializer(Serializer):
    """
    Deserialize a stream of batches followed by batch order information. Used in
    PandasConversionMixin._collect_as_arrow() after invoking Dataset.collectAsArrowToPython()
    in the JVM.
    """

    def __init__(self):
        self.serializer = ArrowStreamSerializer()

    def dump_stream(self, iterator, stream):
        return self.serializer.dump_stream(iterator, stream)

    def load_stream(self, stream):
        """
        Load a stream of un-ordered Arrow RecordBatches, where the last iteration yields
        a list of indices that can be used to put the RecordBatches in the correct order.
        """
        # load the batches
        for batch in self.serializer.load_stream(stream):
            yield batch

        # load the batch order indices or propagate any error that occurred in the JVM
        num = read_int(stream)
        if num == -1:
            error_msg = UTF8Deserializer().loads(stream)
            raise RuntimeError("An error occurred while calling "
                               "ArrowCollectSerializer.load_stream: {}".format(error_msg))
        batch_order = []
        for i in xrange(num):
            index = read_int(stream)
            batch_order.append(index)
        yield batch_order

    def __repr__(self):
        return "ArrowCollectSerializer(%s)" % self.serializer


class ArrowStreamSerializer(Serializer):
    """
    Serializes Arrow record batches as a stream.
    """

    def dump_stream(self, iterator, stream):
        import pyarrow as pa
        writer = None
        try:
            for batch in iterator:
                if writer is None:
                    writer = pa.RecordBatchStreamWriter(stream, batch.schema)
                writer.write_batch(batch)
        finally:
            if writer is not None:
                writer.close()

    def load_stream(self, stream):
        import pyarrow as pa
        reader = pa.ipc.open_stream(stream)
        for batch in reader:
            yield batch

    def __repr__(self):
        return "ArrowStreamSerializer"


class ArrowStreamPandasSerializer(ArrowStreamSerializer):
    """
    Serializes Pandas.Series as Arrow data with Arrow streaming format.

    :param timezone: A timezone to respect when handling timestamp values
    :param safecheck: If True, conversion from Arrow to Pandas checks for overflow/truncation
    :param assign_cols_by_name: If True, then Pandas DataFrames will get columns by name
    """

    def __init__(self, timezone, safecheck, assign_cols_by_name):
        super(ArrowStreamPandasSerializer, self).__init__()
        self._timezone = timezone
        self._safecheck = safecheck
        self._assign_cols_by_name = assign_cols_by_name

    def arrow_to_pandas(self, arrow_column):
        from pyspark.sql.pandas.types import _check_series_localize_timestamps

        # If the given column is a date type column, creates a series of datetime.date directly
        # instead of creating datetime64[ns] as intermediate data to avoid overflow caused by
        # datetime64[ns] type handling.
        s = arrow_column.to_pandas(date_as_object=True)

        s = _check_series_localize_timestamps(s, self._timezone)
        return s

    def _create_batch(self, series):
        """
        Create an Arrow record batch from the given pandas.Series or list of Series,
        with optional type.

        :param series: A single pandas.Series, list of Series, or list of (series, arrow_type)
        :return: Arrow RecordBatch
        """
        import pandas as pd
        import pyarrow as pa
        from pyspark.sql.pandas.types import _check_series_convert_timestamps_internal
        # Make input conform to [(series1, type1), (series2, type2), ...]
        if not isinstance(series, (list, tuple)) or \
                (len(series) == 2 and isinstance(series[1], pa.DataType)):
            series = [series]
        series = ((s, None) if not isinstance(s, (list, tuple)) else s for s in series)

        def create_array(s, t):
            mask = s.isnull()
            # Ensure timestamp series are in expected form for Spark internal representation
            if t is not None and pa.types.is_timestamp(t):
                s = _check_series_convert_timestamps_internal(s, self._timezone)
            try:
                array = pa.Array.from_pandas(s, mask=mask, type=t, safe=self._safecheck)
            except pa.ArrowException as e:
                error_msg = "Exception thrown when converting pandas.Series (%s) to Arrow " + \
                            "Array (%s). It can be caused by overflows or other unsafe " + \
                            "conversions warned by Arrow. Arrow safe type check can be " + \
                            "disabled by using SQL config " + \
                            "`spark.sql.execution.pandas.arrowSafeTypeConversion`."
                raise RuntimeError(error_msg % (s.dtype, t), e)
            return array

        arrs = []
        for s, t in series:
            if t is not None and pa.types.is_struct(t):
                if not isinstance(s, pd.DataFrame):
                    raise ValueError("A field of type StructType expects a pandas.DataFrame, "
                                     "but got: %s" % str(type(s)))

                # Input partition and result pandas.DataFrame empty, make empty Arrays with struct
                if len(s) == 0 and len(s.columns) == 0:
                    arrs_names = [(pa.array([], type=field.type), field.name) for field in t]
                # Assign result columns by schema name if user labeled with strings
                elif self._assign_cols_by_name and any(isinstance(name, basestring)
                                                       for name in s.columns):
                    arrs_names = [(create_array(s[field.name], field.type), field.name)
                                  for field in t]
                # Assign result columns by  position
                else:
                    arrs_names = [(create_array(s[s.columns[i]], field.type), field.name)
                                  for i, field in enumerate(t)]

                struct_arrs, struct_names = zip(*arrs_names)
                arrs.append(pa.StructArray.from_arrays(struct_arrs, struct_names))
            else:
                arrs.append(create_array(s, t))

        return pa.RecordBatch.from_arrays(arrs, ["_%d" % i for i in xrange(len(arrs))])

    def dump_stream(self, iterator, stream):
        """
        Make ArrowRecordBatches from Pandas Series and serialize. Input is a single series or
        a list of series accompanied by an optional pyarrow type to coerce the data to.
        """
        batches = (self._create_batch(series) for series in iterator)
        super(ArrowStreamPandasSerializer, self).dump_stream(batches, stream)

    def load_stream(self, stream):
        """
        Deserialize ArrowRecordBatches to an Arrow table and return as a list of pandas.Series.
        """
        batches = super(ArrowStreamPandasSerializer, self).load_stream(stream)
        import pyarrow as pa
        for batch in batches:
            yield [self.arrow_to_pandas(c) for c in pa.Table.from_batches([batch]).itercolumns()]

    def __repr__(self):
        return "ArrowStreamPandasSerializer"


class ArrowStreamPandasUDFSerializer(ArrowStreamPandasSerializer):
    """
    Serializer used by Python worker to evaluate Pandas UDFs
    """

    def __init__(self, timezone, safecheck, assign_cols_by_name, df_for_struct=False):
        super(ArrowStreamPandasUDFSerializer, self) \
            .__init__(timezone, safecheck, assign_cols_by_name)
        self._df_for_struct = df_for_struct

    def arrow_to_pandas(self, arrow_column):
        import pyarrow.types as types

        if self._df_for_struct and types.is_struct(arrow_column.type):
            import pandas as pd
            series = [super(ArrowStreamPandasUDFSerializer, self).arrow_to_pandas(column)
                      .rename(field.name)
                      for column, field in zip(arrow_column.flatten(), arrow_column.type)]
            s = pd.concat(series, axis=1)
        else:
            s = super(ArrowStreamPandasUDFSerializer, self).arrow_to_pandas(arrow_column)
        return s

    def dump_stream(self, iterator, stream):
        """
        Override because Pandas UDFs require a START_ARROW_STREAM before the Arrow stream is sent.
        This should be sent after creating the first record batch so in case of an error, it can
        be sent back to the JVM before the Arrow stream starts.
        """

        def init_stream_yield_batches():
            should_write_start_length = True
            for series in iterator:
                batch = self._create_batch(series)
                if should_write_start_length:
                    write_int(SpecialLengths.START_ARROW_STREAM, stream)
                    should_write_start_length = False
                yield batch

        return ArrowStreamSerializer.dump_stream(self, init_stream_yield_batches(), stream)

    def __repr__(self):
        return "ArrowStreamPandasUDFSerializer"


class CogroupUDFSerializer(ArrowStreamPandasUDFSerializer):

    def load_stream(self, stream):
        """
        Deserialize Cogrouped ArrowRecordBatches to a tuple of Arrow tables and yield as two
        lists of pandas.Series.
        """
        import pyarrow as pa
        dataframes_in_group = None

        while dataframes_in_group is None or dataframes_in_group > 0:
            dataframes_in_group = read_int(stream)

            if dataframes_in_group == 2:
                batch1 = [batch for batch in ArrowStreamSerializer.load_stream(self, stream)]
                batch2 = [batch for batch in ArrowStreamSerializer.load_stream(self, stream)]
                yield (
                    [self.arrow_to_pandas(c) for c in pa.Table.from_batches(batch1).itercolumns()],
                    [self.arrow_to_pandas(c) for c in pa.Table.from_batches(batch2).itercolumns()]
                )

            elif dataframes_in_group != 0:
                raise ValueError(
                    'Invalid number of pandas.DataFrames in group {0}'.format(dataframes_in_group))
