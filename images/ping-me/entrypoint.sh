#!/bin/bash

# Start PHP-FPM
service php8.1-fpm start

# Start Nginx
nginx

# Start SSH in foreground (keeps container alive)
exec /usr/sbin/sshd -D
