#!/bin/bash -e

CACHE_DIR_PREFIX_KEY="rubix.cache.dirprefix.list"
CACHE_DIR_SUFFIX_KEY="rubix.cache.dirsuffix"
CACHE_DIR_MAX_DISKS_KEY="rubix.cache.max.disks"
CACHE_DIR_PREFIX_VALUE=/media/ephemeral
CACHE_DIR_SUFFIX_VALUE=/fcache/
CACHE_DIR_MAX_DISKS_VALUE=5

BASE_DIR=`dirname $0`
BASE_DIR=`cd "$BASE_DIR"; pwd`

RUN_DIR=${BASE_DIR}/bks
PID_FILE=${RUN_DIR}/bks.pid
LOG4J_FILE=${RUN_DIR}/log4j.properties

CUR_DATE=$(date '+%Y-%m-%dT%H-%M-%S')
LOG_DIR=${RUN_DIR}/logs
LOG_FILE=${LOG_DIR}/bks-${CUR_DATE}.log
SCRIPT_LOG_FILE=${LOG_DIR}/bks-script.log

HADOOP_DIR=/usr/lib/hadoop2
HADOOP_JAR_DIR=${HADOOP_DIR}/share/hadoop/tools/lib

setup-log4j() {
(cat << EOF

log4j.rootLogger=DEBUG, R

log4j.appender.R=org.apache.log4j.RollingFileAppender
log4j.appender.R.File=${LOG_FILE}
log4j.appender.R.MaxFileSize=100MB
log4j.appender.R.MaxBackupIndex=5
log4j.appender.R.layout=org.apache.log4j.PatternLayout
log4j.appender.R.layout.ConversionPattern=%d{yy/MM/dd HH:mm:ss,SSS} %p %t %c{2}: %m%n

log4j.logger.com.qubole.rubix=DEBUG
log4j.logger.org.apache.hadoop.fs.s3a.S3AFileSystem=DEBUG

EOF
) > ${LOG4J_FILE}
}

set-cache-options() {
  for option in "$@"
  do
    option_key=$(echo "${option}" | sed -n 's/-D\(.*\)=\(.*\)/\1/p')
    option_value=$(echo "${option}" | sed -n 's/-D\(.*\)=\(.*\)/\2/p')
    case "$option_key" in
      ${CACHE_DIR_PREFIX_KEY}) CACHE_DIR_PREFIX_VALUE="${option_value}";;
      ${CACHE_DIR_SUFFIX_KEY}) CACHE_DIR_SUFFIX_VALUE="${option_value}";;
      ${CACHE_DIR_MAX_DISKS_KEY}) CACHE_DIR_MAX_DISKS_VALUE="${option_value}";;
    esac
  done
}

setup-disks() {
  PREFIX=${CACHE_DIR_PREFIX_VALUE}
  SUFFIX=${CACHE_DIR_SUFFIX_VALUE}
  MAX_DISKS=${CACHE_DIR_MAX_DISKS_VALUE}

  for i in $(seq 0 $((MAX_DISKS-1)))
  do
    CACHE_DIR=${PREFIX}${i}${SUFFIX}
    mkdir -p ${CACHE_DIR}
    chmod -R 777 ${CACHE_DIR}
  done
}

copy-jars-for-containers() {
  SCRIPT_DIR=$1

  RUBIX_JARS=`ls rubix-*/target/rubix-*.jar | grep -E -v 'tests|client|rpm|presto'`
  cp ${RUBIX_JARS} ${SCRIPT_DIR}/docker/jars/

  RUBIX_CLIENT_TEST_JAR=`ls rubix-client/target/rubix-client-*-tests.jar`
  cp ${RUBIX_CLIENT_TEST_JAR} ${SCRIPT_DIR}/docker/jars/

  RUBIX_CORE_TEST_JAR=`ls rubix-core/target/rubix-core-*-tests.jar`
  cp ${RUBIX_CORE_TEST_JAR} ${SCRIPT_DIR}/docker/jars/rubix-core_tests.jar
}


getNumberOfWorkers () {
  echo "testing 3A"
  numWorkerString=$1
  echo ${numWorkerString}
  echo "testing 3B"
  array=$(echo $numWorkerString | tr "=" "\n")
  echo ${array}
  echo "testing 3C"
  value=`echo $array | awk '{ print $2 }'`
  echo ${value}
  echo "testing 3D"
  return $value
}

create-docker-compose() {
  echo "testing 5A"
  numberOfWOrkers=$1
  SCRIPT_DIR=$2
  echo "testing 5B"
#  bbb="version: '3.7'\nservices:\n\srubix-master:\n\s\sbuild:\n\t\t\tcontext: .\n\t\t\targs:\n\t\t\t\tis_master: \"true\"\n\t\tvolumes:\n\t\t\t- \${DATADIR}:/tmp/rubixTests\n\t\tnetworks:\n\t\t\tdefault:\n\t\t\t\tipv4_address: 172.18.8.0\n"
  bbb="version: '3.7'
services:
  rubix-master:
    build:
      context: .
      args:
        is_master: \"true\"
  volumes:
    \${DATADIR}:/tmp/rubixTests
  networks:
    default:
      ipv4_address:172.18.8.0"
  #aaa=$("\n\t\tdepends_on:\n\t\t\t- rubix-master\n\t\tbuild:\n\t\t\tcontext: .\n\t\t\targs:\n\t\t\t\tis_master: \"false\"\n\t\tvolumes:\n\t\t\t- \${DATADIR}:/tmp/rubixTests\n\t\tnetworks:\n\t\t\tdefault:\n\t\t\t\tipv4_address:")
  aaa="
    depends_on:
     - rubix-master
    build:
      context: .
      args:
        is_master: \"false\"
    volumes:
      \${DATADIR}:/tmp/rubixTests
    networks:
      default:
        ipv4_address:"
  echo "testing 5C"
  echo ${SCRIPT_DIR}
  echo "testing 5D"
  rm -f ${SCRIPT_DIR}/docker/docker-compose.yml
  echo "printing 2"
  echo -e "${bbb}" >> ${SCRIPT_DIR}/docker/docker-compose.yml

  for idx in $(seq 1 $(($numberOfWOrkers)))
  do
     echo ${idx}
     ipAddress=172.18.8.${idx}
     echo ${ipAddress}
     serviceName=rubix-worker-${idx}
     echo ${serviceName}
     serviceEntry="  ${serviceName}:${aaa}${ipAddress}"
#     echo -e "${serviceEntry}" >> ${SCRIPT_DIR}/docker/docker-compose.yml
  done
  echo "printing 3"
}

start-cluster() {
#  BKS_OPTIONS=$@
#  BKS_OPTIONS=${@:1:$#-1}
  echo "testing 2"
  lastOption="${@: -1}"
  echo ${lastOption}
  echo "testing 3"
  set +e
  getNumberOfWorkers ${lastOption}
  numberOfWorkers=`echo $?`
  echo ${numberOfWorkers}
  set -e
  SCRIPT_DIR=$(dirname "$0")
  echo "testing 5"
#  create-docker-compose ${numberOfWorkers} ${SCRIPT_DIR}
#  python ${SCRIPT_DIR}/docker/testing.py ${numberOfWorkers} ${SCRIPT_DIR}
  echo "testing 6"
  rm -f ${SCRIPT_DIR}/docker/jars/*
  mkdir -p ${SCRIPT_DIR}/docker/jars

  ###

  # :LINK JARS VIA DOCKER VOLUME

  ###
  copy-jars-for-containers ${SCRIPT_DIR}

  export DATADIR=$1
  docker-compose -f ${SCRIPT_DIR}/docker/docker-compose.yml up -d --build
}

stop-cluster() {
  SCRIPT_DIR=$(dirname "$0")

  export DATADIR=$1
  docker-compose -f ${SCRIPT_DIR}/docker/docker-compose.yml down -t 1
}

start-bks() {
  BKS_OPTIONS=$@
  set-cache-options ${BKS_OPTIONS}

  mkdir -p ${RUN_DIR}
  mkdir -p ${LOG_DIR}
  chmod -R 777 ${LOG_DIR}

  setup-disks
  setup-log4j

  bookkeeper_jars=( ${HADOOP_JAR_DIR}/rubix-bookkeeper-*.jar )
  bookkeeper_jar=${bookkeeper_jars[0]}

  export HADOOP_OPTS="-Dlog4j.configuration=file://${LOG4J_FILE}"
  nohup ${HADOOP_DIR}/bin/hadoop jar ${bookkeeper_jar} com.qubole.rubix.bookkeeper.BookKeeperServer ${BKS_OPTIONS} > ${LOG_DIR}/cbk.log 2>&1 &
  echo "$!" > ${PID_FILE}
  echo "Starting Cache BookKeeper server with pid `cat ${PID_FILE}`"

  sleep 1
}

stop-bks() {
  BKS_OPTIONS=$@
  set-cache-options ${BKS_OPTIONS}

  PID=`cat ${PID_FILE}`
  kill -9 ${PID}
  rm -f ${PID_FILE}

  PREFIX=${CACHE_DIR_PREFIX_VALUE}
  SUFFIX=${CACHE_DIR_SUFFIX_VALUE}
  MAX_DISKS=${CACHE_DIR_MAX_DISKS_VALUE}

  for i in $(seq 0 $((MAX_DISKS-1)))
  do
    rm -rf ${PREFIX}${i}${SUFFIX}
  done
}

cmd=$1
echo "testing 1"
case "$cmd" in
  start-bks) shift ; start-bks $@;;
  stop-bks) shift ; stop-bks $@;;
  start-cluster) shift ; start-cluster $@;;
  stop-cluster) shift ; stop-cluster $@;;
esac

exit 0;
