# LabCAS environment setup

export OODT_HOME=$LABCAS_HOME
export CATALINA_HOME=$LABCAS_HOME/apache-tomcat
export JAVA_OPTS=-Xss2m

export SOLR_DIR=$LABCAS_HOME
export SOLR_HOME=$LABCAS_HOME/solr-home
export SOLR_DATA_DIR=$LABCAS_HOME/solr-index
export SOLR_URL=http://localhost:8983/solr/oodt-fm

#export PGE_ROOT=$LABCAS_HOME/pges

export FILEMGR_URL=http://localhost:9000
export WORKFLOW_URL=http://localhost:9001
export RESMGR_URL=http://localhost:9002


