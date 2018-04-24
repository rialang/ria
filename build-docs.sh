#!/usr/bin/env bash
BASE_DIR=`dirname $0`

${BASE_DIR}/ria.sh -doc docs/modules modules/core/* modules/compiler/eval.ria modules/compiler/showtype.ria modules/compiler/doc.ria "$@"
