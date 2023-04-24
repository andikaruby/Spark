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
import json


ERROR_CLASSES_JSON = """
{
  "ARGUMENT_REQUIRED": {
    "message": [
      "Argument `<arg_name>` is required when <condition>."
    ]
  },
  "ATTRIBUTE_NOT_CALLABLE" : {
    "message" : [
      "Attribute `<attr_name>` in provided object `<obj_name>` is not callable."
    ]
  },
  "CANNOT_ACCESS_TO_DUNDER": {
    "message": [
      "Dunder(double underscore) attribute is for internal use only."
    ]
  },
  "CANNOT_APPLY_IN_FOR_COLUMN": {
    "message": [
      "Cannot apply 'in' operator against a column: please use 'contains' in a string column or 'array_contains' function for an array column."
    ]
  },
  "CANNOT_BE_EMPTY": {
    "message": [
      "At least one <item> must be specified."
    ]
  },
  "CANNOT_BE_NONE": {
    "message": [
      "Argument `<arg_name>` can not be None."
    ]
  },
  "CANNOT_CONVERT_COLUMN_INTO_BOOL": {
    "message": [
      "Cannot convert column into bool: please use '&' for 'and', '|' for 'or', '~' for 'not' when building DataFrame boolean expressions."
    ]
  },
  "CANNOT_INFER_ARRAY_TYPE": {
    "message": [
      "Can not infer Array Type from an list with None as the first element."
    ]
  },
  "CANNOT_PARSE_DATATYPE": {
    "message": [
      "Unable to parse datatype from schema. <error>."
    ]
  },
  "CANNOT_PROVIDE_METADATA": {
    "message": [
      "metadata can only be provided for a single column."
    ]
  },
  "CANNOT_SET_TOGETHER": {
    "message": [
      "<arg_list> should not be set together."
    ]
  },
  "CANNOT_SPECIFY_RETURN_TYPE_FOR_UDF": {
    "message": [
      "returnType can not be specified when `<arg_name>` is a user-defined function, but got <return_type>."
    ]
  },
  "COLUMN_IN_LIST": {
    "message": [
      "`<func_name>` does not allow a Column in a list."
    ]
  },
  "DISALLOWED_TYPE_FOR_CONTAINER" : {
    "message" : [
      "Argument `<arg_name>`(type: <arg_type>) should only contain a type in [<allowed_types>], got <return_type>"
    ]
  },
  "HIGHER_ORDER_FUNCTION_SHOULD_RETURN_COLUMN" : {
    "message" : [
      "Function `<func_name>` should return Column, got <return_type>."
    ]
  },
  "INVALID_CALL_ON_UNRESOLVED_OBJECT": {
    "message": [
      "Invalid call to `<func_name>` on unresolved object."
    ]
  },
  "INVALID_ITEM_FOR_CONTAINER": {
    "message": [
      "All items in `<arg_name>` should be in <allowed_types>, got <item_type>."
    ]
  },
  "INVALID_RETURN_TYPE_FOR_PANDAS_UDF": {
    "message": [
      "Pandas UDF should return StructType for <eval_type>, got <return_type>."
    ]
  },
  "INVALID_TIMEOUT_TIMESTAMP" : {
    "message" : [
      "Timeout timestamp (<timestamp>) cannot be earlier than the current watermark (<watermark>)."
    ]
  },
  "INVALID_UDF_EVAL_TYPE" : {
    "message" : [
      "Eval type for UDF must be SQL_BATCHED_UDF, SQL_SCALAR_PANDAS_UDF, SQL_SCALAR_PANDAS_ITER_UDF or SQL_GROUPED_AGG_PANDAS_UDF."
    ]
  },
  "INVALID_WHEN_USAGE": {
    "message": [
      "when() can only be applied on a Column previously generated by when() function, and cannot be applied once otherwise() is applied."
    ]
  },
  "JVM_ATTRIBUTE_NOT_SUPPORTED" : {
    "message" : [
      "Attribute `<attr_name>` is not supported in Spark Connect as it depends on the JVM. If you need to use this attribute, do not use Spark Connect when creating your session."
    ]
  },
  "LENGTH_SHOULD_BE_THE_SAME" : {
    "message" : [
      "<arg1> and <arg2> should be of the same length, got <arg1_length> and <arg2_length>."
    ]
  },
  "MISSING_VALID_PLAN" : {
    "message" : [
      "Argument to <operator> does not contain a valid plan."
    ]
  },
  "MIXED_TYPE_REPLACEMENT" : {
    "message" : [
      "Mixed type replacements are not supported."
    ]
  },
  "NEGATIVE_VALUE" : {
    "message" : [
      "Value for `<arg_name>` must be greater than or equal to 0, got '<arg_value>'."
    ]
  },
  "NOT_BOOL" : {
    "message" : [
      "Argument `<arg_name>` should be a bool, got <arg_type>."
    ]
  },
  "NOT_BOOL_OR_DICT_OR_FLOAT_OR_INT_OR_LIST_OR_STR_OR_TUPLE" : {
    "message" : [
      "Argument `<arg_name>` should be a bool, dict, float, int, str or tuple, got <arg_type>."
    ]
  },
  "NOT_BOOL_OR_DICT_OR_FLOAT_OR_INT_OR_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a bool, dict, float, int or str, got <arg_type>."
    ]
  },
  "NOT_BOOL_OR_FLOAT_OR_INT" : {
    "message" : [
      "Argument `<arg_name>` should be a bool, float or str, got <arg_type>."
    ]
  },
  "NOT_BOOL_OR_FLOAT_OR_INT_OR_LIST_OR_NONE_OR_STR_OR_TUPLE" : {
    "message" : [
      "Argument `<arg_name>` should be a bool, float, int, list, None, str or tuple, got <arg_type>."
    ]
  },
  "NOT_BOOL_OR_FLOAT_OR_INT_OR_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a bool, float, int or str, got <arg_type>."
    ]
  },
  "NOT_BOOL_OR_LIST" : {
    "message" : [
      "Argument `<arg_name>` should be a bool or list, got <arg_type>."
    ]
  },
  "NOT_BOOL_OR_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a bool or str, got <arg_type>."
    ]
  },
  "NOT_CALLABLE" : {
    "message" : [
      "Argument `<arg_name>` should be a callable, got <arg_type>."
    ]
  },
  "NOT_COLUMN" : {
    "message" : [
      "Argument `<arg_name>` should be a Column, got <arg_type>."
    ]
  },
  "NOT_COLUMN_OR_DATATYPE_OR_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a Column, str or DataType, but got <arg_type>."
    ]
  },
  "NOT_COLUMN_OR_FLOAT_OR_INT_OR_LIST_OR_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a column, float, integer, list or string, got <arg_type>."
    ]
  },
  "NOT_COLUMN_OR_INT" : {
    "message" : [
      "Argument `<arg_name>` should be a Column or int, got <arg_type>."
    ]
  },
  "NOT_COLUMN_OR_INT_OR_LIST_OR_STR_OR_TUPLE" : {
    "message" : [
      "Argument `<arg_name>` should be a Column, int, list, str or tuple, got <arg_type>."
    ]
  },
  "NOT_COLUMN_OR_INT_OR_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a Column, int or str, got <arg_type>."
    ]
  },
  "NOT_COLUMN_OR_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a Column or str, got <arg_type>."
    ]
  },
  "NOT_DATAFRAME" : {
    "message" : [
      "Argument `<arg_name>` should be a DataFrame, got <arg_type>."
    ]
  },
  "NOT_DATATYPE_OR_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a DataType or str, got <arg_type>."
    ]
  },
  "NOT_DICT" : {
    "message" : [
      "Argument `<arg_name>` should be a dict, got <arg_type>."
    ]
  },
  "NOT_EXPRESSION" : {
    "message" : [
      "Argument `<arg_name>` should be a Expression, got <arg_type>."
    ]
  },
  "NOT_FLOAT_OR_INT" : {
    "message" : [
      "Argument `<arg_name>` should be a float or int, got <arg_type>."
    ]
  },
  "NOT_FLOAT_OR_INT_OR_LIST_OR_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a float, int, list or str, got <arg_type>."
    ]
  },
  "NOT_INT" : {
    "message" : [
      "Argument `<arg_name>` should be an int, got <arg_type>."
    ]
  },
  "NOT_ITERABLE" : {
    "message" : [
      "<objectName> is not iterable."
    ]
  },
  "NOT_LIST_OF_COLUMN" : {
    "message" : [
      "Argument `<arg_name>` should be a list[Column]."
    ]
  },
  "NOT_LIST_OF_FLOAT_OR_INT" : {
    "message" : [
      "Argument `<arg_name>` should be a list[float, int], got <arg_type>."
    ]
  },
  "NOT_LIST_OF_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a list[str], got <arg_type>."
    ]
  },
  "NOT_LIST_OR_STR_OR_TUPLE" : {
    "message" : [
      "Argument `<arg_name>` should be a list, str or tuple, got <arg_type>."
    ]
  },
  "NOT_LIST_OR_TUPLE" : {
    "message" : [
      "Argument `<arg_name>` should be a list or tuple, got <arg_type>."
    ]
  },
  "NOT_OBSERVATION_OR_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a Observation or str, got <arg_type>."
    ]
  },
  "NOT_SAME_TYPE" : {
    "message" : [
      "Argument `<arg_name1>` and `<arg_name2>` should be the same type, got <arg_type1> and <arg_type2>."
    ]
  },
  "NOT_STR" : {
    "message" : [
      "Argument `<arg_name>` should be a str, got <arg_type>."
    ]
  },
  "NOT_STR_OR_LIST_OF_RDD" : {
    "message" : [
      "Argument `<arg_name>` should be a str or list[RDD], got <arg_type>."
    ]
  },
  "NOT_STR_OR_STRUCT" : {
    "message" : [
      "Argument `<arg_name>` should be a str or structType, got <arg_type>."
    ]
  },
  "NOT_WINDOWSPEC" : {
    "message" : [
      "Argument `<arg_name>` should be a WindowSpec, got <arg_type>."
    ]
  },
  "NO_ACTIVE_SESSION" : {
    "message" : [
      "No active Spark session found. Please create a new Spark session before running the code."
    ]
  },
  "ONLY_ALLOWED_FOR_SINGLE_COLUMN" : {
    "message" : [
      "Argument `<arg_name>` can only be provided for a single column."
    ]
  },
  "ONLY_ALLOW_SINGLE_TRIGGER" : {
    "message" : [
      "Only a single trigger is allowed."
    ]
  },
  "SLICE_WITH_STEP" : {
    "message" : [
      "Slice with step is not supported."
    ]
  },
  "STATE_NOT_EXISTS" : {
    "message" : [
      "State is either not defined or has already been removed."
    ]
  },
  "UNSUPPORTED_DATA_TYPE" : {
    "message" : [
      "Unsupported DataType `<data_type>`."
    ]
  },
  "UNSUPPORTED_LITERAL" : {
    "message" : [
      "Unsupported Literal '<literal>'."
    ]
  },
  "UNSUPPORTED_NUMPY_ARRAY_SCALAR" : {
    "message" : [
      "The type of array scalar '<dtype>' is not supported."
    ]
  },
  "UNSUPPORTED_PARAM_TYPE_FOR_HIGHER_ORDER_FUNCTION" : {
    "message" : [
      "Function `<func_name>` should use only POSITIONAL or POSITIONAL OR KEYWORD arguments."
    ]
  },
  "VALUE_NOT_ANY_OR_ALL" : {
    "message" : [
      "Value for `<arg_name>` must be 'any' or 'all', got '<arg_value>'."
    ]
  },
  "VALUE_NOT_NON_EMPTY_STR" : {
    "message" : [
      "Value for `<arg_name>` must be a non empty string, got '<arg_value>'."
    ]
  },
  "VALUE_NOT_PEARSON" : {
    "message" : [
      "Value for `<arg_name>` only supports the 'pearson', got '<arg_value>'."
    ]
  },
  "VALUE_NOT_POSITIVE" : {
    "message" : [
      "Value for `<arg_name>` must be positive, got '<arg_value>'."
    ]
  },
  "VALUE_NOT_TRUE" : {
    "message" : [
      "Value for `<arg_name>` must be True, got '<arg_value>'."
    ]
  },
  "VALUE_OUT_OF_BOUND" : {
    "message" : [
      "Value for `<arg_name>` must be between <min> and <max>."
    ]
  },
  "WRONG_NUM_ARGS_FOR_HIGHER_ORDER_FUNCTION" : {
    "message" : [
      "Function `<func_name>` should take between 1 and 3 arguments, but provided function takes <num_args>."
    ]
  },
  "WRONG_NUM_COLUMNS" : {
    "message" : [
      "Function `<func_name>` should take at least <num_cols> columns."
    ]
  }
}
"""

ERROR_CLASSES_MAP = json.loads(ERROR_CLASSES_JSON)
