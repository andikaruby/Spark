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
# source: spark/connect/ml_common.proto
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database

# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from pyspark.sql.connect.proto import expressions_pb2 as spark_dot_connect_dot_expressions__pb2


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(
    b'\n\x1dspark/connect/ml_common.proto\x12\rspark.connect\x1a\x1fspark/connect/expressions.proto"\xdd\x02\n\x08MlParams\x12;\n\x06params\x18\x01 \x03(\x0b\x32#.spark.connect.MlParams.ParamsEntryR\x06params\x12Q\n\x0e\x64\x65\x66\x61ult_params\x18\x02 \x03(\x0b\x32*.spark.connect.MlParams.DefaultParamsEntryR\rdefaultParams\x1a\\\n\x0bParamsEntry\x12\x10\n\x03key\x18\x01 \x01(\tR\x03key\x12\x37\n\x05value\x18\x02 \x01(\x0b\x32!.spark.connect.Expression.LiteralR\x05value:\x02\x38\x01\x1a\x63\n\x12\x44\x65\x66\x61ultParamsEntry\x12\x10\n\x03key\x18\x01 \x01(\tR\x03key\x12\x37\n\x05value\x18\x02 \x01(\x0b\x32!.spark.connect.Expression.LiteralR\x05value:\x02\x38\x01"\xd4\x01\n\x07MlStage\x12\x12\n\x04name\x18\x01 \x01(\tR\x04name\x12/\n\x06params\x18\x02 \x01(\x0b\x32\x17.spark.connect.MlParamsR\x06params\x12\x10\n\x03uid\x18\x03 \x01(\tR\x03uid\x12\x34\n\x04type\x18\x04 \x01(\x0e\x32 .spark.connect.MlStage.StageTypeR\x04type"<\n\tStageType\x12\x0f\n\x0bUNSPECIFIED\x10\x00\x12\r\n\tESTIMATOR\x10\x01\x12\x0f\n\x0bTRANSFORMER\x10\x02"\x1a\n\x08ModelRef\x12\x0e\n\x02id\x18\x01 \x01(\tR\x02idB"\n\x1eorg.apache.spark.connect.protoP\x01\x62\x06proto3'
)


_MLPARAMS = DESCRIPTOR.message_types_by_name["MlParams"]
_MLPARAMS_PARAMSENTRY = _MLPARAMS.nested_types_by_name["ParamsEntry"]
_MLPARAMS_DEFAULTPARAMSENTRY = _MLPARAMS.nested_types_by_name["DefaultParamsEntry"]
_MLSTAGE = DESCRIPTOR.message_types_by_name["MlStage"]
_MODELREF = DESCRIPTOR.message_types_by_name["ModelRef"]
_MLSTAGE_STAGETYPE = _MLSTAGE.enum_types_by_name["StageType"]
MlParams = _reflection.GeneratedProtocolMessageType(
    "MlParams",
    (_message.Message,),
    {
        "ParamsEntry": _reflection.GeneratedProtocolMessageType(
            "ParamsEntry",
            (_message.Message,),
            {
                "DESCRIPTOR": _MLPARAMS_PARAMSENTRY,
                "__module__": "spark.connect.ml_common_pb2"
                # @@protoc_insertion_point(class_scope:spark.connect.MlParams.ParamsEntry)
            },
        ),
        "DefaultParamsEntry": _reflection.GeneratedProtocolMessageType(
            "DefaultParamsEntry",
            (_message.Message,),
            {
                "DESCRIPTOR": _MLPARAMS_DEFAULTPARAMSENTRY,
                "__module__": "spark.connect.ml_common_pb2"
                # @@protoc_insertion_point(class_scope:spark.connect.MlParams.DefaultParamsEntry)
            },
        ),
        "DESCRIPTOR": _MLPARAMS,
        "__module__": "spark.connect.ml_common_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.MlParams)
    },
)
_sym_db.RegisterMessage(MlParams)
_sym_db.RegisterMessage(MlParams.ParamsEntry)
_sym_db.RegisterMessage(MlParams.DefaultParamsEntry)

MlStage = _reflection.GeneratedProtocolMessageType(
    "MlStage",
    (_message.Message,),
    {
        "DESCRIPTOR": _MLSTAGE,
        "__module__": "spark.connect.ml_common_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.MlStage)
    },
)
_sym_db.RegisterMessage(MlStage)

ModelRef = _reflection.GeneratedProtocolMessageType(
    "ModelRef",
    (_message.Message,),
    {
        "DESCRIPTOR": _MODELREF,
        "__module__": "spark.connect.ml_common_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.ModelRef)
    },
)
_sym_db.RegisterMessage(ModelRef)

if _descriptor._USE_C_DESCRIPTORS == False:

    DESCRIPTOR._options = None
    DESCRIPTOR._serialized_options = b"\n\036org.apache.spark.connect.protoP\001"
    _MLPARAMS_PARAMSENTRY._options = None
    _MLPARAMS_PARAMSENTRY._serialized_options = b"8\001"
    _MLPARAMS_DEFAULTPARAMSENTRY._options = None
    _MLPARAMS_DEFAULTPARAMSENTRY._serialized_options = b"8\001"
    _MLPARAMS._serialized_start = 82
    _MLPARAMS._serialized_end = 431
    _MLPARAMS_PARAMSENTRY._serialized_start = 238
    _MLPARAMS_PARAMSENTRY._serialized_end = 330
    _MLPARAMS_DEFAULTPARAMSENTRY._serialized_start = 332
    _MLPARAMS_DEFAULTPARAMSENTRY._serialized_end = 431
    _MLSTAGE._serialized_start = 434
    _MLSTAGE._serialized_end = 646
    _MLSTAGE_STAGETYPE._serialized_start = 586
    _MLSTAGE_STAGETYPE._serialized_end = 646
    _MODELREF._serialized_start = 648
    _MODELREF._serialized_end = 674
# @@protoc_insertion_point(module_scope)
