# Data Access API: Examples

This folder contains example programs in the [Python](https://www.python.org/) programming language that use the LabCAS Data Access API—specifically the [Solr](https://solr.apache.org/) API—to perform various queries. These examples serve as a supplement to the [LabCAS Solr API](https://github.com/EDRN/labcas-backend/wiki/Solr-API) documentation.


## Prerequisites

To use the software, you'll need [Python](https://www.python.org/). Python version 3.9 or later is recommended.

- **For Windows users**, installing Python from the Microsoft Store or directly from the Python website is usually best. You can also use [Anaconda](https://anaconda.com/) or [Miniconda](https://docs.conda.io/projects/conda/en/latest/user-guide/install/index.html) if you prefer.
- Virtually all **macOS**, **Linux**, and other **Unix** users will have Python pre-installed.

You'll also need an EDRN username and a password.


## Installing this Software from Source

To install this software from its source code, we recommend using a Python virtual environment. This keeps the dependencies of this software separate from other Python applications on your system.


### Creating a Virtual Environment on Windows

On Windows, run (from PowerShell):

    python3 -m venv examples

You may need to replace `python3` with `python3.9`, `python3.10`, etc., or even simply `python`, depending on how you installed Python. This creates a new virtual environment in the folder `examples` in the current directory. Then run:

    .\examples\Scripts\activate.ps1

Your PowerShell prompt will change to show `(examples)` in it, indicating that the virtual environment is now active.


### Creating a Virtual Environment on macOS and Other Unix-like Systems

Open a terminal prompt and run:

    python3 -m venv examples
    source examples/bin/activate

`csh`, `tcsh`, etc., users will want to use `examples/bin/activate.csh` instead.


### Downloading and Installing the Software

Now that you have a virtual environment created, download the software from GitHub:

1. Visit the [GitHub repository for the `labcas-backend`](https://github.com/EDRN/labcas-backend)
2. Click the green "< > Code ▼" button and choose "Download ZIP".
3. Unzip the downloaded ZIP file in the same directory as your virtual environment, i.e., in your `examples` directory.
4. Run `pip install --editable labcas-backend-master/api-examples` from within your `examples` directory.


## Running the Examples

Your virtual environment now has two new commands available, one for each of the example programs:

- `cibbbcd-events` — this program, which runs [cibbbcd_events.py](https://github.com/EDRN/labcas-backend/blob/master/api-examples/src/jpl/labcas/examples/cibbbcd_events.py), writes a CSV file of event IDs broken down by `Training` and `Validation` sets for the Combined Imaging and Blood Biomarkers for Breast Cancer Diagnosis data collection in LabCAS
- `events-by-blind` — this progra, which runs [events_by_blind.py](https://github.com/EDRN/labcas-backend/blob/master/api-examples/src/jpl/labcas/examples/events_by_blind.py), takes as a parameter the name of a blinded site ID and writes all event IDs associated with that site.

The next section describes how you can specify your EDRN credentials (username and password). The subsequent subsections show how to run each program. However, as these are _examples_, you're encouraged to read the [source code](https://github.com/EDRN/labcas-backend/tree/master/api-examples/src/jpl/labcas/examples) to see how they work and to develop your own API clients in Python or other programming languages.


### Credentials

Note that accessing the LabCAS Data Access API (and its Solr API) requires EDRN username and password credentials. The two example programs support `--username` and `--password` command-line arguments that let you specify your EDRN username and its matching EDRN password.

For convenience and security, though, you may wish to specify the EDRN username and password so they're not visible on the command line. You can do so using two environment variables, `EDRN_USERNAME` and `EDRN_PASSWORD`. On Windows in PowerShell, run:

    $env:EDRN_USERNAME = "jdoe"
    $env:EDRN_PASSWORD = "s3cret-p4ssw0rd"

On macOS and other Unix-like systems, run:

    export EDRN_USERNAME="jdoe"
    export EDRN_PASSWORD="s3cret-p4ssw0rd"

If you're using csh, tcsh, etc., instead ro:

    setenv EDRN_USERNAME "jdoe"
    setenv EDRN_PASSWORD "s3cret-p4ssw0rd"


### Example: `cibbbcd-events`

This program demonstrates how to use the "datasets" Solr query ask for all datasets that belongs to the collection "Combined Imaging and Blood Biomarkers for Breast Cancer Diagnosis" and extract the event IDs out of them. In this collection, datasets have an identifier (an `id` field) that looks like this:

    Combined_Imaging_and_Blood_Biomarkers_for_Breast_Cancer_Diagnosis/KIND/EVENT-ID…

where:

- `KIND` is either `Training` or `Validation`
- `EVENT-ID` is an event ID

Any remaining text following the `EVENT-ID` are subfolders of the various datasets. The goal of this program is to write a CSV file that can be opened with Numbers, Microsoft Excel, Google Sheets, etc., with two columns:

- Column A is the `KIND` (either `Training` or `Validation`)
- Column B is the `EVENT-ID`

To run this program, execute

    cibbbcd-events --username USERNAME --password PASSWORD > file.csv

If you've set the `EDRN_USERNAME` and `EDRN_PASSWORD` environment variables, simply run:

    cibbbcd-events > file.csv

You can then open `file.csv` with your favorite spreadsheet program. The `file.csv` looks like the following:
```
Kind,Event ID
Training,E0044
Training,E0001
Training,E0003
Training,E0153
(285 other lines not shown)
```

This program also supports `--help`, which you can run to see additional options or information:

    cibbbcd-events --help


### Example: `events-by-blind`

This program demonstrates how to use the "files" Solr query to ask for all of the event IDs given a blinded site ID. This takes advantage of the `eventID` field managed by LabCAS Solr by asking for its value given all of the files that match a given `BlindedSiteID` field, also in Solr.

To run this program, execute

    events-by-blind --username USERNAME --password PASSWORD BLINDED-SITE-ID

If you've set the `EDRN_USERNAME` and `EDRN_PASSWORD` environment variables, simply run:

    events-by-blind BLINDED-SITE-ID

The output is a list of all event IDs that go with the `BLINDED-SITE-ID`. For example:

    events-by-blind --quiet qfP7OH9pjawWGA

The blinded site ID `qfP7OH9pjawWGA` is part of the Prostate MRI collection. The `--quiet` reduces the logging information that's produced. The output will look like the following:
```
1131802
1560819
1794559
(27 other lines not shown)
…
```
Alternatively, try

    events-by-blind --quiet ldytNSGnHnrBQ

which produces output similar to:
```
1073021
1165313
1445185
(104 other lines not shown)
…
```

This program also supports the `--help` option, which you can run to see additional options or information:

    events-by-blind --help


## License

The code is licensed under the [Apache version 2](LICENSE.md) license.
