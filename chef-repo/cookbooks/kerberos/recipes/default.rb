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
ldap_master_password = Digest::SHA256.hexdigest("ldap_#{Chef::Recipe::Base.secret}")
base_bag = data_bag_item('cloudos', 'base')
ldap_domain_string = "dc=" + base_bag['parent_domain'].gsub(/\./, ",dc=")
realm = base_bag['parent_domain'].upcase

bash 'temporarily override /etc/hosts for ldap setup' do
  user 'root'
  code <<-EOF
mv /etc/hosts /etc/hosts.bak
echo "127.0.0.1 localhost
127.0.1.1 `hostname`" > /etc/hosts
EOF
end

%w( slapd ldap-utils krb5-kdc krb5-kdc-ldap krb5-admin-server).each do |pkg|
  package pkg do
    action :install
  end
end

bash 'restore /etc/hosts' do
  user 'root'
  code <<-EOF
mv /etc/hosts.bak /etc/hosts
  EOF
end

bash 'set ldap root password' do
  user 'root'
  code <<-EOF
echo "dn: olcDatabase={1}hdb,cn=config
replace: olcRootPW
olcRootPW: `slappasswd -s "#{ldap_master_password}"`" | ldapmodify -Y EXTERNAL -H ldapi:///
  EOF
end

%w( /tmp/schema_convert.conf ).each do |conf|
  template conf do
    owner 'root'
    group 'root'
    mode '0700'
    action :create
  end
end

bash 'extract & install kerberos schema, set up ACLs' do
  user 'root'
  code <<-EOF
gzip -d /usr/share/doc/krb5-kdc-ldap/kerberos.schema.gz
cp /usr/share/doc/krb5-kdc-ldap/kerberos.schema /etc/ldap/schema/
mkdir /tmp/ldif_output
slapcat -f /tmp/schema_convert.conf -F /tmp/ldif_output -n0 -s "cn={12}kerberos,cn=schema,cn=config" > /tmp/cn=kerberos.ldif
sed -i 's/{12}kerberos/kerberos/g' /tmp/cn\=kerberos.ldif
head -n -8 /tmp/cn\=kerberos.ldif > /tmp/kerberos.ldif.head
mv /tmp/kerberos.ldif.head /tmp/cn\=kerberos.ldif
ldapadd -Y EXTERNAL -H ldapi:/// -D cn=admin,cn=config -w "#{ldap_master_password}" -f /tmp/cn\=kerberos.ldif

echo "dn: olcDatabase={1}hdb,cn=config
add: olcDbIndex
olcDbIndex: krbPrincipalName eq,pres,sub" | ldapmodify -Y EXTERNAL -H ldapi:/// -D cn=admin,cn=config

echo "dn: olcDatabase={1}hdb,cn=config
replace: olcAccess
olcAccess: to attrs=userPassword,shadowLastChange,krbPrincipalKey by dn="cn=admin,#{ldap_domain_string}" write by anonymous auth by self write by * none
-
add: olcAccess
olcAccess: to dn.base="" by * read
-
add: olcAccess
olcAccess: to * by dn="cn=admin,#{ldap_domain_string}" write by * read" | ldapmodify -Y EXTERNAL -H ldapi:///  -D cn=admin,cn=config
EOF
end

bash 'init kerberos logging files' do
  user 'root'
  code <<-EOF
mkdir /var/log/kerberos
touch /var/log/kerberos/{krb5kdc,kadmin,krb5lib}.log
chmod -R 750  /var/log/kerberos
EOF
end

%w( /etc/krb5.conf /etc/krb5kdc/kadm5.acl /etc/ldap/ldap.conf).each do |conf|
  template conf do
    owner 'root'
    group 'root'
    mode '0644'
    action :create
    variables ({
        :domain => base_bag['parent_domain'],
        :realm => realm,
        :ldap_domain => ldap_domain_string
    })
  end
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
#{krb_master_password}" | kdb5_ldap_util -D cn=admin,#{ldap_domain_string} create -subtrees cn=krb5,#{ldap_domain_string} \
  -r #{realm} -s -w #{ldap_master_password} -H ldapi:///

echo "#{ldap_master_password}
#{ldap_master_password}
#{ldap_master_password}" | kdb5_ldap_util -D cn=admin,#{ldap_domain_string} stashsrvpw \
  -f /etc/krb5kdc/service.keyfile cn=admin,#{ldap_domain_string}

# stop random seeding
kill %1 %2 %3

EOF
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
