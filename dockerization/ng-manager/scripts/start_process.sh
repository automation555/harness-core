#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

if [[ -v "{hostname}" ]]; then
   export HOSTNAME=$(hostname)
fi

if [[ -z "$MEMORY" ]]; then
   export MEMORY=2096m
fi

if [[ -z "$COMMAND" ]]; then
   export COMMAND=server
fi

echo "Using memory " "$MEMORY"

if [[ -z "$CAPSULE_JAR" ]]; then
   export CAPSULE_JAR=/opt/harness/ng-manager-capsule.jar
fi

if [[ "${ENABLE_SERIALGC}" == "true" ]]; then
    export GC_PARAMS=" -XX:+UseSerialGC -Dfile.encoding=UTF-8"
else
    export GC_PARAMS=" -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=40 -XX:MaxGCPauseMillis=1000 -Dfile.encoding=UTF-8"
fi

export JAVA_OPTS="-Xmx${MEMORY} -XX:+HeapDumpOnOutOfMemoryError -Xloggc:mygclogfilename.gc $GC_PARAMS $JAVA_ADVANCED_FLAGS"

if [[ "${ENABLE_APPDYNAMICS}" == "true" ]]; then
    mkdir /opt/harness/AppServerAgent-20.8.0.30686 && unzip AppServerAgent-20.8.0.30686.zip -d /opt/harness/AppServerAgent-20.8.0.30686
    node_name="-Dappdynamics.agent.nodeName=$(hostname)"
    JAVA_OPTS=$JAVA_OPTS" -javaagent:/opt/harness/AppServerAgent-20.8.0.30686/javaagent.jar -Dappdynamics.jvm.shutdown.mark.node.as.historical=true"
    JAVA_OPTS="$JAVA_OPTS $node_name"
    echo "Using Appdynamics java agent"
fi

if [[ "${ENABLE_OVEROPS}" == "true" ]] ; then
    echo "OverOps is enabled"
    JAVA_OPTS=$JAVA_OPTS" -agentpath:/opt/harness/takipi/lib/libTakipiAgent.so -Dtakipi.etl -Dtakipi.application.name=${OVEROPS_APPLICATION_NAME} -Dtakipi.deployment.name=${OVEROPS_DEPLOYMENT_NAME}"
    echo "Using Overops Java Agent"
fi


if [[ "${DEPLOY_MODE}" == "KUBERNETES" || "${DEPLOY_MODE}" == "KUBERNETES_ONPREM" || "${DEPLOY_VERSION}" == "COMMUNITY" ]]; then
    java $JAVA_OPTS -jar $CAPSULE_JAR $COMMAND /opt/harness/config.yml
else
    java $JAVA_OPTS -jar $CAPSULE_JAR $COMMAND /opt/harness/config.yml > /opt/harness/logs/ng-manager.log 2>&1
fi
