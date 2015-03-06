#!/bin/bash
#
# Usage: build-solo-json.sh <list of apps (cookbooks)>
#
# This script will overwrite solo.json with a file containing the given cookbooks.
# It will also include any ::lib or ::validate recipes found
#

THISDIR=$(cd $(dirname $0) && pwd)
apps="$@"

temp=$(mktemp /tmp/$(basename $0)-solo.json.XXXXXXX)

echo "{ \"run_list\": [ " >> ${temp}

added=0
for app in ${apps} ; do
    if [ -f ${THISDIR}/cookbooks/${app}/recipes/lib.rb ] ; then
        if [ $added -ne 0 ] ; then echo ", " >> ${temp} ; fi
        echo -n "\"recipe[${app}::lib]\"" >> ${temp}
        added=1
    fi
done

for app in ${apps} ; do
    if [ -f ${THISDIR}/cookbooks/${app}/recipes/default.rb ] ; then
        if [ $added -ne 0 ] ; then echo ", " >> ${temp} ; fi
        echo -n "\"recipe[${app}]\"" >> ${temp}
        added=1
    else
      echo "No default recipe found for ${app}"
      rm -f ${temp}
      exit 1
    fi
done

for app in ${apps} ; do
    if [ -f ${THISDIR}/cookbooks/${app}/recipes/validate.rb ] ; then
        if [ $added -ne 0 ] ; then echo ", " >> ${temp} ; fi
        echo -n "\"recipe[${app}::validate]\"" >> ${temp}
        added=1
    fi
done

echo "" >> ${temp}
echo "] }" >> ${temp}

SOLO=${THISDIR}/solo.json
cp ${SOLO} ${THISDIR}/backup-solo.json.$(date +%Y-%m-%d-%H-%m-%S)
cat ${temp} > ${SOLO}
rm -f ${temp}
