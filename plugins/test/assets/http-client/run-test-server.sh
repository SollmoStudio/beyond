#!/usr/bin/env bash

mkfifo fifo || exit
trap 'rm -f fifo' 0

# Run server
node plugins/test/assets/http-client/test-server.js > fifo &

# Wait until reading 'listening'
while read line; do
  case $line in
    listening) break;
  esac
done < fifo
