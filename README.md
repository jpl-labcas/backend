LabCAS Backend
==============

Repository containing back-end services and configuration
for executing EDRN LabCAS data processing workflows.


=================

INSTALLATION INSTRUCTIONS

o Prerequisites

Java 1.7+
Unix OS (Linux, MacOSX, ...)

o Define environment

export LABCAS_HOME=/usr/local/labcas
	- location where all software components will be installed

export DATA_ARCHIVE=/usr/local/data
	- location where files will be archived


o Check out code from repository

cd <any source directory>
git checkout https://github.com/EDRN/labcas-backend.git

o Compile and install

cd labcas-workflows
mvn install
	- will install all back-end services
	
mvn install -Dworkflow=biomarker-discovery
	- will install the specific configuration for "biomarker-discovery" workflow on top of the existing services
	
to only upgrade specific components: cd to the module subdirectory and mvn install, for example:
cd solr
mvn install

o Clean up

mvn clean
	- will remove all services from $LABCAS_HOME

mvn clean -Dworkflow=biomarker-discovery
	- will remove the specific workflow configuration
	

