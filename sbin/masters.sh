#!/usr/bin/env bash

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

# Run a shell command on all master hosts.
#
# Environment Variables
#
#   SPARK_MASTERS   File naming remote hosts.
#     Default is ${SPARK_CONF_DIR}/masters.
#   SPARK_CONF_DIR  Alternate conf dir. Default is ${SPARK_HOME}/conf.
#   SPARK_MASTER_SLEEP Seconds to sleep between spawning remote commands.
#   SPARK_SSH_OPTS Options passed to ssh when running remote commands.
##

usage="Usage: masters.sh [--config <conf-dir>] command..."

# if no args specified, show usage
if [ $# -le 0 ]; then
  echo $usage
  exit 1
fi

sbin="`dirname "$0"`"
sbin="`cd "$sbin"; pwd`"

. "$sbin/spark-config.sh"

# If the master file is specified in the command line,
# then it takes precedence over the definition in
# spark-env.sh. Save it here.
if [ -f "$SPARK_MASTERS" ]; then
  HOSTLIST=`cat "$SPARK_MASTERS"`
fi

# Check if --config is passed as an argument. It is an optional parameter.
# Exit if the argument is not a directory.
if [ "$1" == "--config" ]
then
  shift
  conf_dir="$1"
  if [ ! -d "$conf_dir" ]
  then
    echo "ERROR : $conf_dir is not a directory"
    echo $usage
    exit 1
  else
    export SPARK_CONF_DIR="$conf_dir"
  fi
  shift
fi

. "$SPARK_PREFIX/bin/load-spark-env.sh"

if [ "$HOSTLIST" = "" ]; then
  if [ "$SPARK_MASTERS" = "" ]; then
    if [ -f "${SPARK_CONF_DIR}/masters" ]; then
      HOSTLIST=`cat "${SPARK_CONF_DIR}/masters"`
    else
      HOSTLIST=localhost
    fi
  else
    HOSTLIST=`cat "${SPARK_MASTERS}"`
  fi
fi



# By default disable strict host key checking
if [ "$SPARK_SSH_OPTS" = "" ]; then
  SPARK_SSH_OPTS="-o StrictHostKeyChecking=no"
fi

for master in `echo "$HOSTLIST"|sed  "s/#.*$//;/^$/d"`; do
  if [ -n "${SPARK_SSH_FOREGROUND}" ]; then
    ssh $SPARK_SSH_OPTS "$master" $"${@// /\\ }" \
      2>&1 | sed "s/^/$master: /"
  else
    ssh $SPARK_SSH_OPTS "$master" $"${@// /\\ }" \
      2>&1 | sed "s/^/$master: /" &
  fi
  if [ "$SPARK_MASTER_SLEEP" != "" ]; then
    sleep $SPARK_MASTER_SLEEP
  fi
done

wait
