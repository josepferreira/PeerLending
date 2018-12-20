#!/bin/bash
# Starts server
../../gpb/bin/protoc-erl -I. ccs.proto
rebar3 compile
erlc  -I ../../gpb/include ccs.erl
echo "c(login_manager) . c(frontend) . c(frontend_client) . c(frontend_state) . frontend:start(). " | erl -pa _build/default/lib/chumak/ebin -eval