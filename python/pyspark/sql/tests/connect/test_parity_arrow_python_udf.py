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

from pyspark.errors import AnalysisException, PythonException
from pyspark.sql.functions import udf
from pyspark.sql.tests.connect.test_parity_udf import UDFParityTests
from pyspark.sql.tests.test_arrow_python_udf import PythonUDFArrowTestsMixin


class ArrowPythonUDFParityTests(UDFParityTests, PythonUDFArrowTestsMixin):
    @classmethod
    def setUpClass(cls):
        super(ArrowPythonUDFParityTests, cls).setUpClass()
        cls.spark.conf.set("spark.sql.execution.pythonUDF.arrow.enabled", "true")

    @classmethod
    def tearDownClass(cls):
        try:
            cls.spark.conf.unset("spark.sql.execution.pythonUDF.arrow.enabled")
        finally:
            super(ArrowPythonUDFParityTests, cls).tearDownClass()

    def test_named_arguments_negative(self):
        @udf("int")
        def test_udf(a, b):
            return a + b

        self.spark.udf.register("test_udf", test_udf)

        with self.assertRaisesRegex(
            AnalysisException,
            "DUPLICATE_ROUTINE_PARAMETER_ASSIGNMENT.DOUBLE_NAMED_ARGUMENT_REFERENCE",
        ):
            self.spark.sql("SELECT test_udf(a => id, a => id * 10) FROM range(2)").show()

        with self.assertRaisesRegex(AnalysisException, "UNEXPECTED_POSITIONAL_ARGUMENT"):
            self.spark.sql("SELECT test_udf(a => id, id * 10) FROM range(2)").show()

        with self.assertRaises(PythonException):
            self.spark.sql("SELECT test_udf(c => 'x') FROM range(2)").show()

        with self.assertRaises(PythonException):
            self.spark.sql("SELECT test_udf(id, a => id * 10) FROM range(2)").show()

    @unittest.skip("Spark Connect does not validate return type in client.")
    def test_err_return_type(self):
        super.test_err_return_type()


if __name__ == "__main__":
    import unittest
    from pyspark.sql.tests.connect.test_parity_arrow_python_udf import *  # noqa: F401

    try:
        import xmlrunner  # type: ignore[import]

        testRunner = xmlrunner.XMLTestRunner(output="target/test-reports", verbosity=2)
    except ImportError:
        testRunner = None
    unittest.main(testRunner=testRunner, verbosity=2)
