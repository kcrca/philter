#!/bin/sh
set -e
cd `dirname $0`
./favicon.sh
dst_dir="philter"
rsync -c -avz --delete --exclude=src --exclude='.??*' --exclude='*'.ai --exclude='*'.sh --exclude='?' --exclude='.?' --exclude='favicon.p*' . kcrca_claritypack@ssh.phx.nearlyfreespeech.net:$dst_dir/
