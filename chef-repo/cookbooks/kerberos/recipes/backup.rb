#
# Cookbook Name:: kerberos
# Recipe:: backup
#
# Copyright 2014, cloudstead
#

# creates a tarball containing kerberos config files + database dump
# structure of the tarball:
# etc/ - contains the backup of the config files
# tmp/ - contains the kdb dump
# restore/ - contains the restore chef recipe
# restore-solo.rb - chef-solo config to restore just these data
# restore.json - chef-solo runlist for just these data

timestamp = Time.new.strftime("%Y-%m-%d-%H%M%S")
tarball = "sso-#{timestamp}.tar.bz2"
dumpfile = "kdb-#{timestamp}.dump"
restorefile = "restore-sso-#{timestamp}.rb"
backup_dir = "/var/cloudos/backup/sso"

run_as = 'cloudos'

directory backup_dir do
  owner "root"
  group "root"
  mode 0644
  recursive true
  action :create
end

directory "#{backup_dir}/restore/recipes" do
  owner "root"
  group "root"
  mode 0644
  recursive true
  action :create
end

template "#{backup_dir}/restore/recipes/#{restorefile}" do
  source "restore.erb"
  owner "root"
  group "root"
  mode 0644
  variables({
    :backup_dir => backup_dir,
    :dumpfile => dumpfile,
    :restorefile => restorefile
  })
end

template "#{backup_dir}/restore.json" do
  source "restore-json.erb"
  owner "root"
  group "root"
  mode 0644
  variables({
      :recipe => "restore-sso-#{timestamp}"
  })
end

template "#{backup_dir}/restore-solo.rb" do
  source "restore-solo.erb"
  owner "root"
  group "root"
  mode 0644
end

bash "back up kerberos data & config" do
  user "root"
  cwd backup_dir
  code <<-EOF
TIMESTAMP="#{timestamp}"
BACKUP_DIR=#{backup_dir}
TARBALL="#{tarball}"
RUNAS="#{run_as}"
KEY="/home/${RUNAS}/.backup-key"

echo "creating backup tarball at ${BACKUP_DIR}/${TARBALL}"

if [ ! -d $BACKUP_DIR ] ; then
	mkdir -p $BACKUP_DIR
fi

# dump the kerberos database
KDBDUMP="#{dumpfile}"
kdb5_util dump /tmp/${KDBDUMP}
if [ ! -f /tmp/${KDBDUMP}.dump_ok ] ; then
	echo "unable to dump kerberos database to ${KDBDUMP}"
	exit 1
fi

tar cjfp $BACKUP_DIR/$TARBALL /etc/krb* /tmp/${KDBDUMP} restore/recipes/#{restorefile} restore.json restore-solo.rb
openssl enc -e -aes256 -pass file:$KEY -in $BACKUP_DIR/$TARBALL -out $BACKUP_DIR/$TARBALL.enc

if [ -f $BACKUP_DIR/$TARBALL ] ; then
	echo "backup tarball created"
	rm /tmp/${KDBDUMP}*
  rm -rf restore
  rm restore.json
  rm restore-solo.rb
  if [ -f $BACKUP_DIR/$TARBALL.enc ] ; then
		echo "backup tarball encrypted"
		rm $BACKUP_DIR/$TARBALL
	fi
else
	echo "couldn't create tarball"
	exit 1
fi
  EOF
end

