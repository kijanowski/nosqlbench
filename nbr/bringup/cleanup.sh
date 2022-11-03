docker stop grafana
docker stop prom
docker stop graphite-exporter
docker stop victoria

rm -rf ${HOME}/.nosqlbench/
