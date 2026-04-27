# Dockerfile for the LabCAS backend
#
# To build the image for local use:
#
#    docker image build --tag labcas-backend --file docker/Dockerfile .
#
# The docker-compose.yaml usually starts this container, but you can manually start it with:
#
#     docker container run --rm --publish 8000:8000 \
#         --env LABCAS_ENV=docker \
#         --env LABCAS_DIRECTORY_PROVIDER=ldap \
#         --env LABCAS_LDAP_URI=ldaps://host.docker.internal:1636 \
#         --env LABCAS_LDAP_BIND_DN=uid=service,dc=edrn,dc=jpl,dc=nasa,dc=gov \
#         --env LABCAS_LDAP_PASSWORD=CHANGEME \
#         --env LABCAS_LDAP_USER_BASE=dc=edrn,dc=jpl,dc=nasa,dc=gov \
#         --env LABCAS_LDAP_GROUP_BASE=dc=edrn,dc=jpl,dc=nasa,dc=gov \
#         --env LABCAS_JWT_SECRET=super-CHANGEME \
#         --env LABCAS_REDIS_URL=redis://host.docker.internal:6379/0 \
#         --env LABCAS_SOLR_URL=https://host.docker.internal:8984/solr \
#         --env LABCAS_SOLR_VERIFY_SSL=False \
#         --env LABCAS_DOWNLOAD_BASE_URL=https://localhost:8000/data-access-api/download \
#         --env LABCAS_SUPER_OWNER_PRINCIPAL=cn=Super User,dc=edrn,dc=jpl,dc=nasa,dc=gov \
#         --env LABCAS_PUBLIC_OWNER_PRINCIPAL=cn=All Users,dc=edrn,dc=jpl,dc=nasa,dc=gov \
#         --env AWS_REGION=us-west-2 \
#         --env AWS_ACCESS_KEY_ID=CHANGEME \
#         --env AWS_SECRET_ACCESS_KEY=CHANGEME \
#         --env S3_BUCKET=s3://edrn-labcas \
#         --env LABCAS_FILE_PATH_PREFIX_REPLACEMENTS=/usr/local/labcas/backend:/Users/kelly,/labcas-data:/Users/kelly \
#         --env LABCAS_ZIPPERLAB_URL=https://host.docker.internal:6468/edrn/ \
#         --env ACCEPT_ANY_JWT=True \
#         labcas-backend
#
# Or use `--env-file /PATH/TO/YOUR/.env` to pass the environment file.
#
# The above assumes
#
# - The LDAP server is tunneled and available on ldaps://localhost:1636
# - Redist is running on redis://localhost:6379/0
# - The Redis URL is redis://host.docker.internal:6379/0
# - The Solr URL is https://host.docker.internal:8984/solr
# - The Zipperlab URL is https://host.docker.internal:6468/edrn/
# - And that the EDRN Directory's LDAP password for the service user is `CHANGEME`
#

FROM python:3.13-alpine

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY pyproject.toml README.md LICENSE.md /app/
COPY src /app/src
COPY tests /app/tests

RUN \
    apk update --quiet &&\
    apk add --no-cache openssl curl &&\
    :

RUN \
    pip install --upgrade pip &&\
    pip install --editable . &&\
    :

EXPOSE 8000

HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
    CMD curl --fail --insecure --silent https://localhost:8000/health || exit 1

CMD ["labcas-backend", "--host", "0.0.0.0", "--port", "8000", "--tls"]
