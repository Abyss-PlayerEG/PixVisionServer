import os
import sys
import requests

API_URL = "http://localhost:9090/api/image/work/upload"
SUPPORTED_EXTS = {".jpg", ".jpeg", ".png"}

def load_token():
    token_file = os.path.join(os.path.dirname(os.path.abspath(__file__)), "token.txt")
    if not os.path.exists(token_file):
        print("错误: 脚本目录下未找到 token.txt")
        sys.exit(1)
    with open(token_file, "r", encoding="utf-8") as f:
        return f.read().strip()

def load_title_template(image_dir):
    title_file = os.path.join(image_dir, "title.txt")
    if not os.path.exists(title_file):
        print(f"错误: {image_dir} 下未找到 title.txt")
        sys.exit(1)
    with open(title_file, "r", encoding="utf-8") as f:
        return f.read().strip()

def get_images(image_dir):
    images = []
    for f in sorted(os.listdir(image_dir)):
        full = os.path.join(image_dir, f)
        if os.path.isfile(full) and os.path.splitext(f)[1].lower() in SUPPORTED_EXTS:
            images.append(full)
    return images

def upload(token, image_path, work_title, series_id, is_original, out_url):
    headers = {"Authorization": f"Bearer {token}"}
    ext = os.path.splitext(image_path)[1].lower()
    content_type = "image/png" if ext == ".png" else "image/jpeg"

    with open(image_path, "rb") as f:
        files = {"file": (os.path.basename(image_path), f, content_type)}
        data = {
            "workTitle": work_title,
            "seriesId": str(series_id),
            "isOriginal": str(is_original).lower(),
        }
        if out_url:
            data["outUrl"] = out_url
        return requests.post(API_URL, headers=headers, files=files, data=data)

def main():
    token = load_token()
    print("Token 已加载")

    image_dir = input("请输入图片文件夹路径: ").strip().strip('"').strip("'")
    if not os.path.isdir(image_dir):
        print(f"错误: 目录不存在 -> {image_dir}")
        sys.exit(1)

    title_template = load_title_template(image_dir)
    images = get_images(image_dir)
    if not images:
        print("未找到图片文件 (仅支持 jpg/jpeg/png)")
        sys.exit(1)
    print(f"找到 {len(images)} 张图片")

    series_id = input("请输入 seriesId (0=不属于任何系列): ").strip()
    series_id = int(series_id) if series_id.isdigit() else 0

    is_orig = input("是否原创? (true/false): ").strip().lower()
    is_original = is_orig == "true"

    out_url = ""
    if not is_original:
        out_url = input("请输入外部转载链接 (outUrl): ").strip()

    print(f"\n开始上传... 标题模板: {title_template}\n")

    for idx, img_path in enumerate(images, 1):
        work_title = title_template.replace("<index>", str(idx))
        fname = os.path.basename(img_path)
        print(f"[{idx}/{len(images)}] {fname} -> {work_title}", end=" ")

        try:
            resp = upload(token, img_path, work_title, series_id, is_original, out_url)
            if resp.status_code == 200:
                j = resp.json()
                if j.get("data") is not None:
                    print(f"成功 (workId={j['data']})")
                else:
                    print(f"失败: {j.get('msg', '')}")
            else:
                print(f"HTTP {resp.status_code}: {resp.text[:100]}")
        except Exception as e:
            print(f"异常: {e}")

    print("\n全部上传完成")

if __name__ == "__main__":
    main()
