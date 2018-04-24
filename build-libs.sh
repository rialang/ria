#!/usr/bin/env bash
echo "Building library in $1"
java -cp "$1:$2" ria.lang.compiler.RiaBoot modules/core/ "$1" ria/lang/std
java -cp "$1:$2" ria.lang.compiler.RiaBoot modules/compiler/ "$1" ria/lang/std:ria/lang/io
# At this point the compiler should be working
java -cp "$1:$2" ria.lang.compiler.ria -d "$1" -preload ria/lang/std:ria/lang/io modules/libs/
