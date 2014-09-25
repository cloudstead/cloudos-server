#!/bin/bash

DEPLOY=${1}
if [ -z ${DEPLOY} ] ; then
  echo "Usage $0 <deploy-dir>"
  exit 1
fi

mkdir -p ${DEPLOY}/email && cp email/* ${DEPLOY}/email
