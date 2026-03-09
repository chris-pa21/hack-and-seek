#!/bin/bash
pgrep nginx > /dev/null 2>&1 || exit 1
pgrep php-fpm > /dev/null 2>&1 || exit 1
RESPONSE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:80/)
[ "$RESPONSE" -ge 200 ] && [ "$RESPONSE" -lt 400 ] || exit 1
[ -d /var/www/notes ] || exit 1
exit 0
