#!/usr/bin/env lua
-- 用户生成器 (Lua 版本)
-- 
-- 运行该脚本会生成n个测试用户，并插入数据库中。
-- 依赖: luasql-mysql 或 mysql-lua
-- 安装: luarocks install luasql-mysql

local mysql = require "luasql.mysql"

-- 数据库配置
local DB_CONFIG = {
    host = "localhost",
    port = 3306,
    user = "root",
    password = "123456",
    database = "db_pix_vision"
}

-- 形容词和名词列表
local ADJECTIVES = {"happy", "sunny", "gentle", "brave", "clever", "kind", "quick", "bright", "warm", "cool", "lovely", "peaceful", "friendly", "cute", "sweet"}
local NOUNS = {"cat", "dog", "bird", "fish", "fox", "bear", "panda", "rabbit", "lion", "tiger", "eagle", "owl", "wolf", "deer", "moon", "star", "sun", "cloud", "rain", "wind"}

-- 随机数种子
math.randomseed(os.time())

-- 加密字符串函数
local function encrypt_string(s)
    local result = {}
    for i = 1, #s do
        local c = s:sub(i, i)
        if c:match("%d") then
            local num = tonumber(c)
            local new_num = (num + 5) % 10
            table.insert(result, string.char(new_num + string.byte("0")))
        elseif c:match("[a-zA-Z]") then
            local lower_c = c:lower()
            local ascii_val = string.byte(lower_c)
            local new_char = string.char((ascii_val - string.byte("a") + 3) % 26 + string.byte("a"))
            table.insert(result, new_char)
        else
            table.insert(result, c)
        end
    end
    return table.concat(result)
end

-- 数字转字符串函数
local function number2str(s)
    local num = 0
    for i = 1, #s do
        local c = s:sub(i, i)
        if c:match("%d") then
            num = num + string.byte(c)
        end
    end
    return tostring(num % 1000000) .. s
end

-- 生成昵称
local function generate_nickname()
    local adj = ADJECTIVES[math.random(#ADJECTIVES)]
    local noun = NOUNS[math.random(#NOUNS)]
    local num = math.random(10, 99)
    -- 首字母大写
    adj = adj:sub(1, 1):upper() .. adj:sub(2)
    noun = noun:sub(1, 1):upper() .. noun:sub(2)
    return string.format("%s%s%d", adj, noun, num)
end

-- 生成 UUID (十六进制)
local function generate_uuid_hex()
    local hex_chars = "0123456789abcdef"
    local uuid = {}
    for i = 1, 32 do
        table.insert(uuid, hex_chars:sub(math.random(1, 16), math.random(1, 16)))
    end
    return table.concat(uuid)
end

-- 生成头像路径
local function generate_avatar()
    local num = math.random(1, 21)
    return string.format("default/%d.png", num)
end

-- 生成用户数据
local function generate_users(count)
    local users = {}
    local used_usernames = {}
    local used_emails = {}

    while #users < count do
        local username = string.format("user_%d", math.random(100000000, 999999999))
        
        -- 检查用户名是否已使用
        if not used_usernames[username] then
            used_usernames[username] = true
            
            local nickname = generate_nickname()
            local email = string.format("%s@example.com", username)
            
            -- 检查邮箱是否已使用
            if not used_emails[email] then
                used_emails[email] = true
                
                local hashed_password = "a540d25f4997f6cf504647ccc20d870bdd9562cb01ca86bacf4bf144508e24df" -- 123456
                local user_uuid = generate_uuid_hex()
                local avatar_url = generate_avatar()

                table.insert(users, {
                    user_uuid = user_uuid,
                    username = username,
                    password = hashed_password,
                    nickname = nickname,
                    avatar_url = avatar_url,
                    email = email
                })
            end
        end
    end

    return users
end

-- 插入用户到数据库
local function insert_users(users)
    local env = assert(mysql.mysql())
    local conn = assert(env:connect(DB_CONFIG.database, DB_CONFIG.user, DB_CONFIG.password, DB_CONFIG.host, DB_CONFIG.port))
    
    local sql = [[
        INSERT INTO tb_user
        (user_uuid, username, password, nickname, user_role, avatar_url, email, is_delete, status, create_time, create_user)
        VALUES
        (UNHEX(?), ?, ?, ?, 11, ?, ?, 0, 10, NOW(), 0)
    ]]

    local stmt = assert(conn:prepare(sql))
    
    for _, user in ipairs(users) do
        local success = stmt:execute(
            user.user_uuid,
            user.username,
            user.password,
            user.nickname,
            user.avatar_url,
            user.email
        )
        if not success then
            print(string.format("Error inserting user: %s", user.username))
        end
    end
    
    stmt:close()
    conn:close()
    env:close()
    
    print(string.format("Successfully inserted %d users!", #users))
end

-- 主函数
local function main()
    io.write("Enter number of test users to generate: ")
    io.flush()
    local count = tonumber(io.read())
    
    if not count or count <= 0 then
        print("Invalid number!")
        return
    end
    
    print(string.format("Generating %d test users...", count))

    local users = generate_users(count)

    -- 打印生成的用户信息（前5个）
    print("\nGenerated users:")
    for i = 1, math.min(5, #users) do
        local u = users[i]
        print(string.format("  Username: %s, Email: %s, Nickname: %s", u.username, u.email, u.nickname))
    end
    if #users > 5 then
        print(string.format("  ... and %d more", #users - 5))
    end

    -- 尝试插入数据库
    local success, err = pcall(insert_users, users)
    if not success then
        print(string.format("Database error: %s", err))
        print("\nUsers to be inserted:")
        for _, u in ipairs(users) do
            print(string.format("  %s, %s", u.username, u.email))
        end
    end
end

-- 运行主函数
main()
