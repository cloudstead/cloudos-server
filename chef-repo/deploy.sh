#!/bin/bash
#
# Usage: ./deploy.sh [host]
#
# Required environment variables:
# SSH_KEY    -- path to the private key to use when connecting
#
# INIT_FILES -- a directory containing data bags and certs for the chef-run. See README.md
#

function die {
  echo 1>&2 "${1}"
  exit 1
}

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}
CLOUDOS_BASE=$(cd ${BASE}/../.. && pwd)

DEPLOYER=${BASE}/deploy_lib.sh
if [ ! -x ${DEPLOYER} ] ; then
  DEPLOYER=${CLOUDOS_BASE}/cloudos-lib/chef-repo/deploy_lib.sh
  if [ ! -x ${DEPLOYER} ] ; then
    die "ERROR: deployer not found or not executable: ${DEPLOYER}"
  fi
fi

host="${1:?no user@host specified}"

if [ -z ${SSH_KEY} ] ; then
  die "SSH_KEY is not defined in the environment."
fi

REQUIRED=" \
data_bags/cloudos/base.json \
data_bags/cloudos/init.json \
data_bags/cloudos/ports.json \
data_bags/email/init.json \
certs/cloudos/ssl-https.key \
certs/cloudos/ssl-https.pem \
"

COOKBOOK_SOURCES=" \
${CLOUDOS_BASE}/cloudos-lib/chef-repo/cookbooks \
$(find ${CLOUDOS_BASE}/cloudos-apps/apps -type d -name cookbooks) \
"

SOLO_JSON="${BASE}/solo-base.json"
TMP_SOLO=$(mktemp /tmp/cloudos-solo-json.XXXXXX) || die "Error creating temp file for solo.json"

CLOUDOS_INIT_BAG="${INIT_FILES}/data_bags/cloudos/init.json"
if [ ! -f ${CLOUDOS_INIT_BAG} ] ; then
  die "No ${CLOUDOS_INIT_BAG} found"
fi

# If not using Dyn directly from cloudos, install cloudos-dns
JSON_EDIT="java -cp ${BASE}/../target/cloudos-server-*.jar org.cobbzilla.util.json.main.JsonEditor"
DYN_ZONE=$(${JSON_EDIT} -f ${CLOUDOS_INIT_BAG} -o read -p dns.zone | tr -d ' ')
if [ -z "${DYN_ZONE}" ] ; then
  echo "No DynDNS config detected, enabling cloudos-dns..."
  REQUIRED="${REQUIRED} \
    data_bags/cloudos-dns/init.json \
    data_bags/cloudos-dns/ports.json"

  # Add cloudos-dns recipe
  ${JSON_EDIT} -f ${SOLO_JSON} -o write -p run_list[] -v \"recipe[cloudos-dns]\" > ${TMP_SOLO}
else
  cat ${SOLO_JSON} > ${TMP_SOLO}
fi

# Add cloudos recipe
SOLO_JSON=$(mktemp /tmp/cloudos-solo-json.XXXXXX) || die "Error creating final temp file for solo.json"
${JSON_EDIT} -f ${TMP_SOLO} -o write -p run_list[] -v \"recipe[cloudos]\" > ${SOLO_JSON}

${DEPLOYER} ${host} ${INIT_FILES} "${REQUIRED}" "${COOKBOOK_SOURCES}" ${SOLO_JSON}
