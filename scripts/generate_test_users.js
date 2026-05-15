#!/usr/bin/env node
/**
 * 用户生成器 (JavaScript 版本)
 *
 * 运行该脚本会生成n个测试用户，并插入数据库中。
 * 依赖: mysql2
 * 安装: npm install mysql2
 */

const mysql = require('mysql2/promise');

// 数据库配置
const DB_CONFIG = {
  host: 'localhost',
  port: 3306,
  user: 'root',
  password: '123456',
  database: 'db_pix_vision'
};

const ADJECTIVES = ['happy', 'sunny', 'gentle', 'brave', 'clever', 'kind', 'quick', 'bright', 'warm', 'cool', 'lovely', 'peaceful', 'friendly', 'cute', 'sweet'];
const NOUNS = ['cat', 'dog', 'bird', 'fish', 'fox', 'bear', 'panda', 'rabbit', 'lion', 'tiger', 'eagle', 'owl', 'wolf', 'deer', 'moon', 'star', 'sun', 'cloud', 'rain', 'wind'];

/**
 * 加密字符串函数
 */
function encryptString(s) {
  let result = [];
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (/\d/.test(c)) {
      const num = parseInt(c);
      const newNum = (num + 5) % 10;
      result.push(String.fromCharCode(newNum + '0'.charCodeAt(0)));
    } else if (/[a-zA-Z]/.test(c)) {
      const asciiVal = c.toLowerCase().charCodeAt(0);
      result.push(String.fromCharCode((asciiVal - 'a'.charCodeAt(0) + 3) % 26 + 'a'.charCodeAt(0)));
    } else {
      result.push(c);
    }
  }
  return result.join('');
}

/**
 * 数字转字符串函数
 */
function number2str(s) {
  let num = 0;
  for (let i = 0; i < s.length; i++) {
    const c = s[i];
    if (/\d/.test(c)) {
      num += c.charCodeAt(0);
    }
  }
  return (num % 1000000).toString() + s;
}

/**
 * 生成昵称
 */
function generateNickname() {
  const adj = ADJECTIVES[Math.floor(Math.random() * ADJECTIVES.length)];
  const noun = NOUNS[Math.floor(Math.random() * NOUNS.length)];
  const num = Math.floor(Math.random() * 90) + 10; // 10-99

  // 首字母大写
  const capitalizedAdj = adj.charAt(0).toUpperCase() + adj.slice(1);
  const capitalizedNoun = noun.charAt(0).toUpperCase() + noun.slice(1);

  return `${capitalizedAdj}${capitalizedNoun}${num}`;
}

/**
 * 生成 UUID (十六进制)
 */
function generateUuidHex() {
  const hexChars = '0123456789abcdef';
  let uuid = '';
  for (let i = 0; i < 32; i++) {
    uuid += hexChars[Math.floor(Math.random() * 16)];
  }
  return uuid;
}

/**
 * 生成头像路径
 */
function generateAvatar() {
  const num = Math.floor(Math.random() * 21) + 1; // 1-21
  return `default/${num}.png`;
}

/**
 * 生成用户数据
 */
function generateUsers(count) {
  const users = [];
  const usedUsernames = new Set();
  const usedEmails = new Set();

  while (users.length < count) {
    const username = `user_${Math.floor(Math.random() * 900000000) + 100000000}`;

    if (usedUsernames.has(username)) {
      continue;
    }
    usedUsernames.add(username);

    const nickname = generateNickname();
    const email = `${username}@example.com`;

    if (usedEmails.has(email)) {
      continue;
    }
    usedEmails.add(email);

    const hashedPassword = 'a540d25f4997f6cf504647ccc20d870bdd9562cb01ca86bacf4bf144508e24df'; // 123456
    const userUuid = generateUuidHex();
    const avatarUrl = generateAvatar();

    users.push({
      user_uuid: userUuid,
      username: username,
      password: hashedPassword,
      nickname: nickname,
      avatar_url: avatarUrl,
      email: email
    });
  }

  return users;
}

/**
 * 插入用户到数据库
 */
async function insertUsers(users) {
  const connection = await mysql.createConnection(DB_CONFIG);

  const sql = `
    INSERT INTO tb_user
    (user_uuid, username, password, nickname, user_role, avatar_url, email, is_delete, status, create_time, create_user)
    VALUES (UNHEX(?), ?, ?, ?, 11, ?, ?, 0, 10, NOW(), 0)
  `;

  try {
    for (const user of users) {
      await connection.execute(sql, [
        user.user_uuid,
        user.username,
        user.password,
        user.nickname,
        user.avatar_url,
        user.email
      ]);
    }

    console.log(`Successfully inserted ${users.length} users!`);
  } catch (error) {
    throw error;
  } finally {
    await connection.end();
  }
}

/**
 * 主函数
 */
async function main() {
  const readline = require('readline');
  const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout
  });

  const question = (query) => new Promise((resolve) => rl.question(query, resolve));

  try {
    const countStr = await question('Enter number of test users to generate: ');
    const count = parseInt(countStr);

    if (isNaN(count) || count <= 0) {
      console.log('Invalid number!');
      rl.close();
      return;
    }

    console.log(`Generating ${count} test users...`);

    const users = generateUsers(count);

    try {
      await insertUsers(users);

      console.log('\nGenerated users:');
      users.slice(0, 5).forEach(u => {
        console.log(`  Username: ${u.username}, Email: ${u.email}, Nickname: ${u.nickname}`);
      });
      if (users.length > 5) {
        console.log(`  ... and ${users.length - 5} more`);
      }
    } catch (error) {
      console.error(`Database error: ${error.message}`);
      console.log('\nUsers to be inserted:');
      users.forEach(u => {
        console.log(`  ${u.username}, ${u.email}`);
      });
    }
  } finally {
    rl.close();
  }
}

// 运行主函数
main().catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
