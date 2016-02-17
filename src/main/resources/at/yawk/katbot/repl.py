#!/usr/bin/env python3

import subprocess
import sys

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


for command_to_run in sys.stdin:
    run(command_to_run)
