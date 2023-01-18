#!/bin/sh
set -e

cd $(dirname $0)

convert icon.png -define icon:auto-resize=256,128,64,48,32,16 favicon.ico
