"""
用户生成器

运行该脚本会生成n个测试用户，并插入数据库中。
"""
import mysql.connector
import hashlib
import random
import string

DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'db_pix_vision'
}

ADJECTIVES = ['happy', 'sunny', 'gentle', 'brave', 'clever', 'kind', 'quick', 'bright', 'warm', 'cool', 'lovely', 'peaceful', 'friendly', 'cute', 'sweet']
NOUNS = ['cat', 'dog', 'bird', 'fish', 'fox', 'bear', 'panda', 'rabbit', 'lion', 'tiger', 'eagle', 'owl', 'wolf', 'deer', 'moon', 'star', 'sun', 'cloud', 'rain', 'wind']

def passwd_to_hash256(password):
    temp_str = hashlib.sha1(password.encode()).hexdigest()
    for _ in range(5):
        temp_str = encrypt_string(temp_str)
        temp_str = number2str(temp_str)
    temp_str = encrypt_string(temp_str)
    res_str = hashlib.sha256(temp_str.encode()).hexdigest()
    return res_str

def encrypt_string(s):
    result = []
    for i, c in enumerate(s):
        if c.isdigit():
            result.append(chr((int(c) + 5) % 10 + ord('0')))
        elif c.isalpha():
            ascii_val = ord(c.lower())
            result.append(chr((ascii_val - ord('a') + 3) % 26 + ord('a')))
        else:
            result.append(c)
    return ''.join(result)

def number2str(s):
    num = sum(ord(c) for c in s if c.isdigit())
    return str(num % 1000000) + s

def generate_nickname():
    adj = random.choice(ADJECTIVES)
    noun = random.choice(NOUNS)
    num = random.randint(10, 99)
    return f"{adj.capitalize()}{noun.capitalize()}{num}"

def generate_uuid_hex():
    return ''.join(random.choices(string.hexdigits.lower(), k=32))

def generate_avatar():
    num = random.randint(1, 21)
    return f"default/{num}.png"

def generate_users(count):
    users = []
    used_usernames = set()
    used_emails = set()

    while len(users) < count:
        username = f"user_{random.randint(10**8, 10**9-1)}"
        if username in used_usernames:
            continue
        used_usernames.add(username)

        nickname = generate_nickname()
        email = f"{username}@example.com"
        if email in used_emails:
            continue
        used_emails.add(email)

        hashed_password = "a540d25f4997f6cf504647ccc20d870bdd9562cb01ca86bacf4bf144508e24df" # 123456
        user_uuid = generate_uuid_hex()
        avatar_url = generate_avatar()

        users.append({
            'user_uuid': user_uuid,
            'username': username,
            'password': hashed_password,
            'nickname': nickname,
            'avatar_url': avatar_url,
            'email': email
        })

    return users

def insert_users(users):
    conn = mysql.connector.connect(**DB_CONFIG)
    cursor = conn.cursor()

    sql = """
    INSERT INTO tb_user
    (user_uuid, username, password, nickname, user_role, avatar_url, email, is_delete, status, create_time, create_user)
    VALUES
    (UNHEX(%s), %s, %s, %s, 11, %s, %s, 0, 10, NOW(), 0)
    """

    for user in users:
        values = (
            user['user_uuid'],
            user['username'],
            user['password'],
            user['nickname'],
            user['avatar_url'],
            user['email']
        )
        cursor.execute(sql, values)

    conn.commit()
    cursor.close()
    conn.close()
    print(f"Successfully inserted {len(users)} users!")

def main():
    count = int(input("Enter number of test users to generate: "))
    print(f"Generating {count} test users...")

    users = generate_users(count)

    try:
        insert_users(users)
        print("\nGenerated users:")
        for u in users[:5]:
            print(f"  Username: {u['username']}, Email: {u['email']}, Nickname: {u['nickname']}")
        if len(users) > 5:
            print(f"  ... and {len(users) - 5} more")
    except mysql.connector.Error as e:
        print(f"Database error: {e}")
        print("\nUsers to be inserted:")
        for u in users:
            print(f"  {u['username']}, {u['email']}")

if __name__ == '__main__':
    main()
