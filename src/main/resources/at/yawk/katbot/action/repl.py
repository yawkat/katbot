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
    kib = 1024
    mib = kib * 1024

    resource.setrlimit(resource.RLIMIT_NPROC, (512, 512))  # max processes
    resource.setrlimit(resource.RLIMIT_NOFILE, (64, 64))  # max open files
    resource.setrlimit(resource.RLIMIT_CPU, (5, 5))  # max cpu time
    resource.setrlimit(resource.RLIMIT_CORE, (16 * mib, 16 * mib))  # core file maximum size bytes
    resource.setrlimit(resource.RLIMIT_DATA, (16 * mib, 16 * mib))  # max heap size bytes
    resource.setrlimit(resource.RLIMIT_RSS, (16 * mib, 16 * mib))  # max resident size bytes


def run(command):
    def kill():
        while subprocess.call(("pkill", "-KILL", "-u", USER)) == 0:
            subprocess.call(("pkill", "-STOP", "-u", USER))
            pass

    with subprocess.Popen(
            ("sudo", "-u", USER, "script", "-qfc", command, "/dev/null"),
            stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            preexec_fn=set_limits) as process:
        try:
            stdout, _ = process.communicate(timeout=TIMEOUT)
        except subprocess.TimeoutExpired:
            process.kill()
            kill()
            stdout, _ = process.communicate()
        kill()
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

subprocess.call(("userdel", "katbot"))
subprocess.call(("useradd", "-m", "katbot"))
shutil.chown("/home/" + USER, USER, USER)
os.chdir("/home/" + USER)

for command_to_run in sys.stdin:
    run(command_to_run)
