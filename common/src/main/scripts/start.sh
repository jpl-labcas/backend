#!/bin/bash
. ./env.sh

# Start all LabCAS backend services

# OODT File Manager
cd $OODT_HOME/cas-filemgr/bin
./filemgr start

# OODT Workflow Manager
cd $OODT_HOME/cas-workflow/bin
./wmgr start

# OODT Resource Manager
cd $OODT_HOME/cas-resource/bin
./resmgr start

# Apache Tomcat
cd $CATALINA_HOME/bin
./catalina.sh start

# Solr
cd $SOLR_DIR/solr/bin
./solr start -p 8983 -s $SOLR_HOME

cd $LABCAS_HOME

echo ""
echo "----------------------------------"
echo "Currently running LabCAS services:"
echo "----------------------------------"
ps -ef | grep oodt
ps -ef | grep catalina
ps -ef | grep solr
