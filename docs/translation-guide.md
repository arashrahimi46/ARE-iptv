# Translation Guide — ARE iptv

> Status: **Binding.** Read this before editing any `values-*/strings.xml`. The mechanics section is
> hard rules — violating them either breaks the build or silently ships English. The quality section
> is the part that actually gets rejected in review: the recurring failure here is not missing keys,
> it is translations that are *correct English rendered in another language* instead of what a native
> speaker would have written. Persian was the worst offender and had to be rewritten wholesale.

## 1. The locale set

**English base + 23 translated locales.** All of them ship: `android:supportsRtl="true"` is set in
`AndroidManifest.xml`, and there is **no** `locales_config.xml` and **no** `resourceConfigurations`
filter, so every `values-*` directory on disk reaches users.

| Dir | Language | | Dir | Language |
|-----|----------|-|-----|----------|
| `values-ar` | Arabic **(RTL)** | | `values-hu` | Hungarian |
| `values-az` | Azerbaijani | | `values-it` | Italian |
| `values-b+pt+BR` | Portuguese (Brazil) | | `values-nb` | Norwegian Bokmål |
| `values-b+pt+PT` | Portuguese (Portugal) | | `values-nl` | Dutch |
| `values-bg` | Bulgarian | | `values-pl` | Polish |
| `values-cs` | Czech | | `values-ro` | Romanian |
| `values-da` | Danish | | `values-ru` | Russian |
| `values-de` | German | | `values-sv` | Swedish |
| `values-el` | Greek | | `values-tr` | Turkish |
| `values-es` | Spanish | | `values-uk` | Ukrainian |
| `values-fa` | Persian **(RTL)** | | | |
| `values-fi` | Finnish | | | |
| `values-fr` | French | | | |

**Why the count is called out this hard:** `CLAUDE.md` used to say "21 locales" and enumerate a list
that omitted `fa` and `ar`. Contributors followed it literally, updated 21 directories, and fa/ar
drifted **27 keys behind** the other 22 — the entire `hud_ctl_*`, `hud_editor_*`,
`settings_sidebar_style*`, `player_playback_speed`, `player_audio_delay`, `settings_rearrange_hud*`
wave. Nothing failed. The app just quietly showed English inside two RTL layouts for months. **The
RTL pair is always the pair that gets dropped — check it first, not last.**

## 2. Mechanics — hard rules

**Key set must equal the English base minus `legal_*`, exactly.**
- Never change, "fix", or localize a `name=` attribute. The key is code.
- No extra keys, no missing keys. A locale is correct when its key set is set-equal to
  `values/strings.xml` **less the 32 English-only `legal_*` keys** — see §2's legal rule. The
  base is currently 690 keys, so a correct locale has 658.
- **A missing key does not fail the build.** Android falls back to the English base resource
  silently. This is the single reason gaps survive for months and the entire reason this sync
  discipline is written down.

**Positional format args are load-bearing.**
- Every index in the English string (`%1$s`, `%1$d`, `%2$d`, …) must appear in the translation
  **exactly once**.
- **Reordering is allowed and often required** — `"%2$s at %1$s"` is a legitimate translation of
  `"%1$s at %2$s"` when the target language's word order demands it.
- **Dropping an index is not allowed**, even when the sentence reads fine without it. Missing
  arguments are a runtime formatting crash, not a cosmetic issue.
- Never renumber to positionless `%s`. This project uses positional args everywhere.

**Escaping.**
- `'` must be written `\'` — an apostrophe is a string delimiter in Android resources.
- `&`, `<`, `>` must be XML-escaped (`&amp;`, `&lt;`, `&gt;`).
- **High-risk languages: French, Italian, Turkish, Azerbaijani.** These are apostrophe-dense
  (`l'écran`, `dell'app`, `Türkiye'nin`, `IPTV'ni`) and are where this goes wrong.
- An unescaped apostrophe is an **`aapt` build failure**, not a silent bug. It will stop the release
  APK from building. This is the good failure mode — the bad one is a missing key.

**"ARE iptv" is never translated and never transliterated.** Not into Cyrillic, not into Arabic or
Persian script, not case-normalized. It is the brand mark; it stays Latin `ARE iptv` in all 23
locales, including inside RTL sentences.

**Never translate `legal_*`. They are English-only by design.** The 32 keys `legal_doc_title`,
`legal_doc_meta` and `legal_s1..s15_title`/`_body` are the binding Privacy Policy & Terms. A
machine-translated legal clause is still legally binding on us and can contradict the English
text — and §13 of the document states the English version governs. The prohibition is written
above the keys themselves in `values/strings.xml`; read it before touching them.

The surrounding chrome (`privacy_*`, `settings_legal_*`, the summary, buttons and Settings rows)
**is** translated, so no locale sees a broken screen.

Practical consequence: **a correct locale file has 32 fewer keys than the English base, not the
same number.** Your key-set check must compare against *base minus `legal_*`* and separately
assert zero `legal_*` keys are present — a locale whose count equals the base's is failing, not
passing. This has been got wrong before, in both directions.

## 3. Quality bar — the part that gets things rejected

### Translate the meaning and the voice, not the words

The app's English voice is **warm and personal** — it is a one-person side project and it says so.
The target is a **natural consumer-software register in that language**, not corporate boilerplate
and not a gloss of the English syntax.

**The worked example.** English:

> `action_buy_coffee` → "Buy me a coffee"
> `support_dialog_body` → "… If it earns its place on your TV, a coffee keeps it going."

The rejected Persian was a word-for-word calque:

> ✗ «برایم یک قهوه بخرید» / «زنده نگه می‌دارد»

Grammatical, parseable, and wrong. Persian does not "buy someone a coffee" — the idiom is
**مهمان کردن** (to treat/host someone). And «زنده نگه می‌دارد» ("keeps it alive") is a literal transfer
of an English metaphor that reads clinical in Persian. The fix uses the actual idiom:
«یه قهوه مهمونم کن».

Compare the French, which is what good looks like:

> ✓ "Offre-moi un café" / "un café la garde en vie"

`offrir` is what French actually says here — not `acheter`. It found the target-language idiom
instead of transporting the English one.

**Test:** would a native speaker who has never seen the English write this sentence? If the answer
is "they'd understand it," that is a fail. Understandable is the floor, not the bar.

### One register, held across the whole file

Pick the formality register once per language and **do not vary it**. Mixed register is the single
most common defect found in this repo's locale files — a file that addresses the user as `Sie` in
Settings and `du` in a dialog, or that mixes Persian formal `شما` verb forms with colloquial ones.

- Decide the T/V choice (`tu`/`vous`, `du`/`Sie`, `ты`/`вы`, `sen`/`siz`, informal/formal Persian).
- Decide imperative style for buttons (infinitive vs. imperative — German UI conventionally uses the
  infinitive: "Speichern", not "Speichere").
- Then apply it to all ~690 strings. Consistency beats any individual choice.

For this app, warm-but-not-sloppy: the informal register where it is the consumer-software norm
(French `vous` is still standard for UI; German `du` is now common in consumer apps; Persian
colloquial-but-polite), never the enterprise register.

### Use the terms native streaming apps use

UI nouns come from what the local Netflix/YouTube/TV-app ecosystem actually says, not from a
dictionary. Play, Live TV, Season, Episode, Continue watching, Guide/EPG, Favorites, Subtitles,
Playback speed — every one of these has an established local convention. Use it. A literal gloss of
"Continue watching" that no local streaming service uses is a defect even though it is accurate.

`EPG` in particular: some languages keep the acronym, some translate to "programme guide". Follow
local practice; do not invent a third option.

### Length matters — this is a TV app

Nav labels, buttons, and tabs are **width-constrained** on a 10-foot UI. There is already a commit
`i18n(tv): shorten the longest sidebar nav labels` because English overflowed.

- **German, Greek, Hungarian, Finnish, and the Romance languages run long.** Assume 1.3–1.5× the
  English character count if you translate naturally.
- For nav/button/tab strings, **abbreviate or pick a shorter synonym rather than overflow.**
  A slightly terser correct word beats an ellipsised perfect one.
- Body copy in dialogs and settings descriptions can breathe — the constraint is on the chrome.

## 4. Language-family traps

| Family | Trap |
|--------|------|
| **Slavic** (ru, uk, pl, cs, bg) | Nouns take case from their syntactic slot. An interpolated `%1$s` arrives in nominative — do not write a sentence that requires it to be genitive/accusative. Restructure so the arg sits in a case-neutral position. Also: these languages have 3–4 plural forms, and **this project uses no `<plurals>` at all**. Every numeric string must therefore be phrased count-generically (a "N: items" or "Items — N" shape), never "N файла". |
| **Turkic + Hungarian** (tr, az, hu) | Vowel harmony. Case/possessive suffixes change shape with the preceding vowel, so **never hardcode a suffix onto `%1$s`** — `%1$s'nin` is wrong roughly half the time because you cannot know the channel name's last vowel. Restructure the sentence to avoid suffixing the interpolation. |
| **Romance** (fr, it, es, ro, pt) | Apostrophe escaping (see §2) — this is where builds break. Plus: **pt-BR and pt-PT must genuinely differ.** If the two files are identical, the work was not done. Minimum divergences: *ecrã* (PT) / *tela* (BR), *ficheiro* (PT) / *arquivo* (BR), *utilizador* (PT) / *usuário* (BR), and PT's gerund-avoiding *a carregar* vs BR's *carregando*. |
| **Ukrainian** | `values-uk` must not be a russified transform of `values-ru`. Different lexical choices, not orthographic substitution. If it reads as Russian with Ukrainian spelling, it is wrong. |
| **Azerbaijani** | `values-az` must not be a find-and-replace of `values-tr`. Related, not the same language — vocabulary, some orthography, and register conventions differ. |
| **RTL** (fa, ar) | Do not end a clause on a preposition immediately before an interpolated LTR run — a URL, a bare number, or the Latin brand mark `ARE iptv`. The bidi algorithm reorders the boundary and the punctuation lands visually wrong. Restructure so the LTR run is not sentence-final or preposition-adjacent. Also: Persian uses ZWNJ (`‌`) in compounds like `می‌شود` — keep it; it is not whitespace to be stripped. Arabic-Indic vs Western digits: follow whatever the existing file already does, consistently. |

## 5. Workflow for a translator agent

Ordered. Do not skip step 1 to "just start translating".

1. **Audit key gaps first.** Run the validation script (§6) against your locale before writing
   anything. Know exactly which keys are missing. The gap is usually larger than the task
   description implies.
2. **Translate the missing keys**, to the §3 bar, in the register the existing file already uses.
3. **Quality-pass the existing strings** — but do **not churn good copy.** Change what is a calque,
   a register break, or an overflow risk. Leave correct, idiomatic strings alone. A diff that
   rewrites 600 acceptable strings to reach 20 bad ones is a worse diff.
4. **Run the validation script.** It must report zero issues.
5. **Compile-validate:** `./gradlew :tv:assembleDebug` — this runs `aapt`, which validates all
   string resources and is what catches unescaped apostrophes and malformed args.
6. **Report:** keys added, strings revised and why, anything you deliberately left alone.

Do **not** edit `values/strings.xml` (the English base) while doing a translation pass, and do not
touch another agent's locale directory — locale work is frequently parallelized across agents.

### Quick key-set diff (shell)

```bash
cd tv/src/main/res
base=$(grep -o 'name="[^"]*"' values/strings.xml | sort -u)
for d in values-*; do
  loc=$(grep -o 'name="[^"]*"' "$d/strings.xml" | sort -u)
  echo "$d missing=$(comm -23 <(echo "$base") <(echo "$loc") | wc -l)"
done
```

Every locale should report the same number. **A locale reporting more than its peers is the bug** —
that is exactly how the fa/ar 27-key gap was found.

## 6. Validation script

Copy-paste runnable. Checks XML well-formedness, key-set equality against the English base, format-arg
index equality, and unescaped apostrophes.

```python
#!/usr/bin/env python3
"""Validate ARE iptv locale files against the English base.
Usage: python3 validate_strings.py [tv/src/main/res] [locale ...]"""
import re, sys, os
import xml.etree.ElementTree as ET

RES = sys.argv[1] if len(sys.argv) > 1 else "tv/src/main/res"
ONLY = set(sys.argv[2:])
ARG = re.compile(r"%(\d+)\$[sd]")
# an apostrophe not preceded by a backslash, in the raw source text
BAD_APOS = re.compile(r"(?<!\\)'")


def load(path):
    """-> (dict name->raw_text, list of parse errors)"""
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError as e:
        return None, [f"XML parse error: {e}"]
    out = {}
    for el in root:
        if el.tag != "string":
            continue  # this project uses no <plurals>/<string-array> for UI copy
        name = el.get("name")
        out[name] = "".join(el.itertext())
    return out, []


def raw_strings(path):
    """Raw per-key source text, for escape checks ET would have already resolved."""
    src = open(path, encoding="utf-8").read()
    return dict(re.findall(r'<string name="([^"]+)"[^>]*>(.*?)</string>', src, re.S))


base, err = load(os.path.join(RES, "values", "strings.xml"))
if err:
    sys.exit("FATAL base: " + "; ".join(err))

locales = sorted(d for d in os.listdir(RES) if d.startswith("values-"))
if ONLY:
    locales = [d for d in locales if d in ONLY or d[len("values-"):] in ONLY]

fail = 0
for d in locales:
    path = os.path.join(RES, d, "strings.xml")
    if not os.path.exists(path):
        print(f"{d}: MISSING strings.xml"); fail += 1; continue
    loc, err = load(path)
    if err:
        print(f"{d}: " + "; ".join(err)); fail += 1; continue

    issues = []
    # legal_* is English-only by design (see §2) -- a locale carrying them is WRONG,
    # so they are excluded from the expected set and reported separately.
    expected = {k for k in base if not k.startswith("legal_")}
    leaked = sorted(k for k in loc if k.startswith("legal_"))
    missing = sorted(expected - set(loc))
    extra = sorted(set(loc) - set(base))
    if leaked:
        issues.append(f"{len(leaked)} translated legal_* key(s) -- MUST be English-only: "
                      + ", ".join(leaked[:5]) + (" …" if len(leaked) > 5 else ""))
    if missing:
        issues.append(f"{len(missing)} missing key(s): {', '.join(missing[:8])}"
                      + (" …" if len(missing) > 8 else ""))
    if extra:
        issues.append(f"{len(extra)} unknown key(s): {', '.join(extra[:8])}")

    for k in sorted(set(base) & set(loc)):
        b, l = sorted(ARG.findall(base[k])), sorted(ARG.findall(loc[k]))
        if b != l:
            issues.append(f"{k}: format args {b or '[]'} -> {l or '[]'}")

    for k, raw in raw_strings(path).items():
        # strip CDATA and entities before looking for stray apostrophes
        if BAD_APOS.search(raw):
            issues.append(f"{k}: unescaped apostrophe (use \\')")

    if issues:
        fail += 1
        print(f"\n{d}  ({len(issues)} issue(s))")
        for i in issues:
            print("   -", i)
    else:
        print(f"{d}: OK ({len(loc)} keys)")

print("\nRESULT:", "FAIL" if fail else "PASS", f"({fail} locale(s) with issues)")
sys.exit(1 if fail else 0)
```

Run from the repo root: `python3 validate_strings.py` (all locales) or
`python3 validate_strings.py tv/src/main/res fa ar` (just the RTL pair).

Passing this script is necessary, not sufficient — it cannot see calques, register drift, or a nav
label that overflows the sidebar. Those are §3, and they are what review is for.
