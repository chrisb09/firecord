#!/bin/bash

current_user=$(whoami)

if [ "$current_user" = "christian" ]; then

echo "Running for user christian..."


if [[ -z "$(ls -A /mnt/legendofwar)" ]]; then
    ~/SSH/tool/linux_ssh.sh "echo 'Unlocked keepass...'"
    echo "Mount is empty. Mounting..."
    sshfs root@legendofwar.net:/ /mnt/legendofwar -p 22
else
    echo "/mnt/legendofwar already mounted."
fi

echo "Maven test, compile, package & install"
mvn clean install
echo "Copy files to remote staging folder"
find target/ -type f -regex '.*/[A-Za-z]+-[0-9]+\.[0-9]+\.jar' \
 -exec echo {} \; \
 -exec cp {} /mnt/legendofwar/minecraft/staging/ \;
echo "Copy from staging to paths"
ssh legendofwar@legendofwar.net 'find /minecraft/staging/ -type f \
 -exec echo {} \; \
 -exec cp {} /minecraft/server/paper/lobby/custom_plugins/ \; \
 -exec cp {} /minecraft/server/paper/construct/custom_plugins/ \; \
 -exec cp {} /minecraft/server/bungeecord/master/plugins/ \; \
 -exec cp {} /minecraft/server/multipaper/alpha/plugins/ \; \
 -exec cp {} /minecraft/server/multipaper/bravo/plugins/ \; \
 -delete'
echo "Done"

elif [ "$current_user" = "legendofwar" ]; then
echo "Running for user legendofwar..."

echo "Maven test, compile, package & install"
mvn clean install
echo "Copy files to server folders folder"
find target/ -type f -regex '.*/[A-Za-z]+-[0-9]+\.[0-9]+\(\.[0-9]+\)?\(\.[0-9]+\)?\.jar' \
 -exec echo {} \; \
 -exec cp {} /minecraft/server/paper/lobby/custom_plugins/ \; \
 -exec cp {} /minecraft/server/paper/construct/custom_plugins/ \; \
 -exec cp {} /minecraft/server/bungeecord/master/plugins/ \; \
 -exec cp {} /minecraft/server/multipaper/alpha/plugins/ \; \
 -exec cp {} /minecraft/server/multipaper/bravo/plugins/ \;

else
  echo "User $current_user is not know."
fi
