#!/bin/bash
# Starts server
../../gpb/bin/protoc-erl -I. ccs.proto
erlc  -I ../../gpb/include ccs.erl
echo "c(login_manager) . c(frontend) . c(frontend_state) . frontend:start(). " | erl