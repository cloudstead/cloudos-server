#
# Cookbook Name:: cloudos
# Recipe:: pre-base-image
#
# Copyright 2014, cloudstead
#

%w(
openntpd 
emacs24-nox tshark xtail screen inotify-tools 
memcached 
krb5-kdc krb5-admin-server
postgresql
apache2
libapache2-mod-php5
php5-pgsql php5-imap php5-gd php5-json php5-mysql php5-curl php5-intl php5-mcrypt php5-imagick
daemontools daemontools-run ucspi-tcp djbdns
openjdk-7-jre-headless
openjdk-7-jdk
dovecot-core dovecot-imapd dovecot-pop3d dovecot-gssapi libpam-krb5 postfix
amavisd-new spamassassin clamav-daemon clamav-freshclam re2c pyzor razor spamc
git
).each do |pkg|
  package pkg do
    action :install
  end
end

