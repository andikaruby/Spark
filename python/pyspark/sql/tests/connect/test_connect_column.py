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

from pyspark.sql.tests.connect.test_connect_basic import SparkConnectSQLTestCase
from pyspark.testing.sqlutils import have_pandas

if have_pandas:
    pass

if have_pandas:
    from pyspark.sql.connect.session import SparkSession as RemoteSparkSession
    from pyspark.sql.connect.client import ChannelBuilder
    from pyspark.sql.connect.function_builder import udf
    from pyspark.sql.connect.functions import lit, col


class SparkConnectTests(SparkConnectSQLTestCase):
    def test_column_operator(self):
        # SPARK-41351: Column needs to support !=
        df = self.connect.range(10)
        self.assertEqual(9, len(df.filter(df.id != lit(1)).collect()))


if __name__ == "__main__":
    from pyspark.sql.tests.connect.test_connect_column import *  # noqa: F401

    try:
        import xmlrunner  # type: ignore

        testRunner = xmlrunner.XMLTestRunner(output="target/test-reports", verbosity=2)
    except ImportError:
        testRunner = None

    unittest.main(testRunner=testRunner, verbosity=2)
