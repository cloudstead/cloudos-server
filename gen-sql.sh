#!/bin/bash

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}

outfile=${BASE}/../cloudos-apps/apps/cloudos/files/cloudos.sql

SILENT="${1}"
if [ ! -z "${SILENT}" ] ; then
  ${BASE}/../cloudos-lib/gen-sql.sh cloudos_test ${outfile} 1> /dev/null 2> /dev/null
else
  ${BASE}/../cloudos-lib/gen-sql.sh cloudos_test ${outfile}
fi
