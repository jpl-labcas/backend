# LabCAS environment setup

export OODT_HOME=$LABCAS_HOME
export CATALINA_HOME=$LABCAS_HOME/apache-tomcat

export SOLR_HOME=$LABCAS_HOME/solr-home
export SOLR_DATA_DIR=$LABCAS_HOME/solr-index
export CATALINA_OPTS='-Dsolr.solr.home=$SOLR_HOME -Dsolr.data.dir=$SOLR_DATA_DIR'

#export PGE_ROOT=$LABCAS_HOME/pges

export FILEMGR_URL=http://localhost:9000
export WORKFLOW_URL=http://localhost:9001

