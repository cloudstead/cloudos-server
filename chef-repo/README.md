When running deploy.sh, you will need to pass an environment variable named INIT_FILES

The value of INIT_FILES should be a directory that contains the following files:

* a file named "hostname" whose contents are the hostname that should be assigned to the machine
* the SSL key and certificate for the machine (for apache/etc)
* the cloudos-init.json data bag containing machine-appropriate settings (a sample version is in init_files)

The deploy.sh script copies these files to /tmp before running chef-solo

chef-solo expects to find them in /tmp, otherwise the chef run will fail

The directory "my_init_files" is in the .gitignore, so that's a good place for INIT_FILES during development.