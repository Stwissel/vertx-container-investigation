#!/bin/sh
# Starts the vert.x sample application
export V_PATH=/opt/demo/resources:/opt/demp/classes:/opt/demo/libs/*
export CLASSPATH=.:$V_PATH:$CLASSPATH
java -XX:+HeapDumpOnOutOfMemoryError -XX:+UseG1GC -XX:+UseStringDeduplicationJVM -agentlib:jdwp=transport=dt_socket,server=y,address=0.0.0.0:8000,suspend=y -cp $CLASSPATH demo.sherlock.container.MainVerticle
