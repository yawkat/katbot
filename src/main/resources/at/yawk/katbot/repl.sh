#!/usr/bin/env bash

USER=katbot
LOG_FILE=/log
OUTPUT_PREFIX=" OUTPUT "
EOF="EOF"

# user
useradd -m ${USER} 2> /dev/null
eval "cd ~${USER}"

# hostname
echo "127.0.0.1 $(hostname)" > /etc/hosts

# prevent persistent scripts
touch .bashrc
chown root:root .bashrc

# process limit
#ulimit -Su 100
#ulimit -Hu 100

run() {
 sudo -u ${USER} script -qfc "$1" /dev/null > ${LOG_FILE} 2>&1 &
 pid="$!"
 # max 1s run time
 (sleep 1s && (kill -9 ${pid} & pkill -9 -u ${USER})) &

 wait ${pid}

 while IFS='' read -r line || [[ -n "$line" ]]; do
  echo "$OUTPUT_PREFIX$line"
 done < ${LOG_FILE}
 echo ${EOF}
 rm ${LOG_FILE}
}

# repl
while :; do
 read line
 run "$line"
done
