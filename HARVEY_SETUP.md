# Harvey setup notes for Testmetry

Harvey ([ethanplusai/harvey](https://github.com/ethanplusai/harvey), MIT) is an autonomous cold outreach agent. It finds businesses, reads their sites, writes email, waits for your approval, sends, and handles replies. Model calls run through your Claude Code login instead of an API key.

## What was verified (cloud session, 2026-10-14)

1. Cloned at `main`, installed with `pip install -e '.[dev]'` on Python 3.13.
2. Test suite: **428 passed, 19 skipped, 0 failed** in about 90 seconds.
3. `harvey status` and `harvey signals` run cleanly on an empty database. 23 signals ship, none confirmed.

What was not done, on purpose: no `harvey run`, no training, no discovery, no email. Harvey is a long running loop that needs your Claude login, a mailbox and OAuth in a browser. A cloud container that gets recycled is the wrong home for it. Run it on your own machine or a small always on box.

## Setup on your machine

```bash
git clone https://github.com/ethanplusai/harvey.git
cd harvey
python3.13 -m venv .venv && source .venv/bin/activate && pip install -e .
cp .env.example .env          # fill in mail provider + one verifier key
claude login                  # Harvey uses this login for model calls
harvey train https://www.testmetry.com
harvey signals                # read every one before confirming
harvey discover --estimate    # see the cost before spending anything
harvey dashboard              # http://localhost:5555
```

Leave `require_approval: true` in `harvey.yaml` until you have read at least 50 drafts and would sign every one.

## The stress test: read this before you send a single email

**1. Is there a product to sell?** Right now `testmetry.com` reads as a blog and community. Harvey sells an offer, it does not invent one. If you train it on the blog, it will write cold email pitching "join my community", and strangers will mark that as spam. Decide the offer first (QA consulting, a test audit, a training cohort, sponsored content) and write the price down. No offer, no Harvey.

**2. ICP mismatch.** Harvey's free discovery source is OpenStreetMap, which is built for local trades: roofers, clinics, salons. If your buyer is a QA lead or engineering manager at a software company, that source is almost useless. You will need paid discovery (Serper or DataForSEO) and a sharp `icp` block with titles and company size. Budget a few dollars and test small.

**3. Your main domain is at risk.** Never send cold email from `testmetry.com`. Buy a separate domain, set up SPF, DKIM and DMARC, warm it for two to three weeks, and stay under 30 sends a day at the start. One bad week of bounces can poison a domain for months.

**4. Unverified email is a bounce generator.** Add at least one verifier key (Reoon free tier is the easy start). Without it Harvey marks addresses as "guess" and refuses to send, which is the correct behavior. Do not work around it.

**5. LinkedIn automation breaks LinkedIn's terms.** The repo says so itself. Turn `channels.linkedin.enabled` to `false` unless you are fine losing the account. Your LinkedIn profile is a real asset for a thought leadership brand; do not gamble it.

**6. Law depends on where your recipients live.** US recipients fall under CAN SPAM (physical address, working unsubscribe). EU and UK recipients fall under GDPR and PECR, which are far stricter for cold B2B email to individuals. India has the DPDP Act. If you cannot answer "what is my lawful basis for emailing this person", do not send to that region.

**7. Subscription usage limits and terms.** Harvey runs a loop every 15 minutes on your Claude plan. It throttles itself (`max_daily_claude_percent: 80`), but your own Claude work will compete with it. Check your plan's current terms on automated use before running it around the clock.

**8. A 0.3.0 tool written by one person.** The tests are solid, but it is beta software that sends email in your name. Read `harvey/gate.py` and the outbox flow yourself before turning approval off.

## Verdict

The tool is well built and the "approve every email" default is the right one. The weak link is not Harvey, it is the input. Nail the offer, the buyer and the sending domain, and Harvey becomes a tireless research assistant. Skip those, and it becomes a fast way to burn your reputation. Start with 20 hand approved emails to a tight list and measure replies before scaling anything.
