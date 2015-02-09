/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.status.api;

import com.google.common.base.Joiner;

import java.util.Arrays;

public enum StageStatus {
    Active,
    Complete,
    Failed,
    Pending;

    private static String VALID_VALUES = Joiner.on(", ").join(
            Arrays.asList(values()));

    public static StageStatus fromString(String str) {
        if (str == null) {
            return null;
        }
        for (StageStatus status: values()) {
            if (status.name().equalsIgnoreCase(str))
                return status;
        }
        throw new IllegalArgumentException(
                String.format("Illegal type='%s'. Supported type values: %s",
                        str, VALID_VALUES));
    }
}
