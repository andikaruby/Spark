/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.security.token;

import java.util.Collection;

import org.apache.hadoop.io.Text;

/**
 * Select token of type T from tokens for use with named service
 * 
 * @param <T>
 *          T extends TokenIdentifier
 */
public interface TokenSelector<T extends TokenIdentifier> {
  Token<T> selectToken(Text service,
      Collection<Token<? extends TokenIdentifier>> tokens);
}
