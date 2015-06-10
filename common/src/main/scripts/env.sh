# LabCAS environment setup

export OODT_HOME=$LABCAS_HOME
export CATALINA_HOME=$LABCAS_HOME/apache-tomcat
export SOLR_HOME=$LABCAS_HOME/solr-home
export CATALINA_OPTS='-Dsolr.solr.home=$SOLR_HOME'
# FIXME
#export PGE_ROOT=$LABCAS_HOME/biomarker-discovery

export FILEMGR_URL=http://localhost:9000
export WORKFLOW_URL=http://localhost:9001

