#!/usr/bin/env python3

import os
import resource
import shutil
import socket
import subprocess
import sys

USER = "katbot"
OUTPUT_PREFIX = " OUTPUT "
EOF = "EOF"
TIMEOUT = 1  # seconds
MAX_OUTPUT_LENGTH = 1000


def set_limits():
    resource.setrlimit(resource.RLIMIT_NPROC, (16, 16))  # max processes
    resource.setrlimit(resource.RLIMIT_CPU, (1, 1))  # max cpu time


def run(command):
    with subprocess.Popen(
            ("sudo", "-u", USER, "script", "-qfc", command, "/dev/null"),
            stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            preexec_fn=set_limits) as process:
        try:
            stdout, _ = process.communicate(timeout=TIMEOUT)
        except subprocess.TimeoutExpired:
            process.kill()
            subprocess.call(("pkill", "-STOP", "-u", USER))
            while subprocess.call(("pkill", "-KILL", "-u", USER)) == 0:
                pass
            stdout, _ = process.communicate()
    output = stdout.decode("utf-8")  # type: str
    # remove non-ascii chars
    output = output.encode("ascii", errors='ignore').decode("ascii")
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
