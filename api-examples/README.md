# Data Access API: Examples

This folder contains example programs in the [Python](https://www.python.org/) programming language that access the LabCAS Data Access API—specifically the [Solr](https://solr.apache.org/) API—to perform various queries. These examples serve as a supplement to the [LabCAS Solr API](https://github.com/EDRN/labcas-backend/wiki/Solr-API) documentation.


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


### Downloading the Software

Now that you have a virtual environment created, download the software from GitHub.

