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
# NO CHECKED-IN PROTOBUF GENCODE
# source: spark/connect/ml.proto
# Protobuf Python Version: 5.28.3
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import runtime_version as _runtime_version
from google.protobuf import symbol_database as _symbol_database
from google.protobuf.internal import builder as _builder

_runtime_version.ValidateProtobufRuntimeVersion(
    _runtime_version.Domain.PUBLIC, 5, 28, 3, "", "spark/connect/ml.proto"
)
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from pyspark.sql.connect.proto import expressions_pb2 as spark_dot_connect_dot_expressions__pb2
from pyspark.sql.connect.proto import relations_pb2 as spark_dot_connect_dot_relations__pb2
from pyspark.sql.connect.proto import ml_common_pb2 as spark_dot_connect_dot_ml__common__pb2


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(
    b'\n\x16spark/connect/ml.proto\x12\rspark.connect\x1a\x1fspark/connect/expressions.proto\x1a\x1dspark/connect/relations.proto\x1a\x1dspark/connect/ml_common.proto"\xda\x07\n\tMlCommand\x12\x30\n\x03\x66it\x18\x01 \x01(\x0b\x32\x1c.spark.connect.MlCommand.FitH\x00R\x03\x66it\x12\x39\n\nfetch_attr\x18\x02 \x01(\x0b\x32\x18.spark.connect.FetchAttrH\x00R\tfetchAttr\x12\x39\n\x06\x64\x65lete\x18\x03 \x01(\x0b\x32\x1f.spark.connect.MlCommand.DeleteH\x00R\x06\x64\x65lete\x12\x37\n\x05write\x18\x04 \x01(\x0b\x32\x1f.spark.connect.MlCommand.WriterH\x00R\x05write\x12\x35\n\x04read\x18\x05 \x01(\x0b\x32\x1f.spark.connect.MlCommand.ReaderH\x00R\x04read\x1a\xa2\x01\n\x03\x46it\x12\x37\n\testimator\x18\x01 \x01(\x0b\x32\x19.spark.connect.MlOperatorR\testimator\x12/\n\x06params\x18\x02 \x01(\x0b\x32\x17.spark.connect.MlParamsR\x06params\x12\x31\n\x07\x64\x61taset\x18\x03 \x01(\x0b\x32\x17.spark.connect.RelationR\x07\x64\x61taset\x1a;\n\x06\x44\x65lete\x12\x31\n\x07obj_ref\x18\x01 \x01(\x0b\x32\x18.spark.connect.ObjectRefR\x06objRef\x1a\xf2\x02\n\x06Writer\x12\x37\n\x08operator\x18\x01 \x01(\x0b\x32\x19.spark.connect.MlOperatorH\x00R\x08operator\x12\x33\n\x07obj_ref\x18\x02 \x01(\x0b\x32\x18.spark.connect.ObjectRefH\x00R\x06objRef\x12/\n\x06params\x18\x03 \x01(\x0b\x32\x17.spark.connect.MlParamsR\x06params\x12\x12\n\x04path\x18\x04 \x01(\tR\x04path\x12)\n\x10should_overwrite\x18\x05 \x01(\x08R\x0fshouldOverwrite\x12\x46\n\x07options\x18\x06 \x03(\x0b\x32,.spark.connect.MlCommand.Writer.OptionsEntryR\x07options\x1a:\n\x0cOptionsEntry\x12\x10\n\x03key\x18\x01 \x01(\tR\x03key\x12\x14\n\x05value\x18\x02 \x01(\tR\x05value:\x02\x38\x01\x42\x06\n\x04type\x1aS\n\x06Reader\x12\x35\n\x08operator\x18\x01 \x01(\x0b\x32\x19.spark.connect.MlOperatorR\x08operator\x12\x12\n\x04path\x18\x02 \x01(\tR\x04pathB\t\n\x07\x63ommand"\xcd\x02\n\x0fMlCommandResult\x12,\n\x05param\x18\x01 \x01(\x0b\x32\x14.spark.connect.ParamH\x00R\x05param\x12T\n\roperator_info\x18\x02 \x01(\x0b\x32-.spark.connect.MlCommandResult.MlOperatorInfoH\x00R\x0coperatorInfo\x1a\xa6\x01\n\x0eMlOperatorInfo\x12\x33\n\x07obj_ref\x18\x01 \x01(\x0b\x32\x18.spark.connect.ObjectRefH\x00R\x06objRef\x12\x14\n\x04name\x18\x02 \x01(\tH\x00R\x04name\x12\x10\n\x03uid\x18\x03 \x01(\tR\x03uid\x12/\n\x06params\x18\x04 \x01(\x0b\x32\x17.spark.connect.MlParamsR\x06paramsB\x06\n\x04typeB\r\n\x0bresult_typeB"\n\x1eorg.apache.spark.connect.protoP\x01\x62\x06proto3'
)

_globals = globals()
_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, _globals)
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, "pyspark.sql.connect.proto.ml_pb2", _globals)
if not _descriptor._USE_C_DESCRIPTORS:
    _globals["DESCRIPTOR"]._loaded_options = None
    _globals["DESCRIPTOR"]._serialized_options = b"\n\036org.apache.spark.connect.protoP\001"
    _globals["_MLCOMMAND_WRITER_OPTIONSENTRY"]._loaded_options = None
    _globals["_MLCOMMAND_WRITER_OPTIONSENTRY"]._serialized_options = b"8\001"
    _globals["_MLCOMMAND"]._serialized_start = 137
    _globals["_MLCOMMAND"]._serialized_end = 1123
    _globals["_MLCOMMAND_FIT"]._serialized_start = 431
    _globals["_MLCOMMAND_FIT"]._serialized_end = 593
    _globals["_MLCOMMAND_DELETE"]._serialized_start = 595
    _globals["_MLCOMMAND_DELETE"]._serialized_end = 654
    _globals["_MLCOMMAND_WRITER"]._serialized_start = 657
    _globals["_MLCOMMAND_WRITER"]._serialized_end = 1027
    _globals["_MLCOMMAND_WRITER_OPTIONSENTRY"]._serialized_start = 961
    _globals["_MLCOMMAND_WRITER_OPTIONSENTRY"]._serialized_end = 1019
    _globals["_MLCOMMAND_READER"]._serialized_start = 1029
    _globals["_MLCOMMAND_READER"]._serialized_end = 1112
    _globals["_MLCOMMANDRESULT"]._serialized_start = 1126
    _globals["_MLCOMMANDRESULT"]._serialized_end = 1459
    _globals["_MLCOMMANDRESULT_MLOPERATORINFO"]._serialized_start = 1278
    _globals["_MLCOMMANDRESULT_MLOPERATORINFO"]._serialized_end = 1444
# @@protoc_insertion_point(module_scope)
