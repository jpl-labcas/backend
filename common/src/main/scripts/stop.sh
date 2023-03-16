#!/bin/bash

. $LABCAS_HOME/env.sh

# Stop all LabCAS backend services

# Solr
cd $SOLR_DIR/solr/bin
./solr stop -p 8984

# Apache Tomcat
cd $CATALINA_HOME/bin
./catalina.sh stop

# CAS File Manager
cd $OODT_HOME/cas-filemgr/bin
./filemgr stop

# CAS Workflow Manager
cd $OODT_HOME/cas-workflow/bin
./wmgr stop

# CAS Resource Manager
cd $OODT_HOME/cas-resource/bin
./resmgr stop

cd $LABCAS_HOME
