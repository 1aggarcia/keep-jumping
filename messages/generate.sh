#!/bin/bash

# Generate protobuf classes in TypeScript and Java, put them in the correct
# places in the /client and /server directories

TS_PLUGIN_PATH="../client/node_modules/.bin/protoc-gen-ts"
TS_OUT_PATH="../client/src/generated"
JAVA_OUT_PATH="../server/src/main/java"
PROTO_FILE_PATH="./socketMessage.proto"

protoc \
    --plugin="protoc-gen-ts=$TS_PLUGIN_PATH" \
    --ts_opt=esModuleInterop=true \
    --java_out=$JAVA_OUT_PATH \
    --js_out=$TS_OUT_PATH \
    --ts_out=$TS_OUT_PATH \
    $PROTO_FILE_PATH

# The JS files arent needed, but protoc needs to generate them for the TS file
rm ../client/src/generated/*.js
