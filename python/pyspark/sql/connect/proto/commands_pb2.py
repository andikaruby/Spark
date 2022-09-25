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
# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: spark/connect/commands.proto
"""Generated protocol buffer code."""
from google.protobuf.internal import builder as _builder
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database

# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from pyspark.sql.connect.proto import types_pb2 as spark_dot_connect_dot_types__pb2


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(
    b'\n\x1cspark/connect/commands.proto\x12\rspark.connect\x1a\x19spark/connect/types.proto"i\n\x07\x43ommand\x12N\n\x0f\x63reate_function\x18\x01 \x01(\x0b\x32#.spark.connect.CreateScalarFunctionH\x00R\x0e\x63reateFunctionB\x0e\n\x0c\x63ommand_type"\x8f\x04\n\x14\x43reateScalarFunction\x12\x14\n\x05parts\x18\x01 \x03(\tR\x05parts\x12P\n\x08language\x18\x02 \x01(\x0e\x32\x34.spark.connect.CreateScalarFunction.FunctionLanguageR\x08language\x12\x1c\n\ttemporary\x18\x03 \x01(\x08R\ttemporary\x12:\n\x0e\x61rgument_types\x18\x04 \x03(\x0b\x32\x13.spark.connect.TypeR\rargumentTypes\x12\x34\n\x0breturn_type\x18\x05 \x01(\x0b\x32\x13.spark.connect.TypeR\nreturnType\x12\x31\n\x13serialized_function\x18\x06 \x01(\x0cH\x00R\x12serializedFunction\x12\'\n\x0eliteral_string\x18\x07 \x01(\tH\x00R\rliteralString"\x8b\x01\n\x10\x46unctionLanguage\x12!\n\x1d\x46UNCTION_LANGUAGE_UNSPECIFIED\x10\x00\x12\x19\n\x15\x46UNCTION_LANGUAGE_SQL\x10\x01\x12\x1c\n\x18\x46UNCTION_LANGUAGE_PYTHON\x10\x02\x12\x1b\n\x17\x46UNCTION_LANGUAGE_SCALA\x10\x03\x42\x15\n\x13\x66unction_definitionBM\n\x1eorg.apache.spark.connect.protoP\x01Z)github.com/databricks/spark-connect/protob\x06proto3'
)

_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, globals())
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, "spark.connect.commands_pb2", globals())
if _descriptor._USE_C_DESCRIPTORS == False:

    DESCRIPTOR._options = None
    DESCRIPTOR._serialized_options = (
        b"\n\036org.apache.spark.connect.protoP\001Z)github.com/databricks/spark-connect/proto"
    )
    _COMMAND._serialized_start = 74
    _COMMAND._serialized_end = 179
    _CREATESCALARFUNCTION._serialized_start = 182
    _CREATESCALARFUNCTION._serialized_end = 709
    _CREATESCALARFUNCTION_FUNCTIONLANGUAGE._serialized_start = 547
    _CREATESCALARFUNCTION_FUNCTIONLANGUAGE._serialized_end = 686
# @@protoc_insertion_point(module_scope)
