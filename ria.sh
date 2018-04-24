#!/usr/bin/env bash
BASE_DIR=`dirname $0`
# Build the package if it doesn't exist
[ -f ${BASE_DIR}/target/ria-0.7.0.jar ] || mvn package
# Now run the package
java -jar ${BASE_DIR}/target/ria-0.7.0.jar "$@"