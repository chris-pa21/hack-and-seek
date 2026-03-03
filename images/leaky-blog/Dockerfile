FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

LABEL type="challenge"

RUN apt-get update && apt-get install -y \
    openssh-server \
    nginx \
    sudo \
    nano \
    vim \
    curl \
    net-tools \
    iputils-ping \
    iproute2

RUN mkdir /run/sshd

# Root-Passwort fuer Verteidiger
RUN echo "root:defendme" | chpasswd \
    && sed -i 's/#PermitRootLogin.*/PermitRootLogin yes/' /etc/ssh/sshd_config

# Verwundbarer User mit schwachem Passwort (Angriffsziel)
RUN useradd -m -s /bin/bash jdoe \
    && echo "jdoe:159753" | chpasswd

# Flag im Home-Verzeichnis
RUN echo "flag{w3ak_p4ssw0rd_1s_n0_s3cur1ty}" > /home/jdoe/flag.txt \
    && chown jdoe:jdoe /home/jdoe/flag.txt \
    && chmod 400 /home/jdoe/flag.txt

# Blog-Dateien kopieren
COPY html/ /var/www/html/

# Entrypoint
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 22 80

ENTRYPOINT ["/entrypoint.sh"]
