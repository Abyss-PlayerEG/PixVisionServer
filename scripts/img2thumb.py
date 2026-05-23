#!/usr/bin/env python3
"""
img_url → thumb_url 数据库批量回填脚本

连接 MySQL，读取 tb_works 表中所有已有 img_url 但 thumb_url 为空的记录，
将 img_url 的扩展名 (.png/.jpg/.jpeg) 替换为 _thumb.webp 后写回 thumb_url 字段。

依赖: pip install pymysql

用法:
  python3 img2thumb.py            # 默认连接 localhost
  python3 img2thumb.py --dry-run  # 仅预览，不写入
"""

import re
import sys

import mysql.connector

# ---------- 数据库配置 ----------
DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "db_pix_vision",
    "charset": "utf8mb4",
}

# ---------- 转换逻辑 ----------

THUMB_RE = re.compile(r"\.(png|jpg|jpeg)$", re.IGNORECASE)


def img_to_thumb(img_url: str) -> str:
    """从 img_url 派生 thumb_url"""
    return THUMB_RE.sub("_thumb.webp", img_url)


# ---------- 主流程 ----------

def main():
    dry_run = "--dry-run" in sys.argv

    try:
        conn = mysql.connector.connect(**DB_CONFIG)
    except mysql.connector.Error as e:
        print(f"数据库连接失败: {e}", file=sys.stderr)
        sys.exit(1)

    cursor = conn.cursor()

    # 查询所有 thumb_url 为空但有 img_url 的记录
    cursor.execute(
        "SELECT work_id, img_url FROM tb_works "
        "WHERE img_url IS NOT NULL AND img_url != '' "
        "AND (thumb_url IS NULL OR thumb_url = '')"
    )
    rows = cursor.fetchall()

    if not rows:
        print("没有需要回填的记录")
        cursor.close()
        conn.close()
        return

    print(f"找到 {len(rows)} 条需回填的记录" + (" (预览模式)" if dry_run else ""))

    updated = 0
    skipped = 0

    for work_id, img_url in rows:
        thumb_url = img_to_thumb(img_url)

        if dry_run:
            print(f"  work_id={work_id:>6}  {img_url}  ->  {thumb_url}")
            updated += 1
            continue

        try:
            cursor.execute(
                "UPDATE tb_works SET thumb_url = %s WHERE work_id = %s",
                (thumb_url, work_id),
            )
            if cursor.rowcount > 0:
                updated += 1
                print(f"  [OK] work_id={work_id:>6}  {img_url}  ->  {thumb_url}")
            else:
                skipped += 1
                print(f"  [跳过] work_id={work_id:>6}  未匹配到行")
        except mysql.connector.Error as e:
            skipped += 1
            print(f"  [失败] work_id={work_id:>6}  {e}", file=sys.stderr)

    if not dry_run:
        conn.commit()

    cursor.close()
    conn.close()

    print(f"\n完成: 更新 {updated} 条, 跳过 {skipped} 条")


if __name__ == "__main__":
    main()
