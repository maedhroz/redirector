#!/bin/bash

USAGE="Usage: $0 [port] [env] [JVM size, ex 600m]"

PORT=${1:?$USAGE}
ENV=${2:?$USAGE}
SIZE=${3:?$USAGE}

if [ "$4" == "debug" ]; then
        export DEBUG_OPTS='-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044'
        echo "Shard is starting in debug mode with hook @ port 1044."
fi

if [ "$5" == "suspend" ]; then
        export DEBUG_OPTS='-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044'
        echo "Suspend mode is activated. Please make sure you connect remotely to handle the breaks."
fi

export CLASSPATH=redirector.jar:lib/*

export JVM_GENERAL_OPTS="-Dapp-name=redirector"

export JVM_MEM_OPTS="-server -XX:+UseParNewGC -Xms$SIZE -Xmx$SIZE"

java -cp $CLASSPATH $DEBUG_OPTS $JVM_GENERAL_OPTS $JVM_MEM_OPTS com.steelhouse.honeycomb.redirector.RedirectorLauncher -p $PORT -e $ENV > /var/log/honeycomb/redirector/stdout.log 2> /var/log/honeycomb/redirector/stderr.log & echo $! > /var/run/honeycomb/redirector.pid
