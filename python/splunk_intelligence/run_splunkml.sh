#! /bin/bash

BASEDIR=$(dirname "$0")
cd $BASEDIR

if [ ! -d ".pyenv" ]; then
   easy_install virtualenv
   make init
fi

make dist
echo $@
#Running locally
if [ -d "dist" ]; then
    source .pyenv/bin/activate; cd dist/splunk_pyml; python SplunkIntelOptimized.pyc $@
else
    source .pyenv/bin/activate; python SplunkIntelOptimized.pyc $@
fi
