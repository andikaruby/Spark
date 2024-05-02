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

from pyspark.sql.tests.pandas.test_pandas_grouped_map import GroupedApplyInPandasTestsMixin
from pyspark.testing.connectutils import ReusedConnectTestCase


class GroupedApplyInPandasTests(GroupedApplyInPandasTestsMixin, ReusedConnectTestCase):
    # TODO(SPARK-42857): Support CreateDataFrame from Decimal128
    @unittest.skip("Fails in Spark Connect, should enable.")
    def test_supported_types(self):
        super().test_supported_types()

    @unittest.skip("Fails in Spark Connect, should enable.")
    def test_wrong_return_type(self):
        super().test_wrong_return_type()

    @unittest.skip("Fails in Spark Connect, should enable.")
    def test_unsupported_types(self):
        super().test_unsupported_types()

    @unittest.skip("Fails in Spark Connect, should enable.")
    def test_apply_in_pandas_returning_incompatible_type(self):
        super().test_apply_in_pandas_returning_incompatible_type()


if __name__ == "__main__":
    from pyspark.sql.tests.connect.test_parity_pandas_grouped_map import *  # noqa: F401

    try:
        import xmlrunner

        testRunner = xmlrunner.XMLTestRunner(output="target/test-reports", verbosity=2)
    except ImportError:
        testRunner = None
    unittest.main(testRunner=testRunner, verbosity=2)
