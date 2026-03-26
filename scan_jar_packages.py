#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Scan JAR files recursively, extract common class package prefixes, and update
cfr_class_filter.conf [class] rules with global de-duplication.
"""

import argparse
import os
import re
import zipfile
from collections import Counter
from typing import Dict, List, Sequence, Set, Tuple


EXCLUDED_PREFIX_PATTERNS = [
    r"^java\\.",
    r"^javax\\.",
    r"^sun\\.",
    r"^com\\.sun\\.",
    r"^jdk\\.",
    r"^org\\.w3c",
    r"^org\\.xml",
    r"^org\\.omg",
    r"^org\\.ietf",
    r"^org\\.jcp",
    r"^meta\\.",
]


def extract_package_prefixes(jar_path: str) -> Counter:
    """
    Extract two-level package prefixes from class entries in one jar.
    Example: org/example/foo/Bar.class -> org.example
    """
    prefixes = Counter()
    with zipfile.ZipFile(jar_path, "r") as zf:
        for name in zf.namelist():
            if not name.endswith(".class"):
                continue
            dir_path = "/".join(name.split("/")[:-1])
            if not dir_path or dir_path.startswith("META-INF"):
                continue
            parts = dir_path.split("/")
            if len(parts) >= 2:
                prefixes[".".join(parts[:2])] += 1
    return prefixes


def scan_jar_files(directory: str, progress_step: int = 200) -> Counter:
    """Recursively scan all jar files and return package-prefix class counts."""
    prefix_counter = Counter()
    jar_count = 0
    error_count = 0

    print(f"开始递归扫描目录: {directory}")
    for root, _, files in os.walk(directory):
        for file_name in files:
            if not file_name.lower().endswith(".jar"):
                continue
            jar_path = os.path.join(root, file_name)
            jar_count += 1
            if progress_step > 0 and jar_count % progress_step == 0:
                print(f"  已扫描 {jar_count} 个 jar 文件...")
            try:
                prefix_counter.update(extract_package_prefixes(jar_path))
            except Exception:
                error_count += 1

    print("\n扫描完成:")
    print(f"  - 共扫描 {jar_count} 个 jar 文件")
    print(f"  - {error_count} 个文件无法读取")
    print(f"  - 发现 {len(prefix_counter)} 个唯一二级包名前缀")
    print(f"  - 总计 {sum(prefix_counter.values())} 个类文件")
    return prefix_counter


def is_valid_package_prefix(prefix: str) -> bool:
    """Validate dotted package prefix parts."""
    parts = prefix.split(".")
    for part in parts:
        if not re.match(r"^[a-zA-Z_][a-zA-Z0-9_]*$", part):
            return False
    return True


def canonical_rule(rule: str) -> str:
    """Normalize trailing dot for coverage checks."""
    return rule.strip().rstrip(".")


def is_covered_by_broader(specific: str, broader: str) -> bool:
    """
    True when `broader` is same as or broader package of `specific`.
    Example: org.apache covers org.apache.commons.
    """
    return specific == broader or specific.startswith(broader + ".")


def split_config_sections(lines: Sequence[str]) -> Tuple[List[str], List[str]]:
    """Return prefix lines (up to [class]) and existing class rules."""
    class_idx = None
    for i, line in enumerate(lines):
        if line.strip().lower() == "[class]":
            class_idx = i
            break
    if class_idx is None:
        raise ValueError("Missing [class] section in config file.")

    prefix_lines = list(lines[: class_idx + 1])
    class_rules: List[str] = []
    for line in lines[class_idx + 1 :]:
        value = line.strip()
        if not value or value.startswith("#"):
            continue
        class_rules.append(value)
    return prefix_lines, class_rules


def choose_existing_rule_text(existing_forms: Sequence[str], canonical: str) -> str:
    """Keep dotted style if that canonical already has a dotted representation."""
    dotted = [x for x in existing_forms if x.endswith(".")]
    if dotted:
        return dotted[0]
    if existing_forms:
        return existing_forms[0]
    return canonical


def build_candidate_prefixes(
    prefix_counter: Counter,
    min_count: int,
    excluded_prefix_patterns: Sequence[re.Pattern],
) -> List[Tuple[str, int]]:
    candidates: List[Tuple[str, int]] = []
    for prefix, count in prefix_counter.most_common():
        if count < min_count:
            continue
        if not is_valid_package_prefix(prefix):
            continue
        if any(pat.match(prefix) for pat in excluded_prefix_patterns):
            continue
        candidates.append((prefix, count))
    return candidates


def merge_and_minimize_class_rules(
    existing_rules: Sequence[str], candidates: Sequence[Tuple[str, int]]
) -> Tuple[List[str], List[str]]:
    """
    Merge candidates into existing rules, then globally remove precise rules
    that are covered by broader rules.
    """
    existing_by_canonical: Dict[str, List[str]] = {}
    for rule in existing_rules:
        c = canonical_rule(rule)
        existing_by_canonical.setdefault(c, []).append(rule)

    merged_canonical: Set[str] = set(existing_by_canonical.keys())
    added_before_min: List[str] = []
    for prefix, _ in candidates:
        c = canonical_rule(prefix)
        if c in merged_canonical:
            continue
        if any(is_covered_by_broader(c, ex) for ex in merged_canonical):
            continue
        merged_canonical.add(c)
        added_before_min.append(c)

    # Keep broader prefixes first.
    sorted_all = sorted(merged_canonical, key=lambda x: (x.count("."), len(x), x))
    minimal: List[str] = []
    for c in sorted_all:
        if any(is_covered_by_broader(c, broad) for broad in minimal):
            continue
        minimal.append(c)

    final_rules = [
        choose_existing_rule_text(existing_by_canonical.get(c, []), c)
        for c in sorted(minimal)
    ]
    added_kept = [c for c in added_before_min if c in set(minimal)]
    return final_rules, added_kept


def update_class_rules_in_config(config_file: str, class_rules: Sequence[str], encoding: str) -> None:
    with open(config_file, "r", encoding=encoding) as f:
        lines = f.read().splitlines()
    prefix_lines, _ = split_config_sections(lines)
    new_content = "\n".join(prefix_lines + list(class_rules)) + "\n"
    with open(config_file, "w", encoding=encoding) as f:
        f.write(new_content)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scan jar package prefixes and update cfr_class_filter.conf [class] rules."
    )
    parser.add_argument("--scan-dir", required=True, help="Directory to recursively scan for jars.")
    parser.add_argument(
        "--config-file",
        default="cfr_class_filter.conf",
        help="Path to cfr_class_filter.conf (default: ./cfr_class_filter.conf)",
    )
    parser.add_argument(
        "--min-count",
        type=int,
        default=10,
        help="Minimum class count threshold for a scanned prefix to be considered.",
    )
    parser.add_argument(
        "--top",
        type=int,
        default=100,
        help="Show top N scanned prefixes by class count.",
    )
    parser.add_argument(
        "--encoding",
        default="utf-8",
        help="Config file encoding (default: utf-8).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Only print planned changes, do not modify config file.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    compiled_excludes = [re.compile(x, re.IGNORECASE) for x in EXCLUDED_PREFIX_PATTERNS]

    prefix_counter = scan_jar_files(args.scan_dir)

    print("\n" + "=" * 60)
    print(f"所有二级包名前缀 (按类文件数量排序，前{args.top}个):")
    print("=" * 60)
    shown = 0
    for prefix, count in prefix_counter.most_common():
        if not is_valid_package_prefix(prefix):
            continue
        print(f"  {prefix}: {count} 个类")
        shown += 1
        if shown >= args.top:
            break

    with open(args.config_file, "r", encoding=args.encoding) as f:
        config_lines = f.read().splitlines()
    _, existing_class_rules = split_config_sections(config_lines)

    candidates = build_candidate_prefixes(
        prefix_counter=prefix_counter,
        min_count=args.min_count,
        excluded_prefix_patterns=compiled_excludes,
    )
    final_class_rules, added_kept = merge_and_minimize_class_rules(
        existing_rules=existing_class_rules, candidates=candidates
    )

    print("\n" + "=" * 60)
    print("类前缀规则更新摘要:")
    print("=" * 60)
    print(f"  配置中原有[class]规则: {len(existing_class_rules)}")
    print(f"  扫描候选规则(阈值>={args.min_count}): {len(candidates)}")
    print(f"  新增后保留规则: {len(added_kept)}")
    print(f"  最终[class]规则总数: {len(final_class_rules)}")

    if added_kept:
        print("\n新增并保留的规则(前100条):")
        for item in added_kept[:100]:
            print(f"  {item}")
    else:
        print("\n无新增规则（均已存在或被更广泛前缀覆盖）。")

    if args.dry_run:
        print("\nDRY RUN: 未写入配置文件。")
        return

    update_class_rules_in_config(args.config_file, final_class_rules, args.encoding)
    print(f"\n已更新配置文件: {args.config_file}")


if __name__ == "__main__":
    main()
