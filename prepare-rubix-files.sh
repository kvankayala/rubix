#!/bin/bash -ex

while getopts "d:s:" opt; do
    case $opt in
        d)
            dest_dir=$OPTARG
            ;;
        s)
            src_dir=$OPTARG
            ;;
        \?)
            echo "Invalid option: -$OPTARG" >&2
            exit 1
            ;;
        :)
            echo "Option -$OPTARG requires an argument." >&2
            exit 1
            ;;
    esac
done

echo "Current Directory"
echo `pwd`d
echo "ls in src_dir"
echo `ls ${src_dir}`

#src_dir="/home/qubole-mason/karma2/workspace/build-pkgs@2/rubix"
#dest_dir= "/home/qubole-mason/karma2/workspace/build-pkgs/tmp/tmpxNbBzQ/rubix"
mkdir -p ${dest_dir}
mkdir -p ${dest_dir}/lib/
mkdir -p ${dest_dir}/bin/
dest_lib_dir="${dest_dir}/lib/"
dest_bin_dir="${dest_dir}/bin/"

RUBIX_SPI_JAR="${src_dir}/rubix-spi/target/rubix-spi-*SNAPSHOT.jar"
RUBIX_CORE_JAR="${src_dir}/rubix-core/target/rubix-core-*SNAPSHOT.jar"
RUBIX_COMMON_JAR="${src_dir}/rubix-common/target/rubix-common-*SNAPSHOT.jar"
RUBIX_PRESTO_JAR="${src_dir}/rubix-presto/target/rubix-presto-*SNAPSHOT.jar"
RUBIX_HADOOP2_JAR="${src_dir}/rubix-hadoop2/target/rubix-hadoop2-*SNAPSHOT.jar"
RUBIX_BOOKKEEPER_JAR="${src_dir}/rubix-bookkeeper/target/rubix-bookkeeper-*SNAPSHOT.jar"

CACHE_BOOKKEEPER="${src_dir}/cache-bookkeeper"

sudo cp $CACHE_BOOKKEEPER $dest_bin_dir
sudo cp $RUBIX_HADOOP2_JAR $dest_lib_dir
sudo cp $RUBIX_BOOKKEEPER_JAR $dest_lib_dir
sudo cp $RUBIX_COMMON_JAR $dest_lib_dir
sudo cp $RUBIX_CORE_JAR $dest_lib_dir
sudo cp $RUBIX_PRESTO_JAR $dest_lib_dir
sudo cp $RUBIX_SPI_JAR $dest_lib_dir

chmod a+x ${CACHE_BOOKKEEPER}
