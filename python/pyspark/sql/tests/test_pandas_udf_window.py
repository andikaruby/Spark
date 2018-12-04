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

import unittest

from pyspark.sql.utils import AnalysisException
from pyspark.sql.window import Window
from pyspark.testing.sqlutils import ReusedSQLTestCase, have_pandas, have_pyarrow, \
    pandas_requirement_message, pyarrow_requirement_message
from pyspark.testing.utils import QuietTest


@unittest.skipIf(
    not have_pandas or not have_pyarrow,
    pandas_requirement_message or pyarrow_requirement_message)
class WindowPandasUDFTests(ReusedSQLTestCase):
    @property
    def data(self):
        from pyspark.sql.functions import array, explode, col, lit
        return self.spark.range(10).toDF('id') \
            .withColumn("vs", array([lit(i * 1.0) + col('id') for i in range(20, 30)])) \
            .withColumn("v", explode(col('vs'))) \
            .drop('vs') \
            .withColumn('w', lit(1.0))

    @property
    def python_plus_one(self):
        from pyspark.sql.functions import udf
        return udf(lambda v: v + 1, 'double')

    @property
    def pandas_scalar_time_two(self):
        from pyspark.sql.functions import pandas_udf
        return pandas_udf(lambda v: v * 2, 'double')

    @property
    def pandas_agg_mean_udf(self):
        from pyspark.sql.functions import pandas_udf, PandasUDFType

        @pandas_udf('double', PandasUDFType.GROUPED_AGG)
        def avg(v):
            return v.mean()
        return avg

    @property
    def pandas_agg_max_udf(self):
        from pyspark.sql.functions import pandas_udf, PandasUDFType

        @pandas_udf('double', PandasUDFType.GROUPED_AGG)
        def max(v):
            return v.max()
        return max

    @property
    def pandas_agg_min_udf(self):
        from pyspark.sql.functions import pandas_udf, PandasUDFType

        @pandas_udf('double', PandasUDFType.GROUPED_AGG)
        def min(v):
            return v.min()
        return min

    @property
    def unbounded_window(self):
        return Window.partitionBy('id') \
            .rowsBetween(Window.unboundedPreceding, Window.unboundedFollowing)

    @property
    def ordered_window(self):
        return Window.partitionBy('id').orderBy('v')

    @property
    def unpartitioned_window(self):
        return Window.partitionBy()

    def test_simple(self):
        from pyspark.sql.functions import mean

        df = self.data
        w = self.unbounded_window

        mean_udf = self.pandas_agg_mean_udf

        result1 = df.withColumn('mean_v', mean_udf(df['v']).over(w))
        expected1 = df.withColumn('mean_v', mean(df['v']).over(w))

        result2 = df.select(mean_udf(df['v']).over(w))
        expected2 = df.select(mean(df['v']).over(w))

        self.assertPandasEqual(expected1.toPandas(), result1.toPandas())
        self.assertPandasEqual(expected2.toPandas(), result2.toPandas())

    def test_multiple_udfs(self):
        from pyspark.sql.functions import max, min, mean

        df = self.data
        w = self.unbounded_window

        result1 = df.withColumn('mean_v', self.pandas_agg_mean_udf(df['v']).over(w)) \
                    .withColumn('max_v', self.pandas_agg_max_udf(df['v']).over(w)) \
                    .withColumn('min_w', self.pandas_agg_min_udf(df['w']).over(w))

        expected1 = df.withColumn('mean_v', mean(df['v']).over(w)) \
                      .withColumn('max_v', max(df['v']).over(w)) \
                      .withColumn('min_w', min(df['w']).over(w))

        self.assertPandasEqual(expected1.toPandas(), result1.toPandas())

    def test_replace_existing(self):
        from pyspark.sql.functions import mean

        df = self.data
        w = self.unbounded_window

        result1 = df.withColumn('v', self.pandas_agg_mean_udf(df['v']).over(w))
        expected1 = df.withColumn('v', mean(df['v']).over(w))

        self.assertPandasEqual(expected1.toPandas(), result1.toPandas())

    def test_mixed_sql(self):
        from pyspark.sql.functions import mean

        df = self.data
        w = self.unbounded_window
        mean_udf = self.pandas_agg_mean_udf

        result1 = df.withColumn('v', mean_udf(df['v'] * 2).over(w) + 1)
        expected1 = df.withColumn('v', mean(df['v'] * 2).over(w) + 1)

        self.assertPandasEqual(expected1.toPandas(), result1.toPandas())

    def test_mixed_udf(self):
        from pyspark.sql.functions import mean

        df = self.data
        w = self.unbounded_window

        plus_one = self.python_plus_one
        time_two = self.pandas_scalar_time_two
        mean_udf = self.pandas_agg_mean_udf

        result1 = df.withColumn(
            'v2',
            plus_one(mean_udf(plus_one(df['v'])).over(w)))
        expected1 = df.withColumn(
            'v2',
            plus_one(mean(plus_one(df['v'])).over(w)))

        result2 = df.withColumn(
            'v2',
            time_two(mean_udf(time_two(df['v'])).over(w)))
        expected2 = df.withColumn(
            'v2',
            time_two(mean(time_two(df['v'])).over(w)))

        self.assertPandasEqual(expected1.toPandas(), result1.toPandas())
        self.assertPandasEqual(expected2.toPandas(), result2.toPandas())

    def test_without_partitionBy(self):
        from pyspark.sql.functions import mean

        df = self.data
        w = self.unpartitioned_window
        mean_udf = self.pandas_agg_mean_udf

        result1 = df.withColumn('v2', mean_udf(df['v']).over(w))
        expected1 = df.withColumn('v2', mean(df['v']).over(w))

        result2 = df.select(mean_udf(df['v']).over(w))
        expected2 = df.select(mean(df['v']).over(w))

        self.assertPandasEqual(expected1.toPandas(), result1.toPandas())
        self.assertPandasEqual(expected2.toPandas(), result2.toPandas())

    def test_mixed_sql_and_udf(self):
        from pyspark.sql.functions import max, min, rank, col

        df = self.data
        w = self.unbounded_window
        ow = self.ordered_window
        max_udf = self.pandas_agg_max_udf
        min_udf = self.pandas_agg_min_udf

        result1 = df.withColumn('v_diff', max_udf(df['v']).over(w) - min_udf(df['v']).over(w))
        expected1 = df.withColumn('v_diff', max(df['v']).over(w) - min(df['v']).over(w))

        # Test mixing sql window function and window udf in the same expression
        result2 = df.withColumn('v_diff', max_udf(df['v']).over(w) - min(df['v']).over(w))
        expected2 = expected1

        # Test chaining sql aggregate function and udf
        result3 = df.withColumn('max_v', max_udf(df['v']).over(w)) \
                    .withColumn('min_v', min(df['v']).over(w)) \
                    .withColumn('v_diff', col('max_v') - col('min_v')) \
                    .drop('max_v', 'min_v')
        expected3 = expected1

        # Test mixing sql window function and udf
        result4 = df.withColumn('max_v', max_udf(df['v']).over(w)) \
                    .withColumn('rank', rank().over(ow))
        expected4 = df.withColumn('max_v', max(df['v']).over(w)) \
                      .withColumn('rank', rank().over(ow))

        self.assertPandasEqual(expected1.toPandas(), result1.toPandas())
        self.assertPandasEqual(expected2.toPandas(), result2.toPandas())
        self.assertPandasEqual(expected3.toPandas(), result3.toPandas())
        self.assertPandasEqual(expected4.toPandas(), result4.toPandas())

    def test_array_type(self):
        from pyspark.sql.functions import pandas_udf, PandasUDFType

        df = self.data
        w = self.unbounded_window

        array_udf = pandas_udf(lambda x: [1.0, 2.0], 'array<double>', PandasUDFType.GROUPED_AGG)
        result1 = df.withColumn('v2', array_udf(df['v']).over(w))
        self.assertEquals(result1.first()['v2'], [1.0, 2.0])

    def test_invalid_args(self):
        from pyspark.sql.functions import pandas_udf, PandasUDFType

        df = self.data
        w = self.unbounded_window
        ow = self.ordered_window
        mean_udf = self.pandas_agg_mean_udf

        with QuietTest(self.sc):
            with self.assertRaisesRegexp(
                    AnalysisException,
                    '.*not supported within a window function'):
                foo_udf = pandas_udf(lambda x: x, 'v double', PandasUDFType.GROUPED_MAP)
                df.withColumn('v2', foo_udf(df['v']).over(w))

        with QuietTest(self.sc):
            with self.assertRaisesRegexp(
                    AnalysisException,
                    '.*Only unbounded window frame is supported.*'):
                df.withColumn('mean_v', mean_udf(df['v']).over(ow))


if __name__ == "__main__":
    from pyspark.sql.tests.test_pandas_udf_window import *

    try:
        import xmlrunner
        testRunner = xmlrunner.XMLTestRunner(output='target/test-reports')
    except ImportError:
        testRunner = None
    unittest.main(testRunner=testRunner, verbosity=2)
