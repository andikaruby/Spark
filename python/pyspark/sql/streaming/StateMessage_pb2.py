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
# source: StateMessage.proto
# Protobuf Python Version: 5.27.1
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database
from google.protobuf.internal import builder as _builder

# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(
    b'\n\x12StateMessage.proto\x12.org.apache.spark.sql.execution.streaming.state"\xe9\x02\n\x0cStateRequest\x12\x0f\n\x07version\x18\x01 \x01(\x05\x12\x66\n\x15statefulProcessorCall\x18\x02 \x01(\x0b\x32\x45.org.apache.spark.sql.execution.streaming.state.StatefulProcessorCallH\x00\x12\x64\n\x14stateVariableRequest\x18\x03 \x01(\x0b\x32\x44.org.apache.spark.sql.execution.streaming.state.StateVariableRequestH\x00\x12p\n\x1aimplicitGroupingKeyRequest\x18\x04 \x01(\x0b\x32J.org.apache.spark.sql.execution.streaming.state.ImplicitGroupingKeyRequestH\x00\x42\x08\n\x06method"H\n\rStateResponse\x12\x12\n\nstatusCode\x18\x01 \x01(\x05\x12\x14\n\x0c\x65rrorMessage\x18\x02 \x01(\t\x12\r\n\x05value\x18\x03 \x01(\x0c"\x89\x03\n\x15StatefulProcessorCall\x12X\n\x0esetHandleState\x18\x01 \x01(\x0b\x32>.org.apache.spark.sql.execution.streaming.state.SetHandleStateH\x00\x12Y\n\rgetValueState\x18\x02 \x01(\x0b\x32@.org.apache.spark.sql.execution.streaming.state.StateCallCommandH\x00\x12X\n\x0cgetListState\x18\x03 \x01(\x0b\x32@.org.apache.spark.sql.execution.streaming.state.StateCallCommandH\x00\x12W\n\x0bgetMapState\x18\x04 \x01(\x0b\x32@.org.apache.spark.sql.execution.streaming.state.StateCallCommandH\x00\x42\x08\n\x06method"z\n\x14StateVariableRequest\x12X\n\x0evalueStateCall\x18\x01 \x01(\x0b\x32>.org.apache.spark.sql.execution.streaming.state.ValueStateCallH\x00\x42\x08\n\x06method"\xe0\x01\n\x1aImplicitGroupingKeyRequest\x12X\n\x0esetImplicitKey\x18\x01 \x01(\x0b\x32>.org.apache.spark.sql.execution.streaming.state.SetImplicitKeyH\x00\x12^\n\x11removeImplicitKey\x18\x02 \x01(\x0b\x32\x41.org.apache.spark.sql.execution.streaming.state.RemoveImplicitKeyH\x00\x42\x08\n\x06method"5\n\x10StateCallCommand\x12\x11\n\tstateName\x18\x01 \x01(\t\x12\x0e\n\x06schema\x18\x02 \x01(\t"\xe1\x02\n\x0eValueStateCall\x12\x11\n\tstateName\x18\x01 \x01(\t\x12H\n\x06\x65xists\x18\x02 \x01(\x0b\x32\x36.org.apache.spark.sql.execution.streaming.state.ExistsH\x00\x12\x42\n\x03get\x18\x03 \x01(\x0b\x32\x33.org.apache.spark.sql.execution.streaming.state.GetH\x00\x12\\\n\x10valueStateUpdate\x18\x04 \x01(\x0b\x32@.org.apache.spark.sql.execution.streaming.state.ValueStateUpdateH\x00\x12\x46\n\x05\x63lear\x18\x05 \x01(\x0b\x32\x35.org.apache.spark.sql.execution.streaming.state.ClearH\x00\x42\x08\n\x06method"\x1d\n\x0eSetImplicitKey\x12\x0b\n\x03key\x18\x01 \x01(\x0c"\x13\n\x11RemoveImplicitKey"\x08\n\x06\x45xists"\x05\n\x03Get"!\n\x10ValueStateUpdate\x12\r\n\x05value\x18\x01 \x01(\x0c"\x07\n\x05\x43lear"\\\n\x0eSetHandleState\x12J\n\x05state\x18\x01 \x01(\x0e\x32;.org.apache.spark.sql.execution.streaming.state.HandleState*K\n\x0bHandleState\x12\x0b\n\x07\x43REATED\x10\x00\x12\x0f\n\x0bINITIALIZED\x10\x01\x12\x12\n\x0e\x44\x41TA_PROCESSED\x10\x02\x12\n\n\x06\x43LOSED\x10\x03\x62\x06proto3'  # noqa: E501
)

_globals = globals()
_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, _globals)
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, "StateMessage_pb2", _globals)
if not _descriptor._USE_C_DESCRIPTORS:
    DESCRIPTOR._loaded_options = None
    _globals["_HANDLESTATE"]._serialized_start = 1873
    _globals["_HANDLESTATE"]._serialized_end = 1948
    _globals["_STATEREQUEST"]._serialized_start = 71
    _globals["_STATEREQUEST"]._serialized_end = 432
    _globals["_STATERESPONSE"]._serialized_start = 434
    _globals["_STATERESPONSE"]._serialized_end = 506
    _globals["_STATEFULPROCESSORCALL"]._serialized_start = 509
    _globals["_STATEFULPROCESSORCALL"]._serialized_end = 902
    _globals["_STATEVARIABLEREQUEST"]._serialized_start = 904
    _globals["_STATEVARIABLEREQUEST"]._serialized_end = 1026
    _globals["_IMPLICITGROUPINGKEYREQUEST"]._serialized_start = 1029
    _globals["_IMPLICITGROUPINGKEYREQUEST"]._serialized_end = 1253
    _globals["_STATECALLCOMMAND"]._serialized_start = 1255
    _globals["_STATECALLCOMMAND"]._serialized_end = 1308
    _globals["_VALUESTATECALL"]._serialized_start = 1311
    _globals["_VALUESTATECALL"]._serialized_end = 1664
    _globals["_SETIMPLICITKEY"]._serialized_start = 1666
    _globals["_SETIMPLICITKEY"]._serialized_end = 1695
    _globals["_REMOVEIMPLICITKEY"]._serialized_start = 1697
    _globals["_REMOVEIMPLICITKEY"]._serialized_end = 1716
    _globals["_EXISTS"]._serialized_start = 1718
    _globals["_EXISTS"]._serialized_end = 1726
    _globals["_GET"]._serialized_start = 1728
    _globals["_GET"]._serialized_end = 1733
    _globals["_VALUESTATEUPDATE"]._serialized_start = 1735
    _globals["_VALUESTATEUPDATE"]._serialized_end = 1768
    _globals["_CLEAR"]._serialized_start = 1770
    _globals["_CLEAR"]._serialized_end = 1777
    _globals["_SETHANDLESTATE"]._serialized_start = 1779
    _globals["_SETHANDLESTATE"]._serialized_end = 1871
# @@protoc_insertion_point(module_scope)
