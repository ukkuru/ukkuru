#!/usr/bin/env python3
"""Builds the slice of androidx.test (monitor + espresso idling resource) that
Robolectric needs at runtime, straight from the Apache 2.0 source on GitHub.

Why: those AARs are only published on Google's Maven, which this build
environment can't reach. Usage:
    androidx_test_from_source.py WORK_DIR ANDROID_ALL_JAR OUT_JAR
"""
import os
import re
import shutil
import subprocess
import sys
import urllib.request

BASE = "https://raw.githubusercontent.com/android/android-test/main/"
ROOTS = ["runner/monitor/java/", "espresso/idling_resource/java/", "core/java/"]
# Every androidx.test class referenced by the Robolectric 4.17 jars.
SEEDS = """
androidx.test.runner.lifecycle.Stage androidx.test.runner.lifecycle.ApplicationStage
androidx.test.runner.lifecycle.ActivityLifecycleMonitor androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
androidx.test.runner.lifecycle.ActivityLifecycleCallback androidx.test.runner.lifecycle.ApplicationLifecycleMonitor
androidx.test.runner.lifecycle.ApplicationLifecycleMonitorRegistry androidx.test.runner.intent.IntentStubber
androidx.test.runner.intent.IntentStubberRegistry androidx.test.runner.intent.IntentMonitor
androidx.test.runner.intent.IntentMonitorRegistry androidx.test.platform.app.InstrumentationRegistry
androidx.test.platform.ui.UiController androidx.test.platform.ui.InjectEventSecurityException
androidx.test.internal.runner.lifecycle.ApplicationLifecycleMonitorImpl
androidx.test.internal.runner.lifecycle.ActivityLifecycleMonitorImpl
androidx.test.internal.runner.intent.IntentMonitorImpl androidx.test.internal.platform.app.ActivityInvoker
androidx.test.internal.platform.util.TestOutputEmitter androidx.test.internal.platform.os.ControlledLooper
androidx.test.internal.platform.content.PermissionGranter androidx.test.internal.platform.ThreadChecker
androidx.test.espresso.IdlingResource androidx.test.espresso.IdlingRegistry
""".split()

STUBS = {
    "androidx/annotation/RestrictTo.java":
        "package androidx.annotation;\npublic @interface RestrictTo { Scope[] value(); "
        "enum Scope { LIBRARY, LIBRARY_GROUP, LIBRARY_GROUP_PREFIX, GROUP_ID, TESTS, SUBCLASSES } }\n",
    "androidx/annotation/NonNull.java": "package androidx.annotation;\npublic @interface NonNull {}\n",
    "androidx/annotation/Nullable.java": "package androidx.annotation;\npublic @interface Nullable {}\n",
    "androidx/annotation/VisibleForTesting.java":
        "package androidx.annotation;\npublic @interface VisibleForTesting { int otherwise() default 2; "
        "int PRIVATE = 2; int PACKAGE_PRIVATE = 3; int PROTECTED = 4; int NONE = 5; }\n",
    "com/google/errorprone/annotations/CanIgnoreReturnValue.java":
        "package com.google.errorprone.annotations;\npublic @interface CanIgnoreReturnValue {}\n",
}


def main(work, android_all, out_jar):
    src, stubs, classes = (os.path.join(work, d) for d in ("src", "stubs", "classes"))
    for d in (src, stubs, classes):
        shutil.rmtree(d, ignore_errors=True)
        os.makedirs(d)
    for rel, body in STUBS.items():
        p = os.path.join(stubs, rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        open(p, "w").write(body)

    done = set()

    def fetch(cls):
        if cls in done:
            return
        rel = cls.replace(".", "/") + ".java"
        for root in ROOTS:
            try:
                data = urllib.request.urlopen(BASE + root + rel, timeout=30).read().decode()
            except Exception:
                continue
            done.add(cls)
            p = os.path.join(src, rel)
            os.makedirs(os.path.dirname(p), exist_ok=True)
            open(p, "w").write(data)
            for imp in re.findall(r"^import (?:static )?(androidx\.test\.[\w.]+);", data, re.M):
                parts = imp.split(".")
                for i, part in enumerate(parts):
                    if part[:1].isupper():
                        fetch(".".join(parts[:i + 1]))
                        break
            return
        raise SystemExit("could not fetch " + cls)

    for s in SEEDS:
        fetch(s)

    # Same package references have no import line; pull them in from compiler errors.
    for _ in range(8):
        files = [os.path.join(r, f) for d in (src, stubs) for r, _, fs in os.walk(d) for f in fs if f.endswith(".java")]
        res = subprocess.run(["javac", "-nowarn", "-proc:none", "-d", classes, "-cp", android_all] + files,
                             capture_output=True, text=True)
        if res.returncode == 0:
            break
        need = set()
        for m in re.finditer(r"(\S+\.java):\d+: error: cannot find symbol\n.*\n.*\n\s+symbol:\s+(?:class|variable) ([A-Z]\w*)",
                             res.stderr):
            pkg = re.search(r"^package ([\w.]+);", open(m.group(1)).read(), re.M).group(1)
            need.add(pkg + "." + m.group(2))
        if not need:
            raise SystemExit(res.stderr)
        for n in need:
            fetch(n)
    else:
        raise SystemExit("did not converge")

    subprocess.run(["jar", "cf", out_jar, "-C", classes, "androidx"], check=True)
    print("built", out_jar, "from", len(done), "source files")


if __name__ == "__main__":
    main(*sys.argv[1:4])
