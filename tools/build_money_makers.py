"""
Builds src/main/resources/com/fredflips/money_makers.json from the OSRS Wiki's money making guides.

It keeps methods that turn items you can buy on the GE into items you can sell on the GE (enchanting,
fletching, herblore, crafting, Plank Make...). FredFlips then works out live GP/hr from those recipes and
current prices. Methods you can do standing at a bank are tagged so the Money tab can show just those.

Recipes and rates come from the OSRS Wiki (https://oldschool.runescape.wiki/w/Money_making_guide),
licensed CC BY-NC-SA 3.0.

Run from the project folder:  python tools/build_money_makers.py
Add --report to list every guide that was skipped and why.
"""
import ast
import json
import operator
import re
import sys
import time
import urllib.parse
import urllib.request
from pathlib import Path

API = "https://oldschool.runescape.wiki/api.php"
PRICES = "https://prices.runescape.wiki/api/v1/osrs/mapping"
USER_AGENT = "FredFlips RuneLite plugin data builder - github.com/Ninjapex/fredflips"
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/com/fredflips/money_makers.json"
BATCH = 50
COINS = 995

# Words in a guide's Location that mean you can do it standing at a bank.
BANK_WORDS = ("bank", "grand exchange", "anywhere", "any location", "clan hall", "castle wars", "ferox")


def get_json(url, params=None):
    if params:
        url += "?" + urllib.parse.urlencode(params)
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def guide_titles():
    titles = []
    params = {"action": "query", "list": "allpages", "apprefix": "Money making guide/",
              "aplimit": "max", "format": "json"}
    while True:
        data = get_json(API, params)
        titles += [page["title"] for page in data["query"]["allpages"]]
        if "continue" not in data:
            return titles
        params.update(data["continue"])


def page_texts(titles):
    for start in range(0, len(titles), BATCH):
        data = get_json(API, {"action": "query", "prop": "revisions", "rvprop": "content", "rvslots": "main",
                              "titles": "|".join(titles[start:start + BATCH]), "format": "json",
                              "formatversion": "2"})
        for page in data["query"]["pages"]:
            if "revisions" in page:
                yield page["title"], page["revisions"][0]["slots"]["main"]["content"]
        time.sleep(1)  # be gentle with the wiki


def template_params(text):
    """The one-line |Key = value parameters of the {{Mmgtable}} template."""
    text = re.sub(r"<!--.*?-->", "", text, flags=re.S)
    start = text.find("{{Mmgtable")
    if start < 0:
        return None
    params = {}
    for line in text[start:].splitlines()[1:]:
        if line.startswith("}}"):
            break
        match = re.match(r"\|\s*([^=|]+?)\s*=\s*(.*)$", line)
        if match:
            params[match.group(1)] = match.group(2).strip()
    return params


def field(text, name):
    """A |name = value line anywhere in the page, for guides whose tables are full of wiki formulas."""
    match = re.search(r"^\|\s*" + re.escape(name) + r"\s*=\s*(.*)$", re.sub(r"<!--.*?-->", "", text, flags=re.S),
                      re.MULTILINE)
    return match.group(1).strip() if match else None


def rate(value):
    """Actions per hour. Unlike item amounts, a missing rate is an error, never a default of 1."""
    if not value:
        return None
    result = number(value)
    return result if result and result >= 10 else None


def plain(text):
    text = re.sub(r"\[\[[^|\]]*\|([^\]]*)\]\]", r"\1", text)
    text = re.sub(r"\[\[([^\]]*)\]\]", r"\1", text)
    text = re.sub(r"\{\{[^}]*\}\}", "", text)
    text = re.sub(r"<[^>]+>", "", text)
    text = text.replace("'''", "").replace("''", "")
    return re.sub(r"\s+", " ", text).strip()


_OPERATORS = {ast.Add: operator.add, ast.Sub: operator.sub, ast.Mult: operator.mul, ast.Div: operator.truediv}


def _evaluate(node):
    """Plain arithmetic only - numbers, + - * / and brackets. Anything else is rejected."""
    if isinstance(node, ast.Expression):
        return _evaluate(node.body)
    if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
        return float(node.value)
    if isinstance(node, ast.BinOp) and type(node.op) in _OPERATORS:
        return _OPERATORS[type(node.op)](_evaluate(node.left), _evaluate(node.right))
    if isinstance(node, ast.UnaryOp) and isinstance(node.op, ast.USub):
        return -_evaluate(node.operand)
    raise ValueError("not plain arithmetic")


def number(value):
    """Amounts like "27", "1,600" or "1500 * 27". None if it uses wiki variables or anything else."""
    if value is None or value == "":
        return 1.0
    text = value.replace(",", "").replace("&times;", "*").replace("×", "*").strip()
    wrapped = re.fullmatch(r"\{\{#expr:(.*)\}\}", text)
    if wrapped:
        text = wrapped.group(1).strip()
    if not re.fullmatch(r"[0-9.+\-*/() ]+", text):
        return None
    try:
        return _evaluate(ast.parse(text, mode="eval"))
    except (ValueError, SyntaxError, ZeroDivisionError):
        return None


def items(params, prefix, ids):
    """[{id, name, quantity}] for Input1..N or Output1..N, or a string saying why it can't be used."""
    result = []
    n = 1
    while params.get(prefix + str(n)):
        name = plain(params[prefix + str(n)])
        quantity = number(params.get(prefix + str(n) + "num"))
        if quantity is None:
            return "amount isn't a plain number: " + params.get(prefix + str(n) + "num", "")
        if quantity <= 0:
            return "zero amount"
        if name.lower() == "coins":
            result.append({"id": COINS, "name": "Coins", "quantity": quantity})
        else:
            item_id = ids.get(name.lower())
            if item_id is None:
                return "not on the GE: " + name
            result.append({"id": item_id, "name": name, "quantity": quantity})
        n += 1
    return result


def skill_requirements(text):
    found = re.findall(r"\{\{SCP\|([A-Za-z]+)\|(\d+)", text or "")
    return [{"skill": skill, "level": int(level)} for skill, level in found]


def quest_note(params):
    quest = plain(params.get("Quest", ""))
    return None if not quest or quest.lower() == "none" else "Needs " + quest + "."


def is_bank_standing(params):
    category = params.get("Category", "").lower()
    location = plain(params.get("Location", "")).lower()
    return category.startswith("processing") or any(word in location for word in BANK_WORDS)


# ---------------------------------------------------------------- guides that pick their inputs on the fly

PLANK_MAKE_PAGE = "Money making guide/Casting plank make"
# Variable prefix in the guide -> (log, plank)
PLANK_TYPES = {
    "log": ("Logs", "Plank"),
    "oak": ("Oak logs", "Oak plank"),
    "teak": ("Teak logs", "Teak plank"),
    "mahogany": ("Mahogany logs", "Mahogany plank"),
    "camphor": ("Camphor logs", "Camphor plank"),
    "ironwood": ("Ironwood logs", "Ironwood plank"),
    "rosewood": ("Rosewood logs", "Rosewood plank"),
}


def plank_make_methods(text, ids):
    """
    The Plank Make guide picks the best log on the fly, so it has no fixed inputs to read.
    Build one method per log type from the fees and casts per hour in that guide instead.
    Each cast: 1 log + 2 astral runes + 1 nature rune + a coin fee -> 1 plank (earth runes from a staff).
    """
    casts = rate(field(text, "kph"))
    fees = {name: float(fee) for name, fee in re.findall(r"#vardefine:(\w+)fee\|(\d+)", text)}
    if not casts:
        return []
    methods = []
    for prefix, (log, plank) in PLANK_TYPES.items():
        if prefix not in fees or log.lower() not in ids or plank.lower() not in ids:
            continue
        methods.append({
            "name": "Plank Make (" + plank.lower() + "s)",
            "page": PLANK_MAKE_PAGE,
            "category": "Processing",
            "members": True,
            "bankStanding": True,
            "requirements": [{"skill": "Magic", "level": 86}],
            "notes": "Needs Dream Mentor (Lunar spellbook). Assumes a staff supplies earth runes.",
            "actionsPerHour": casts,
            "inputsPerHour": [
                {"id": ids[log.lower()], "name": log, "quantity": casts},
                {"id": ids["astral rune"], "name": "Astral rune", "quantity": 2 * casts},
                {"id": ids["nature rune"], "name": "Nature rune", "quantity": casts},
                {"id": COINS, "name": "Coins", "quantity": fees[prefix] * casts},
            ],
            "outputsPerHour": [{"id": ids[plank.lower()], "name": plank, "quantity": casts}],
        })
    return methods


# Guides that pick between options with wiki formulas. Each option becomes its own method, using the guide's
# own actions per hour, skill and quest requirements. Amounts are per action (cast).
VARIANTS = {
    "Money making guide/Casting Superglass Make": {
        "note": "Assumes staves supply the air and fire runes.",
        "options": [
            ("Superglass Make (giant seaweed)",
             [("Giant seaweed", 3), ("Bucket of sand", 18), ("Astral rune", 2)], [("Molten glass", 26.1)]),
            ("Superglass Make (seaweed)",
             [("Seaweed", 13), ("Bucket of sand", 13), ("Astral rune", 2)], [("Molten glass", 16.9)]),
        ],
    },
    "Money making guide/Casting Tan Leather": {
        "note": "Also needs the hard Fremennik Diary. Assumes a fire staff.",
        "options": [
            ("Tan Leather (" + colour.lower() + " dragonhide)",
             [(colour + " dragonhide", 5), ("Nature rune", 1), ("Astral rune", 2)], [(colour + " dragon leather", 5)])
            for colour in ("Green", "Blue", "Red", "Black")
        ],
    },
    "Money making guide/Enchanting topaz jewellery": {
        "note": "Lvl-3 Enchant. Assumes a staff supplies fire runes.",
        "options": [
            ("Enchanting " + piece.lower(), [(piece, 1), ("Cosmic rune", 1)], [(product, 1)])
            for piece, product in (("Topaz ring", "Efaritay's aid"), ("Topaz necklace", "Necklace of faith"),
                                   ("Topaz bracelet", "Bracelet of slaughter"), ("Topaz amulet", "Burning amulet(5)"))
        ],
    },
    "Money making guide/Enchanting dragonstone jewellery": {
        "note": "Lvl-5 Enchant. Assumes staves supply earth and water runes.",
        "options": [
            ("Enchanting " + label, [(piece, 1), ("Cosmic rune", 1)], [(product, 1)])
            for piece, label, product in (("Dragonstone ring", "dragonstone rings", "Ring of wealth"),
                                          ("Dragon necklace", "dragonstone necklaces", "Skills necklace"),
                                          ("Dragonstone bracelet", "dragonstone bracelets", "Combat bracelet"),
                                          ("Dragonstone amulet", "dragonstone amulets", "Amulet of glory"))
        ],
    },
    "Money making guide/Fletching gem-tipped dragon bolts": {
        "note": "",
        "options": [
            ("Fletching " + gem.lower() + " dragon bolts", [(gem + " bolt tips", 1), ("Dragon bolts", 1)],
             [(gem + " dragon bolts", 1)])
            for gem in ("Opal", "Jade", "Pearl", "Topaz", "Sapphire", "Emerald", "Ruby", "Diamond", "Dragonstone", "Onyx")
        ],
    },
    "Money making guide/Poisoning ammunition": {
        "note": "",
        "options": [
            ("Poisoning " + ammo.lower() + "s " + suffix, [(poison, 1), (ammo, 5)], [(poisoned, 5)])
            for ammo, poisoned_base in (("Rune arrow", "Rune arrow"), ("Amethyst arrow", "Amethyst arrow"),
                                        ("Dragon arrow", "Dragon arrow"), ("Rune dart", "Rune dart"),
                                        ("Adamant dart", "Adamant dart"), ("Amethyst dart", "Amethyst dart"),
                                        ("Dragon dart", "Dragon dart"), ("Dragon javelin", "Dragon javelin"),
                                        ("Dragon knife", "Dragon knife"), ("Runite bolts", "Runite bolts "))
            for poison, suffix, poisoned in ((
                "Weapon poison", "(p)", poisoned_base + "(p)"), (
                "Weapon poison(+)", "(p+)", poisoned_base + "(p+)"), (
                "Weapon poison(++)", "(p++)", poisoned_base + "(p++)"))
        ],
    },
    "Money making guide/Making super combat potions": {
        "note": "",
        "options": [
            ("Making super combat potions (from " + torstol.lower() + ")",
             [("Super attack(4)", 1), ("Super defence(4)", 1), ("Super strength(4)", 1), (torstol, 1)],
             [("Super combat potion(4)", 1), ("Vial", 2)])
            for torstol in ("Torstol", "Torstol potion (unf)")
        ],
    },
    "Money making guide/Tanning cowhide": {
        "note": "At a tanner. Includes 1 energy potion per 26 hides for running.",
        "options": [
            ("Tanning cowhide into " + leather.lower(), [("Coins", fee), ("Cowhide", 1), ("Energy potion(4)", 1 / 26)],
             [(leather, 1)])
            for leather, fee in (("Leather", 1), ("Hard leather", 3))
        ],
    },
}


def variant_methods(title, text, ids):
    spec = VARIANTS[title]
    kph = rate(field(text, "kph"))
    if not kph:
        return []
    params = {"Quest": field(text, "Quest") or "", "Skill": field(text, "Skill") or ""}
    quest = quest_note(params)
    note = " ".join(part for part in (quest, spec["note"]) if part)
    methods = []
    for name, inputs, outputs in spec["options"]:
        if any(item.lower() not in ids and item != "Coins" for item, _ in inputs + outputs):
            continue
        methods.append({
            "name": name,
            "page": title,
            "category": "Processing",
            "members": True,
            "bankStanding": True,
            "requirements": skill_requirements(params.get("Skill")),
            "notes": note or None,
            "actionsPerHour": kph,
            "inputsPerHour": [{"id": COINS if i == "Coins" else ids[i.lower()], "name": i, "quantity": q * kph}
                              for i, q in inputs],
            "outputsPerHour": [{"id": ids[o.lower()], "name": o, "quantity": q * kph} for o, q in outputs],
        })
    return methods


# Guides handled by hand above, by title.
SPECIAL_PAGES = {PLANK_MAKE_PAGE: plank_make_methods}
SPECIAL_PAGES.update({title: (lambda text, ids, t=title: variant_methods(t, text, ids)) for title in VARIANTS})


def main():
    report = "--report" in sys.argv
    ids = {item["name"].lower(): item["id"] for item in get_json(PRICES)}
    titles = [t for t in guide_titles() if t != "Money making guide/"]
    print(f"{len(titles)} money making guides on the wiki")

    methods = []
    skipped = {}
    skipped_processing = []
    for title, text in page_texts(titles):
        if title in SPECIAL_PAGES:
            built = SPECIAL_PAGES[title](text, ids)
            methods += built
            if not built:
                skipped_processing.append((title, "special guide changed - update the builder"))
            continue

        params = template_params(text)
        if not params:
            skipped["no money making table"] = skipped.get("no money making table", 0) + 1
            continue

        kph = number(params.get("kph")) if params.get("kph") else None
        # With isperkill, input/output amounts are per action and kph is actions per hour.
        # Without it, the amounts are already per hour.
        per_action = params.get("isperkill", "").lower().startswith("y")
        inputs = items(params, "Input", ids)
        outputs = items(params, "Output", ids)
        bank = is_bank_standing(params)

        reason = None
        if kph is None or kph <= 0:
            reason = "actions per hour isn't a plain number"
        elif isinstance(inputs, str):
            reason = "input " + inputs
        elif isinstance(outputs, str):
            reason = "output " + outputs
        elif not [i for i in inputs if i["id"] != COINS] or not [o for o in outputs if o["id"] != COINS]:
            reason = "nothing bought or nothing sold on the GE"
        if reason:
            key = reason.split(":")[0]
            skipped[key] = skipped.get(key, 0) + 1
            if bank:
                skipped_processing.append((title, reason))
            continue

        per_hour = kph if per_action else 1.0
        methods.append({
            "name": plain(params.get("Activity", title.split("/", 1)[1])),
            "page": title,
            "category": params.get("Category", ""),
            "members": params.get("Members", "Yes").lower().startswith("y"),
            "bankStanding": bank,
            "requirements": skill_requirements(params.get("Skill")),
            "notes": quest_note(params),
            "actionsPerHour": kph if per_action else None,
            "inputsPerHour": [dict(i, quantity=i["quantity"] * per_hour) for i in inputs],
            "outputsPerHour": [dict(o, quantity=o["quantity"] * per_hour) for o in outputs],
        })

    methods.sort(key=lambda m: m["name"].lower())
    OUT.write_text(json.dumps({
        "source": "OSRS Wiki money making guides (CC BY-NC-SA 3.0) - https://oldschool.runescape.wiki/w/Money_making_guide",
        "methods": methods,
    }, indent=1), encoding="utf-8")
    bank_count = sum(1 for m in methods if m["bankStanding"])
    print(f"kept {len(methods)} methods ({bank_count} bank-standing) -> {OUT}")
    for reason, count in sorted(skipped.items(), key=lambda kv: -kv[1]):
        print(f"  skipped {count}: {reason}")
    if report or skipped_processing:
        print(f"\nBank-standing guides that couldn't be read ({len(skipped_processing)}):")
        for title, reason in sorted(skipped_processing):
            print(f"  {title.split('/', 1)[1]}: {reason}")


if __name__ == "__main__":
    main()
