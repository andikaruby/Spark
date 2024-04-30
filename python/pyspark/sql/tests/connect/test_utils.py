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
import os
import unittest

from pyspark.testing.connectutils import ReusedConnectTestCase
from pyspark.sql.tests.test_utils import UtilsTestsMixin


class ConnectUtilsTests(ReusedConnectTestCase, UtilsTestsMixin):
    @unittest.skipIf(
        "SPARK_SKIP_CONNECT_COMPAT_TESTS" in os.environ, "Failed with different Client <> Server"
    )
    def test_assert_approx_equal_decimaltype_custom_rtol_pass(self):
        super(ConnectUtilsTests, self).test_assert_approx_equal_decimaltype_custom_rtol_pass()


if __name__ == "__main__":
    import unittest
    from pyspark.sql.tests.connect.test_utils import *  # noqa: F401

    try:
        import xmlrunner

        testRunner = xmlrunner.XMLTestRunner(output="target/test-reports", verbosity=2)
    except ImportError:
        testRunner = None
    unittest.main(testRunner=testRunner, verbosity=2)
