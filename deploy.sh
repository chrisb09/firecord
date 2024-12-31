#!/bin/bash

current_user=$(whoami)

if [ "$current_user" = "christian" ]; then

echo "Running for user christian..."

~/SSH/tool/linux_ssh.sh "echo 'Unlocked keepass...'"

echo "Maven test, compile, package & install"
mvn clean install
echo "Copy files to remote staging folder"
find target/ -type f -regex '.*/[A-Za-z]+-[0-9]+\.[0-9]+\(\.[0-9]+\)?\(\.[0-9]+\)?\.jar' \
 -exec echo {} \; \
 -exec rsync {} legendofwar@legendofwar.net:/minecraft/staging/ --progress \;
echo "Copied to staging, now distributing it on the server..."
ssh legendofwar@legendofwar.net 'find /minecraft/staging/ -type f \
 -exec echo {} \; \
 -exec cp {} /minecraft/server/paper/lobby/plugins/ \; \
 -exec cp {} /minecraft/server/paper/construct/plugins/ \; \
 -exec cp {} /minecraft/server/velocity/master/plugins/ \; \
 -exec cp {} /minecraft/server/paper/main/plugins/ \; \
 -delete'

echo "Done"

elif [ "$current_user" = "legendofwar" ]; then
echo "Running for user legendofwar..."

echo "Maven test, compile, package & install"
mvn clean install
echo "Copy files to server folders folder"
find target/ -type f -regex '.*/[A-Za-z]+-[0-9]+\.[0-9]+\(\.[0-9]+\)?\(\.[0-9]+\)?\.jar' \
 -exec echo {} \; \
 -exec cp {} /minecraft/server/paper/lobby/plugins/ \; \
 -exec cp {} /minecraft/server/paper/construct/plugins/ \; \
 -exec cp {} /minecraft/server/velocity/master/plugins/ \; \
 -exec cp {} /minecraft/server/paper/main/plugins/ \;

else
  echo "User $current_user is not know."
fi
