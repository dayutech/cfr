#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
递归扫描 JAR：
- 提取 class 包名前缀，更新 [class]
- 提取 jar 文件名前缀，更新 [jar]

特性：
- 自动聚合相似前缀，只保留最大覆盖范围
- 排除常见域名前缀（org/com/net/io 等）作为聚合结果
- 仅输出实际写入配置文件的新规则
"""

import argparse
import os
import re
import zipfile
from collections import Counter, defaultdict
from typing import Dict, Iterable, List, Sequence, Set, Tuple


EXCLUDED_CLASS_PREFIX_PATTERNS = [
    r"^java\.",
    r"^javax\.",
    r"^sun\.",
    r"^com\.sun\.",
    r"^jdk\.",
    r"^org\.w3c",
    r"^org\.xml",
    r"^org\.omg",
    r"^org\.ietf",
    r"^org\.jcp",
    r"^meta\.",
]

COMMON_DOMAIN_ROOTS = {
    "org", "com", "net", "io", "edu", "gov",
    "cn", "uk", "de", "jp", "fr", "ru", "au", "us", "int",
}


def extract_class_package_prefixes(jar_path: str) -> Counter:
    """从 class 路径提取二级包名前缀：org/example/Foo.class -> org.example"""
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


def normalize_jar_prefix_from_filename(file_name: str) -> str:
    """由文件名提取 jar 规则前缀（去 .jar、去尾部版本号）。"""
    name = file_name.strip().lower()
    if name.endswith(".jar"):
        name = name[:-4]

    # spring-core-5.3.31 -> spring-core
    # netty-all-4.1.108.final -> netty-all
    m = re.match(r"^(.*?)(?:[-_.]v?\d[0-9a-z._-]*)$", name)
    if m and m.group(1):
        return m.group(1).rstrip("-_.")
    return name


def is_valid_package_prefix(prefix: str) -> bool:
    parts = prefix.split(".")
    for part in parts:
        if not re.match(r"^[a-zA-Z_][a-zA-Z0-9_]*$", part):
            return False
    return True


def is_valid_jar_prefix(prefix: str) -> bool:
    if not prefix:
        return False
    for c in prefix:
        if not (c.isalnum() or c in "-_."):
            return False
    return True


def _count_total_jars(directory: str) -> int:
    total = 0
    for root, _, files in os.walk(directory):
        for file_name in files:
            if file_name.lower().endswith(".jar"):
                total += 1
    return total


def scan_jar_files(directory: str) -> Tuple[Counter, Counter]:
    class_prefix_counter = Counter()
    jar_prefix_counter = Counter()
    total_jars = _count_total_jars(directory)
    progress_step = 10 if total_jars <= 200 else 200
    scanned = 0

    print(f"开始递归扫描目录: {directory}")
    print(f"待扫描 JAR 数量: {total_jars}")

    for root, _, files in os.walk(directory):
        for file_name in files:
            if not file_name.lower().endswith(".jar"):
                continue
            scanned += 1

            jar_prefix = normalize_jar_prefix_from_filename(file_name)
            if is_valid_jar_prefix(jar_prefix):
                jar_prefix_counter[jar_prefix] += 1

            jar_path = os.path.join(root, file_name)
            try:
                class_prefix_counter.update(extract_class_package_prefixes(jar_path))
            except Exception:
                # 损坏包或权限问题直接跳过
                pass

            if scanned % progress_step == 0 or scanned == total_jars:
                print(f"已扫描 {scanned}/{total_jars} 个 JAR")

    print("扫描完成")

    return class_prefix_counter, jar_prefix_counter


def canonical_class_rule(rule: str) -> str:
    return rule.strip().rstrip(".")


def class_rule_covers(specific: str, broader: str) -> bool:
    return specific == broader or specific.startswith(broader + ".")


def canonical_jar_rule(rule: str) -> str:
    return rule.strip().lower()


def jar_rule_covers(specific: str, broader: str) -> bool:
    # 与 ClassFilter.matchesJarPrefix 保持一致
    return (
        specific == broader
        or specific.startswith(broader + "-")
        or specific.startswith(broader + "_")
        or specific.startswith(broader + ".")
    )


def parse_config_sections(lines: Sequence[str]) -> Tuple[List[str], List[str], List[str]]:
    jar_idx = None
    class_idx = None
    for i, line in enumerate(lines):
        v = line.strip().lower()
        if v == "[jar]" and jar_idx is None:
            jar_idx = i
        elif v == "[class]" and class_idx is None:
            class_idx = i

    if jar_idx is None or class_idx is None or class_idx <= jar_idx:
        raise ValueError("Config must contain [jar] and [class] sections in order")

    header = [x for x in lines[:jar_idx] if x.strip()]

    jar_rules: List[str] = []
    for line in lines[jar_idx + 1 : class_idx]:
        v = line.strip()
        if not v or v.startswith("#"):
            continue
        jar_rules.append(v)

    class_rules: List[str] = []
    for line in lines[class_idx + 1 :]:
        v = line.strip()
        if not v or v.startswith("#"):
            continue
        class_rules.append(v)

    return header, jar_rules, class_rules


def choose_existing_class_text(existing_forms: Sequence[str], canonical: str) -> str:
    # 统一格式：class 规则全部使用 trailing '.'，明确包边界匹配语义。
    return canonical + "."


def choose_existing_jar_text(existing_forms: Sequence[str], canonical: str) -> str:
    if existing_forms:
        return existing_forms[0]
    return canonical


def build_class_candidates(
    class_counter: Counter,
    class_min_count: int,
    excluded_patterns: Sequence[re.Pattern],
    class_copt: int,
) -> Dict[str, int]:
    candidates: Dict[str, int] = {}
    picked = 0
    for prefix, count in class_counter.most_common():
        if count < class_min_count:
            continue
        if not is_valid_package_prefix(prefix):
            continue
        if any(pat.match(prefix) for pat in excluded_patterns):
            continue
        candidates[canonical_class_rule(prefix)] = count
        picked += 1
        if class_copt > 0 and picked >= class_copt:
            break
    return candidates


def build_jar_candidates(jar_counter: Counter, jar_min_count: int, jar_top: int) -> Dict[str, int]:
    candidates: Dict[str, int] = {}
    picked = 0
    for prefix, count in jar_counter.most_common():
        if count < jar_min_count:
            continue
        if not is_valid_jar_prefix(prefix):
            continue
        candidates[canonical_jar_rule(prefix)] = count
        picked += 1
        if jar_top > 0 and picked >= jar_top:
            break
    return candidates


def aggregate_class_candidates(raw: Dict[str, int]) -> Dict[str, int]:
    """
    对 class 候选做公共前缀聚合：
    - brave.baggage + brave.handler -> brave
    - 但 org/com/net/io 等顶层前缀不作为聚合结果
    """
    if not raw:
        return {}

    agg = Counter(raw)
    child_branches: Dict[str, Set[str]] = defaultdict(set)

    for p, c in raw.items():
        parts = p.split(".")
        for i in range(1, len(parts)):
            parent = ".".join(parts[:i])
            child_branches[parent].add(parts[i])
            agg[parent] += c

    selected: List[str] = []
    for p in sorted(agg.keys(), key=lambda x: (x.count("."), -agg[x], x)):
        segs = p.split(".")
        if len(segs) == 1 and segs[0].lower() in COMMON_DOMAIN_ROOTS:
            continue
        if any(class_rule_covers(p, s) for s in selected):
            continue

        keep = (p in raw) or (len(child_branches.get(p, set())) >= 2)
        if keep:
            selected.append(p)

    return {p: int(agg[p]) for p in selected}


def _jar_parent_once(prefix: str) -> Tuple[str, str]:
    idx = max(prefix.rfind("-"), prefix.rfind("_"), prefix.rfind("."))
    if idx <= 0:
        return "", ""
    return prefix[:idx], prefix[idx + 1 :]


def aggregate_jar_candidates(raw: Dict[str, int]) -> Dict[str, int]:
    """
    对 jar 候选做公共前缀聚合：
    - brave-handler + brave-internal -> brave
    - org/com/net/io 等顶层前缀不作为聚合结果
    """
    if not raw:
        return {}

    agg = Counter(raw)
    child_branches: Dict[str, Set[str]] = defaultdict(set)

    for p, c in raw.items():
        cur = p
        while True:
            parent, child = _jar_parent_once(cur)
            if not parent:
                break
            child_branches[parent].add(child)
            agg[parent] += c
            cur = parent

    selected: List[str] = []
    for p in sorted(agg.keys(), key=lambda x: (len(re.split(r"[-_.]", x)), -agg[x], x)):
        if p in COMMON_DOMAIN_ROOTS:
            continue
        if any(jar_rule_covers(p, s) for s in selected):
            continue

        keep = (p in raw) or (len(child_branches.get(p, set())) >= 2)
        if keep:
            selected.append(p)

    return {p: int(agg[p]) for p in selected}


def merge_and_minimize_class_rules(existing_rules: Sequence[str], candidates: Dict[str, int]) -> List[str]:
    existing_by_canonical: Dict[str, List[str]] = {}
    for rule in existing_rules:
        c = canonical_class_rule(rule)
        existing_by_canonical.setdefault(c, []).append(rule)

    merged: Set[str] = set(existing_by_canonical.keys()) | set(candidates.keys())

    selected: List[str] = []
    for c in sorted(merged, key=lambda x: (x.count("."), -candidates.get(x, 0), x)):
        segs = c.split(".")
        if len(segs) == 1 and segs[0].lower() in COMMON_DOMAIN_ROOTS:
            continue
        if any(class_rule_covers(c, b) for b in selected):
            continue
        selected.append(c)

    return [
        choose_existing_class_text(existing_by_canonical.get(c, []), c)
        for c in sorted(selected)
    ]


def merge_and_minimize_jar_rules(existing_rules: Sequence[str], candidates: Dict[str, int]) -> List[str]:
    existing_by_canonical: Dict[str, List[str]] = {}
    for rule in existing_rules:
        c = canonical_jar_rule(rule)
        existing_by_canonical.setdefault(c, []).append(rule)

    merged: Set[str] = set(existing_by_canonical.keys()) | set(candidates.keys())

    selected: List[str] = []
    for c in sorted(merged, key=lambda x: (len(re.split(r"[-_.]", x)), -candidates.get(x, 0), x)):
        if c in COMMON_DOMAIN_ROOTS:
            continue
        if any(jar_rule_covers(c, b) for b in selected):
            continue
        selected.append(c)

    return [
        choose_existing_jar_text(existing_by_canonical.get(c, []), c)
        for c in sorted(selected)
    ]


def write_config(
    config_file: str,
    header_lines: Sequence[str],
    jar_rules: Sequence[str],
    class_rules: Sequence[str],
    encoding: str,
) -> None:
    out: List[str] = []
    out.extend([x for x in header_lines if x.strip()])
    out.append("[jar]")
    out.extend(jar_rules)
    out.append("[class]")
    out.extend(class_rules)

    with open(config_file, "w", encoding=encoding) as f:
        f.write("\n".join(out) + "\n")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scan jars and update both [jar]/[class] rules with aggregation.",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter,
    )
    parser.add_argument("--scan-dir", required=True, help="递归扫描的根目录")
    parser.add_argument("--config-file", default="cfr_class_filter.conf", help="过滤规则配置文件路径")

    # 用户要求重命名
    parser.add_argument(
        "--class-min-count",
        "--min-count",
        dest="class_min_count",
        type=int,
        default=0,
        help="类名前缀最小出现次数阈值（小于该值不纳入候选）",
    )
    parser.add_argument(
        "--class-top",
        "--class-copt",
        "--top",
        dest="class_top",
        type=int,
        default=100,
        help="参与聚合的 class 候选前缀最大数量（按频次排序后截断）",
    )

    parser.add_argument(
        "--jar-min-count",
        type=int,
        default=2,
        help="JAR 包名前缀最小出现次数阈值（小于该值不纳入候选）",
    )
    parser.add_argument(
        "--jar-top",
        type=int,
        default=100,
        help="参与聚合的 jar 候选前缀最大数量（按频次排序后截断）",
    )
    parser.add_argument("--encoding", default="utf-8", help="配置文件读写编码")
    parser.add_argument("--yes", action="store_true", help="跳过交互确认，直接写入新增规则")
    parser.add_argument("--dry-run", action="store_true", help="只显示拟新增规则，不写入配置文件")
    return parser.parse_args()


def _parse_exclusion_input(text: str, is_class: bool) -> Set[str]:
    """
    解析逗号/空白分隔输入。
    class 模式会去除末尾 '.' 再 canonical 化。
    """
    if not text.strip():
        return set()
    parts = re.split(r"[,\s]+", text.strip())
    res: Set[str] = set()
    for item in parts:
        if not item:
            continue
        x = item.strip()
        if is_class:
            res.add(canonical_class_rule(x))
        else:
            res.add(canonical_jar_rule(x))
    return res


def _display_pending_additions(added_jar: Sequence[str], added_class: Sequence[str]) -> None:
    print("拟新增 [jar] 规则:")
    if not added_jar:
        print("  (无)")
    else:
        for r in added_jar:
            print(f"  {r}")

    print("拟新增 [class] 规则:")
    if not added_class:
        print("  (无)")
    else:
        for r in added_class:
            print(f"  {r}.")


def _resolve_exclusion(text: str, is_class: bool, all_candidates: Sequence[str]) -> Set[str]:
    t = text.strip()
    if not t:
        # 留空表示不删除
        return set()
    if t.upper() == "ALL":
        return {canonical_class_rule(x) if is_class else canonical_jar_rule(x) for x in all_candidates}
    return _parse_exclusion_input(t, is_class=is_class)


def main() -> None:
    args = parse_args()

    class_counter, jar_counter = scan_jar_files(args.scan_dir)

    with open(args.config_file, "r", encoding=args.encoding) as f:
        config_lines = f.read().splitlines()

    header_lines, existing_jar_rules, existing_class_rules = parse_config_sections(config_lines)

    class_excludes = [re.compile(x, re.IGNORECASE) for x in EXCLUDED_CLASS_PREFIX_PATTERNS]
    class_raw = build_class_candidates(class_counter, args.class_min_count, class_excludes, args.class_top)
    jar_raw = build_jar_candidates(jar_counter, args.jar_min_count, args.jar_top)

    class_agg = aggregate_class_candidates(class_raw)
    jar_agg = aggregate_jar_candidates(jar_raw)

    final_class_rules = merge_and_minimize_class_rules(existing_class_rules, class_agg)
    final_jar_rules = merge_and_minimize_jar_rules(existing_jar_rules, jar_agg)

    old_class_set = {canonical_class_rule(x) for x in existing_class_rules}
    old_jar_set = {canonical_jar_rule(x) for x in existing_jar_rules}
    new_class_set = {canonical_class_rule(x) for x in final_class_rules}
    new_jar_set = {canonical_jar_rule(x) for x in final_jar_rules}

    added_class = sorted(new_class_set - old_class_set)
    added_jar = sorted(new_jar_set - old_jar_set)

    if not added_jar and not added_class:
        print("扫描完成，没有发现新增规则。")
        return

    if args.dry_run:
        # dry-run 输出将写入规则（不落盘）
        _display_pending_additions(added_jar, added_class)
        for r in added_jar:
            print(f"[jar] {r}")
        for r in added_class:
            print(f"[class] {r}.")
        return

    # 默认交互确认：先确认是否整体写入
    if not args.yes:
        _display_pending_additions(added_jar, added_class)
        try:
            ans = input(
                f"检测到新增规则: [jar]={len(added_jar)}, [class]={len(added_class)}。是否写入? [y/N]: "
            ).strip().lower()
        except EOFError:
            ans = ""

        if ans not in ("y", "yes"):
            # 按用户要求：否认后允许输入“哪些应删除不添加”
            try:
                jar_ex_text = input("请输入不添加的 [jar] 规则（逗号/空格分隔；留空=都保留，ALL=全部删除）: ")
                class_ex_text = input("请输入不添加的 [class] 规则（逗号/空格分隔；留空=都保留，ALL=全部删除）: ")
            except EOFError:
                jar_ex_text = ""
                class_ex_text = ""

            jar_ex = _resolve_exclusion(jar_ex_text, is_class=False, all_candidates=added_jar)
            class_ex = _resolve_exclusion(class_ex_text, is_class=True, all_candidates=added_class)

            jar_agg = {k: v for k, v in jar_agg.items() if k not in jar_ex}
            class_agg = {k: v for k, v in class_agg.items() if k not in class_ex}

            final_class_rules = merge_and_minimize_class_rules(existing_class_rules, class_agg)
            final_jar_rules = merge_and_minimize_jar_rules(existing_jar_rules, jar_agg)

            new_class_set = {canonical_class_rule(x) for x in final_class_rules}
            new_jar_set = {canonical_jar_rule(x) for x in final_jar_rules}
            added_class = sorted(new_class_set - old_class_set)
            added_jar = sorted(new_jar_set - old_jar_set)

            if not added_jar and not added_class:
                print("过滤后没有可写入新增规则，已取消写入。")
                return

    write_config(
        config_file=args.config_file,
        header_lines=header_lines,
        jar_rules=final_jar_rules,
        class_rules=final_class_rules,
        encoding=args.encoding,
    )

    # 仅输出被写入的规则
    for r in added_jar:
        print(f"[jar] {r}")
    for r in added_class:
        print(f"[class] {r}.")


if __name__ == "__main__":
    main()
