#!/bin/bash

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}

outfile=${BASE}/../cloudos-apps/apps/cloudos/files/cloudos.sql

${BASE}/../cloudos-lib/gen-sql.sh cloudos_test ${outfile}
