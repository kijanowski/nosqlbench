#!/bin/bash
export USER_ID=$(id -u)
export GROUP_ID=$(id -u)

set -x
for rw_dir in grafana victoria-metrics-data
do
 fqdir=${HOME}/.nosqlbench/$rw_dir
 mkdir -p $fqdir
 chmod -R 775 $fqdir
done

docker-compose -f docker-metrics.yaml up -d

