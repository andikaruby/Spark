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

from abc import ABCMeta

from pyspark.ml.util import Identifiable


__all__ = ["Param"]


class Param(object):
    """
    A param with self-contained documentation and optionally default value.
    """

    def __init__(self, parent, name, doc, defaultValue=None):
        self.parent = parent
        self.name = name
        self.doc = doc
        self.defaultValue = defaultValue

    def __str__(self):
        return self.parent + "_" + self.name

    def __repr__(self):
        return self.parent + "_" + self.name


class Params(Identifiable):
    """
    Components that take parameters. This also provides an internal
    param map to store parameter values attached to the instance.
    """

    __metaclass__ = ABCMeta

    def __init__(self):
        super(Params, self).__init__()
        #: Internal param map.
        self.paramMap = {}

    def params(self):
        """
        Returns all params. The default implementation uses
        :py:func:`dir` to get all attributes of type
        :py:class:`Param`.
        """
        return [attr for attr in dir(self) if isinstance(attr, Param)]
