#!/bin/bash
# Arranca backend y nginx; si uno de los dos termina, el contenedor se detiene con su código.
setpriv --reuid=app --regid=app --init-groups java -jar /app/app.jar &
nginx -g 'daemon off;' &
wait -n
exit $?
