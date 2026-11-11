"""Replay: run a prompt on past base dates and grade it right away (nothing is stored in the DB).

Why: a live prediction takes 1-5 trading days to grade, too slow to iterate on prompt/input changes.
The API rebuilds the input as it would have looked at 20:00 KST on each past trading day
(prices, dividend history, news, market context up to then) and grades with the same rules
and baselines as the live grading.

    python replay.py run --prompt prompt.md --from 2026-09-22 --to 2026-11-03 [--reps 1] [--morning]
    python replay.py summary [--split 2026-10-20]

Results append to %LOCALAPPDATA%/bluesky-market-analysis/replay/results.jsonl.
Inputs are cached per date under replay/inputs (delete it after an API input change).
Settings come from the same user environment variables as the scheduled jobs
(BLUESKY_USER_ID, BLUESKY_STOCK_API, CLAUDE_CODE_OAUTH_TOKEN, BLUESKY_CLAUDE_EXE).

Caveats: news is only kept since collection started (2026-11-09), so older dates replay
without news. The model may have seen some of these dates in training; compare versions
on the same dates rather than reading absolute hit rates.
"""

import argparse
import datetime as dt
import glob
import json
import os
import re
import shutil
import ssl
import subprocess
import sys
import urllib.request
from collections import defaultdict

HERE = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(os.environ.get('LOCALAPPDATA', HERE), 'bluesky-market-analysis', 'replay')
# BLUESKY_REPLAY_RESULTS: a separate file per process when runs go in parallel (merge afterwards)
RESULTS = os.environ.get('BLUESKY_REPLAY_RESULTS') or os.path.join(OUT_DIR, 'results.jsonl')
# Past inputs do not change, and each /input call reads the KIS config and calls KIS for the market lines.
# Delete this folder to rebuild them (after an input change in the API).
INPUTS = os.path.join(OUT_DIR, 'inputs')


def user_env(name):
    value = os.environ.get(name)
    if value:
        return value
    try:
        import winreg
        with winreg.OpenKey(winreg.HKEY_CURRENT_USER, 'Environment') as key:
            return winreg.QueryValueEx(key, name)[0]
    except OSError:
        return None


API = (user_env('BLUESKY_STOCK_API') or 'http://localhost:30134').rstrip('/')
USER_ID = user_env('BLUESKY_USER_ID')
SSL = ssl.create_default_context()


def http(method, path, body=None, timeout=300):
    data = None if body is None else json.dumps(body).encode('utf-8')
    req = urllib.request.Request(API + path, data=data, method=method)
    if data is not None:
        req.add_header('Content-Type', 'application/json; charset=utf-8')
    with urllib.request.urlopen(req, timeout=timeout, context=SSL) as res:
        return json.loads(res.read().decode('utf-8'))


def resolve_claude():
    candidates = [user_env('BLUESKY_CLAUDE_EXE'), shutil.which('claude.exe'),
                  os.path.join(os.environ.get('APPDATA', ''), 'npm', 'node_modules', '@anthropic-ai',
                               'claude-code', 'bin', 'claude.exe')]
    candidates += sorted(glob.glob(os.path.join(os.environ.get('APPDATA', ''), 'Claude', 'claude-code',
                                                '*', '*', 'claude.exe')), key=os.path.getmtime, reverse=True)
    for path in candidates:
        # npm leaves a ~500 byte placeholder claude.exe when its postinstall fails
        if path and os.path.isfile(path) and os.path.getsize(path) > 1024 * 1024:
            return path
    raise SystemExit('claude executable not found (set BLUESKY_CLAUDE_EXE)')


def run_claude(prompt, schema, model):
    # Started from inside a Claude Code session, the session's CLAUDE_* / ANTHROPIC_* variables
    # break the child's login (same as common.ps1 Invoke-Claude).
    env = {k: v for k, v in os.environ.items() if not re.match(r'^(CLAUDE|ANTHROPIC)', k)}
    token = user_env('CLAUDE_CODE_OAUTH_TOKEN')
    if token:
        env['CLAUDE_CODE_OAUTH_TOKEN'] = token
    work = os.path.join(OUT_DIR, 'work')
    os.makedirs(work, exist_ok=True)
    args = [resolve_claude(), '-p', '--model', model, '--tools', '', '--output-format', 'json',
            '--json-schema', schema, '--no-session-persistence', '--strict-mcp-config',
            '--setting-sources', 'local']
    proc = subprocess.run(args, input=prompt.encode('utf-8'), capture_output=True, cwd=work, env=env,
                          timeout=15 * 60)
    out = proc.stdout.decode('utf-8', 'replace')
    if not out:
        raise RuntimeError('claude exit %s: %s' % (proc.returncode, proc.stderr.decode('utf-8', 'replace')[:300]))
    result = json.loads(out)
    if result.get('is_error') or result.get('structured_output') is None:
        raise RuntimeError('claude error: %s' % str(result.get('result'))[:300])
    return result


def prompt_version(template):
    m = re.search(r'promptVersion:\s*(\S+)', template)
    if not m:
        raise SystemExit('prompt has no promptVersion line')
    return m.group(1)


def weekdays(start, end):
    d = start
    while d <= end:
        if d.weekday() < 5:
            yield d
        d += dt.timedelta(days=1)


def cmd_run(a):
    if not USER_ID:
        raise SystemExit('BLUESKY_USER_ID is not set')
    template = open(a.prompt, encoding='utf-8').read()
    version = prompt_version(template)
    schema = open(os.path.join(HERE, 'schema.json'), encoding='utf-8').read()
    os.makedirs(OUT_DIR, exist_ok=True)
    start, end = dt.date.fromisoformat(a.date_from), dt.date.fromisoformat(a.date_to)
    trading = 0
    done = set()
    if a.resume:  # skip (version, date, rep) already in the main or this run's results file
        main = os.path.join(OUT_DIR, 'results.jsonl')
        done = {(l['version'], l['asOf'], l['rep']) for p in {main, RESULTS} if os.path.exists(p)
                for l in map(json.loads, open(p, encoding='utf-8')) if l.get('mode') == ('morning' if a.morning else 'evening')}
    for day in weekdays(start, end):
        cached = os.path.join(INPUTS, '%s%s.json' % (day, '@am' if a.morning else ''))
        if os.path.exists(cached):
            input_json = json.load(open(cached, encoding='utf-8'))
        else:
            input_json = http('GET', '/api/stock/analysis/input?userId=%s&asOf=%s&morning=%s'
                              % (USER_ID, day, 'true' if a.morning else 'false'))
            os.makedirs(INPUTS, exist_ok=True)
            with open(cached, 'w', encoding='utf-8') as f:
                json.dump(input_json, f, ensure_ascii=False)
        bases = {it.get('baseDate') for it in input_json['items'] if it.get('baseDate')}
        if str(day) not in bases:
            print(day, 'holiday - skip', flush=True)
            continue
        trading += 1
        if (trading - 1) % a.step:
            continue
        for rep in range(a.reps):
            if (version + ('@am' if a.morning else ''), str(day), rep) in done:
                continue
            try:
                result = run_claude(template.replace('{{INPUT_JSON}}', json.dumps(input_json, ensure_ascii=False)),
                                    schema, a.model)
            except Exception as e:  # one bad day must not stop a long replay
                print(day, 'rep', rep, 'FAILED', e, flush=True)
                continue
            predictions = result['structured_output']['predictions']
            ev = http('POST', '/api/stock/analysis/evaluate?asOf=%s' % day,
                      {'modelId': a.model, 'promptVersion': version, 'predictions': predictions})
            line = {'version': version + ('@am' if a.morning else ''), 'asOf': str(day), 'rep': rep,
                    'model': a.model, 'mode': 'morning' if a.morning else 'evening',
                    'at': dt.datetime.now().isoformat(timespec='seconds'), 'eval': ev,
                    'predictions': predictions}
            with open(RESULTS, 'a', encoding='utf-8') as f:
                f.write(json.dumps(line, ensure_ascii=False) + '\n')
            rows = {r['horizonDays']: r for r in ev['byHorizon']}
            print(day, version, 'rep', rep,
                  ' '.join('D+%d hit %s edge %s' % (h, r['hitRatePct'], r['edgePct']) for h, r in sorted(rows.items())),
                  flush=True)


def load_results():
    if not os.path.exists(RESULTS):
        return []
    with open(RESULTS, encoding='utf-8') as f:
        return [json.loads(line) for line in f if line.strip()]


def summarize(lines, split=None):
    """Pooled over all graded predictions per (version, part, horizon).

    edge = hit rate minus the best fixed rule: always UP, always DOWN, or the prior-period trend.
    Always DOWN matters for the distribution ETFs: their price drops by the payout every month,
    and in a falling market it alone beats most prompts.
    """
    stats = defaultdict(lambda: defaultdict(float))
    for line in lines:
        part = 'all' if not split else ('tune' if line['asOf'] < split else 'valid')
        for r in line['eval']['byHorizon']:
            if r['graded'] and r['trendRatePct'] is not None:
                s = stats[(line['version'], part, r['horizonDays'])]
                s['trend'] += r['trendRatePct'] * r['graded'] / 100
                s['trend_n'] += r['graded']
        for e in line['eval']['lines']:
            if e['hit'] is None:
                continue
            s = stats[(line['version'], part, e['horizonDays'])]
            s['n'] += 1
            s['hit'] += e['hit']
            s['up'] += e['actualDirection'] == 'UP'
            s['down'] += e['actualDirection'] == 'DOWN'
            s['flat'] += e['direction'] == 'FLAT'
    out = []
    for key in sorted(stats):
        s = stats[key]
        n = s['n']
        if not n:
            continue
        pct = lambda x: round(x * 100 / n, 1)
        trend = round(s['trend'] * 100 / s['trend_n'], 1) if s['trend_n'] else 0.0
        best = max(pct(s['up']), pct(s['down']), trend)
        out.append({'version': key[0], 'part': key[1], 'h': key[2], 'n': int(n), 'hit': pct(s['hit']),
                    'alwaysUp': pct(s['up']), 'alwaysDown': pct(s['down']), 'trend': trend,
                    'edge': round(pct(s['hit']) - best, 1), 'flat': pct(s['flat'])})
    return out


def cmd_summary(a):
    rows = summarize([l for l in load_results() if not a.version or l['version'] in a.version], a.split)
    print('%-10s %-6s %-4s %5s %6s %6s %6s %6s %7s %6s' % (
        'version', 'part', 'h', 'n', 'hit', 'allUp', 'allDn', 'trend', 'edge', 'flat'))
    for r in rows:
        print('%-10s %-6s D+%-2d %5d %6.1f %6.1f %6.1f %6.1f %+7.1f %6.1f' % (
            r['version'], r['part'], r['h'], r['n'], r['hit'], r['alwaysUp'], r['alwaysDown'], r['trend'],
            r['edge'], r['flat']))


def main():
    ap = argparse.ArgumentParser()
    sub = ap.add_subparsers(dest='cmd', required=True)
    r = sub.add_parser('run')
    r.add_argument('--prompt', default=os.path.join(HERE, 'prompt.md'))
    r.add_argument('--from', dest='date_from', required=True)
    r.add_argument('--to', dest='date_to', required=True)
    r.add_argument('--reps', type=int, default=1)
    r.add_argument('--step', type=int, default=1, help='every Nth trading day (long windows at lower cost)')
    r.add_argument('--model', default='sonnet')
    r.add_argument('--resume', action='store_true', help='skip dates already in the results (after failures)')
    r.add_argument('--morning', action='store_true',
                   help='analyze at 08:00 KST the next day (the scheduled morning job) instead of 20:00 KST')
    r.set_defaults(fn=cmd_run)
    s = sub.add_parser('summary')
    s.add_argument('--split', help='dates before this are "tune", from it on "valid"')
    s.add_argument('--version', nargs='*')
    s.set_defaults(fn=cmd_summary)
    a = ap.parse_args()
    a.fn(a)


if __name__ == '__main__':
    sys.stdout.reconfigure(encoding='utf-8')
    main()
