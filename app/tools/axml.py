#!/usr/bin/env python3
"""Writes AndroidManifest.xml in Android's binary XML format.

The usual tool for this (aapt2) is not reachable from the build environment,
and the app has no resources, so a small writer is enough. Usage:

    python3 axml.py OUT_FILE PACKAGE VERSION_CODE VERSION_NAME MIN_SDK TARGET_SDK LABEL ACTIVITY
"""
import struct
import sys

ANDROID_NS = "http://schemas.android.com/apk/res/android"

# Framework attribute resource IDs (verified against android.R$attr in android.jar).
ATTR_IDS = {
    "label": 0x01010001,
    "name": 0x01010003,
    "exported": 0x01010010,
    "screenOrientation": 0x0101001E,
    "configChanges": 0x0101001F,
    "minSdkVersion": 0x0101020C,
    "versionCode": 0x0101021B,
    "versionName": 0x0101021C,
    "targetSdkVersion": 0x01010270,
}

TYPE_STRING, TYPE_INT_DEC, TYPE_INT_HEX, TYPE_BOOL = 0x03, 0x10, 0x11, 0x12


def build(package, version_code, version_name, min_sdk, target_sdk, label, activity):
    S = lambda v: (TYPE_STRING, v)
    I = lambda v: (TYPE_INT_DEC, v)
    tree = ("manifest", [(None, "package", S(package)),
                         ("android", "versionCode", I(version_code)),
                         ("android", "versionName", S(version_name))], [
        ("uses-sdk", [("android", "minSdkVersion", I(min_sdk)),
                      ("android", "targetSdkVersion", I(target_sdk))], []),
        ("application", [("android", "label", S(label))], [
            ("activity", [("android", "name", S(activity)),
                          ("android", "exported", (TYPE_BOOL, True)),
                          ("android", "screenOrientation", I(1)),  # portrait (PRD v0.2)
                          # keyboardHidden | orientation | screenSize | screenLayout
                          ("android", "configChanges", (TYPE_INT_HEX, 0x20 | 0x80 | 0x400 | 0x100))], [
                ("intent-filter", [], [
                    ("action", [("android", "name", S("android.intent.action.MAIN"))], []),
                    ("category", [("android", "name", S("android.intent.category.LAUNCHER"))], []),
                ]),
            ]),
        ]),
    ])

    # String pool: android attribute names first, in the same order as the resource map.
    attr_names = []

    def collect_attrs(node):
        for ns, name, _ in node[1]:
            if ns == "android" and name not in attr_names:
                attr_names.append(name)
        for child in node[2]:
            collect_attrs(child)

    collect_attrs(tree)
    strings = list(attr_names)

    def idx(s):
        if s not in strings:
            strings.append(s)
        return strings.index(s)

    for s in ["android", ANDROID_NS]:
        idx(s)

    def collect_rest(node):
        idx(node[0])
        for ns, name, (t, v) in node[1]:
            idx(name)
            if t == TYPE_STRING:
                idx(v)
        for child in node[2]:
            collect_rest(child)

    collect_rest(tree)

    body = bytearray()
    line = [1]

    def chunk(ctype, header_size, payload):
        return struct.pack("<HHI", ctype, header_size, 8 + len(payload)) + payload

    def emit(node):
        name, attrs, children = node
        # aapt sorts attributes by resource ID; plain attributes (no ID) go first.
        attrs = sorted(attrs, key=lambda a: ATTR_IDS.get(a[1], -1) if a[0] == "android" else -1)
        attr_bytes = bytearray()
        for ns, aname, (t, v) in attrs:
            ns_idx = idx(ANDROID_NS) if ns == "android" else 0xFFFFFFFF
            if t == TYPE_STRING:
                raw, data = idx(v), idx(v)
            elif t == TYPE_BOOL:
                raw, data = 0xFFFFFFFF, 0xFFFFFFFF if v else 0
            else:
                raw, data = 0xFFFFFFFF, v
            attr_bytes += struct.pack("<IIIHBBI", ns_idx, idx(aname), raw, 8, 0, t, data)
        ext = struct.pack("<IIHHHHHH", 0xFFFFFFFF, idx(name), 20, 20, len(attrs), 0, 0, 0)
        payload = struct.pack("<II", line[0], 0xFFFFFFFF) + ext + attr_bytes
        body.extend(chunk(0x0102, 16, payload))
        line[0] += 1
        for child in children:
            emit(child)
        payload = struct.pack("<IIII", line[0], 0xFFFFFFFF, 0xFFFFFFFF, idx(name))
        body.extend(chunk(0x0103, 16, payload))
        line[0] += 1

    ns_payload = struct.pack("<IIII", 0, 0xFFFFFFFF, idx("android"), idx(ANDROID_NS))
    body_start = chunk(0x0100, 16, ns_payload)
    emit(tree)
    body_end = chunk(0x0101, 16, ns_payload)

    # UTF-16 string pool
    offsets, data = [], bytearray()
    for s in strings:
        offsets.append(len(data))
        enc = s.encode("utf-16-le")
        data += struct.pack("<H", len(s)) + enc + b"\x00\x00"
    while len(data) % 4:
        data += b"\x00"
    header_size = 28
    strings_start = header_size + 4 * len(strings)
    pool_payload = struct.pack("<IIIII", len(strings), 0, 0, strings_start, 0)
    pool_payload += b"".join(struct.pack("<I", o) for o in offsets) + data
    pool = chunk(0x0001, header_size, pool_payload)

    resmap = chunk(0x0180, 8, b"".join(struct.pack("<I", ATTR_IDS[n]) for n in attr_names))

    content = pool + resmap + body_start + bytes(body) + body_end
    return struct.pack("<HHI", 0x0003, 8, 8 + len(content)) + content


if __name__ == "__main__":
    out, package, vcode, vname, min_sdk, target_sdk, label, activity = sys.argv[1:9]
    with open(out, "wb") as f:
        f.write(build(package, int(vcode), vname, int(min_sdk), int(target_sdk), label, activity))
