#!/usr/bin/env python3
"""Inventory pinned tracked sources. Lexical evidence only, not compiler coverage."""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def git(repo, *args):
    return subprocess.check_output(["git", "-C", str(repo), *args], text=True).strip()


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--bootstrap-root", required=True, type=Path)
    parser.add_argument("--output", type=Path, default=ROOT / "reports/assessment.json")
    args = parser.parse_args()
    lock = json.loads((ROOT / "upstream/assessment-lock.json").read_text())
    compat = args.bootstrap_root / "teavm/teavm-gwt-compat/src/main/java"
    if not compat.is_dir():
        raise SystemExit("Bootstrap compatibility sources not found")
    compat_files = sorted(compat.rglob("*.java"))
    compat_hashes = {str(p.relative_to(compat)): digest(p.read_bytes()) for p in compat_files}
    report = {
        "method": "Tracked src/main/java files; regex occurrences may include comments. Import presence does not prove member signatures or runtime behavior. Wildcards are unresolved. No compilation or browser execution performed.",
        "bootstrapCompatibility": {
            "commit": git(args.bootstrap_root, "rev-parse", "HEAD"),
            "workingTreeDirty": bool(git(args.bootstrap_root, "status", "--porcelain")),
            "javaFiles": len(compat_files),
            "sourceManifestSha256": digest(json.dumps(compat_hashes, sort_keys=True).encode()),
        },
        "repositories": {},
    }
    for name, source in sorted(lock["repositories"].items()):
        repo = args.source_root / name
        if git(repo, "rev-parse", "HEAD") != source["commit"]:
            raise SystemExit(f"{name}: checkout differs from assessment lock")
        if git(repo, "status", "--porcelain", "--untracked-files=no"):
            raise SystemExit(f"{name}: tracked files have changed")
        files = git(repo, "ls-files", "-z").rstrip("\0").split("\0")
        java = [p for p in files if "src/main/java/" in p and p.endswith(".java")]
        templates = [p for p in files if p.endswith(".ui.xml")]
        imports, sites, java_hashes = set(), [], {}
        counts = {"mainJavaFiles": len(java), "uiTemplates": len(templates),
                  "jsniBodiesLexical": 0, "jsTypeAnnotationsLexical": 0,
                  "gwtCreateCallsLexical": 0}
        patterns = {"jsni": r"/\*-\{", "jsType": r"@JsType\b", "gwtCreate": r"\bGWT\s*\.\s*create\s*\("}
        for path in java:
            data = (repo / path).read_bytes()
            java_hashes[path] = digest(data)
            text = data.decode("utf-8")
            imports.update(re.findall(r"^import\s+(?:static\s+)?([\w.*]+);", text, re.M))
            for kind, pattern in patterns.items():
                matches = list(re.finditer(pattern, text))
                key = {"jsni": "jsniBodiesLexical", "jsType": "jsTypeAnnotationsLexical", "gwtCreate": "gwtCreateCallsLexical"}[kind]
                counts[key] += len(matches)
                sites.extend({"kind": kind, "path": path, "line": text.count("\n", 0, m.start()) + 1} for m in matches)
        present, missing, wildcards = [], [], []
        for imported in sorted(i for i in imports if i.startswith("com.google.")):
            if imported.endswith(".*"):
                wildcards.append(imported)
                continue
            parts = imported.split(".")
            exists = any((compat / ("/".join(parts[:n]) + ".java")).exists()
                         for n in range(3, len(parts) + 1))
            (present if exists else missing).append(imported)
        page_prefix = "src/main/java/gmd/core/demo/client/application/page/"
        report["repositories"][name] = {
            **source, **counts,
            "imports": sorted(imports),
            "gwtImportNameCandidatesPresent": present,
            "gwtImportNameCandidatesMissing": missing,
            "gwtWildcardImportsUnresolved": wildcards,
            "seamLocations": sites,
            "javaFileSha256": java_hashes,
            "templateSha256": {p: digest((repo / p).read_bytes()) for p in templates},
            "coreShowcasePageDirectories": sorted({p[len(page_prefix):].split("/")[0]
                                                    for p in java if p.startswith(page_prefix)}),
        }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    for name, data in report["repositories"].items():
        print(f"{name}: {data['mainJavaFiles']} Java files, {data['uiTemplates']} templates, "
              f"{data['jsniBodiesLexical']} JSNI bodies, "
              f"{len(data['gwtImportNameCandidatesMissing'])} missing explicit GWT import candidates")


if __name__ == "__main__":
    main()
