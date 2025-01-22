#!/bin/sh

: "${LABCAS_HOME:?The LABCAS_HOME must be set}"

if [ $# -ne 1 ]; then
    echo "Usage: file.jwt" 1>&2
    exit 1
fi

web_inf=$LABCAS_HOME/apache-tomcat/webapps/labcas-backend-data-access-api/WEB-INF
lib_dir=${web_inf}/lib
classes_dir=${web_inf}/classes

if [ \! -d $web_inf ]; then
    echo "The directory $web_inf does not exist; we need the .class and .jar files under it" 1>&2
    exit 1
fi

cp=${classes_dir}:`echo ${lib_dir}/* | tr ' ' ':'`

exec java -classpath $cp gov.nasa.jpl.labcas.data_access_api.jwt.JwtConsumer "$@"
