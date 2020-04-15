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

from pyspark.resource.executorresourcerequest import ExecutorResourceRequest
from pyspark.resource.resourceprofile import ResourceProfile
from pyspark.resource.taskresourcerequest import TaskResourceRequest
from pyspark.resource.taskresourcerequests import TaskResourceRequests


class ResourceProfileBuilder(object):

    """
    .. note:: Evolving

    Resource profile Builder to build a resource profile to associate with an RDD.
    A ResourceProfile allows the user to specify executor and task requirements for
    an RDD that will get applied during a stage. This allows the user to change the
    resource requirements between stages.

    .. versionadded:: 3.1.0
    """

    def __init__(self):
        from pyspark.context import SparkContext
        _jvm = SparkContext._jvm
        if _jvm is not None:
            self._java_resource_profile_builder = \
                _jvm.org.apache.spark.resource.ResourceProfileBuilder()
        else:
            self._java_resource_profile_builder = None
            self._executor_resource_requests = None
            self._task_resource_requests = None

    def require(self, resourceRequest):
        if isinstance(resourceRequest, TaskResourceRequests):
            if self._java_resource_profile_builder is not None:
                self._java_resource_profile_builder.require(
                    resourceRequest._java_task_resource_requests)
            else:
                self._task_resource_requests = resourceRequest
        else:
            if self._java_resource_profile_builder is not None:
                self._java_resource_profile_builder.require(
                    resourceRequest._java_executor_resource_requests)
            else:
                self._executor_resource_requests = resourceRequest
        return self

    def clearExecutorResourceRequests(self):
        if self._java_resource_profile_builder is not None:
            self._java_resource_profile_builder.clearExecutorResourceRequests()
        else:
            self._executor_resource_requests = None

    def clearTaskResourceRequests(self):
        if self._java_resource_profile_builder is not None:
            self._java_resource_profile_builder.clearTaskResourceRequests()
        else:
            self._task_resource_requests = None

    @property
    def taskResources(self):
        if self._java_resource_profile_builder is not None:
            taskRes = self._java_resource_profile_builder.taskResourcesJMap()
            result = {}
            for k, v in taskRes.items():
                result[k] = TaskResourceRequest(v.resourceName(), v.amount())
            return result
        else:
            return self._task_resource_requests

    @property
    def executorResources(self):
        if self._java_resource_profile_builder is not None:
            result = {}
            execRes = self._java_resource_profile_builder.executorResourcesJMap()
            for k, v in execRes.items():
                result[k] = ExecutorResourceRequest(v.resourceName(), v.amount(),
                                                    v.discoveryScript(), v.vendor())
            return result
        else:
            return self._executor_resource_requests

    @property
    def build(self):
        if self._java_resource_profile_builder is not None:
            jresourceProfile = self._java_resource_profile_builder.build()
            return ResourceProfile(_java_resource_profile=jresourceProfile)
        else:
            return ResourceProfile(_exec_req=self._executor_resource_requests,
                                   _task_req=self._task_resource_requests)
