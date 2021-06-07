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
An internal immutable DataFrame with some metadata to manage indexes.
"""
import re
from typing import Any, Dict, List, Optional, Sequence, Tuple, Union, TYPE_CHECKING, cast
from itertools import accumulate
import py4j

import numpy as np
import pandas as pd
from pandas.api.types import CategoricalDtype  # noqa: F401
from pyspark import sql as spark
from pyspark._globals import _NoValue, _NoValueType
from pyspark.sql import functions as F, Window
from pyspark.sql.functions import PandasUDFType, pandas_udf
from pyspark.sql.types import (  # noqa: F401
    BooleanType,
    DataType,
    IntegralType,
    LongType,
    StructField,
    StructType,
    StringType,
)

# For running doctests and reference resolution in PyCharm.
from pyspark import pandas as ps

if TYPE_CHECKING:
    # This is required in old Python 3.5 to prevent circular reference.
    from pyspark.pandas.series import Series  # noqa: F401 (SPARK-34943)
from pyspark.pandas.spark.utils import as_nullable_spark_type, force_decimal_precision_scale
from pyspark.pandas.data_type_ops.base import DataTypeOps
from pyspark.pandas.typedef import (
    Dtype,
    as_spark_type,
    extension_dtypes,
    infer_pd_series_spark_type,
    spark_type_to_pandas_dtype,
)
from pyspark.pandas.utils import (
    column_labels_level,
    default_session,
    is_name_like_tuple,
    is_testing,
    lazy_property,
    name_like_string,
    scol_for,
    spark_column_equals,
    verify_temp_column_name,
)


# A function to turn given numbers to Spark columns that represent pandas-on-Spark index.
SPARK_INDEX_NAME_FORMAT = "__index_level_{}__".format
SPARK_DEFAULT_INDEX_NAME = SPARK_INDEX_NAME_FORMAT(0)
# A pattern to check if the name of a Spark column is a pandas-on-Spark index name or not.
SPARK_INDEX_NAME_PATTERN = re.compile(r"__index_level_[0-9]+__")

NATURAL_ORDER_COLUMN_NAME = "__natural_order__"

HIDDEN_COLUMNS = {NATURAL_ORDER_COLUMN_NAME}

DEFAULT_SERIES_NAME = 0
SPARK_DEFAULT_SERIES_NAME = str(DEFAULT_SERIES_NAME)


class InternalField:
    """
    The internal field to store the dtype as well as the Spark's StructField optionally.

    Parameters
    ----------
    dtype : numpy.dtype or pandas' ExtensionDtype
        The dtype for the field
    struct_field : StructField, optional
        The `StructField` for the field. If None, InternalFrame will properly set.
    """

    def __init__(self, dtype: Dtype, struct_field: Optional[StructField] = None):
        self._dtype = dtype
        self._struct_field = struct_field

    @staticmethod
    def from_struct_field(
        struct_field: StructField, *, use_extension_dtypes: bool = False
    ) -> "InternalField":
        """
        Returns a new InternalField object created from the given StructField.

        The dtype will be inferred from the data type of the given StructField.

        Parameters
        ----------
        struct_field : StructField
            The StructField used to create a new InternalField object.
        use_extension_dtypes : bool
            If True, try to use the extension dtypes.

        Returns
        -------
        InternalField
        """
        return InternalField(
            dtype=spark_type_to_pandas_dtype(
                struct_field.dataType, use_extension_dtypes=use_extension_dtypes
            ),
            struct_field=struct_field,
        )

    @property
    def dtype(self) -> Dtype:
        """Return the dtype for the field."""
        return self._dtype

    @property
    def struct_field(self) -> Optional[StructField]:
        """Return the StructField for the field."""
        return self._struct_field

    @property
    def name(self) -> str:
        """Return the field name if the StructField exists."""
        assert self.struct_field is not None
        return self.struct_field.name

    @property
    def spark_type(self) -> DataType:
        """Return the spark data type for the field if the StructField exists."""
        assert self.struct_field is not None
        return self.struct_field.dataType

    @property
    def nullable(self) -> bool:
        """Return the nullability for the field if the StructField exists."""
        assert self.struct_field is not None
        return self.struct_field.nullable

    @property
    def metadata(self) -> Dict[str, Any]:
        """Return the metadata for the field if the StructField exists."""
        assert self.struct_field is not None
        return self.struct_field.metadata

    @property
    def is_extension_dtype(self) -> bool:
        """Return whether the dtype for the field is an extension type or not."""
        return isinstance(self.dtype, extension_dtypes)

    def normalize_spark_type(self) -> "InternalField":
        """Return a new InternalField object with normalized Spark data type."""
        assert self.struct_field is not None
        return self.copy(
            spark_type=force_decimal_precision_scale(as_nullable_spark_type(self.spark_type)),
            nullable=True,
        )

    def copy(
        self,
        *,
        name: Union[str, _NoValueType] = _NoValue,
        dtype: Union[Dtype, _NoValueType] = _NoValue,
        spark_type: Union[DataType, _NoValueType] = _NoValue,
        nullable: Union[bool, _NoValueType] = _NoValue,
        metadata: Union[Optional[Dict[str, Any]], _NoValueType] = _NoValue,
    ) -> "InternalField":
        """Copy the InternalField object."""
        if name is _NoValue:
            name = self.name
        if dtype is _NoValue:
            dtype = self.dtype
        if spark_type is _NoValue:
            spark_type = self.spark_type
        if nullable is _NoValue:
            nullable = self.nullable
        if metadata is _NoValue:
            metadata = self.metadata
        return InternalField(
            dtype=cast(Dtype, dtype),
            struct_field=StructField(
                name=cast(str, name),
                dataType=cast(DataType, spark_type),
                nullable=cast(bool, nullable),
                metadata=cast(Optional[Dict[str, Any]], metadata),
            ),
        )

    def __repr__(self) -> str:
        return "InternalField(dtype={dtype},struct_field={struct_field})".format(
            dtype=self.dtype, struct_field=self.struct_field
        )


class InternalFrame(object):
    """
    The internal immutable DataFrame which manages Spark DataFrame and column names and index
    information.

    .. note:: this is an internal class. It is not supposed to be exposed to users and users
        should not directly access to it.

    The internal immutable DataFrame represents the index information for a DataFrame it belongs to.
    For instance, if we have a pandas-on-Spark DataFrame as below, pandas DataFrame does not
    store the index as columns.

    >>> psdf = ps.DataFrame({
    ...     'A': [1, 2, 3, 4],
    ...     'B': [5, 6, 7, 8],
    ...     'C': [9, 10, 11, 12],
    ...     'D': [13, 14, 15, 16],
    ...     'E': [17, 18, 19, 20]}, columns = ['A', 'B', 'C', 'D', 'E'])
    >>> psdf  # doctest: +NORMALIZE_WHITESPACE
       A  B   C   D   E
    0  1  5   9  13  17
    1  2  6  10  14  18
    2  3  7  11  15  19
    3  4  8  12  16  20

    However, all columns including index column are also stored in Spark DataFrame internally
    as below.

    >>> psdf._internal.to_internal_spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE
    +-----------------+---+---+---+---+---+
    |__index_level_0__|  A|  B|  C|  D|  E|
    +-----------------+---+---+---+---+---+
    |                0|  1|  5|  9| 13| 17|
    |                1|  2|  6| 10| 14| 18|
    |                2|  3|  7| 11| 15| 19|
    |                3|  4|  8| 12| 16| 20|
    +-----------------+---+---+---+---+---+

    In order to fill this gap, the current metadata is used by mapping Spark's internal column
    to pandas-on-Spark's index. See the method below:

    * `spark_frame` represents the internal Spark DataFrame

    * `data_spark_column_names` represents non-indexing Spark column names

    * `data_spark_columns` represents non-indexing Spark columns

    * `data_fields` represents non-indexing InternalFields

    * `index_spark_column_names` represents internal index Spark column names

    * `index_spark_columns` represents internal index Spark columns

    * `index_fields` represents index InternalFields

    * `spark_column_names` represents all columns

    * `index_names` represents the external index name as a label

    * `to_internal_spark_frame` represents Spark DataFrame derived by the metadata. Includes index.

    * `to_pandas_frame` represents pandas DataFrame derived by the metadata

    >>> internal = psdf._internal
    >>> internal.spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE +ELLIPSIS
    +-----------------+---+---+---+---+---+-----------------+
    |__index_level_0__|  A|  B|  C|  D|  E|__natural_order__|
    +-----------------+---+---+---+---+---+-----------------+
    |                0|  1|  5|  9| 13| 17|              ...|
    |                1|  2|  6| 10| 14| 18|              ...|
    |                2|  3|  7| 11| 15| 19|              ...|
    |                3|  4|  8| 12| 16| 20|              ...|
    +-----------------+---+---+---+---+---+-----------------+
    >>> internal.data_spark_column_names
    ['A', 'B', 'C', 'D', 'E']
    >>> internal.index_spark_column_names
    ['__index_level_0__']
    >>> internal.spark_column_names
    ['__index_level_0__', 'A', 'B', 'C', 'D', 'E']
    >>> internal.index_names
    [None]
    >>> internal.data_fields    # doctest: +NORMALIZE_WHITESPACE
    [InternalField(dtype=int64,struct_field=StructField(A,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(B,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(C,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(D,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(E,LongType,false))]
    >>> internal.index_fields
    [InternalField(dtype=int64,struct_field=StructField(__index_level_0__,LongType,false))]
    >>> internal.to_internal_spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE
    +-----------------+---+---+---+---+---+
    |__index_level_0__|  A|  B|  C|  D|  E|
    +-----------------+---+---+---+---+---+
    |                0|  1|  5|  9| 13| 17|
    |                1|  2|  6| 10| 14| 18|
    |                2|  3|  7| 11| 15| 19|
    |                3|  4|  8| 12| 16| 20|
    +-----------------+---+---+---+---+---+
    >>> internal.to_pandas_frame
       A  B   C   D   E
    0  1  5   9  13  17
    1  2  6  10  14  18
    2  3  7  11  15  19
    3  4  8  12  16  20

    In case that index is set to one of the existing column as below:

    >>> psdf1 = psdf.set_index("A")
    >>> psdf1  # doctest: +NORMALIZE_WHITESPACE
       B   C   D   E
    A
    1  5   9  13  17
    2  6  10  14  18
    3  7  11  15  19
    4  8  12  16  20

    >>> psdf1._internal.to_internal_spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE
    +---+---+---+---+---+
    |  A|  B|  C|  D|  E|
    +---+---+---+---+---+
    |  1|  5|  9| 13| 17|
    |  2|  6| 10| 14| 18|
    |  3|  7| 11| 15| 19|
    |  4|  8| 12| 16| 20|
    +---+---+---+---+---+

    >>> internal = psdf1._internal
    >>> internal.spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE +ELLIPSIS
    +-----------------+---+---+---+---+---+-----------------+
    |__index_level_0__|  A|  B|  C|  D|  E|__natural_order__|
    +-----------------+---+---+---+---+---+-----------------+
    |                0|  1|  5|  9| 13| 17|              ...|
    |                1|  2|  6| 10| 14| 18|              ...|
    |                2|  3|  7| 11| 15| 19|              ...|
    |                3|  4|  8| 12| 16| 20|              ...|
    +-----------------+---+---+---+---+---+-----------------+
    >>> internal.data_spark_column_names
    ['B', 'C', 'D', 'E']
    >>> internal.index_spark_column_names
    ['A']
    >>> internal.spark_column_names
    ['A', 'B', 'C', 'D', 'E']
    >>> internal.index_names
    [('A',)]
    >>> internal.data_fields
    [InternalField(dtype=int64,struct_field=StructField(B,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(C,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(D,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(E,LongType,false))]
    >>> internal.index_fields
    [InternalField(dtype=int64,struct_field=StructField(A,LongType,false))]
    >>> internal.to_internal_spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE
    +---+---+---+---+---+
    |  A|  B|  C|  D|  E|
    +---+---+---+---+---+
    |  1|  5|  9| 13| 17|
    |  2|  6| 10| 14| 18|
    |  3|  7| 11| 15| 19|
    |  4|  8| 12| 16| 20|
    +---+---+---+---+---+
    >>> internal.to_pandas_frame  # doctest: +NORMALIZE_WHITESPACE
       B   C   D   E
    A
    1  5   9  13  17
    2  6  10  14  18
    3  7  11  15  19
    4  8  12  16  20

    In case that index becomes a multi index as below:

    >>> psdf2 = psdf.set_index("A", append=True)
    >>> psdf2  # doctest: +NORMALIZE_WHITESPACE
         B   C   D   E
      A
    0 1  5   9  13  17
    1 2  6  10  14  18
    2 3  7  11  15  19
    3 4  8  12  16  20

    >>> psdf2._internal.to_internal_spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE
    +-----------------+---+---+---+---+---+
    |__index_level_0__|  A|  B|  C|  D|  E|
    +-----------------+---+---+---+---+---+
    |                0|  1|  5|  9| 13| 17|
    |                1|  2|  6| 10| 14| 18|
    |                2|  3|  7| 11| 15| 19|
    |                3|  4|  8| 12| 16| 20|
    +-----------------+---+---+---+---+---+

    >>> internal = psdf2._internal
    >>> internal.spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE +ELLIPSIS
    +-----------------+---+---+---+---+---+-----------------+
    |__index_level_0__|  A|  B|  C|  D|  E|__natural_order__|
    +-----------------+---+---+---+---+---+-----------------+
    |                0|  1|  5|  9| 13| 17|              ...|
    |                1|  2|  6| 10| 14| 18|              ...|
    |                2|  3|  7| 11| 15| 19|              ...|
    |                3|  4|  8| 12| 16| 20|              ...|
    +-----------------+---+---+---+---+---+-----------------+
    >>> internal.data_spark_column_names
    ['B', 'C', 'D', 'E']
    >>> internal.index_spark_column_names
    ['__index_level_0__', 'A']
    >>> internal.spark_column_names
    ['__index_level_0__', 'A', 'B', 'C', 'D', 'E']
    >>> internal.index_names
    [None, ('A',)]
    >>> internal.data_fields  # doctest: +NORMALIZE_WHITESPACE
    [InternalField(dtype=int64,struct_field=StructField(B,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(C,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(D,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(E,LongType,false))]
    >>> internal.index_fields  # doctest: +NORMALIZE_WHITESPACE
    [InternalField(dtype=int64,struct_field=StructField(__index_level_0__,LongType,false)),
     InternalField(dtype=int64,struct_field=StructField(A,LongType,false))]
    >>> internal.to_internal_spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE
    +-----------------+---+---+---+---+---+
    |__index_level_0__|  A|  B|  C|  D|  E|
    +-----------------+---+---+---+---+---+
    |                0|  1|  5|  9| 13| 17|
    |                1|  2|  6| 10| 14| 18|
    |                2|  3|  7| 11| 15| 19|
    |                3|  4|  8| 12| 16| 20|
    +-----------------+---+---+---+---+---+
    >>> internal.to_pandas_frame  # doctest: +NORMALIZE_WHITESPACE
         B   C   D   E
      A
    0 1  5   9  13  17
    1 2  6  10  14  18
    2 3  7  11  15  19
    3 4  8  12  16  20

    For multi-level columns, it also holds column_labels

    >>> columns = pd.MultiIndex.from_tuples([('X', 'A'), ('X', 'B'),
    ...                                      ('Y', 'C'), ('Y', 'D')])
    >>> psdf3 = ps.DataFrame([
    ...     [1, 2, 3, 4],
    ...     [5, 6, 7, 8],
    ...     [9, 10, 11, 12],
    ...     [13, 14, 15, 16],
    ...     [17, 18, 19, 20]], columns = columns)
    >>> psdf3  # doctest: +NORMALIZE_WHITESPACE
        X       Y
        A   B   C   D
    0   1   2   3   4
    1   5   6   7   8
    2   9  10  11  12
    3  13  14  15  16
    4  17  18  19  20

    >>> internal = psdf3._internal
    >>> internal.spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE +ELLIPSIS
    +-----------------+------+------+------+------+-----------------+
    |__index_level_0__|(X, A)|(X, B)|(Y, C)|(Y, D)|__natural_order__|
    +-----------------+------+------+------+------+-----------------+
    |                0|     1|     2|     3|     4|              ...|
    |                1|     5|     6|     7|     8|              ...|
    |                2|     9|    10|    11|    12|              ...|
    |                3|    13|    14|    15|    16|              ...|
    |                4|    17|    18|    19|    20|              ...|
    +-----------------+------+------+------+------+-----------------+
    >>> internal.data_spark_column_names
    ['(X, A)', '(X, B)', '(Y, C)', '(Y, D)']
    >>> internal.column_labels
    [('X', 'A'), ('X', 'B'), ('Y', 'C'), ('Y', 'D')]

    For Series, it also holds scol to represent the column.

    >>> psseries = psdf1.B
    >>> psseries
    A
    1    5
    2    6
    3    7
    4    8
    Name: B, dtype: int64

    >>> internal = psseries._internal
    >>> internal.spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE +ELLIPSIS
    +-----------------+---+---+---+---+---+-----------------+
    |__index_level_0__|  A|  B|  C|  D|  E|__natural_order__|
    +-----------------+---+---+---+---+---+-----------------+
    |                0|  1|  5|  9| 13| 17|              ...|
    |                1|  2|  6| 10| 14| 18|              ...|
    |                2|  3|  7| 11| 15| 19|              ...|
    |                3|  4|  8| 12| 16| 20|              ...|
    +-----------------+---+---+---+---+---+-----------------+
    >>> internal.data_spark_column_names
    ['B']
    >>> internal.index_spark_column_names
    ['A']
    >>> internal.spark_column_names
    ['A', 'B']
    >>> internal.index_names
    [('A',)]
    >>> internal.data_fields
    [InternalField(dtype=int64,struct_field=StructField(B,LongType,false))]
    >>> internal.index_fields
    [InternalField(dtype=int64,struct_field=StructField(A,LongType,false))]
    >>> internal.to_internal_spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE
    +---+---+
    |  A|  B|
    +---+---+
    |  1|  5|
    |  2|  6|
    |  3|  7|
    |  4|  8|
    +---+---+
    >>> internal.to_pandas_frame  # doctest: +NORMALIZE_WHITESPACE
       B
    A
    1  5
    2  6
    3  7
    4  8
    """

    def __init__(
        self,
        spark_frame: spark.DataFrame,
        index_spark_columns: Optional[List[spark.Column]],
        index_names: Optional[List[Optional[Tuple]]] = None,
        index_fields: Optional[List[InternalField]] = None,
        column_labels: Optional[List[Tuple]] = None,
        data_spark_columns: Optional[List[spark.Column]] = None,
        data_fields: Optional[List[InternalField]] = None,
        column_label_names: Optional[List[Optional[Tuple]]] = None,
    ):
        """
        Create a new internal immutable DataFrame to manage Spark DataFrame, column fields and
        index fields and names.

        :param spark_frame: Spark DataFrame to be managed.
        :param index_spark_columns: list of Spark Column
                                    Spark Columns for the index.
        :param index_names: list of tuples
                            the index names.
        :param index_fields: list of InternalField
                             the InternalFields for the index columns
        :param column_labels: list of tuples with the same length
                              The multi-level values in the tuples.
        :param data_spark_columns: list of Spark Column
                                   Spark Columns to appear as columns. If this is None, calculated
                                   from spark_frame.
        :param data_fields: list of InternalField
                            the InternalFields for the data columns
        :param column_label_names: Names for each of the column index levels.

        See the examples below to refer what each parameter means.

        >>> column_labels = pd.MultiIndex.from_tuples(
        ...     [('a', 'x'), ('a', 'y'), ('b', 'z')], names=["column_labels_a", "column_labels_b"])
        >>> row_index = pd.MultiIndex.from_tuples(
        ...     [('foo', 'bar'), ('foo', 'bar'), ('zoo', 'bar')],
        ...     names=["row_index_a", "row_index_b"])
        >>> psdf = ps.DataFrame(
        ...     [[1, 2, 3], [4, 5, 6], [7, 8, 9]], index=row_index, columns=column_labels)
        >>> psdf.set_index(('a', 'x'), append=True, inplace=True)
        >>> psdf  # doctest: +NORMALIZE_WHITESPACE
        column_labels_a                  a  b
        column_labels_b                  y  z
        row_index_a row_index_b (a, x)
        foo         bar         1       2  3
                                4       5  6
        zoo         bar         7       8  9

        >>> internal = psdf._internal

        >>> internal.spark_frame.show()  # doctest: +NORMALIZE_WHITESPACE +ELLIPSIS
        +-----------------+-----------------+------+------+------+...
        |__index_level_0__|__index_level_1__|(a, x)|(a, y)|(b, z)|...
        +-----------------+-----------------+------+------+------+...
        |              foo|              bar|     1|     2|     3|...
        |              foo|              bar|     4|     5|     6|...
        |              zoo|              bar|     7|     8|     9|...
        +-----------------+-----------------+------+------+------+...

        >>> internal.index_spark_columns  # doctest: +SKIP
        [Column<'__index_level_0__'>, Column<'__index_level_1__'>, Column<'(a, x)'>]

        >>> internal.index_names
        [('row_index_a',), ('row_index_b',), ('a', 'x')]

        >>> internal.index_fields  # doctest: +NORMALIZE_WHITESPACE
        [InternalField(dtype=object,struct_field=StructField(__index_level_0__,StringType,false)),
         InternalField(dtype=object,struct_field=StructField(__index_level_1__,StringType,false)),
         InternalField(dtype=int64,struct_field=StructField((a, x),LongType,false))]

        >>> internal.column_labels
        [('a', 'y'), ('b', 'z')]

        >>> internal.data_spark_columns  # doctest: +SKIP
        [Column<'(a, y)'>, Column<'(b, z)'>]

        >>> internal.data_fields  # doctest: +NORMALIZE_WHITESPACE
        [InternalField(dtype=int64,struct_field=StructField((a, y),LongType,false)),
         InternalField(dtype=int64,struct_field=StructField((b, z),LongType,false))]

        >>> internal.column_label_names
        [('column_labels_a',), ('column_labels_b',)]
        """

        assert isinstance(spark_frame, spark.DataFrame)
        assert not spark_frame.isStreaming, "pandas-on-Spark does not support Structured Streaming."

        if not index_spark_columns:
            if data_spark_columns is not None:
                if column_labels is not None:
                    data_spark_columns = [
                        scol.alias(name_like_string(label))
                        for scol, label in zip(data_spark_columns, column_labels)
                    ]
                spark_frame = spark_frame.select(data_spark_columns)

            assert not any(SPARK_INDEX_NAME_PATTERN.match(name) for name in spark_frame.columns), (
                "Index columns should not appear in columns of the Spark DataFrame. Avoid "
                "index column names [%s]." % SPARK_INDEX_NAME_PATTERN
            )

            # Create default index.
            spark_frame, force_nullable = InternalFrame.attach_default_index(spark_frame)
            index_spark_columns = [scol_for(spark_frame, SPARK_DEFAULT_INDEX_NAME)]

            index_fields = [
                InternalField.from_struct_field(
                    StructField(SPARK_DEFAULT_INDEX_NAME, LongType(), nullable=False)
                )
            ]

            if data_spark_columns is not None:
                data_struct_fields = [
                    field
                    for field in spark_frame.schema.fields
                    if field.name != SPARK_DEFAULT_INDEX_NAME
                ]
                data_spark_columns = [
                    scol_for(spark_frame, field.name) for field in data_struct_fields
                ]
                if data_fields is not None:
                    data_fields = [
                        field.copy(
                            name=name_like_string(struct_field.name),
                            nullable=(force_nullable or field.nullable),
                        )
                        for field, struct_field in zip(data_fields, data_struct_fields)
                    ]

        if NATURAL_ORDER_COLUMN_NAME not in spark_frame.columns:
            spark_frame = spark_frame.withColumn(
                NATURAL_ORDER_COLUMN_NAME, F.monotonically_increasing_id()
            )

        self._sdf = spark_frame  # type: spark.DataFrame

        # index_spark_columns
        assert all(
            isinstance(index_scol, spark.Column) for index_scol in index_spark_columns
        ), index_spark_columns

        self._index_spark_columns = index_spark_columns  # type: List[spark.Column]

        # data_spark_columns
        if data_spark_columns is None:
            data_spark_columns = [
                scol_for(spark_frame, col)
                for col in spark_frame.columns
                if all(
                    not spark_column_equals(scol_for(spark_frame, col), index_scol)
                    for index_scol in index_spark_columns
                )
                and col not in HIDDEN_COLUMNS
            ]
            self._data_spark_columns = data_spark_columns  # type: List[spark.Column]
        else:
            assert all(isinstance(scol, spark.Column) for scol in data_spark_columns)
            self._data_spark_columns = data_spark_columns

        # fields
        if index_fields is None:
            index_fields = [None] * len(index_spark_columns)
        if data_fields is None:
            data_fields = [None] * len(data_spark_columns)

        assert len(index_spark_columns) == len(index_fields), (
            len(index_spark_columns),
            len(index_fields),
        )
        assert len(data_spark_columns) == len(data_fields), (
            len(data_spark_columns),
            len(data_fields),
        )

        if any(field is None or field.struct_field is None for field in index_fields) and any(
            field is None or field.struct_field is None for field in data_fields
        ):
            schema = spark_frame.select(index_spark_columns + data_spark_columns).schema
            fields = [
                InternalField.from_struct_field(struct_field)
                if field is None
                else InternalField(field.dtype, struct_field)
                if field.struct_field is None
                else field
                for field, struct_field in zip(index_fields + data_fields, schema.fields)
            ]
            index_fields = fields[: len(index_spark_columns)]
            data_fields = fields[len(index_spark_columns) :]
        elif any(field is None or field.struct_field is None for field in index_fields):
            schema = spark_frame.select(index_spark_columns).schema
            index_fields = [
                InternalField.from_struct_field(struct_field)
                if field is None
                else InternalField(field.dtype, struct_field)
                if field.struct_field is None
                else field
                for field, struct_field in zip(index_fields, schema.fields)
            ]
        elif any(field is None or field.struct_field is None for field in data_fields):
            schema = spark_frame.select(data_spark_columns).schema
            data_fields = [
                InternalField.from_struct_field(struct_field)
                if field is None
                else InternalField(field.dtype, struct_field)
                if field.struct_field is None
                else field
                for field, struct_field in zip(data_fields, schema.fields)
            ]

        assert all(
            isinstance(ops.dtype, Dtype.__args__)  # type: ignore
            and (
                ops.dtype == np.dtype("object")
                or as_spark_type(ops.dtype, raise_error=False) is not None
            )
            for ops in index_fields
        ), index_fields

        if is_testing():
            struct_fields = spark_frame.select(index_spark_columns).schema.fields
            assert all(
                index_field.struct_field == struct_field
                for index_field, struct_field in zip(index_fields, struct_fields)
            ), (index_fields, struct_fields)

        self._index_fields = index_fields  # type: List[InternalField]

        assert all(
            isinstance(ops.dtype, Dtype.__args__)  # type: ignore
            and (
                ops.dtype == np.dtype("object")
                or as_spark_type(ops.dtype, raise_error=False) is not None
            )
            for ops in data_fields
        ), data_fields

        if is_testing():
            struct_fields = spark_frame.select(data_spark_columns).schema.fields
            assert all(
                data_field.struct_field == struct_field
                for data_field, struct_field in zip(data_fields, struct_fields)
            ), (data_fields, struct_fields)

        self._data_fields = data_fields  # type: List[InternalField]

        # index_names
        if not index_names:
            index_names = [None] * len(index_spark_columns)

        assert len(index_spark_columns) == len(index_names), (
            len(index_spark_columns),
            len(index_names),
        )
        assert all(
            is_name_like_tuple(index_name, check_type=True) for index_name in index_names
        ), index_names

        self._index_names = index_names  # type: List[Optional[Tuple]]

        # column_labels
        if column_labels is None:
            self._column_labels = [
                (col,) for col in spark_frame.select(self._data_spark_columns).columns
            ]  # type: List[Tuple]
        else:
            assert len(column_labels) == len(self._data_spark_columns), (
                len(column_labels),
                len(self._data_spark_columns),
            )
            if len(column_labels) == 1:
                column_label = column_labels[0]
                assert is_name_like_tuple(column_label, check_type=True), column_label
            else:
                assert all(
                    is_name_like_tuple(column_label, check_type=True)
                    for column_label in column_labels
                ), column_labels
                assert len(set(len(label) for label in column_labels)) <= 1, column_labels
            self._column_labels = column_labels

        # column_label_names
        if column_label_names is None:
            self._column_label_names = [None] * column_labels_level(
                self._column_labels
            )  # type: List[Optional[Tuple]]
        else:
            if len(self._column_labels) > 0:
                assert len(column_label_names) == column_labels_level(self._column_labels), (
                    len(column_label_names),
                    column_labels_level(self._column_labels),
                )
            else:
                assert len(column_label_names) > 0, len(column_label_names)
            assert all(
                is_name_like_tuple(column_label_name, check_type=True)
                for column_label_name in column_label_names
            ), column_label_names
            self._column_label_names = column_label_names

    @staticmethod
    def attach_default_index(
        sdf: spark.DataFrame, default_index_type: Optional[str] = None
    ) -> Tuple[spark.DataFrame, bool]:
        """
        This method attaches a default index to Spark DataFrame. Spark does not have the index
        notion so corresponding column should be generated.
        There are several types of default index can be configured by `compute.default_index_type`.

        >>> spark_frame = ps.range(10).to_spark()
        >>> spark_frame
        DataFrame[id: bigint]

        It adds the default index column '__index_level_0__'.

        >>> spark_frame = InternalFrame.attach_default_index(spark_frame)[0]
        >>> spark_frame
        DataFrame[__index_level_0__: bigint, id: bigint]

        It throws an exception if the given column name already exists.

        >>> InternalFrame.attach_default_index(spark_frame)[0]
        ... # doctest: +ELLIPSIS
        Traceback (most recent call last):
          ...
        AssertionError: '__index_level_0__' already exists...
        """
        index_column = SPARK_DEFAULT_INDEX_NAME
        assert (
            index_column not in sdf.columns
        ), "'%s' already exists in the Spark column names '%s'" % (index_column, sdf.columns)

        if default_index_type is None:
            default_index_type = ps.get_option("compute.default_index_type")

        if default_index_type == "sequence":
            return InternalFrame.attach_sequence_column(sdf, column_name=index_column)
        elif default_index_type == "distributed-sequence":
            return InternalFrame.attach_distributed_sequence_column(sdf, column_name=index_column)
        elif default_index_type == "distributed":
            return InternalFrame.attach_distributed_column(sdf, column_name=index_column)
        else:
            raise ValueError(
                "'compute.default_index_type' should be one of 'sequence',"
                " 'distributed-sequence' and 'distributed'"
            )

    @staticmethod
    def attach_sequence_column(
        sdf: spark.DataFrame, column_name: str
    ) -> Tuple[spark.DataFrame, bool]:
        scols = [scol_for(sdf, column) for column in sdf.columns]
        sequential_index = (
            F.row_number().over(Window.orderBy(F.monotonically_increasing_id())).cast("long") - 1
        )
        return sdf.select(sequential_index.alias(column_name), *scols), False

    @staticmethod
    def attach_distributed_column(
        sdf: spark.DataFrame, column_name: str
    ) -> Tuple[spark.DataFrame, bool]:
        scols = [scol_for(sdf, column) for column in sdf.columns]
        return sdf.select(F.monotonically_increasing_id().alias(column_name), *scols), False

    @staticmethod
    def attach_distributed_sequence_column(
        sdf: spark.DataFrame, column_name: str
    ) -> Tuple[spark.DataFrame, bool]:
        """
        This method attaches a Spark column that has a sequence in a distributed manner.
        This is equivalent to the column assigned when default index type 'distributed-sequence'.

        >>> sdf = ps.DataFrame(['a', 'b', 'c']).to_spark()
        >>> sdf, force_nullable = (
        ...     InternalFrame.attach_distributed_sequence_column(sdf, column_name="sequence")
        ... )
        >>> sdf.show()  # doctest: +NORMALIZE_WHITESPACE
        +--------+---+
        |sequence|  0|
        +--------+---+
        |       0|  a|
        |       1|  b|
        |       2|  c|
        +--------+---+
        >>> force_nullable
        True
        """
        if len(sdf.columns) > 0:
            try:
                jdf = sdf._jdf.toDF()  # type: ignore

                sql_ctx = sdf.sql_ctx
                encoders = sql_ctx._jvm.org.apache.spark.sql.Encoders  # type: ignore
                encoder = encoders.tuple(jdf.exprEnc(), encoders.scalaLong())

                jrdd = jdf.localCheckpoint(False).rdd().zipWithIndex()

                df = spark.DataFrame(
                    sql_ctx.sparkSession._jsparkSession.createDataset(  # type: ignore
                        jrdd, encoder
                    ).toDF(),
                    sql_ctx,
                )
                columns = df.columns
                return (
                    df.selectExpr(
                        "`{}` as `{}`".format(columns[1], column_name), "`{}`.*".format(columns[0])
                    ),
                    True,
                )
            except py4j.protocol.Py4JError:
                if is_testing():
                    raise
                return InternalFrame._attach_distributed_sequence_column(sdf, column_name)
        else:
            cnt = sdf.count()
            if cnt > 0:
                return default_session().range(cnt).toDF(column_name), False
            else:
                return (
                    default_session().createDataFrame(
                        [],
                        schema=StructType().add(column_name, data_type=LongType(), nullable=False),
                    ),
                    False,
                )

    @staticmethod
    def _attach_distributed_sequence_column(
        sdf: spark.DataFrame, column_name: str
    ) -> Tuple[spark.DataFrame, bool]:
        """
        >>> sdf = ps.DataFrame(['a', 'b', 'c']).to_spark()
        >>> sdf, force_nullable = (
        ...     InternalFrame._attach_distributed_sequence_column(sdf, column_name="sequence")
        ... )
        >>> sdf.sort("sequence").show()  # doctest: +NORMALIZE_WHITESPACE
        +--------+---+
        |sequence|  0|
        +--------+---+
        |       0|  a|
        |       1|  b|
        |       2|  c|
        +--------+---+
        >>> force_nullable
        False
        """
        scols = [scol_for(sdf, column) for column in sdf.columns]

        spark_partition_column = verify_temp_column_name(sdf, "__spark_partition_id__")
        offset_column = verify_temp_column_name(sdf, "__offset__")
        row_number_column = verify_temp_column_name(sdf, "__row_number__")

        # 1. Calculates counts per each partition ID. `counts` here is, for instance,
        #     {
        #         1: 83,
        #         6: 83,
        #         3: 83,
        #         ...
        #     }
        sdf = sdf.withColumn(spark_partition_column, F.spark_partition_id())

        # Checkpoint the DataFrame to fix the partition ID.
        sdf = sdf.localCheckpoint(eager=False)

        counts = map(
            lambda x: (x["key"], x["count"]),
            sdf.groupby(sdf[spark_partition_column].alias("key")).count().collect(),
        )

        # 2. Calculates cumulative sum in an order of partition id.
        #     Note that it does not matter if partition id guarantees its order or not.
        #     We just need a one-by-one sequential id.

        # sort by partition key.
        sorted_counts = sorted(counts, key=lambda x: x[0])
        # get cumulative sum in an order of partition key.
        cumulative_counts = [0] + list(accumulate(map(lambda count: count[1], sorted_counts)))
        # zip it with partition key.
        sums = dict(zip(map(lambda count: count[0], sorted_counts), cumulative_counts))

        # 3. Attach offset for each partition.
        @pandas_udf(LongType(), PandasUDFType.SCALAR)
        def offset(id: pd.Series) -> pd.Series:
            current_partition_offset = sums[id.iloc[0]]
            return pd.Series(current_partition_offset).repeat(len(id))

        sdf = sdf.withColumn(offset_column, offset(spark_partition_column))

        # 4. Calculate row_number in each partition.
        w = Window.partitionBy(spark_partition_column).orderBy(F.monotonically_increasing_id())
        row_number = F.row_number().over(w)
        sdf = sdf.withColumn(row_number_column, row_number)

        # 5. Calculate the index.
        return (
            sdf.select(
                (sdf[offset_column] + sdf[row_number_column] - 1).alias(column_name), *scols
            ),
            False,
        )

    def spark_column_for(self, label: Tuple) -> spark.Column:
        """Return Spark Column for the given column label."""
        column_labels_to_scol = dict(zip(self.column_labels, self.data_spark_columns))
        if label in column_labels_to_scol:
            return column_labels_to_scol[label]
        else:
            raise KeyError(name_like_string(label))

    def spark_column_name_for(self, label_or_scol: Union[Tuple, spark.Column]) -> str:
        """Return the actual Spark column name for the given column label."""
        if isinstance(label_or_scol, spark.Column):
            return self.spark_frame.select(label_or_scol).columns[0]
        else:
            return self.field_for(label_or_scol).name

    def spark_type_for(self, label_or_scol: Union[Tuple, spark.Column]) -> DataType:
        """Return DataType for the given column label."""
        if isinstance(label_or_scol, spark.Column):
            return self.spark_frame.select(label_or_scol).schema[0].dataType
        else:
            return self.field_for(label_or_scol).spark_type

    def spark_column_nullable_for(self, label_or_scol: Union[Tuple, spark.Column]) -> bool:
        """Return nullability for the given column label."""
        if isinstance(label_or_scol, spark.Column):
            return self.spark_frame.select(label_or_scol).schema[0].nullable
        else:
            return self.field_for(label_or_scol).nullable

    def field_for(self, label: Tuple) -> InternalField:
        """Return InternalField for the given column label."""
        column_labels_to_fields = dict(zip(self.column_labels, self.data_fields))
        if label in column_labels_to_fields:
            return column_labels_to_fields[label]
        else:
            raise KeyError(name_like_string(label))

    @property
    def spark_frame(self) -> spark.DataFrame:
        """Return the managed Spark DataFrame."""
        return self._sdf

    @lazy_property
    def data_spark_column_names(self) -> List[str]:
        """Return the managed column field names."""
        return [field.name for field in self.data_fields]

    @property
    def data_spark_columns(self) -> List[spark.Column]:
        """Return Spark Columns for the managed data columns."""
        return self._data_spark_columns

    @property
    def index_spark_column_names(self) -> List[str]:
        """Return the managed index field names."""
        return [field.name for field in self.index_fields]

    @property
    def index_spark_columns(self) -> List[spark.Column]:
        """Return Spark Columns for the managed index columns."""
        return self._index_spark_columns

    @lazy_property
    def spark_column_names(self) -> List[str]:
        """Return all the field names including index field names."""
        return self.spark_frame.select(self.spark_columns).columns

    @lazy_property
    def spark_columns(self) -> List[spark.Column]:
        """Return Spark Columns for the managed columns including index columns."""
        index_spark_columns = self.index_spark_columns
        return index_spark_columns + [
            spark_column
            for spark_column in self.data_spark_columns
            if all(not spark_column_equals(spark_column, scol) for scol in index_spark_columns)
        ]

    @property
    def index_names(self) -> List[Optional[Tuple]]:
        """Return the managed index names."""
        return self._index_names

    @lazy_property
    def index_level(self) -> int:
        """Return the level of the index."""
        return len(self._index_names)

    @property
    def column_labels(self) -> List[Tuple]:
        """Return the managed column index."""
        return self._column_labels

    @lazy_property
    def column_labels_level(self) -> int:
        """Return the level of the column index."""
        return len(self._column_label_names)

    @property
    def column_label_names(self) -> List[Optional[Tuple]]:
        """Return names of the index levels."""
        return self._column_label_names

    @property
    def index_fields(self) -> List[InternalField]:
        """Return InternalFields for the managed index columns."""
        return self._index_fields

    @property
    def data_fields(self) -> List[InternalField]:
        """Return InternalFields for the managed columns."""
        return self._data_fields

    @lazy_property
    def to_internal_spark_frame(self) -> spark.DataFrame:
        """
        Return as Spark DataFrame. This contains index columns as well
        and should be only used for internal purposes.
        """
        index_spark_columns = self.index_spark_columns
        data_columns = []
        for spark_column in self.data_spark_columns:
            if all(not spark_column_equals(spark_column, scol) for scol in index_spark_columns):
                data_columns.append(spark_column)
        return self.spark_frame.select(index_spark_columns + data_columns)

    @lazy_property
    def to_pandas_frame(self) -> pd.DataFrame:
        """Return as pandas DataFrame."""
        sdf = self.to_internal_spark_frame
        pdf = sdf.toPandas()
        if len(pdf) == 0 and len(sdf.schema) > 0:
            pdf = pdf.astype(
                {field.name: spark_type_to_pandas_dtype(field.dataType) for field in sdf.schema}
            )

        return InternalFrame.restore_index(pdf, **self.arguments_for_restore_index)

    @lazy_property
    def arguments_for_restore_index(self) -> Dict:
        """Create arguments for `restore_index`."""
        column_names = []
        fields = self.index_fields.copy()
        ext_fields = {
            col: field
            for col, field in zip(self.index_spark_column_names, self.index_fields)
            if isinstance(field.dtype, extension_dtypes)
        }
        for spark_column, column_name, field in zip(
            self.data_spark_columns, self.data_spark_column_names, self.data_fields
        ):
            for index_spark_column_name, index_spark_column in zip(
                self.index_spark_column_names, self.index_spark_columns
            ):
                if spark_column_equals(spark_column, index_spark_column):
                    column_names.append(index_spark_column_name)
                    break
            else:
                column_names.append(column_name)
                fields.append(field)
                if isinstance(field.dtype, extension_dtypes):
                    ext_fields[column_name] = field

        return dict(
            index_columns=self.index_spark_column_names,
            index_names=self.index_names,
            data_columns=column_names,
            column_labels=self.column_labels,
            column_label_names=self.column_label_names,
            fields=fields,
            ext_fields=ext_fields,
        )

    @staticmethod
    def restore_index(
        pdf: pd.DataFrame,
        *,
        index_columns: List[str],
        index_names: List[Tuple],
        data_columns: List[str],
        column_labels: List[Tuple],
        column_label_names: List[Tuple],
        fields: Dict[str, InternalField] = None,
        ext_fields: Dict[str, InternalField] = None,
    ) -> pd.DataFrame:
        """
        Restore pandas DataFrame indices using the metadata.

        :param pdf: the pandas DataFrame to be processed.
        :param index_columns: the original column names for index columns.
        :param index_names: the index names after restored.
        :param data_columns: the original column names for data columns.
        :param column_labels: the column labels after restored.
        :param column_label_names: the column label names after restored.
        :param fields: the fields after restored.
        :param ext_fields: the map from the original column names to extension data fields.
        :return: the restored pandas DataFrame

        >>> from numpy import dtype
        >>> pdf = pd.DataFrame({"index": [10, 20, 30], "a": ['a', 'b', 'c'], "b": [0, 2, 1]})
        >>> InternalFrame.restore_index(
        ...     pdf,
        ...     index_columns=["index"],
        ...     index_names=[("idx",)],
        ...     data_columns=["a", "b", "index"],
        ...     column_labels=[("x",), ("y",), ("z",)],
        ...     column_label_names=[("lv1",)],
        ...     fields=[
        ...         InternalField(
        ...             dtype=dtype('int64'),
        ...             struct_field=StructField(name='index', dataType=LongType(), nullable=False),
        ...         ),
        ...         InternalField(
        ...             dtype=dtype('object'),
        ...             struct_field=StructField(name='a', dataType=StringType(), nullable=False),
        ...         ),
        ...         InternalField(
        ...             dtype=CategoricalDtype(categories=["i", "j", "k"]),
        ...             struct_field=StructField(name='b', dataType=LongType(), nullable=False),
        ...         ),
        ...     ],
        ...     ext_fields=None,
        ... )  # doctest: +NORMALIZE_WHITESPACE
        lv1  x  y   z
        idx
        10   a  i  10
        20   b  k  20
        30   c  j  30
        """
        if ext_fields is not None and len(ext_fields) > 0:
            pdf = pdf.astype({col: field.dtype for col, field in ext_fields.items()}, copy=True)

        for col, field in zip(pdf.columns, fields):
            pdf[col] = DataTypeOps(field.dtype, field.spark_type).restore(pdf[col])

        append = False
        for index_field in index_columns:
            drop = index_field not in data_columns
            pdf = pdf.set_index(index_field, drop=drop, append=append)
            append = True
        pdf = pdf[data_columns]

        pdf.index.names = [
            name if name is None or len(name) > 1 else name[0] for name in index_names
        ]

        names = [name if name is None or len(name) > 1 else name[0] for name in column_label_names]
        if len(column_label_names) > 1:
            pdf.columns = pd.MultiIndex.from_tuples(column_labels, names=names)
        else:
            pdf.columns = pd.Index(
                [None if label is None else label[0] for label in column_labels],
                name=names[0],
            )

        return pdf

    @lazy_property
    def resolved_copy(self) -> "InternalFrame":
        """Copy the immutable InternalFrame with the updates resolved."""
        sdf = self.spark_frame.select(self.spark_columns + list(HIDDEN_COLUMNS))
        return self.copy(
            spark_frame=sdf,
            index_spark_columns=[scol_for(sdf, col) for col in self.index_spark_column_names],
            data_spark_columns=[scol_for(sdf, col) for col in self.data_spark_column_names],
        )

    def with_new_sdf(
        self,
        spark_frame: spark.DataFrame,
        *,
        index_fields: Optional[List[InternalField]] = None,
        data_columns: Optional[List[str]] = None,
        data_fields: Optional[List[InternalField]] = None,
    ) -> "InternalFrame":
        """Copy the immutable InternalFrame with the updates by the specified Spark DataFrame.

        :param spark_frame: the new Spark DataFrame
        :param index_fields: the new InternalFields for the index columns.
                             If None, the original dtyeps are used.
        :param data_columns: the new column names. If None, the original one is used.
        :param data_fields: the new InternalFields for the data columns.
                            If None, the original dtyeps are used.
        :return: the copied InternalFrame.
        """
        if index_fields is None:
            index_fields = self.index_fields
        else:
            assert len(index_fields) == len(self.index_fields), (
                len(index_fields),
                len(self.index_fields),
            )

        if data_columns is None:
            data_columns = self.data_spark_column_names
        else:
            assert len(data_columns) == len(self.column_labels), (
                len(data_columns),
                len(self.column_labels),
            )

        if data_fields is None:
            data_fields = self.data_fields
        else:
            assert len(data_fields) == len(self.column_labels), (
                len(data_fields),
                len(self.column_labels),
            )

        sdf = spark_frame.drop(NATURAL_ORDER_COLUMN_NAME)
        return self.copy(
            spark_frame=sdf,
            index_spark_columns=[scol_for(sdf, col) for col in self.index_spark_column_names],
            index_fields=index_fields,
            data_spark_columns=[scol_for(sdf, col) for col in data_columns],
            data_fields=data_fields,
        )

    def with_new_columns(
        self,
        scols_or_pssers: Sequence[Union[spark.Column, "Series"]],
        *,
        column_labels: Optional[List[Tuple]] = None,
        data_fields: Optional[List[InternalField]] = None,
        column_label_names: Union[Optional[List[Optional[Tuple]]], _NoValueType] = _NoValue,
        keep_order: bool = True,
    ) -> "InternalFrame":
        """
        Copy the immutable InternalFrame with the updates by the specified Spark Columns or Series.

        :param scols_or_pssers: the new Spark Columns or Series.
        :param column_labels: the new column index.
            If None, the column_labels of the corresponding `scols_or_pssers` is used if it is
            Series; otherwise the original one is used.
        :param data_fields: the new InternalFields for the data columns.
            If None, the dtypes of the corresponding `scols_or_pssers` is used if it is Series;
            otherwise the dtypes will be inferred from the corresponding `scols_or_pssers`.
        :param column_label_names: the new names of the column index levels.
        :return: the copied InternalFrame.
        """
        from pyspark.pandas.series import Series

        if column_labels is None:
            if all(isinstance(scol_or_psser, Series) for scol_or_psser in scols_or_pssers):
                column_labels = [cast(Series, psser)._column_label for psser in scols_or_pssers]
            else:
                assert len(scols_or_pssers) == len(self.column_labels), (
                    len(scols_or_pssers),
                    len(self.column_labels),
                )
                column_labels = []
                for scol_or_psser, label in zip(scols_or_pssers, self.column_labels):
                    if isinstance(scol_or_psser, Series):
                        column_labels.append(scol_or_psser._column_label)
                    else:
                        column_labels.append(label)
        else:
            assert len(scols_or_pssers) == len(column_labels), (
                len(scols_or_pssers),
                len(column_labels),
            )

        data_spark_columns = []
        for scol_or_psser in scols_or_pssers:
            if isinstance(scol_or_psser, Series):
                scol = scol_or_psser.spark.column
            else:
                scol = scol_or_psser
            data_spark_columns.append(scol)

        if data_fields is None:
            data_fields = []
            for scol_or_psser in scols_or_pssers:
                if isinstance(scol_or_psser, Series):
                    data_fields.append(scol_or_psser._internal.data_fields[0])
                else:
                    data_fields.append(None)
        else:
            assert len(scols_or_pssers) == len(data_fields), (
                len(scols_or_pssers),
                len(data_fields),
            )

        sdf = self.spark_frame
        if not keep_order:
            sdf = self.spark_frame.select(self.index_spark_columns + data_spark_columns)
            index_spark_columns = [scol_for(sdf, col) for col in self.index_spark_column_names]
            data_spark_columns = [
                scol_for(sdf, col) for col in self.spark_frame.select(data_spark_columns).columns
            ]
        else:
            index_spark_columns = self.index_spark_columns

        if column_label_names is _NoValue:
            column_label_names = self._column_label_names

        return self.copy(
            spark_frame=sdf,
            index_spark_columns=index_spark_columns,
            column_labels=column_labels,
            data_spark_columns=data_spark_columns,
            data_fields=data_fields,
            column_label_names=column_label_names,
        )

    def with_filter(self, pred: Union[spark.Column, "Series"]) -> "InternalFrame":
        """
        Copy the immutable InternalFrame with the updates by the predicate.

        :param pred: the predicate to filter.
        :return: the copied InternalFrame.
        """
        from pyspark.pandas.series import Series

        if isinstance(pred, Series):
            assert isinstance(pred.spark.data_type, BooleanType), pred.spark.data_type
            condition = pred.spark.column
        else:
            spark_type = self.spark_frame.select(pred).schema[0].dataType
            assert isinstance(spark_type, BooleanType), spark_type
            condition = pred

        return self.with_new_sdf(self.spark_frame.filter(condition).select(self.spark_columns))

    def with_new_spark_column(
        self,
        column_label: Tuple,
        scol: spark.Column,
        *,
        field: Optional[InternalField] = None,
        keep_order: bool = True,
    ) -> "InternalFrame":
        """
        Copy the immutable InternalFrame with the updates by the specified Spark Column.

        :param column_label: the column label to be updated.
        :param scol: the new Spark Column
        :param field: the new InternalField for the data column.
            If not specified, the InternalField will be inferred from the spark Column.
        :return: the copied InternalFrame.
        """
        assert column_label in self.column_labels, column_label

        idx = self.column_labels.index(column_label)
        data_spark_columns = self.data_spark_columns.copy()
        data_spark_columns[idx] = scol
        data_fields = self.data_fields.copy()
        data_fields[idx] = field
        return self.with_new_columns(
            data_spark_columns, data_fields=data_fields, keep_order=keep_order
        )

    def select_column(self, column_label: Tuple) -> "InternalFrame":
        """
        Copy the immutable InternalFrame with the specified column.

        :param column_label: the column label to use.
        :return: the copied InternalFrame.
        """
        assert column_label in self.column_labels, column_label

        return self.copy(
            column_labels=[column_label],
            data_spark_columns=[self.spark_column_for(column_label)],
            data_fields=[self.field_for(column_label)],
            column_label_names=None,
        )

    def copy(
        self,
        *,
        spark_frame: Union[spark.DataFrame, _NoValueType] = _NoValue,
        index_spark_columns: Union[List[spark.Column], _NoValueType] = _NoValue,
        index_names: Union[Optional[List[Optional[Tuple]]], _NoValueType] = _NoValue,
        index_fields: Union[Optional[List[InternalField]], _NoValueType] = _NoValue,
        column_labels: Union[Optional[List[Tuple]], _NoValueType] = _NoValue,
        data_spark_columns: Union[Optional[List[spark.Column]], _NoValueType] = _NoValue,
        data_fields: Union[Optional[List[InternalField]], _NoValueType] = _NoValue,
        column_label_names: Union[Optional[List[Optional[Tuple]]], _NoValueType] = _NoValue,
    ) -> "InternalFrame":
        """
        Copy the immutable InternalFrame.

        :param spark_frame: the new Spark DataFrame. If not specified, the original one is used.
        :param index_spark_columns: the list of Spark Column.
                                    If not specified, the original ones are used.
        :param index_names: the index names. If not specified, the original ones are used.
        :param index_fields: the new InternalFields for the index columns.
                             If not specified, the original metadata are used.
        :param column_labels: the new column labels. If not specified, the original ones are used.
        :param data_spark_columns: the new Spark Columns.
                                   If not specified, the original ones are used.
        :param data_fields: the new InternalFields for the data columns.
                            If not specified, the original metadata are used.
        :param column_label_names: the new names of the column index levels.
                                   If not specified, the original ones are used.
        :return: the copied immutable InternalFrame.
        """
        if spark_frame is _NoValue:
            spark_frame = self.spark_frame
        if index_spark_columns is _NoValue:
            index_spark_columns = self.index_spark_columns
        if index_names is _NoValue:
            index_names = self.index_names
        if index_fields is _NoValue:
            index_fields = self.index_fields
        if column_labels is _NoValue:
            column_labels = self.column_labels
        if data_spark_columns is _NoValue:
            data_spark_columns = self.data_spark_columns
        if data_fields is _NoValue:
            data_fields = self.data_fields
        if column_label_names is _NoValue:
            column_label_names = self.column_label_names
        return InternalFrame(
            spark_frame=cast(spark.DataFrame, spark_frame),
            index_spark_columns=cast(List[spark.Column], index_spark_columns),
            index_names=cast(Optional[List[Optional[Tuple]]], index_names),
            index_fields=cast(Optional[List[InternalField]], index_fields),
            column_labels=cast(Optional[List[Tuple]], column_labels),
            data_spark_columns=cast(Optional[List[spark.Column]], data_spark_columns),
            data_fields=cast(Optional[List[InternalField]], data_fields),
            column_label_names=cast(Optional[List[Optional[Tuple]]], column_label_names),
        )

    @staticmethod
    def from_pandas(pdf: pd.DataFrame) -> "InternalFrame":
        """Create an immutable DataFrame from pandas DataFrame.

        :param pdf: :class:`pd.DataFrame`
        :return: the created immutable DataFrame
        """

        index_names = [
            name if name is None or isinstance(name, tuple) else (name,) for name in pdf.index.names
        ]

        columns = pdf.columns
        if isinstance(columns, pd.MultiIndex):
            column_labels = columns.tolist()
        else:
            column_labels = [(col,) for col in columns]
        column_label_names = [
            name if name is None or isinstance(name, tuple) else (name,) for name in columns.names
        ]

        (
            pdf,
            index_columns,
            index_fields,
            data_columns,
            data_fields,
        ) = InternalFrame.prepare_pandas_frame(pdf)

        schema = StructType([field.struct_field for field in index_fields + data_fields])

        sdf = default_session().createDataFrame(pdf, schema=schema)
        return InternalFrame(
            spark_frame=sdf,
            index_spark_columns=[scol_for(sdf, col) for col in index_columns],
            index_names=index_names,
            index_fields=index_fields,
            column_labels=column_labels,
            data_spark_columns=[scol_for(sdf, col) for col in data_columns],
            data_fields=data_fields,
            column_label_names=column_label_names,
        )

    @staticmethod
    def prepare_pandas_frame(
        pdf: pd.DataFrame, *, retain_index: bool = True
    ) -> Tuple[pd.DataFrame, List[str], List[InternalField], List[str], List[InternalField]]:
        """
        Prepare pandas DataFrame for creating Spark DataFrame.

        :param pdf: the pandas DataFrame to be prepared.
        :param retain_index: whether the indices should be retained.
        :return: the tuple of
            - the prepared pandas dataFrame
            - index column names for Spark DataFrame
            - the InternalFields for the index columns of the given pandas DataFrame
            - data column names for Spark DataFrame
            - the InternalFields for the data columns of the given pandas DataFrame

        >>> pdf = pd.DataFrame(
        ...    {("x", "a"): ['a', 'b', 'c'],
        ...     ("y", "b"): pd.Categorical(["i", "k", "j"], categories=["i", "j", "k"])},
        ...    index=[10, 20, 30])
        >>> prepared, index_columns, index_fields, data_columns, data_fields = (
        ...     InternalFrame.prepare_pandas_frame(pdf)
        ... )
        >>> prepared
           __index_level_0__ (x, a)  (y, b)
        0                 10      a       0
        1                 20      b       2
        2                 30      c       1
        >>> index_columns
        ['__index_level_0__']
        >>> index_fields
        [InternalField(dtype=int64,struct_field=StructField(__index_level_0__,LongType,false))]
        >>> data_columns
        ['(x, a)', '(y, b)']
        >>> data_fields  # doctest: +NORMALIZE_WHITESPACE
        [InternalField(dtype=object,struct_field=StructField((x, a),StringType,false)),
         InternalField(dtype=category,struct_field=StructField((y, b),ByteType,false))]
        """
        pdf = pdf.copy()

        data_columns = [name_like_string(col) for col in pdf.columns]
        pdf.columns = data_columns

        if retain_index:
            index_nlevels = pdf.index.nlevels
            index_columns = [SPARK_INDEX_NAME_FORMAT(i) for i in range(index_nlevels)]
            pdf.index.names = index_columns
            reset_index = pdf.reset_index()
        else:
            index_nlevels = 0
            index_columns = []
            reset_index = pdf

        index_dtypes = list(reset_index.dtypes)[:index_nlevels]
        data_dtypes = list(reset_index.dtypes)[index_nlevels:]

        for col, dtype in zip(reset_index.columns, reset_index.dtypes):
            spark_type = infer_pd_series_spark_type(reset_index[col], dtype)
            reset_index[col] = DataTypeOps(dtype, spark_type).prepare(reset_index[col])

        fields = [
            InternalField(
                dtype=dtype,
                struct_field=StructField(
                    name=name,
                    dataType=infer_pd_series_spark_type(col, dtype),
                    nullable=bool(col.isnull().any()),
                ),
            )
            for (name, col), dtype in zip(reset_index.iteritems(), index_dtypes + data_dtypes)
        ]

        return (
            reset_index,
            index_columns,
            fields[:index_nlevels],
            data_columns,
            fields[index_nlevels:],
        )


def _test() -> None:
    import os
    import doctest
    import sys
    from pyspark.sql import SparkSession
    import pyspark.pandas.internal

    os.chdir(os.environ["SPARK_HOME"])

    globs = pyspark.pandas.internal.__dict__.copy()
    globs["ps"] = pyspark.pandas
    spark = (
        SparkSession.builder.master("local[4]")
        .appName("pyspark.pandas.internal tests")
        .getOrCreate()
    )
    (failure_count, test_count) = doctest.testmod(
        pyspark.pandas.internal,
        globs=globs,
        optionflags=doctest.ELLIPSIS | doctest.NORMALIZE_WHITESPACE,
    )
    spark.stop()
    if failure_count:
        sys.exit(-1)


if __name__ == "__main__":
    _test()
