cloudos-server
==============

The nerve-center of your CloudOs cloudstead, cloudos-server is a Java server that provides:

* a REST API to the cloudstead facilities
* an EmberJS app for the web UI

## Create a CloudOs cloudstead

High-level steps:

* Create a cloudos-server.tar.gz tarball and make it accessible somewhere via HTTP.
* Create SSL certificate
* Create a cloudos-init.json file to describe the initial setup
* Prepare a machine (physical or virtual) to host the CloudOs cloudstead
* Launch the instance

### Create the cloudos-server tarball

Do not clone this repository by itself. Rather, clone cloudstead-uber which includes this repository as a git-submodule. cloudstead-uber contains some useful scripts for building the tarball, among other things.

From the cloudstead-uber directory, run:

    ./prep.sh user@host:/some/path cloudos-server

This will build the tarball and rsync it to another server. The /some/path should be somewhere the tarball will be accessible via HTTP.

### Create SSL certificates

Use your favorite vendor or generate self-signed certificates. Use whatever hostname you plan on using for your cloudstead.

### Create a cloudos-init.json file

An example file can be found in `cloudos-server/chef-repo/init_files/cloudos-init.json`

An annotated example is below (note that the "real" JSON file cannot contain any comments)

    {
        "id": "cloudos-init",    // do not change this
        "base": {
            "hostname": "mycloudos",
            "parent_domain": "example.com"
        },
        "cloudos": {
            // this is where you uploaded the tarball in the step above
            "server_tarball": "http://some-host/path/to/cloudos-server.tar.gz",

            "run_as": "cloudos",

            // hashed password, used to unlock the CloudOs during first-time setup
            // use bcrypt to create the hash
            "admin_initial_pass": "$2a$08$Q9BzVJaAr3UsO9dAD7MHaOHXuE995lC1Ex5vCZDniieSjnsJCsZoq",

             // once the CloudOs has finished setting itself up, it will send an email
             // to this address with setup instructions
            "recovery_email": "you@example.com",

            // CloudOs uses Amazon S3 for backup/restore. 
            // All encryption/decryption is done on the CloudOs, unencrypted data is never written to S3
            "aws_access_key": "aws_access_key",
            "aws_secret_key": "aws_secret_key",

            // CloudOs needs to know which S3 bucket to use  
            "s3_bucket": "some-bucket",

            // Within the above bucket, data will be under a directory named after the IAM user below
            // This allows multiple CloudOs instances to share a single S3 bucket.
            // Ensure that the access/secret keys above allow access to this bucket and path.  
            "aws_iam_user": "some-username",

            // CloudOs uses Sendgrid to send the initial setup email to the recovery_email specified above.
            "sendgrid": {
                "username": "sendgrid-username-here",
                "password": "sendgrid-password-here"
            },
        }
    }

### Prepare a machine

Install Ubuntu 14.04 x64 server on a system. The system may be:

* A physical system that you control
* A virtual machine (VirtualBox or VMware for example)
* A cloud-based system (on Amazon EC2, Rackspace, DigitalOcean, etc)

System Requirements:

* sshd is installed and running
* a regular user account exists that has password-less sudo access and can login with an SSH key.* the instance has a public IP address

###  Launch the instance

From the `cloudos-server/chef-repo` directory, run:

    INIT_FILES=/path/to/init_files/ SSH_KEY=/path/to/private_key ./deploy.sh user@host

Where:

* `INIT_FILES` is a directory containing:
  * The cloudos-init.json file
  * The SSL certificate (as a file named `ssl-https.pem`) and key (as a file named `ssl-https.key`) 
* `SSH_KEY` is the path to the private key that permits `user` to ssh into `host`
* `user` is the account you created above, that has password-less sudo privileges
* `host` is either the hostname or public IP address of the system

What happens during a deploy?

1. The files in INIT_FILES are copied to the instance.
1. The cloudos-server and cloudos-lib chef cookbooks are copied to the instance
1. chef-solo runs the run-list defined in solo.json to setup the CloudOs instance
1. One of the last steps in setup is to send an email to the owner so they can do a first-time setup of the instance.

##### License
For personal or non-commercial use, this code is available under the [GNU Affero General Public License, version 3](https://www.gnu.org/licenses/agpl-3.0.html).
For commercial use, please contact cloudstead.io
