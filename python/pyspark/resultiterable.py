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

__all__ = ["ResultIterable"]


class ResultIterable(object):

    """
    A special result iterable. This is used because the standard
    iterator can not be pickled
    """

    def __init__(self, it):
        self.it = it

    def __iter__(self):
        return iter(self.it)

    def __len__(self):
        try:
            return len(self.it)
        except TypeError:
            return sum(1 for _ in self.it)
