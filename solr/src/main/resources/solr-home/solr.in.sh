SOLR_HEAP="2g"

# By default we now run Solr with SSL on:
SOLR_SSL_ENABLED=true
SOLR_SSL_KEY_STORE=${LABCAS_HOME}/etc/solr-ssl.keystore.p12
SOLR_SSL_KEY_STORE_PASSWORD=secret
SOLR_SSL_TRUST_STORE=${LABCAS_HOME}/etc/solr-ssl.keystore.p12
SOLR_SSL_TRUST_STORE_PASSWORD=secret

# But clients don't need authenticate:
SOLR_SSL_NEED_CLIENT_AUTH=false

# But it'd be nice if they could:
SOLR_SSL_WANT_CLIENT_AUTH=true

# Our certs are terrible:
SOLR_SSL_CHECK_PEER_NAME=false
