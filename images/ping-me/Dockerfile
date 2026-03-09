FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

LABEL type="challenge"
LABEL difficulty="1"
LABEL flag_path="/home/admin/.secret/flag.txt"
LABEL points="100"

RUN apt-get update && apt-get install -y \
    openssh-server \
    nginx \
    php-fpm \
    sudo \
    nano \
    vim \
    curl \
    net-tools \
    iputils-ping \
    iproute2

RUN mkdir /run/sshd

# Root password (overwritten by backend at runtime)
RUN echo "root:defendme" | chpasswd \
    && sed -i 's/#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config

# Admin user with hidden flag directory
RUN useradd -m -s /bin/bash admin \
    && chmod 755 /home/admin \
    && mkdir -p /home/admin/.secret \
    && echo "flag{placeholder}" > /home/admin/.secret/flag.txt \
    && chown -R admin:admin /home/admin/.secret \
    && chmod 755 /home/admin/.secret \
    && chmod 444 /home/admin/.secret/flag.txt

# Web app files
COPY html/ /var/www/html/

# Nginx config for PHP
COPY nginx.conf /etc/nginx/sites-available/default

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 22 80

ENTRYPOINT ["/entrypoint.sh"]
