#!/usr/bin/env python3

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

import os
import sys
import re
import glob

from sparktestsupport import SPARK_HOME


def main():
    log_pattern = r"log(?:Info|Warning|Error)\(.*?\)\n"
    inner_log_pattern = r'".*?"\.format\(.*\)|s?".*?(?:\$|\+(?!.*?[ |\t].*s?")).*|.* \+ ".*?"'
    compiled_inner_log_pattern = re.compile(inner_log_pattern, flags=re.DOTALL)

    # Regex patterns for file paths to exclude from the Structured Logging style check
    excluded_file_patterns = [
        "[Tt]est",
        "/sql/catalyst/src/main/scala/org/apache/spark/sql/catalyst/expressions/codegen/CodeGenerator.scala",
        "/sql/hive-thriftserver/src/main/scala/org/apache/spark/sql/hive/thriftserver/SparkSQLCLIService.scala",
    ]

    nonmigrated_files = {}

    scala_files = glob.glob(os.path.join(SPARK_HOME, "**", "*.scala"), recursive=True)

    for file in scala_files:
        skip_file = False
        for exclude_pattern in excluded_file_patterns:
            if re.search(exclude_pattern, file):
                skip_file = True
                break

        if not skip_file:
            with open(file, "r") as f:
                content = f.read()

                log_statements = re.finditer(log_pattern, content, re.DOTALL)

                if log_statements:
                    nonmigrated_files[file] = []
                    for log_statement in log_statements:
                        log_statement_str = log_statement.group(0).strip()
                        # trim first ( and last )
                        first_paren_index = log_statement_str.find('(')
                        inner_log_statement = log_statement_str[first_paren_index + 1:-1]

                        if compiled_inner_log_pattern.fullmatch(inner_log_statement):
                            # print(f"log statement: ${inner_log_statement}")
                            start_pos = log_statement.start()
                            preceding_content = content[:start_pos]
                            line_number = preceding_content.count("\n") + 1
                            start_char = start_pos - preceding_content.rfind("\n") - 1
                            nonmigrated_files[file].append((line_number, start_char))

    if not nonmigrated_files:
        print("Structured logging style check passed.")
        sys.exit(0)
    else:
        for file_path, issues in nonmigrated_files.items():
            for line_number, start_char in issues:
                pass
                print(f"[error] {file_path}:{line_number}:{start_char}")
                print(
                    """[error]\t\tLogging message should use log"..." instead of s"..." and variables should be wrapped in `MDC`s.
                Refer to Structured Logging Framework guidelines in the file `internal/Logging.scala`."""
                )

        sys.exit(-1)


if __name__ == "__main__":
    main()
