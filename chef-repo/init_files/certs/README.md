SSL Certificates
================

When launching a new CloudOs instance, you'll need an SSL certificate to secure HTTP access.

Replace the files in this directory with the SSL files from your vendor. Do not change the filenames. 

Your SSL certificate should be a wildcard certificate, because many apps require their own hostname,
which your cloudstead will create automatically via DNS. If your certificate is not a wildcard certificate,
you will get SSL connection errors trying to use many of the apps.
