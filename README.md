LabCAS Backend
==============

Repository containing back-end services and configuration for executing EDRN LabCAS data processing workflows.


Documentation
-------------

See the `docs/documentation.pdf` file.


Development
-----------

To build locally, maybe try:

    mkdir /tmp/labcas
    env "JAVA_HOME=`/usr/libexec/java_home --version 1.8.0`" LABCAS_HOME=/tmp/labcas mvn clean install
