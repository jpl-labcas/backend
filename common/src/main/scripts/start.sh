#!/bin/bash

. $LABCAS_HOME/env.sh

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
# solr.autoSoftCommit.maxTime: time to use a new searcher after last update (changes are visible to clients, but not persisted to disk)
# solr.autoCommit.maxTime: time to commit changes to disk after last update
cd $SOLR_DIR/solr/bin
env SOLR_INCLUDE=${LABCAS_HOME}/solr-home/solr.in.sh ./solr start -memory 1g -p 8984 -s $SOLR_HOME -Dsolr.autoSoftCommit.maxTime=1000 -Dsolr.autoCommit.maxTime=10000

cd $LABCAS_HOME

echo ""
echo "----------------------------------"
echo "Currently running LabCAS services:"
echo "----------------------------------"
ps -ef | egrep '[o]odt'
ps -ef | egrep '[c]atalina'
ps -ef | egrep '[s]olr'
