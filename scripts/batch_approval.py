"""
批量修改作品审核状态脚本
=========================
接口: POST /api/admin/works/update/approval-status
权限: 需要角色 55(审核员) 或 77(系统管理员)
审核状态: 10-正常  20-待审核  30-未过审

两种使用方式:
  1. 交互模式（直接运行，一步步输入参数）:
       python batch_approval.py

  2. 命令行模式:
       python batch_approval.py --ids 2,7 --status 30
       python batch_approval.py --ids 1,3,5 --status 10 --token eyJ...
"""

import os
import sys
import argparse
import time
import requests

# ===================== 配置 =====================
BASE_URL = os.environ.get("PIXVISION_API_URL", "http://localhost:9090")
LOGIN_URL = f"{BASE_URL}/api/auth/login"
APPROVAL_URL = f"{BASE_URL}/api/admin/works/update/approval-status"
TIMEOUT = 30
REQUEST_DELAY = 0.3

STATUS_MAP = {
    "10": "正常",
    "20": "待审核",
    "30": "未过审",
}
# ================================================


def load_token_from_file():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    token_file = os.path.join(script_dir, "token.txt")
    if not os.path.exists(token_file):
        return None
    token = open(token_file, "r", encoding="utf-8").read().strip()
    return token if token else None


def login_and_get_token():
    print("\n🔐 登录获取 Token")
    username = input("  用户名或邮箱: ").strip()
    password = input("  密码: ").strip()
    vcode = input("  邮箱验证码(6位): ").strip()

    if not username or not password or not vcode:
        print("❌ 登录信息不完整")
        return None

    try:
        resp = requests.post(
            LOGIN_URL,
            data={"usernameOrEmail": username, "password": password, "vCode": vcode},
            timeout=TIMEOUT,
        )
        data = resp.json()
        if data.get("data") and data["data"].get("token"):
            token = data["data"]["token"]
            script_dir = os.path.dirname(os.path.abspath(__file__))
            token_file = os.path.join(script_dir, "token.txt")
            with open(token_file, "w", encoding="utf-8") as f:
                f.write(token)
            print("✅ 登录成功，Token 已保存到 token.txt")
            return token
        else:
            print(f"❌ 登录失败: {data.get('message', resp.text)}")
            return None
    except requests.RequestException as e:
        print(f"❌ 网络错误: {e}")
        return None


def parse_id_ranges(id_input):
    """
    解析 ID 输入字符串。
      "2,7"        → 2 到 7
      "2,7,10,20"  → 2-7 + 10-20
      "1,3,5"      → 1, 3, 5 单独
      "2,7,99"     → 2-7 + 99
      "5"          → 5
    """
    parts = [x.strip() for x in id_input.split(",") if x.strip()]
    if not parts:
        return []
    for p in parts:
        if not p.lstrip("-").isdigit():
            return []
    nums = [int(p) for p in parts]
    ids = set()
    i = 0
    while i < len(nums):
        if i + 1 < len(nums) and nums[i] < nums[i + 1]:
            ids.update(range(nums[i], nums[i + 1] + 1))
            i += 2
        else:
            ids.add(nums[i])
            i += 1
    return sorted(ids)


def batch_update_approval(token, work_ids, approval_status):
    headers = {"Authorization": f"Bearer {token}"}
    params = [("workIds", str(wid)) for wid in work_ids]
    params.append(("approvalStatus", str(approval_status)))
    try:
        return requests.post(APPROVAL_URL, headers=headers, params=params, timeout=TIMEOUT)
    except requests.RequestException as e:
        print(f"  ⚠️ 网络错误: {e}")
        return None


def do_execute(token, work_ids, approval_status):
    """执行批量更新"""
    status_name = STATUS_MAP.get(str(approval_status), str(approval_status))

    # 预览
    print(f"\n{'=' * 50}")
    print(f"  接口: POST /api/admin/works/update/approval-status")
    print(f"  服务器: {BASE_URL}")
    print(f"  审核状态: {approval_status} ({status_name})")
    print(f"  作品数量: {len(work_ids)}")
    print(f"  ID 列表: {work_ids}")
    print(f"{'=' * 50}")

    confirm = input(f"\n⚠️  确认批量修改 {len(work_ids)} 个作品为「{status_name}」? [y/N]: ")
    if confirm.lower() not in ("y", "yes"):
        print("已取消")
        return

    print(f"\n🚀 开始批量更新...\n")
    success_count = 0
    fail_count = 0

    for idx, wid in enumerate(work_ids, 1):
        pct = idx / len(work_ids) * 100
        print(f"[{idx:>4}/{len(work_ids)}] ({pct:5.1f}%) ID={wid} ... ", end="", flush=True)

        resp = batch_update_approval(token, [wid], approval_status)

        if resp is None:
            print("❌ 请求失败")
            fail_count += 1
        elif resp.status_code == 200:
            data = resp.json()
            if data.get("data") is not None:
                print("✅ 成功")
                success_count += 1
            else:
                print(f"⚠️ 业务失败: {data.get('message', '')}")
                fail_count += 1
        elif resp.status_code == 401:
            print("❌ Token 无效或过期 (401)")
            fail_count += 1
        else:
            print(f"❌ HTTP {resp.status_code}: {resp.text[:80]}")
            fail_count += 1

        if idx < len(work_ids):
            time.sleep(REQUEST_DELAY)

    print(f"\n{'=' * 50}")
    print(f"  总计: {len(work_ids)}  成功: {success_count}  失败: {fail_count}")
    print(f"{'=' * 50}")


# ===================== 交互模式 =====================

def interactive_mode():
    """交互式一步步输入参数"""
    print("=" * 50)
    print("  批量修改作品审核状态")
    print("=" * 50)

    # 1. 输入 ID
    while True:
        print("\n📌 输入作品 ID（支持范围语法）")
        print("   示例: 2,7        → 2 到 7")
        print("   示例: 2,7,10,20  → 范围 2-7 + 范围 10-20")
        print("   示例: 1,3,5      → 单独 ID")
        id_input = input("  ID: ").strip()
        work_ids = parse_id_ranges(id_input)
        if work_ids:
            break
        print("  ❌ 格式错误，请重新输入")

    # 2. 输入状态
    while True:
        print(f"\n📌 选择审核状态")
        for k, v in STATUS_MAP.items():
            print(f"   {k} - {v}")
        status_input = input("  状态: ").strip()
        if status_input in STATUS_MAP:
            approval_status = int(status_input)
            break
        print("  ❌ 无效状态，请输入 10 / 20 / 30")

    # 3. 获取 Token
    token = None
    existing_token = load_token_from_file()

    if existing_token:
        print(f"\n📌 检测到已有 token.txt")
        print("   1 - 使用已有 Token（推荐）")
        print("   2 - 重新登录获取 Token")
        print("   3 - 手动粘贴 Token")
        choice = input("  选择 [1/2/3] (默认1): ").strip() or "1"

        if choice == "1":
            token = existing_token
            token_source = "token.txt"
        elif choice == "2":
            token = login_and_get_token()
            token_source = "登录获取"
        elif choice == "3":
            token = input("  请粘贴 Token: ").strip()
            token_source = "手动输入"
        else:
            print("❌ 无效选择")
            return
    else:
        print(f"\n📌 未找到 token.txt，选择获取方式")
        print("   1 - 通过登录接口获取 Token")
        print("   2 - 手动粘贴 Token")
        choice = input("  选择 [1/2] (默认1): ").strip() or "1"

        if choice == "1":
            token = login_and_get_token()
            token_source = "登录获取"
        elif choice == "2":
            token = input("  请粘贴 Token: ").strip()
            token_source = "手动输入"
        else:
            print("❌ 无效选择")
            return

    if not token:
        print("❌ Token 获取失败")
        return

    print(f"✅ Token 来源: {token_source}")

    # 4. 执行
    do_execute(token, work_ids, approval_status)


# ===================== 命令行模式 =====================

def cli_mode():
    parser = argparse.ArgumentParser(description="批量修改作品审核状态")
    parser.add_argument("--ids", "-i", default=None,
                        help='作品 ID，如 "2,7"=2到7，"2,7,10,20"=范围混合')
    parser.add_argument("--status", "-s", type=int, choices=[10, 20, 30], default=None,
                        help="审核状态: 10=正常  20=待审核  30=未过审")
    parser.add_argument("--token", "-t", default=None, help="JWT Token")
    parser.add_argument("--login", "-l", action="store_true", help="登录获取 Token")
    parser.add_argument("--dry-run", "-n", action="store_true", help="仅预览")

    args = parser.parse_args()

    # 没有任何参数 → 进入交互模式
    has_cli_args = any([
        args.ids, args.status, args.token, args.login, args.dry_run,
    ])
    if not has_cli_args:
        interactive_mode()
        return

    # 命令行模式
    if not args.ids:
        print("❌ 缺少 --ids 参数")
        sys.exit(1)
    if not args.status:
        print("❌ 缺少 --status 参数")
        sys.exit(1)

    work_ids = parse_id_ranges(args.ids)
    if not work_ids:
        print("❌ 未能解析出有效的作品 ID")
        sys.exit(1)

    if args.dry_run:
        status_name = STATUS_MAP.get(str(args.status), str(args.status))
        print(f"\n{'=' * 50}")
        print(f"  审核状态: {args.status} ({status_name})")
        print(f"  作品数量: {len(work_ids)}")
        print(f"  ID 列表: {work_ids}")
        print(f"{'=' * 50}")
        print("\n🔍 [DRY RUN] 仅预览，不执行实际请求。")
        return

    token = None
    if args.login:
        token = login_and_get_token()
    elif args.token:
        token = args.token
        print("✅ 使用命令行传入的 Token")
    else:
        token = load_token_from_file()

    if not token:
        print("\n💡 提示: 使用 --login 或 --token 提供 Token，或不带任何参数进入交互模式")
        sys.exit(1)

    do_execute(token, work_ids, args.status)


def main():
    cli_mode()


if __name__ == "__main__":
    main()
