#!/usr/bin/env bash

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

# This script builds and pushes docker images when run from a release of Spark
# with Kubernetes support.

function error {
  echo "$@" 1>&2
  exit 1
}

if [ -z "${SPARK_HOME}" ]; then
  SPARK_HOME="$(cd "`dirname "$0"`"/..; pwd)"
fi
. "${SPARK_HOME}/bin/load-spark-env.sh"

CTX_DIR="$SPARK_HOME/target/tmp/docker"

function is_dev_build {
  [ ! -f "$SPARK_HOME/RELEASE" ]
}

function cleanup_ctx_dir {
  if is_dev_build; then
    rm -rf "$CTX_DIR"
  fi
}

trap cleanup_ctx_dir EXIT

function image_ref {
  local image="$1"
  local add_repo="${2:-1}"
  if [ $add_repo = 1 ] && [ -n "$REPO" ]; then
    image="$REPO/$image"
  fi
  if [ -n "$TAG" ]; then
    image="$image:$TAG"
  fi
  echo "$image"
}

# Create a smaller build context for docker in dev builds to make the build faster. Docker
# uploads all of the current directory to the daemon, and it can get pretty big with dev
# builds that contain test log files and other artifacts.
#
# Three build contexts are created, one for each image: base, pyspark, and sparkr. For them
# to have the desired effect, the docker command needs to be executed inside the appropriate
# context directory.
#
# Note: docker does not support symlinks in the build context.
function create_dev_build_context {(
  set -e
  local BASE_CTX="$CTX_DIR/base"
  mkdir -p "$BASE_CTX/kubernetes"
  cp -r "resource-managers/kubernetes/docker/src/main/dockerfiles" \
    "$BASE_CTX/kubernetes/dockerfiles"

  cp -r "assembly/target/scala-$SPARK_SCALA_VERSION/jars" "$BASE_CTX/jars"
  cp -r "resource-managers/kubernetes/integration-tests/tests" \
    "$BASE_CTX/kubernetes/tests"

  mkdir "$BASE_CTX/examples"
  cp -r "examples/src" "$BASE_CTX/examples/src"
  # Copy just needed examples jars instead of everything.
  mkdir "$BASE_CTX/examples/jars"
  for i in examples/target/scala-$SPARK_SCALA_VERSION/jars/*; do
    if [ ! -f "$BASE_CTX/jars/$(basename $i)" ]; then
      cp $i "$BASE_CTX/examples/jars"
    fi
  done

  for other in bin sbin data; do
    cp -r "$other" "$BASE_CTX/$other"
  done

  local PYSPARK_CTX="$CTX_DIR/pyspark"
  mkdir -p "$PYSPARK_CTX/kubernetes"
  cp -r "resource-managers/kubernetes/docker/src/main/dockerfiles" \
    "$PYSPARK_CTX/kubernetes/dockerfiles"
  mkdir "$PYSPARK_CTX/python"
  cp -r "python/lib" "$PYSPARK_CTX/python/lib"

  local R_CTX="$CTX_DIR/sparkr"
  mkdir -p "$R_CTX/kubernetes"
  cp -r "resource-managers/kubernetes/docker/src/main/dockerfiles" \
    "$R_CTX/kubernetes/dockerfiles"
  cp -r "R" "$R_CTX/R"
)}

function img_ctx_dir {
  if is_dev_build; then
    echo "$CTX_DIR/$1"
  else
    echo "$SPARK_HOME"
  fi
}

function build {
  local BUILD_ARGS
  local SPARK_ROOT="$SPARK_HOME"

  if is_dev_build; then
    create_dev_build_context
    SPARK_ROOT="$CTX_DIR/base"
  fi

  # Verify that the Docker image content directory is present
  if [ ! -d "$SPARK_ROOT/kubernetes/dockerfiles" ]; then
    error "Cannot find docker image. This script must be run from a runnable distribution of Apache Spark."
  fi

  # Verify that Spark has actually been built/is a runnable distribution
  # i.e. the Spark JARs that the Docker files will place into the image are present
  local TOTAL_JARS=$(ls $SPARK_ROOT/jars/spark-* | wc -l)
  TOTAL_JARS=$(( $TOTAL_JARS ))
  if [ "${TOTAL_JARS}" -eq 0 ]; then
    error "Cannot find Spark JARs. This script assumes that Apache Spark has first been built locally or this is a runnable distribution."
  fi

  local BUILD_ARGS=(${BUILD_PARAMS})
  local BINDING_BUILD_ARGS=(
    ${BUILD_PARAMS}
    --build-arg
    base_img=$(image_ref spark)
  )
  local BASEDOCKERFILE=${BASEDOCKERFILE:-"kubernetes/dockerfiles/spark/Dockerfile"}
  local PYDOCKERFILE=${PYDOCKERFILE:-"kubernetes/dockerfiles/spark/bindings/python/Dockerfile"}
  local RDOCKERFILE=${RDOCKERFILE:-"kubernetes/dockerfiles/spark/bindings/R/Dockerfile"}

  (cd $(img_ctx_dir base) && docker build $NOCACHEARG "${BUILD_ARGS[@]}" \
    -t $(image_ref spark) \
    -f "$BASEDOCKERFILE" .)
  if [ $? -ne 0 ]; then
    error "Failed to build Spark JVM Docker image, please refer to Docker build output for details."
  fi

  (cd $(img_ctx_dir pyspark) && docker build $NOCACHEARG "${BINDING_BUILD_ARGS[@]}" \
    -t $(image_ref spark-py) \
    -f "$PYDOCKERFILE" .)
    if [ $? -ne 0 ]; then
      error "Failed to build PySpark Docker image, please refer to Docker build output for details."
    fi

  (cd $(img_ctx_dir sparkr) && docker build $NOCACHEARG "${BINDING_BUILD_ARGS[@]}" \
    -t $(image_ref spark-r) \
    -f "$RDOCKERFILE" .)
  if [ $? -ne 0 ]; then
    error "Failed to build SparkR Docker image, please refer to Docker build output for details."
  fi
}

function push {
  docker push "$(image_ref spark)"
  if [ $? -ne 0 ]; then
    error "Failed to push Spark JVM Docker image."
  fi
  docker push "$(image_ref spark-py)"
  if [ $? -ne 0 ]; then
    error "Failed to push PySpark Docker image."
  fi
  docker push "$(image_ref spark-r)"
  if [ $? -ne 0 ]; then
    error "Failed to push SparkR Docker image."
  fi
}

function usage {
  cat <<EOF
Usage: $0 [options] [command]
Builds or pushes the built-in Spark Docker image.

Commands:
  build       Build image. Requires a repository address to be provided if the image will be
              pushed to a different registry.
  push        Push a pre-built image to a registry. Requires a repository address to be provided.

Options:
  -f file               Dockerfile to build for JVM based Jobs. By default builds the Dockerfile shipped with Spark.
  -p file               Dockerfile to build for PySpark Jobs. Builds Python dependencies and ships with Spark.
  -R file               Dockerfile to build for SparkR Jobs. Builds R dependencies and ships with Spark.
  -r repo               Repository address.
  -t tag                Tag to apply to the built image, or to identify the image to be pushed.
  -m                    Use minikube's Docker daemon.
  -n                    Build docker image with --no-cache
  -b arg      Build arg to build or push the image. For multiple build args, this option needs to
              be used separately for each build arg.

Using minikube when building images will do so directly into minikube's Docker daemon.
There is no need to push the images into minikube in that case, they'll be automatically
available when running applications inside the minikube cluster.

Check the following documentation for more information on using the minikube Docker daemon:

  https://kubernetes.io/docs/getting-started-guides/minikube/#reusing-the-docker-daemon

Examples:
  - Build image in minikube with tag "testing"
    $0 -m -t testing build

  - Build and push image with tag "v2.3.0" to docker.io/myrepo
    $0 -r docker.io/myrepo -t v2.3.0 build
    $0 -r docker.io/myrepo -t v2.3.0 push
EOF
}

if [[ "$@" = *--help ]] || [[ "$@" = *-h ]]; then
  usage
  exit 0
fi

REPO=
TAG=
BASEDOCKERFILE=
PYDOCKERFILE=
RDOCKERFILE=
NOCACHEARG=
BUILD_PARAMS=
while getopts f:p:R:mr:t:nb: option
do
 case "${option}"
 in
 f) BASEDOCKERFILE=${OPTARG};;
 p) PYDOCKERFILE=${OPTARG};;
 R) RDOCKERFILE=${OPTARG};;
 r) REPO=${OPTARG};;
 t) TAG=${OPTARG};;
 n) NOCACHEARG="--no-cache";;
 b) BUILD_PARAMS=${BUILD_PARAMS}" --build-arg "${OPTARG};;
 m)
   if ! which minikube 1>/dev/null; then
     error "Cannot find minikube."
   fi
   if ! minikube status 1>/dev/null; then
     error "Cannot contact minikube. Make sure it's running."
   fi
   eval $(minikube docker-env)
   ;;
 esac
done

case "${@: -1}" in
  build)
    build
    ;;
  push)
    if [ -z "$REPO" ]; then
      usage
      exit 1
    fi
    push
    ;;
  *)
    usage
    exit 1
    ;;
esac
