#!/bin/bash
#
# Usage: ./deploy.sh user@host [solo-json]
#
# user@host: target to deploy. user must have password-less sudo privileges.
# solo-json: path to a Chef run list (typically solo.json) to use. Otherwise a minimal one is generated based on defaults.
#
# Optional environment variables:
#
#   INIT_FILES  -- if set, a directory containing data bags and certs for the chef-run.
#                  if not set, script checks for a directory named "init_files" in the current directory
#
#   SSH_KEY     -- if set, the path to the private key to use when connecting
#                  if not set, script checks for existence of ~/.ssh/id_dsa or ~/.ssh/id_rsa (in that order)
#
#   JSON_EDIT   -- if set, a command to edit JSON documents, typically a path to "cos json" on a cloudos instance
#                  if not set and the "cos" command is on the PATH, then "cos json" is used.
#                  if not set and the "cos" command is not found, the script assumes a development environment nd
#                     loads JsonEditor directly from the first "target/cloudos-server-*.jar" it finds under
#                     the directory of this script
#
#   DISABLE_DNS -- if set, the cloudos-dns app will not be installed on the cloudstead. This is the case
#                     for cloudsteads launched from cloudstead.io, because they use the cloudos-dns server
#                     that runs there.
#                  if not set, script will install cloudos-dns so owner can fine-tune how DNS is managed,
#                     separately from cloudos
#

function die {
  echo 1>&2 "${1}"
  exit 1
}

BASE=$(cd $(dirname $0) && pwd)
cd ${BASE}
CLOUDOS_BASE=$(cd ${BASE}/../.. && pwd)

if [ -z "${JSON_EDIT}" ] ; then
  COS="$(which cos)"
  if [ ! -z "${COS}" ] ; then
    JSON_EDIT="${COS} json"
  else
    JSON_EDIT="java -cp $(find $(find ${BASE} -type d -name target) -type f -name "cloudos-server-*.jar") | head -1) org.cobbzilla.util.json.main.JsonEditor"
  fi
fi

DEPLOYER=${BASE}/deploy_lib.sh
if [ ! -x ${DEPLOYER} ] ; then
  DEPLOYER=${CLOUDOS_BASE}/cloudos-lib/chef-repo/deploy_lib.sh
  if [ ! -x ${DEPLOYER} ] ; then
    die "ERROR: deployer not found or not executable: ${DEPLOYER}"
  fi
fi

host="${1:?no user@host specified}"
SOLO_JSON="${2}"

# SSH key
DEFAULT_DSA_KEY="${HOME}/.ssh/id_dsa"
DEFAULT_RSA_KEY="${HOME}/.ssh/id_rsa"
if [ -z ${SSH_KEY} ] ; then
  SSH_KEY="${DEFAULT_DSA_KEY}"
  if [ ! -f "${SSH_KEY}" ] ; then
    SSH_KEY="${DEFAULT_RSA_KEY}"
    if [ ! -f "${SSH_KEY}" ] ; then
      die "SSH_KEY environment variable was not defined and neither ${DEFAULT_DSA_KEY} nor ${DEFAULT_RSA_KEY} exists"
    fi
  fi
fi

# init files
if [ -z "${INIT_FILES}" ] ; then
  INIT_FILES="${BASE}/init_files"
fi
if [ ! -d "${INIT_FILES}" ] ; then
  die "No init_files configuration found in ${INIT_FILES}"
fi

function append_recipe () {
  local json="$1"
  local recipe="$2"
  local TMP=$(mktemp /tmp/append_recipe.XXXXXX) || die "append_recipe: error creating temp file"
  ${JSON_EDIT} -f ${json} -o write -p run_list[] -v \"${recipe}\" -w ${TMP} > /dev/null 2>&1 || die "append_recipe: error appending ${recipe} to ${json}"
  echo ${TMP}
}

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

if [ -z "${SOLO_JSON}" ] ; then
  SOLO_JSON="${BASE}/solo-base.json"
  TMP_SOLO=$(mktemp /tmp/cloudos-solo-json.XXXXXX) || die "Error creating temp file for solo.json"

  CLOUDOS_INIT_BAG="${INIT_FILES}/data_bags/cloudos/init.json"
  if [ ! -f ${CLOUDOS_INIT_BAG} ] ; then
    die "No ${CLOUDOS_INIT_BAG} found"
  fi

  # If not using Dyn directly from cloudos, install cloudos-dns
  if [ -z "${DISABLE_DNS}" ] ; then
    DYN_ZONE=$(${JSON_EDIT} -f ${CLOUDOS_INIT_BAG} -o read -p dns.zone | tr -d ' ')
    if [ -z "${DYN_ZONE}" ] ; then
      echo "No DynDNS config detected, enabling cloudos-dns..."
      REQUIRED="${REQUIRED} \
        data_bags/cloudos-dns/init.json \
        data_bags/cloudos-dns/ports.json"

      # Add cloudos-dns recipe
      SOLO_JSON="$(append_recipe ${SOLO_JSON} "recipe[cloudos-dns]")"
    fi
  fi

  # Add cloudos-validate recipe
  SOLO_JSON="$(append_recipe ${SOLO_JSON} "recipe[cloudos::validate]")"
fi

${DEPLOYER} ${host} ${INIT_FILES} "${REQUIRED}" "${COOKBOOK_SOURCES}" ${SOLO_JSON}
