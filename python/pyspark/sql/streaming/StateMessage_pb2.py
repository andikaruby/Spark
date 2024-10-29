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
# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# NO CHECKED-IN PROTOBUF GENCODE
# source: StateMessage.proto
# Protobuf Python Version: 5.27.3
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import runtime_version as _runtime_version
from google.protobuf import symbol_database as _symbol_database
from google.protobuf.internal import builder as _builder

_runtime_version.ValidateProtobufRuntimeVersion(
    _runtime_version.Domain.PUBLIC, 5, 27, 3, "", "StateMessage.proto"
)
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(
    b'\n\x12StateMessage.proto\x12.org.apache.spark.sql.execution.streaming.state"\xbf\x03\n\x0cStateRequest\x12\x0f\n\x07version\x18\x01 \x01(\x05\x12\x66\n\x15statefulProcessorCall\x18\x02 \x01(\x0b\x32\x45.org.apache.spark.sql.execution.streaming.state.StatefulProcessorCallH\x00\x12\x64\n\x14stateVariableRequest\x18\x03 \x01(\x0b\x32\x44.org.apache.spark.sql.execution.streaming.state.StateVariableRequestH\x00\x12p\n\x1aimplicitGroupingKeyRequest\x18\x04 \x01(\x0b\x32J.org.apache.spark.sql.execution.streaming.state.ImplicitGroupingKeyRequestH\x00\x12T\n\x0ctimerRequest\x18\x05 \x01(\x0b\x32<.org.apache.spark.sql.execution.streaming.state.TimerRequestH\x00\x42\x08\n\x06method"H\n\rStateResponse\x12\x12\n\nstatusCode\x18\x01 \x01(\x05\x12\x14\n\x0c\x65rrorMessage\x18\x02 \x01(\t\x12\r\n\x05value\x18\x03 \x01(\x0c"W\n\x1cStateResponseWithLongTypeVal\x12\x12\n\nstatusCode\x18\x01 \x01(\x05\x12\x14\n\x0c\x65rrorMessage\x18\x02 \x01(\t\x12\r\n\x05value\x18\x03 \x01(\x03"\xea\x03\n\x15StatefulProcessorCall\x12X\n\x0esetHandleState\x18\x01 \x01(\x0b\x32>.org.apache.spark.sql.execution.streaming.state.SetHandleStateH\x00\x12Y\n\rgetValueState\x18\x02 \x01(\x0b\x32@.org.apache.spark.sql.execution.streaming.state.StateCallCommandH\x00\x12X\n\x0cgetListState\x18\x03 \x01(\x0b\x32@.org.apache.spark.sql.execution.streaming.state.StateCallCommandH\x00\x12W\n\x0bgetMapState\x18\x04 \x01(\x0b\x32@.org.apache.spark.sql.execution.streaming.state.StateCallCommandH\x00\x12_\n\x0etimerStateCall\x18\x05 \x01(\x0b\x32\x45.org.apache.spark.sql.execution.streaming.state.TimerStateCallCommandH\x00\x42\x08\n\x06method"\xa8\x02\n\x14StateVariableRequest\x12X\n\x0evalueStateCall\x18\x01 \x01(\x0b\x32>.org.apache.spark.sql.execution.streaming.state.ValueStateCallH\x00\x12V\n\rlistStateCall\x18\x02 \x01(\x0b\x32=.org.apache.spark.sql.execution.streaming.state.ListStateCallH\x00\x12T\n\x0cmapStateCall\x18\x03 \x01(\x0b\x32<.org.apache.spark.sql.execution.streaming.state.MapStateCallH\x00\x42\x08\n\x06method"\xe0\x01\n\x1aImplicitGroupingKeyRequest\x12X\n\x0esetImplicitKey\x18\x01 \x01(\x0b\x32>.org.apache.spark.sql.execution.streaming.state.SetImplicitKeyH\x00\x12^\n\x11removeImplicitKey\x18\x02 \x01(\x0b\x32\x41.org.apache.spark.sql.execution.streaming.state.RemoveImplicitKeyH\x00\x42\x08\n\x06method"\xda\x01\n\x0cTimerRequest\x12^\n\x11timerValueRequest\x18\x01 \x01(\x0b\x32\x41.org.apache.spark.sql.execution.streaming.state.TimerValueRequestH\x00\x12`\n\x12\x65xpiryTimerRequest\x18\x02 \x01(\x0b\x32\x42.org.apache.spark.sql.execution.streaming.state.ExpiryTimerRequestH\x00\x42\x08\n\x06method"\xd4\x01\n\x11TimerValueRequest\x12_\n\x12getProcessingTimer\x18\x01 \x01(\x0b\x32\x41.org.apache.spark.sql.execution.streaming.state.GetProcessingTimeH\x00\x12T\n\x0cgetWatermark\x18\x02 \x01(\x0b\x32<.org.apache.spark.sql.execution.streaming.state.GetWatermarkH\x00\x42\x08\n\x06method"/\n\x12\x45xpiryTimerRequest\x12\x19\n\x11\x65xpiryTimestampMs\x18\x01 \x01(\x03"\x13\n\x11GetProcessingTime"\x0e\n\x0cGetWatermark"\x9a\x01\n\x10StateCallCommand\x12\x11\n\tstateName\x18\x01 \x01(\t\x12\x0e\n\x06schema\x18\x02 \x01(\t\x12\x1b\n\x13mapStateValueSchema\x18\x03 \x01(\t\x12\x46\n\x03ttl\x18\x04 \x01(\x0b\x32\x39.org.apache.spark.sql.execution.streaming.state.TTLConfig"\x8f\x02\n\x15TimerStateCallCommand\x12Q\n\x08register\x18\x01 \x01(\x0b\x32=.org.apache.spark.sql.execution.streaming.state.RegisterTimerH\x00\x12M\n\x06\x64\x65lete\x18\x02 \x01(\x0b\x32;.org.apache.spark.sql.execution.streaming.state.DeleteTimerH\x00\x12J\n\x04list\x18\x03 \x01(\x0b\x32:.org.apache.spark.sql.execution.streaming.state.ListTimersH\x00\x42\x08\n\x06method"\xe1\x02\n\x0eValueStateCall\x12\x11\n\tstateName\x18\x01 \x01(\t\x12H\n\x06\x65xists\x18\x02 \x01(\x0b\x32\x36.org.apache.spark.sql.execution.streaming.state.ExistsH\x00\x12\x42\n\x03get\x18\x03 \x01(\x0b\x32\x33.org.apache.spark.sql.execution.streaming.state.GetH\x00\x12\\\n\x10valueStateUpdate\x18\x04 \x01(\x0b\x32@.org.apache.spark.sql.execution.streaming.state.ValueStateUpdateH\x00\x12\x46\n\x05\x63lear\x18\x05 \x01(\x0b\x32\x35.org.apache.spark.sql.execution.streaming.state.ClearH\x00\x42\x08\n\x06method"\x90\x04\n\rListStateCall\x12\x11\n\tstateName\x18\x01 \x01(\t\x12H\n\x06\x65xists\x18\x02 \x01(\x0b\x32\x36.org.apache.spark.sql.execution.streaming.state.ExistsH\x00\x12T\n\x0clistStateGet\x18\x03 \x01(\x0b\x32<.org.apache.spark.sql.execution.streaming.state.ListStateGetH\x00\x12T\n\x0clistStatePut\x18\x04 \x01(\x0b\x32<.org.apache.spark.sql.execution.streaming.state.ListStatePutH\x00\x12R\n\x0b\x61ppendValue\x18\x05 \x01(\x0b\x32;.org.apache.spark.sql.execution.streaming.state.AppendValueH\x00\x12P\n\nappendList\x18\x06 \x01(\x0b\x32:.org.apache.spark.sql.execution.streaming.state.AppendListH\x00\x12\x46\n\x05\x63lear\x18\x07 \x01(\x0b\x32\x35.org.apache.spark.sql.execution.streaming.state.ClearH\x00\x42\x08\n\x06method"\xe1\x05\n\x0cMapStateCall\x12\x11\n\tstateName\x18\x01 \x01(\t\x12H\n\x06\x65xists\x18\x02 \x01(\x0b\x32\x36.org.apache.spark.sql.execution.streaming.state.ExistsH\x00\x12L\n\x08getValue\x18\x03 \x01(\x0b\x32\x38.org.apache.spark.sql.execution.streaming.state.GetValueH\x00\x12R\n\x0b\x63ontainsKey\x18\x04 \x01(\x0b\x32;.org.apache.spark.sql.execution.streaming.state.ContainsKeyH\x00\x12R\n\x0bupdateValue\x18\x05 \x01(\x0b\x32;.org.apache.spark.sql.execution.streaming.state.UpdateValueH\x00\x12L\n\x08iterator\x18\x06 \x01(\x0b\x32\x38.org.apache.spark.sql.execution.streaming.state.IteratorH\x00\x12\x44\n\x04keys\x18\x07 \x01(\x0b\x32\x34.org.apache.spark.sql.execution.streaming.state.KeysH\x00\x12H\n\x06values\x18\x08 \x01(\x0b\x32\x36.org.apache.spark.sql.execution.streaming.state.ValuesH\x00\x12N\n\tremoveKey\x18\t \x01(\x0b\x32\x39.org.apache.spark.sql.execution.streaming.state.RemoveKeyH\x00\x12\x46\n\x05\x63lear\x18\n \x01(\x0b\x32\x35.org.apache.spark.sql.execution.streaming.state.ClearH\x00\x42\x08\n\x06method"\x1d\n\x0eSetImplicitKey\x12\x0b\n\x03key\x18\x01 \x01(\x0c"\x13\n\x11RemoveImplicitKey"\x08\n\x06\x45xists"\x05\n\x03Get"*\n\rRegisterTimer\x12\x19\n\x11\x65xpiryTimestampMs\x18\x01 \x01(\x03"(\n\x0b\x44\x65leteTimer\x12\x19\n\x11\x65xpiryTimestampMs\x18\x01 \x01(\x03" \n\nListTimers\x12\x12\n\niteratorId\x18\x01 \x01(\t"!\n\x10ValueStateUpdate\x12\r\n\x05value\x18\x01 \x01(\x0c"\x07\n\x05\x43lear""\n\x0cListStateGet\x12\x12\n\niteratorId\x18\x01 \x01(\t"\x0e\n\x0cListStatePut"\x1c\n\x0b\x41ppendValue\x12\r\n\x05value\x18\x01 \x01(\x0c"\x0c\n\nAppendList"\x1b\n\x08GetValue\x12\x0f\n\x07userKey\x18\x01 \x01(\x0c"\x1e\n\x0b\x43ontainsKey\x12\x0f\n\x07userKey\x18\x01 \x01(\x0c"-\n\x0bUpdateValue\x12\x0f\n\x07userKey\x18\x01 \x01(\x0c\x12\r\n\x05value\x18\x02 \x01(\x0c"\x1e\n\x08Iterator\x12\x12\n\niteratorId\x18\x01 \x01(\t"\x1a\n\x04Keys\x12\x12\n\niteratorId\x18\x01 \x01(\t"\x1c\n\x06Values\x12\x12\n\niteratorId\x18\x01 \x01(\t"\x1c\n\tRemoveKey\x12\x0f\n\x07userKey\x18\x01 \x01(\x0c"\\\n\x0eSetHandleState\x12J\n\x05state\x18\x01 \x01(\x0e\x32;.org.apache.spark.sql.execution.streaming.state.HandleState"\x1f\n\tTTLConfig\x12\x12\n\ndurationMs\x18\x01 \x01(\x05*`\n\x0bHandleState\x12\x0b\n\x07\x43REATED\x10\x00\x12\x0f\n\x0bINITIALIZED\x10\x01\x12\x12\n\x0e\x44\x41TA_PROCESSED\x10\x02\x12\x13\n\x0fTIMER_PROCESSED\x10\x03\x12\n\n\x06\x43LOSED\x10\x04\x62\x06proto3'
)  # noqa: E501

_globals = globals()
_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, _globals)
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, "StateMessage_pb2", _globals)
if not _descriptor._USE_C_DESCRIPTORS:
    DESCRIPTOR._loaded_options = None
    _globals["_HANDLESTATE"]._serialized_start = 4966
    _globals["_HANDLESTATE"]._serialized_end = 5062
    _globals["_STATEREQUEST"]._serialized_start = 71
    _globals["_STATEREQUEST"]._serialized_end = 518
    _globals["_STATERESPONSE"]._serialized_start = 520
    _globals["_STATERESPONSE"]._serialized_end = 592
    _globals["_STATERESPONSEWITHLONGTYPEVAL"]._serialized_start = 594
    _globals["_STATERESPONSEWITHLONGTYPEVAL"]._serialized_end = 681
    _globals["_STATEFULPROCESSORCALL"]._serialized_start = 684
    _globals["_STATEFULPROCESSORCALL"]._serialized_end = 1174
    _globals["_STATEVARIABLEREQUEST"]._serialized_start = 1177
    _globals["_STATEVARIABLEREQUEST"]._serialized_end = 1473
    _globals["_IMPLICITGROUPINGKEYREQUEST"]._serialized_start = 1476
    _globals["_IMPLICITGROUPINGKEYREQUEST"]._serialized_end = 1700
    _globals["_TIMERREQUEST"]._serialized_start = 1703
    _globals["_TIMERREQUEST"]._serialized_end = 1921
    _globals["_TIMERVALUEREQUEST"]._serialized_start = 1924
    _globals["_TIMERVALUEREQUEST"]._serialized_end = 2136
    _globals["_EXPIRYTIMERREQUEST"]._serialized_start = 2138
    _globals["_EXPIRYTIMERREQUEST"]._serialized_end = 2185
    _globals["_GETPROCESSINGTIME"]._serialized_start = 2187
    _globals["_GETPROCESSINGTIME"]._serialized_end = 2206
    _globals["_GETWATERMARK"]._serialized_start = 2208
    _globals["_GETWATERMARK"]._serialized_end = 2222
    _globals["_STATECALLCOMMAND"]._serialized_start = 2225
    _globals["_STATECALLCOMMAND"]._serialized_end = 2379
    _globals["_TIMERSTATECALLCOMMAND"]._serialized_start = 2382
    _globals["_TIMERSTATECALLCOMMAND"]._serialized_end = 2653
    _globals["_VALUESTATECALL"]._serialized_start = 2656
    _globals["_VALUESTATECALL"]._serialized_end = 3009
    _globals["_LISTSTATECALL"]._serialized_start = 3012
    _globals["_LISTSTATECALL"]._serialized_end = 3540
    _globals["_MAPSTATECALL"]._serialized_start = 3543
    _globals["_MAPSTATECALL"]._serialized_end = 4280
    _globals["_SETIMPLICITKEY"]._serialized_start = 4282
    _globals["_SETIMPLICITKEY"]._serialized_end = 4311
    _globals["_REMOVEIMPLICITKEY"]._serialized_start = 4313
    _globals["_REMOVEIMPLICITKEY"]._serialized_end = 4332
    _globals["_EXISTS"]._serialized_start = 4334
    _globals["_EXISTS"]._serialized_end = 4342
    _globals["_GET"]._serialized_start = 4344
    _globals["_GET"]._serialized_end = 4349
    _globals["_REGISTERTIMER"]._serialized_start = 4351
    _globals["_REGISTERTIMER"]._serialized_end = 4393
    _globals["_DELETETIMER"]._serialized_start = 4395
    _globals["_DELETETIMER"]._serialized_end = 4435
    _globals["_LISTTIMERS"]._serialized_start = 4437
    _globals["_LISTTIMERS"]._serialized_end = 4469
    _globals["_VALUESTATEUPDATE"]._serialized_start = 4471
    _globals["_VALUESTATEUPDATE"]._serialized_end = 4504
    _globals["_CLEAR"]._serialized_start = 4506
    _globals["_CLEAR"]._serialized_end = 4513
    _globals["_LISTSTATEGET"]._serialized_start = 4515
    _globals["_LISTSTATEGET"]._serialized_end = 4549
    _globals["_LISTSTATEPUT"]._serialized_start = 4551
    _globals["_LISTSTATEPUT"]._serialized_end = 4565
    _globals["_APPENDVALUE"]._serialized_start = 4567
    _globals["_APPENDVALUE"]._serialized_end = 4595
    _globals["_APPENDLIST"]._serialized_start = 4597
    _globals["_APPENDLIST"]._serialized_end = 4609
    _globals["_GETVALUE"]._serialized_start = 4611
    _globals["_GETVALUE"]._serialized_end = 4638
    _globals["_CONTAINSKEY"]._serialized_start = 4640
    _globals["_CONTAINSKEY"]._serialized_end = 4670
    _globals["_UPDATEVALUE"]._serialized_start = 4672
    _globals["_UPDATEVALUE"]._serialized_end = 4717
    _globals["_ITERATOR"]._serialized_start = 4719
    _globals["_ITERATOR"]._serialized_end = 4749
    _globals["_KEYS"]._serialized_start = 4751
    _globals["_KEYS"]._serialized_end = 4777
    _globals["_VALUES"]._serialized_start = 4779
    _globals["_VALUES"]._serialized_end = 4807
    _globals["_REMOVEKEY"]._serialized_start = 4809
    _globals["_REMOVEKEY"]._serialized_end = 4837
    _globals["_SETHANDLESTATE"]._serialized_start = 4839
    _globals["_SETHANDLESTATE"]._serialized_end = 4931
    _globals["_TTLCONFIG"]._serialized_start = 4933
    _globals["_TTLCONFIG"]._serialized_end = 4964
# @@protoc_insertion_point(module_scope)
