LabCAS Backend
==============

Repository containing back-end services and configuration for executing EDRN LabCAS data processing workflows.


Documentation
-------------

See the `docs/documentation.pdf` file.

Build and Deployment
--------------------

Run the following as the "edrn" user:
```
cd $LABCAS_HOME
./stop.sh
cd ~/src/backend
git pull
mvn clean install
cd $LABCAS_HOME
./start.sh
```

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

Replace `$BACKUP_PATH` with the location of the downloaded backups.# LabCAS Backend (Python)

This package is the Python rewrite of the LabCAS Data Access API. It mirrors the
existing Java functionality while providing a modern FastAPI-based stack with an
extensible directory abstraction, Redis-backed session management, and a pluggable
search layer.

## Development

```bash
python -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install --editable .
labcas-backend --help
```

Create a `.env` file based on `.env.example` before running the service or tests.


## Solr Migration

This guide explains how to export data from your old Solr instance (https://localhost:8984/solr) and import it into a newer Solr instance.

The migration is handled by the `migrate-solr` console script, which is part of the `jpl.labcas.backend` package. After installing the package, you can use `migrate-solr` directly.

### Prerequisites

- Python 3.13+ (as required by the package)
- The `jpl.labcas.backend` package installed (includes required dependencies like `httpx` and `tqdm`)

### Quick Start

The migration tool operates in two modes: `export` (to save cores to files) and `import` (to load cores from files).

#### Export Mode

```bash
# Export all cores from a Solr instance
migrate-solr --mode export https://localhost:8984/solr

# Export specific cores only
migrate-solr --mode export https://localhost:8984/solr --cores collections,datasets

# Specify backup directory (default: /tmp/solr-backup-<timestamp>)
migrate-solr --mode export https://localhost:8984/solr --backup-dir /path/to/backup
```

#### Import Mode

```bash
# Import all cores into a Solr instance (from current directory)
migrate-solr --mode import https://newhost:8983/solr

# Import from a specific backup directory
migrate-solr --mode import https://newhost:8983/solr --backup-dir /tmp/solr-backup-20240101-120000

# Import specific cores only
migrate-solr --mode import https://newhost:8983/solr --backup-dir /tmp/solr-backup-20240101-120000 --cores collections,datasets
```

### Your Solr Cores

Based on your configuration, you have the following cores:
- `collections` - Collection metadata
- `datasets` - Dataset metadata  
- `files` - File metadata
- `userdata` - User data

### Migration Process

The migration process consists of two phases:

#### 1. Export Phase
- Connects to your old Solr instance
- Exports all documents from each core using cursorMark pagination
- Saves data as JSON files in the backup directory

#### 2. Import Phase
- Connects to your new Solr instance
- Imports documents into corresponding cores
- Commits changes after import

### Important Notes

#### Before Migration

1. **Create cores in new Solr**: The new Solr instance must have the cores created first. You can:
   - Copy the core configuration from `solr/src/main/resources/solr-home/` to your new Solr instance
   - Or use the Solr Admin UI: `http://newhost:8983/solr/admin/cores?action=CREATE&name=<core>&instanceDir=<core>`

2. **Schema compatibility**: Ensure the schema.xml files are compatible between old and new Solr versions. You may need to update field types or configurations.

3. **Backup first**: Always backup your data before migration!

#### During Migration

- The tool will prompt you if a core doesn't exist in the target instance
- Large cores may take significant time to export/import
- Progress is shown during the process with progress bars

#### After Migration

1. **Verify data**: Check document counts match:
   ```bash
   curl "https://oldhost:8984/solr/collections/select?q=*:*&rows=0"
   curl "https://newhost:8983/solr/collections/select?q=*:*&rows=0"
   ```

2. **Update configuration**: Update your application's `LABCAS_SOLR_URL` environment variable

3. **Test thoroughly**: Run your application tests to ensure everything works

### Alternative: Using Solr Backup API

For very large datasets, you might prefer using Solr's built-in backup API:

#### Export (on old Solr)
```bash
# For each core
curl "https://localhost:8984/solr/collections/replication?command=backup&location=/path/to/backup"
```

#### Import (on new Solr)
```bash
# Copy backup files to new Solr server, then restore
curl "https://newhost:8983/solr/collections/replication?command=restore&location=/path/to/backup"
```

**Note**: This method requires filesystem access to both Solr servers and the cores must have identical configurations.

### Troubleshooting

#### SSL Certificate Errors
If you get SSL errors, the tool skips verification by default. Use `--verify-ssl` if you want to verify certificates.

#### Core Not Found
If a core doesn't exist in the new instance, the script will prompt you to create it. Make sure to:
1. Copy the `conf/` directory from the old core
2. Create the core using Solr Admin API or UI
3. Then continue with the import

#### Large Datasets
For very large cores (>1M documents), consider:
- Running exports during off-peak hours
- Running export and import separately (export first, then import in a separate command)
- The tool processes documents in batches of 1000 to manage memory efficiently

#### Memory Issues
If you encounter memory issues:
- The tool processes documents in batches of 1000
- Consider migrating one core at a time using the `--cores` option
- Ensure sufficient disk space for backup files

### Support

For issues or questions, check:
- Solr documentation: https://solr.apache.org/guide/
- Your project's Solr configuration in `solr/src/main/resources/solr-home/`

