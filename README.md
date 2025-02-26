LabCAS Backend
==============

Repository containing back-end services and configuration for executing EDRN LabCAS data processing workflows.


Documentation
-------------

See the `docs/documentation.pdf` file.


Development
-----------

To build locally, maybe try (sh-compatible users):

    mkdir /tmp/labcas
    export "JAVA_HOME=`/usr/libexec/java_home --version 1.8.0`"
    export LABCAS_HOME=/tmp/labcas
    export PATH=${JAVA_HOME}/bin:$PATH
    mvn clean install

Or for csh-compatible users:

    mkdir /tmp/labcas
    setenv JAVA_HOME `/usr/libexec/java_home --version 1.8.0`
    setenv LABCAS_HOME /tmp/labcas
    set path = ( ${JAVA_HOME}/bin $path )
    mvn clean install

To run locally, try (for sh-compatible users):

    export "JAVA_HOME=`/usr/libexec/java_home --version 1.8.0`"
    export LABCAS_HOME=/tmp/labcas
    cd $LABCAS_HOME
    ./start.sh

And if you insist on still using a csh-compatible shell:

    setenv JAVA_HOME `/usr/libexec/java_home --version 1.8.0`
    setenv LABCAS_HOME /tmp/labcas
    cd $LABCAS_HOME
    ./start.sh

Note: you'll need a `~/.keystore` and a `~/labcas.properties`.

To try it, try:

    curl --insecure 'https://localhost:8444/labcas-backend-data-access-api/ping?message=Hello+world'

You can exercise the `/auth` endpoint as follows:

    curl --request POST --insecure \
        --url 'https://localhost:8444/labcas-backend-data-access-api/auth' \
        --header 'Content-type: application/x-www-form-urlencoded' \
        --data-urlencode 'username=USERNAME' \
        --data-urlencode 'password=PASSWORD' > /tmp/jwt

substituting values for `USERNAME` and `PASSWORD`, of course. The JWT will be in `/tmp/jwt`.

To use that JWT, try downloading a file:

    curl --verbose --request GET --insecure \
        --url 'https://localhost:8444/labcas-backend-data-access-api/download?id=UMiami_RP/Documentation/UM_RP.pptx' \
        --cookie "JasonWebToken=`cat /tmp/jwt`" \
        --header "Authentication: Bearer `cat /tmp/jwt`"

Note: I'm not sure why you need both `--cookie` and `--header` 🤷.

To exercise the `UserServiceLdapImpl`'s `main` method:

    java -classpath data-access-api/target/classes gov.nasa.jpl.labcas.data_access_api.filter.UserServiceLdapImpl USERNAME PASSWORD

To exercise JwtConsumer's `main` method (after doing `mvn clean install` and `start.sh`):

    ./support/jwt.sh JWTFILE


## Zipperlab Integration

To test sending queries to Zipperlab, first get your JWT (above) then do:

    curl --request POST --verbose --insecure \
        --cookie "JasonWebToken=`cat /tmp/jwt`" \
        --header "Authentication: Bearer `cat /tmp/jwt`" \
        --header 'Content-Type: application/x-www-form-urlencoded' \
        --data 'email=hello@a.co&query=id:Pre-diagnostic_PDAC_Images/City_of_Hope/COH_0171/COH_01710003/DICOM/I883*' \
        https://localhost:8444/labcas-backend-data-access-api/zip

Make sure you have Zipperlab running and set its URL in `~/labcas.properties`.

If you want to send file IDs instead, do:

    curl --request POST --verbose --insecure \
        --cookie "JasonWebToken=`cat /tmp/jwt`" \
        --header "Authentication: Bearer `cat /tmp/jwt`" \
        --header 'Content-Type: application/x-www-form-urlencoded' \
        --data 'email=hello@a.co&id=FILE1&id=FILE2&id=FILE3' \
        https://localhost:8444/labcas-backend-data-access-api/zip




## Loading Solr Data

If you have [downloaded backups of Solr data you can reload it](https://github.com/EDRN/EDRN-metadata/issues/122) generally as follows:

    curl --insecure --verbose "https://localhost:8984/solr/collections/replication?command=restore&location=$BACKUP_PATH/collections"
    curl --insecure --verbose "https://localhost:8984/solr/datasets/replication?command=restore&location=$BACKUP_PATH/datasets"
    curl --insecure --verbose "https://localhost:8984/solr/files/replication?command=restore&location=$BACKUP_PATH/files"

Replace `$BACKUP_PATH` with the location of the downloaded backups.