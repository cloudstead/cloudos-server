CloudOs Data Files
==================

The files in this directory are optional. What goes in here?

#### cloudos-server.tar.gz

The CloudOs server artifact. If this file is present, it will override the "server_tarball" URL in
the cloudos/init.json data bag. Instead of downloading the cloudos-server artifact from the URL, this
file will be used instead.

How it gets here: Run `prep.sh cloudos-server /path/to/init_files/data_files/cloudos` from the `cloudos` directory.

#### api-docs

If present, this directory contains API documentation for the cloudos-server REST API.
It will be available on the running cloudstead under https://hostname/api-docs/

#### api-examples

If present, this directory contains API examples for the cloudos-server REST API.
It will be available on the running cloudstead under https://hostname/api-examples/
