FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

LABEL type="challenge"
LABEL difficulty="1"
LABEL flag_path="/home/www-data/flag.txt"
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

# Create home directory for www-data and placeholder flag
RUN mkdir -p /home/www-data \
    && chown www-data:www-data /home/www-data \
    && echo "flag{placeholder}" > /home/www-data/flag.txt \
    && chown www-data:www-data /home/www-data/flag.txt \
    && chmod 444 /home/www-data/flag.txt

# Web app files
COPY html/ /var/www/html/
COPY notes/ /var/www/notes/

# Nginx config for PHP
COPY nginx.conf /etc/nginx/sites-available/default

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 22 80

ENTRYPOINT ["/entrypoint.sh"]
