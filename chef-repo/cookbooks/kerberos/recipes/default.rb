#
# Cookbook Name:: kerberos
# Recipe:: default
#
# Copyright 2014, cloudstead
#
# All rights reserved - Do Not Redistribute
#

require 'digest'

krb_master_password = Digest::SHA256.hexdigest("kerberos_#{Chef::Recipe::Base.secret}")
base_bag = data_bag_item('cloudos', 'base')

# common utilities
%w( krb5-kdc krb5-admin-server ).each do |pkg|
  package pkg do
    action :install
  end
end

%w( /etc/krb5.conf /etc/krb5kdc/kdc.conf /etc/krb5kdc/kadm5.acl ).each do |conf|
  template conf do
    owner 'root'
    group 'root'
    mode '0644'
    action :create
    variables ({
        :domain => base_bag['parent_domain']
    })
  end
end

bash 'init kerberos logging files' do
  user 'root'
  code <<-EOF
mkdir /var/log/kerberos
touch /var/log/kerberos/{krb5kdc,kadmin,krb5lib}.log
chmod -R 750  /var/log/kerberos
EOF
end

bash 'setup kerberos realm' do
  user 'root'
  code <<-EOF

# start random seeding with hostname, fixed 'random' value, and ifconfig
hostname > /dev/urandom
ifconfig > /dev/urandom
echo 'lajfl;knf2@#g23vASDqnqw$%1' > /dev/urandom

ROOT_DEVICE=$(mount | head -1 | awk '{print $1}')

# further seed randomness with disk image, ping to www. parent domain, and top via background jobs
parent_domain=$(hostname | awk -F '.' '{print $(NF-1)"."$NF}')
set -m
cat ${ROOT_DEVICE} > /dev/urandom &
ping www.${parent_domain} > /dev/urandom &
top > /dev/urandom &

echo "#{krb_master_password}
#{krb_master_password}" | kdb5_util create -s

rm -f ~/.bash_history
rm -f ~root/.bash_history

# stop random seeding
kill %1 %2 %3

# make sure password is set (for some reason the init step above doesn't always do it)
echo "change_password kadmin/admin
#{krb_master_password}
#{krb_master_password}" | kadmin.local
  EOF
  not_if { File.exists? "/etc/krb5kdc/stash"  }
end

service "krb5-kdc" do
  supports :start => true, :stop => true, :restart => true
  action [ :enable, :start ]
end

service "krb5-admin-server" do
  pattern "/usr/sbin/kadmind"
  supports :start => true, :stop => true, :restart => true
  action [ :enable, :start ]
end
