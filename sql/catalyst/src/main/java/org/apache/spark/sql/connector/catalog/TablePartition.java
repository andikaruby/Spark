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
package org.apache.spark.sql.connector.catalog;

import java.util.HashMap;
import java.util.Map;

public class TablePartition {
    private Map<String, String> partitionSpec;
    private Map<String, String> parametes;

    public TablePartition(
            Map<String, String> partitionSpec) {
        this.partitionSpec = partitionSpec;
        this.parametes = new HashMap<String, String>();
    }

    public TablePartition(
            Map<String, String> partitionSpec,
            Map<String, String> parametes) {
        this.partitionSpec = partitionSpec;
        this.parametes = parametes;
    }

    public Map<String, String> getPartitionSpec() {
        return partitionSpec;
    }

    public void setPartitionSpec(Map<String, String> partitionSpec) {
        this.partitionSpec = partitionSpec;
    }

    public Map<String, String> getParametes() {
        return parametes;
    }

    public void setParameters(Map<String, String> parametes) {
        this.parametes = parametes;
    }
}
