"""Прогон сценария «собираем ТЗ» по стратегиям управления контекстом.

Воспроизводит клиентскую логику сборки контекста (как в smith-frontend/js/app.js)
и вызывает реальный локальный backend smith-backend.
"""

import copy
import http.cookiejar
import json
import os
import pathlib
import sys
import time
import urllib.parse
import urllib.request

BASE = os.environ.get("SMITH_BASE", "http://localhost:8080")
USER = os.environ.get("SMITH_USER", "vap")
PASSWORD = os.environ.get("SMITH_PASSWORD", "")
DIR = pathlib.Path(__file__).resolve().parent
TRANSCRIPTS = DIR / "transcripts"
CONTEXT_CHAR_LIMIT = 12000
MAX_TOKENS = 1500

jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))


def login():
    data = urllib.parse.urlencode({"username": USER, "password": PASSWORD}).encode("utf-8")
    req = urllib.request.Request(
        BASE + "/api/auth/login",
        data=data,
        headers={"Content-Type": "application/x-www-form-urlencoded"},
    )
    with opener.open(req, timeout=30) as response:
        return json.loads(response.read().decode("utf-8"))


def post_json(path, payload):
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request(
        BASE + path, data=data, headers={"Content-Type": "application/json"}
    )
    with opener.open(req, timeout=240) as response:
        return json.loads(response.read().decode("utf-8"))


def usage_total(usage):
    if not usage:
        return 0
    return (usage.get("promptTokens") or 0) + (usage.get("completionTokens") or 0)


def dialog_lines(dialog):
    lines = []
    for message in dialog:
        content = message.get("content")
        if not content:
            continue
        if message["role"] == "user":
            lines.append(f"Пользователь: {content}")
        elif message["role"] == "assistant":
            lines.append(f"Ассистент: {content}")
    return lines


def fit(lines, current, limit=CONTEXT_CHAR_LIMIT):
    all_lines = list(lines) + [f"Пользователь: {current}"]
    text = "\n\n".join(all_lines)
    while len(text) > limit and len(all_lines) > 1:
        all_lines.pop(0)
        text = "\n\n".join(all_lines)
    return text


def build_prompt(strategy, dialog, current, facts, summary, covered, window):
    if strategy == "sliding_window":
        return fit(dialog_lines(dialog[-window:]), current)
    if strategy == "sticky_facts":
        lines = []
        if facts:
            lines.append("Известные факты о задаче:\n" + facts)
        lines += dialog_lines(dialog[-window:])
        return fit(lines, current)
    if strategy == "summarize":
        lines = []
        source = dialog
        if summary:
            lines.append("Саммари предыдущего диалога:\n" + summary)
            source = dialog[covered:]
        lines += dialog_lines(source)
        return fit(lines, current)
    return fit(dialog_lines(dialog), current)


def complete(prompt, scenario):
    payload = {
        "prompt": prompt,
        "model": scenario["model"],
        "system_prompt": scenario.get("system_prompt"),
        "thinking": "disabled",
        "reasoning_effort": "low",
        "max_tokens": MAX_TOKENS,
    }
    return post_json("/api/v1/chat/completions", payload)


def detail_hits(text, details):
    low = (text or "").lower()
    hits = []
    for name, variants in details.items():
        if any(variant.lower() in low for variant in variants):
            hits.append(name)
    return hits


def run_strategy(strategy, scenario):
    turns = scenario["turns"]
    window = scenario["window"]
    dialog = []
    facts = ""
    summary = ""
    covered = 0
    facts_covered = 0
    steps = []
    total_main = 0
    total_service = 0
    last_prompt_tokens = 0

    for index, turn in enumerate(turns, start=1):
        prompt = build_prompt(strategy, dialog, turn, facts, summary, covered, window)
        response = complete(prompt, scenario)
        answer = response.get("content") or ""
        usage = response.get("usage") or {}
        total_main += usage_total(usage)
        last_prompt_tokens = usage.get("promptTokens") or 0

        dialog.append({"role": "user", "content": turn})
        dialog.append({"role": "assistant", "content": answer})

        if strategy == "sticky_facts":
            uncovered = dialog[facts_covered:]
            text = "\n\n".join(dialog_lines(uncovered))
            facts_response = post_json(
                "/api/v1/chat/facts",
                {"text": text, "facts": facts, "model": scenario["model"]},
            )
            facts = facts_response.get("facts") or facts
            facts_covered = len(dialog)
            total_service += usage_total(facts_response.get("usage"))
        elif strategy == "summarize":
            if len(dialog) - covered >= scenario["summary_interval"]:
                lines = []
                if summary:
                    lines.append("Предыдущее саммари:\n" + summary)
                lines += dialog_lines(dialog[covered:])
                summary_response = post_json(
                    "/api/v1/chat/summarize",
                    {"text": "\n\n".join(lines), "model": scenario["model"]},
                )
                summary = summary_response.get("summary") or summary
                covered = len(dialog)
                total_service += usage_total(summary_response.get("usage"))

        steps.append(
            {
                "turn": index,
                "user": turn,
                "prompt": prompt,
                "answer": answer,
                "usage": usage,
                "facts": facts if strategy == "sticky_facts" else None,
                "summary": summary if strategy == "summarize" else None,
            }
        )
        print(f"[{strategy}] turn {index}/{len(turns)} tokens={usage_total(usage)}", flush=True)

    final_answer = steps[-1]["answer"] if steps else ""
    hits = detail_hits(final_answer, scenario["key_details"])
    result = {
        "strategy": strategy,
        "title": scenario["title"],
        "model": scenario["model"],
        "window": window,
        "summary_interval": scenario["summary_interval"],
        "total_main_tokens": total_main,
        "total_service_tokens": total_service,
        "total_tokens": total_main + total_service,
        "last_prompt_tokens": last_prompt_tokens,
        "key_details_total": len(scenario["key_details"]),
        "key_details_hit": len(hits),
        "key_details_missed": [
            name for name in scenario["key_details"] if name not in hits
        ],
        "final_answer": final_answer,
        "steps": steps,
    }
    return result


def continue_branch(name, dialog, turns, scenario):
    branch = copy.deepcopy(dialog)
    steps = []
    total = 0
    last_prompt_tokens = 0
    for index, turn in enumerate(turns, start=1):
        prompt = build_prompt("as_is", branch, turn, "", "", 0, scenario["window"])
        response = complete(prompt, scenario)
        answer = response.get("content") or ""
        usage = response.get("usage") or {}
        total += usage_total(usage)
        last_prompt_tokens = usage.get("promptTokens") or 0
        branch.append({"role": "user", "content": turn})
        branch.append({"role": "assistant", "content": answer})
        steps.append(
            {
                "turn": index,
                "user": turn,
                "prompt": prompt,
                "answer": answer,
                "usage": usage,
            }
        )
        print(f"[branching:{name}] turn {index}/{len(turns)} tokens={usage_total(usage)}", flush=True)
    return {
        "name": name,
        "total_tokens": total,
        "last_prompt_tokens": last_prompt_tokens,
        "final_answer": steps[-1]["answer"] if steps else "",
        "steps": steps,
    }


def run_branching(scenario):
    turns = scenario["turns"]
    checkpoint = scenario["checkpoint_turn"]
    dialog = []
    prefix_steps = []
    total_prefix = 0
    for index, turn in enumerate(turns[:checkpoint], start=1):
        prompt = build_prompt("as_is", dialog, turn, "", "", 0, scenario["window"])
        response = complete(prompt, scenario)
        answer = response.get("content") or ""
        usage = response.get("usage") or {}
        total_prefix += usage_total(usage)
        dialog.append({"role": "user", "content": turn})
        dialog.append({"role": "assistant", "content": answer})
        prefix_steps.append(
            {"turn": index, "user": turn, "prompt": prompt, "answer": answer, "usage": usage}
        )
        print(f"[branching:prefix] turn {index}/{checkpoint} tokens={usage_total(usage)}", flush=True)

    branch_a = continue_branch("A", dialog, turns[checkpoint:], scenario)
    branch_b = continue_branch("B", dialog, scenario["branch_alt_turns"], scenario)

    hits_a = detail_hits(branch_a["final_answer"], scenario["key_details"])
    hits_b = detail_hits(branch_b["final_answer"], scenario["key_details"])
    return {
        "strategy": "branching",
        "title": scenario["title"],
        "model": scenario["model"],
        "checkpoint_turn": checkpoint,
        "prefix_tokens": total_prefix,
        "prefix_steps": prefix_steps,
        "branches": [branch_a, branch_b],
        "branch_a_key_details_hit": len(hits_a),
        "branch_a_key_details_missed": [
            name for name in scenario["key_details"] if name not in hits_a
        ],
        "branch_b_key_details_hit": len(hits_b),
        "branch_b_key_details_missed": [
            name for name in scenario["key_details"] if name not in hits_b
        ],
    }


def main():
    if not PASSWORD:
        print("SMITH_PASSWORD is not set", file=sys.stderr)
        return 2
    TRANSCRIPTS.mkdir(parents=True, exist_ok=True)
    scenario = json.loads((DIR / "scenario.json").read_text(encoding="utf-8"))
    login()

    requested = sys.argv[1:] or [
        "as_is",
        "sliding_window",
        "sticky_facts",
        "summarize",
        "branching",
    ]
    summary_path = DIR / "summary.json"
    summary = {}
    if summary_path.exists():
        try:
            summary = json.loads(summary_path.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            summary = {}
    for strategy in requested:
        start = time.time()
        if strategy == "branching":
            result = run_branching(scenario)
        else:
            result = run_strategy(strategy, scenario)
        result["elapsed_sec"] = round(time.time() - start, 1)
        (TRANSCRIPTS / f"{strategy}.json").write_text(
            json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8"
        )
        if strategy == "branching":
            summary[strategy] = {
                "elapsed_sec": result["elapsed_sec"],
                "prefix_tokens": result["prefix_tokens"],
                "branch_a_tokens": result["branches"][0]["total_tokens"],
                "branch_b_tokens": result["branches"][1]["total_tokens"],
                "branch_a_last_prompt": result["branches"][0]["last_prompt_tokens"],
                "branch_b_last_prompt": result["branches"][1]["last_prompt_tokens"],
                "branch_a_hits": result["branch_a_key_details_hit"],
                "branch_b_hits": result["branch_b_key_details_hit"],
            }
        else:
            summary[strategy] = {
                "elapsed_sec": result["elapsed_sec"],
                "total_main_tokens": result["total_main_tokens"],
                "total_service_tokens": result["total_service_tokens"],
                "total_tokens": result["total_tokens"],
                "last_prompt_tokens": result["last_prompt_tokens"],
                "key_details_hit": result["key_details_hit"],
                "key_details_total": result["key_details_total"],
                "key_details_missed": result["key_details_missed"],
            }
        print(f"=== {strategy} done: {summary[strategy]}", flush=True)

    (DIR / "summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print("ALL DONE")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
