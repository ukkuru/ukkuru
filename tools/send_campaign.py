#!/usr/bin/env python3
"""Send the Zeetius federation campaign from a Namecheap Private Email mailbox.

Reads the "Send list" sheet of the campaign workbook and sends the rows due on a
given date. Dry run by default: nothing is sent unless you pass --send.

Examples
  python send_campaign.py --file Zeetius_Tier1_Emails.xlsx --test you@gmail.com --send
  python send_campaign.py --file Zeetius_Tier1_Emails.xlsx --date 2026-10-22
  python send_campaign.py --file Zeetius_Tier1_Emails.xlsx --date 2026-10-22 --send

The password is read from the SMTP_PASSWORD environment variable, or asked for
at the prompt. It is never written to disk.

Every send is appended to sent_log.csv next to the workbook, and a row already
in the log is never sent twice. Put a federation name on its own line in
replied.txt (same folder) to stop all its remaining emails.
"""
import argparse
import csv
import datetime as dt
import getpass
import os
import smtplib
import ssl
import sys
import time
from email.message import EmailMessage
from email.utils import formatdate, make_msgid
from pathlib import Path

import openpyxl

SMTP_HOST = "mail.privateemail.com"
SMTP_PORT = 465
FROM_NAME = "Anil"
FROM_ADDR = "anil@badmintonplatform.online"


def load_rows(path):
    ws = openpyxl.load_workbook(path, read_only=True)["Send list"]
    rows = ws.iter_rows(values_only=True)
    head = [str(h) for h in next(rows)]
    for r in rows:
        if r[0] is not None:
            yield dict(zip(head, r))


def read_set(path):
    if not path.exists():
        return set()
    return {line.strip() for line in path.read_text(encoding="utf-8").splitlines() if line.strip()}


def sent_keys(log):
    if not log.exists():
        return set()
    with log.open(newline="", encoding="utf-8") as f:
        return {(r["federation"], r["email_no"]) for r in csv.DictReader(f)}


def build(to, subject, body):
    m = EmailMessage()
    m["From"] = f"{FROM_NAME} <{FROM_ADDR}>"
    m["To"] = to
    m["Reply-To"] = FROM_ADDR
    m["Subject"] = subject
    m["Date"] = formatdate(localtime=False)
    m["Message-ID"] = make_msgid(domain=FROM_ADDR.split("@")[1])
    m["List-Unsubscribe"] = f"<mailto:{FROM_ADDR}?subject=unsubscribe>"
    m.set_content(body)
    return m


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--file", required=True, help="campaign workbook (.xlsx)")
    ap.add_argument("--date", default=dt.date.today().isoformat(), help="send date YYYY-MM-DD (default today)")
    ap.add_argument("--test", metavar="ADDRESS", help="send the first due email to this address only")
    ap.add_argument("--send", action="store_true", help="actually send (default is a dry run)")
    ap.add_argument("--delay", type=int, default=90, help="seconds between emails (default 90)")
    a = ap.parse_args()

    book = Path(a.file)
    log = book.with_name("sent_log.csv")
    stopped = read_set(book.with_name("replied.txt"))
    done = sent_keys(log)

    rows = list(load_rows(book))
    if a.test:
        due = [next(r for r in rows if r["Language"] == "English")]
    else:
        due = [r for r in rows if str(r["Send date (local)"]) == a.date]
        due = [r for r in due if r["Federation"] not in stopped]
        due = [r for r in due if (r["Federation"], str(r["Email #"])) not in done]
    if not due:
        print(f"Nothing due on {a.date}.")
        return

    print(f"{'SENDING' if a.send else 'DRY RUN'}: {len(due)} email(s) for {a.date if not a.test else 'test'}\n")
    for r in due:
        to = a.test or r["To"]
        print(f"  {r['Federation']} #{r['Email #']} -> {to} | {r['Subject']}")
    if not a.send:
        print("\nDry run only. Add --send to send.")
        return
    if input("\nType SEND to confirm: ").strip() != "SEND":
        print("Cancelled.")
        return

    password = os.environ.get("SMTP_PASSWORD") or getpass.getpass(f"Password for {FROM_ADDR}: ")
    ctx = ssl.create_default_context()
    with smtplib.SMTP_SSL(SMTP_HOST, SMTP_PORT, context=ctx, timeout=60) as s:
        s.login(FROM_ADDR, password)
        new_log = not log.exists()
        with log.open("a", newline="", encoding="utf-8") as f:
            w = csv.writer(f)
            if new_log:
                w.writerow(["sent_at_utc", "federation", "email_no", "to", "subject", "message_id"])
            for i, r in enumerate(due):
                to = a.test or r["To"]
                subject = ("[TEST] " if a.test else "") + str(r["Subject"])
                m = build(to, subject, str(r["Body (send ready)"]))
                s.send_message(m)
                if not a.test:
                    w.writerow([dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
                                r["Federation"], r["Email #"], to, r["Subject"], m["Message-ID"]])
                    f.flush()
                print(f"  sent: {r['Federation']} #{r['Email #']} -> {to}")
                if i < len(due) - 1:
                    time.sleep(a.delay)
    print("Done.")


if __name__ == "__main__":
    sys.exit(main())
