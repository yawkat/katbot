#!/usr/bin/env python3

import os
import shutil
import subprocess
import sys
import socket

USER = "katbot"
OUTPUT_PREFIX = " OUTPUT "
EOF = "EOF"
TIMEOUT = 1  # seconds
MAX_OUTPUT_LENGTH = 1000


def run(command):
    with subprocess.Popen(
            ("sudo", "-u", USER, "script", "-qfc", command, "/dev/null"),
            stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.STDOUT) as process:
        try:
            stdout, _ = process.communicate(timeout=TIMEOUT)
        except subprocess.TimeoutExpired:
            process.kill()
            subprocess.call(("pkill", "-9", "-u", USER))
            stdout, _ = process.communicate()
    output = stdout.decode("utf-8")  # type: str
    if len(output) > MAX_OUTPUT_LENGTH:
        output = output[0:MAX_OUTPUT_LENGTH]
    lines = output.splitlines(keepends=False)
    for line in lines:
        print(OUTPUT_PREFIX + line)
    print(EOF)

# fix host
with open("/etc/hosts", "w") as f:
    f.write("127.0.0.1 " + socket.gethostname())
# chdir to home
os.chdir("/home/" + USER)
# protect .bashrc
with open(".bashrc", "w"):
    pass
shutil.chown(".bashrc", "root", "root")

for command_to_run in sys.stdin:
    run(command_to_run)
