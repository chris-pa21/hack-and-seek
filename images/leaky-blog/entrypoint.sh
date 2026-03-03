#!/bin/bash

# Nginx starten
nginx

# SSH im Vordergrund starten (haelt Container am Leben)
exec /usr/sbin/sshd -D
