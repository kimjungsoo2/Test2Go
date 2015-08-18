#! /system/bin/sh

PYTHONPATH="${PYTHONPATH}:/data/python/extras/python"
PYTHONPATH="${PYTHONPATH}:/data/python/lib/python2.7/lib-dynload"
export PYTHONPATH
export PYTHONHOME=/data/python
export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/data/python/lib
PATH=$PATH /data/python/bin/python "$@"
