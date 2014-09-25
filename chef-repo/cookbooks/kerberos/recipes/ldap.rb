#
# LDAP
#
%w( slapd libnss-ldap ldap-utils ).each do |pkg|
  package pkg do
    action :install
  end
end

ldap_root_password = node[:kerberos][:ldap_root_password]
bash "set LDAP master password" do
  user "root"
  code <<-EOF
service slapd stop
slappasswd -s #{ldap_root_password}
rm ~root/.bash_history
rm ~/.bash_history
service slapd start
  EOF
end

template "/etc/ldap/ldap.conf" do
  owner "root"
  group "root"
  mode "0644"
  action :create
end

bash "initialize LDAP directory" do
  user "root"
  code <<-EOF
ldapadd -c -Y EXTERNAL -H ldapi:/// -f /etc/ldap/schema/core.ldif || true
ldapadd -c -Y EXTERNAL -H ldapi:/// -f /etc/ldap/schema/cosine.ldif || true
ldapadd -c -Y EXTERNAL -H ldapi:/// -f /etc/ldap/schema/nis.ldif || true
ldapadd -c -Y EXTERNAL -H ldapi:/// -f /etc/ldap/schema/inetorgperson.ldif || true
  EOF
end

%w( ldap-loglevel ldap-uid-eq ldap-admin-access ldap-kerberos-access ).each do |config|

  ldif = "/var/tmp/#{config}.ldif"

  template ldif do
    owner "root"
    group "root"
    mode "0644"
    action :create
  end

  bash "initialize LDAP with #{ldif}" do
    user "root"
    code <<-EOF
ldapmodify -Y EXTERNAL -H ldapi:/// -f #{ldif} || true
    EOF
  end

end

%w( ldap-init ).each do |config|

  ldif = "/var/tmp/#{config}.ldif"

  template ldif do
    owner "root"
    group "root"
    mode "0644"
    action :create
  end

  bash "initialize LDAP with #{ldif}" do
    user "root"
    code <<-EOF
service slapd stop
slapadd -c -v -l #{ldif} || true
service slapd start
    EOF
  end

end

# apt-get install sasl2-bin libsasl2-2 libsasl2-modules libsasl2-modules-gssapi-mit
#
# addprinc -randkey ldap/central.cloudstead.io
# ktadd ldap/central.cloudstead.io
