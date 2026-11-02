"""
Builds src/main/resources/com/fredflips/item_sets.json: every Grand Exchange item set and the pieces it contains,
read from the "Components" section of each set's OSRS Wiki page (https://oldschool.runescape.wiki/w/Item_set).

Run from the project folder:  python tools/build_item_sets.py
"""
import json
import re
import time
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://oldschool.runescape.wiki/api.php"
PRICES = "https://prices.runescape.wiki/api/v1/osrs/mapping"
USER_AGENT = "FredFlips RuneLite plugin data builder - github.com/Ninjapex/fredflips"
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/com/fredflips/item_sets.json"
BATCH = 50


def get_json(url, params=None):
    if params:
        url += "?" + urllib.parse.urlencode(params)
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def set_titles():
    titles = []
    params = {"action": "query", "list": "categorymembers", "cmtitle": "Category:Item sets",
              "cmlimit": "max", "cmnamespace": "0", "format": "json"}
    while True:
        data = get_json(API, params)
        titles += [page["title"] for page in data["query"]["categorymembers"]]
        if "continue" not in data:
            return [t for t in titles if t != "Item set"]
        params.update(data["continue"])


def page_texts(titles):
    for start in range(0, len(titles), BATCH):
        data = get_json(API, {"action": "query", "prop": "revisions", "rvprop": "content", "rvslots": "main",
                              "titles": "|".join(titles[start:start + BATCH]), "format": "json", "formatversion": "2"})
        for page in data["query"]["pages"]:
            if "revisions" in page:
                yield page["title"], page["revisions"][0]["slots"]["main"]["content"]
        time.sleep(1)  # be gentle with the wiki


def components(text):
    """[(name, quantity)] from {{CostLine|Name}} or {{CostLine|Name|quantity}} lines."""
    found = []
    for match in re.finditer(r"\{\{CostLine\|([^|}]+)(?:\|(\d+))?", text):
        found.append((match.group(1).strip(), int(match.group(2) or 1)))
    return found


def main():
    ids = {item["name"].lower(): item["id"] for item in get_json(PRICES)}
    titles = set_titles()
    print(f"{len(titles)} item sets on the wiki")

    sets = []
    skipped = []
    for title, text in page_texts(titles):
        pieces = components(text)
        set_id = ids.get(title.lower())
        if set_id is None or len(pieces) < 2 or any(name.lower() not in ids for name, _ in pieces):
            skipped.append(title)
            continue
        sets.append({
            "id": set_id,
            "name": title,
            "pieces": [{"id": ids[name.lower()], "name": name, "quantity": quantity} for name, quantity in pieces],
        })

    sets.sort(key=lambda s: s["name"].lower())
    OUT.write_text(json.dumps({
        "source": "OSRS Wiki item set pages (CC BY-NC-SA 3.0) - https://oldschool.runescape.wiki/w/Item_set",
        "sets": sets,
    }, indent=1), encoding="utf-8")
    print(f"kept {len(sets)} sets -> {OUT}")
    if skipped:
        print(f"skipped {len(skipped)} (no price data or no components): {', '.join(sorted(skipped)[:15])}")


if __name__ == "__main__":
    main()
