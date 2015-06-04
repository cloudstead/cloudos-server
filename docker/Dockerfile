FROM phusion/baseimage
MAINTAINER Jonathan Cobb <jonathan@cloudstead.io>
RUN apt-get update

# Enable sshd
RUN rm -f /etc/service/sshd/down

# Add our ssh-key
ADD ssh_dsa.pub /tmp/ssh_dsa.pub
RUN cat /tmp/ssh_dsa.pub >> /root/.ssh/authorized_keys2 && chmod 600 /root/.ssh/authorized_keys2 && rm -f /tmp/ssh_dsa.pub

# Add chef user, enable password-less sudo
RUN useradd -m chef && echo "chef ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers

# Install Apache and PHP, required for cloudos-boot webapp
RUN apt-get install -y apache2 libapache2-mod-php5

