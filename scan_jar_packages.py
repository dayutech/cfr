#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
递归扫描指定目录下的所有 jar 包，提取常见的目录前缀（包名），
统计方式：统计类文件数量而不是 jar 文件数量
"""

import os
import zipfile
import re
from collections import Counter


def extract_package_prefixes(jar_path):
    """
    从 jar 包中提取二级及以上的包名前缀
    返回一个包含前缀及其类文件数量的字典
    """
    prefixes = Counter()
    try:
        with zipfile.ZipFile(jar_path, 'r') as zf:
            for name in zf.namelist():
                # 只处理 .class 文件
                if name.endswith('.class'):
                    dir_path = '/'.join(name.split('/')[:-1])
                    
                    # 跳过 META-INF 等特殊目录
                    if not dir_path or dir_path.startswith('META-INF'):
                        continue
                    
                    parts = dir_path.split('/')
                    
                    # 只提取二级包名前缀
                    if len(parts) >= 2:
                        prefix = '.'.join(parts[:2])
                        prefixes[prefix] += 1
    except Exception as e:
        pass
    return prefixes


def scan_jar_files(directory):
    """
    递归扫描目录下所有 jar 文件，统计包名前缀对应的类文件数量
    """
    prefix_counter = Counter()
    jar_count = 0
    error_count = 0
    
    print(f"开始递归扫描目录: {directory}")
    
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.lower().endswith('.jar'):
                jar_path = os.path.join(root, file)
                jar_count += 1
                if jar_count % 200 == 0:
                    print(f"  已扫描 {jar_count} 个 jar 文件...")
                
                try:
                    prefixes = extract_package_prefixes(jar_path)
                    for prefix, count in prefixes.items():
                        prefix_counter[prefix] += count
                except Exception as e:
                    error_count += 1
    
    print(f"\n扫描完成:")
    print(f"  - 共扫描 {jar_count} 个 jar 文件")
    print(f"  - {error_count} 个文件无法读取")
    print(f"  - 发现 {len(prefix_counter)} 个唯一二级包名前缀")
    print(f"  - 总计 {sum(prefix_counter.values())} 个类文件")
    
    return prefix_counter


def is_valid_package_prefix(prefix):
    """
    检查是否是有效的包名前缀
    """
    parts = prefix.split('.')
    for part in parts:
        if not re.match(r'^[a-zA-Z_][a-zA-Z0-9_]*$', part):
            return False
    return True


def get_existing_prefixes():
    """
    获取配置文件中已存在的前缀
    """
    return {
        'com.mysql', 'org.tuckey', 'com.lambdaworks', 'org.owasp',
        'net.spy', 'com.lmax', 'org.springframework', 'org.slf4j',
        'ch.qos.logback', 'io.netty', 'io.reactivex',
        'org.junit', 'org.mockito', 'org.hamcrest', 'org.hibernate',
        'com.amazonaws', 'com.azure', 'com.fasterxml', 'com.google', 'com.microsoft',
        'com.thoughtworks.xstream', 'gov.nist', 'io.frinx', 'io.github',
        'io.grpc', 'io.jsonwebtoken', 'io.micrometer', 'io.vertx',
        'net.bytebuddy', 'net.sf', 'org.apache',
        'software.amazon', 'org.joda', 'org.aspectj', 'org.checkerframework',
        'org.activiti', 'org.bouncycastle', 'org.eclipse', 'org.glassfish',
        'org.jboss', 'org.jvnet', 'org.modelmapper', 'org.opendaylight',
        'org.springdoc', 'reactor', 'org.antlr'
    }


def filter_prefixes(prefix_counter, min_count=10):
    """
    过滤出常见的包名前缀
    min_count: 最小类文件数量
    """
    existing_prefixes = get_existing_prefixes()
    
    # 需要排除的前缀模式
    excluded_patterns = [
        r'^java\.', r'^javax\.', r'^sun\.', r'^com\.sun\.',
        r'^jdk\.', r'^org\.w3c', r'^org\.xml', r'^org\.omg',
        r'^org\.ietf', r'^org\.jcp', r'^META'
    ]
    
    # 需要排除的特定前缀（已在配置中）
    excluded_prefixes = {
        'org.apache', 'org.springframework', 'org.slf4j', 'com.fasterxml',
        'io.netty', 'io.reactivex', 'org.hibernate', 'org.bouncycastle',
        'org.eclipse', 'org.jboss', 'org.glassfish', 'org.junit',
        'org.mockito', 'com.google', 'com.microsoft', 'com.amazonaws',
        'com.azure', 'io.grpc', 'io.micrometer', 'io.vertx',
        'net.bytebuddy', 'net.sf', 'org.aspectj', 'org.joda',
        'reactor', 'org.antlr', 'org.checkerframework', 'org.activiti',
        'org.opendaylight', 'org.springdoc', 'org.modelmapper', 'org.jvnet',
        'software.amazon', 'io.github', 'io.frinx', 'gov.nist',
        'com.thoughtworks.xstream', 'io.jsonwebtoken', 'org.owasp',
        'com.lambdaworks', 'org.tuckey', 'com.lmax', 'net.spy',
        'ch.qos.logback', 'org.hamcrest'
    }
    
    filtered = []
    for prefix, count in prefix_counter.most_common():
        # 检查是否是有效的包名
        if not is_valid_package_prefix(prefix):
            continue
        
        # 检查是否已存在
        if prefix in existing_prefixes or prefix in excluded_prefixes:
            continue
        
        # 检查是否是已存在前缀的子前缀
        is_subprefix = False
        for existing in existing_prefixes | excluded_prefixes:
            if prefix.startswith(existing + '.'):
                is_subprefix = True
                break
        if is_subprefix:
            continue
        
        # 检查是否匹配排除模式
        excluded = False
        for pattern in excluded_patterns:
            if re.match(pattern, prefix):
                excluded = True
                break
        if excluded:
            continue
        
        if count >= min_count:
            filtered.append((prefix, count))
    
    return filtered


def optimize_prefixes(filtered_prefixes):
    """
    优化前缀列表，保留最短的有效前缀
    """
    # 按长度排序，短前缀优先
    sorted_prefixes = sorted(filtered_prefixes, key=lambda x: (len(x[0]), -x[1]))
    
    result = []
    for prefix, count in sorted_prefixes:
        # 检查是否被已有前缀覆盖
        is_covered = False
        for existing_prefix, _ in result:
            if prefix.startswith(existing_prefix + '.'):
                is_covered = True
                break
        if not is_covered:
            result.append((prefix, count))
    
    # 按出现次数排序
    result.sort(key=lambda x: -x[1])
    return result


def main():
    scan_dir = r'C:\CustomData\VulDisc'
    config_file = r'c:\CustomData\cfr\cfr_class_filter.conf'
    
    # 扫描 jar 文件
    prefix_counter = scan_jar_files(scan_dir)
    
    # 显示所有前缀（按类文件数量排序）
    print("\n" + "=" * 60)
    print("所有二级包名前缀 (按类文件数量排序，前100个):")
    print("=" * 60)
    for prefix, count in prefix_counter.most_common(100):
        if is_valid_package_prefix(prefix):
            print(f"  {prefix}: {count} 个类")
    
    # 过滤出常见前缀
    filtered_prefixes = filter_prefixes(prefix_counter, min_count=10)
    
    # 优化前缀列表
    optimized_prefixes = optimize_prefixes(filtered_prefixes)
    
    print("\n" + "=" * 60)
    print("优化后的包名前缀 (需要添加到配置文件):")
    print("=" * 60)
    for prefix, count in optimized_prefixes:
        print(f"  {prefix}: {count} 个类")
    
    # 读取现有配置文件
    with open(config_file, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 找到 [class] 节的位置
    class_section_match = re.search(r'\[class\](.*?)(?=\[|$)', content, re.DOTALL)
    if class_section_match:
        existing_classes = set()
        for line in class_section_match.group(1).strip().split('\n'):
            line = line.strip()
            if line and not line.startswith('#'):
                existing_classes.add(line)
        
        # 找出需要添加的新规则
        new_filters = []
        for prefix, count in optimized_prefixes:
            class_filter = prefix + '.'
            if class_filter not in existing_classes and prefix not in existing_classes:
                new_filters.append(class_filter)
        
        print(f"\n需要添加的新类名过滤规则: {len(new_filters)} 条")
        
        # 将新规则写入文件
        if new_filters:
            new_content = content.rstrip()
            if not new_content.endswith('\n'):
                new_content += '\n'
            
            for nf in sorted(new_filters):
                new_content += nf + '\n'
            
            with open(config_file, 'w', encoding='utf-8') as f:
                f.write(new_content)
            
            print(f"已将 {len(new_filters)} 条新规则添加到配置文件")
        else:
            print("没有需要添加的新规则")


if __name__ == '__main__':
    main()
