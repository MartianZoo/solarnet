"""Copy Terraforming Mars custom implementations into Almanac's static resources."""

import json
import re
import sys
from pathlib import Path


root = Path(__file__).resolve().parents[1]
engine_dir = root / "src/common/dev/martianzoo/tfm/engine"
engine = (engine_dir / "TfmEngine.kt").read_text()
registered = engine.split("public val customClasses: Set<CustomClass> =", 1)[1]
registered = registered.split("setOf(", 1)[1].split("\n      )", 1)[0]
expected = set(re.findall(r"(?m)^\s+([A-Za-z][A-Za-z0-9_]*),$", registered))

sources = {}
for path in (
    engine_dir / "TfmEngine.kt",
    engine_dir / "RequiredWildTag.kt",
    engine_dir / "AllocateCloudTourismWildTags.kt",
):
    text = path.read_text()
    pattern = re.compile(
        r"(?m)^(?P<indent>  |)(?:private|internal) object "
        r"(?P<name>[A-Za-z][A-Za-z0-9_]*) : Custom(?:Instruction|Metric)\(\) \{"
    )
    for match in pattern.finditer(text):
        closing = re.search(rf"(?m)^{re.escape(match['indent'])}\}}$", text[match.end() :])
        if closing is None:
            raise ValueError(f"Missing end of {match['name']} in {path}")
        name = match["name"]
        if name in sources:
            raise ValueError(f"Duplicate custom implementation: {name}")
        sources[name] = text[match.start() : match.end() + closing.end()]

if set(sources) != expected:
    raise ValueError(f"Custom source mismatch: missing={expected - set(sources)}, extra={set(sources) - expected}")

output = root / "pets/src/js/dev/martianzoo/tfm/web/almanac/custom-sources.js"
generated = (
    "// Generated from Solarnet's Kotlin implementations; do not edit.\n"
    "globalThis.almanacCustomSources = "
    + json.dumps(sources, ensure_ascii=False, sort_keys=True, indent=2)
    + ";\n"
)
if sys.argv[1:] == ["--check"]:
    if not output.exists() or output.read_text() != generated:
        raise SystemExit(f"Stale custom sources: run python3 {Path(__file__).relative_to(root)}")
    print(f"Checked {len(sources)} custom implementations")
elif not sys.argv[1:]:
    output.write_text(generated)
    print(f"Copied {len(sources)} custom implementations to {output.relative_to(root)}")
else:
    raise SystemExit("Usage: build_almanac_custom_sources.py [--check]")
