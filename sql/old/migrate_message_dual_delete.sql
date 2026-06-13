-- ============================================
-- 消息系统双标签删除 - 数据库迁移脚本
-- 执行日期：2026-06-11
-- 说明：将单字段删除改为双标签删除机制
-- ============================================

-- 1. 添加新字段
ALTER TABLE tb_messages 
ADD COLUMN `is_delete_by_sender` tinyint(1) NOT NULL DEFAULT 0 COMMENT '发送者删除标记：0-未删除、1-已删除' AFTER `is_delete`,
ADD COLUMN `is_delete_by_receiver` tinyint(1) NOT NULL DEFAULT 0 COMMENT '接收者删除标记：0-未删除、1-已删除' AFTER `is_delete_by_sender`;

-- 2. 迁移数据：将原有 is_delete=1 的记录根据发送者/接收者关系设置对应字段
-- 注意：这里假设 is_delete=1 的消息是接收者删除的（因为旧逻辑只允许接收者删除）
UPDATE tb_messages 
SET is_delete_by_receiver = 1
WHERE is_delete = 1 AND `to` != 0;

-- 系统消息（from_user_id=0）只有接收者能删除
UPDATE tb_messages 
SET is_delete_by_receiver = 1
WHERE is_delete = 1 AND from_user_id = 0;

-- 3. 添加索引（可选，提高查询性能）
CREATE INDEX idx_msg_sender_delete ON tb_messages (from_user_id, is_delete_by_sender);
CREATE INDEX idx_msg_receiver_delete ON tb_messages (`to`, is_delete_by_receiver);

-- 4. 验证数据迁移
SELECT 
    COUNT(*) as total_messages,
    SUM(CASE WHEN is_delete = 1 THEN 1 ELSE 0 END) as old_deleted,
    SUM(CASE WHEN is_delete_by_sender = 1 THEN 1 ELSE 0 END) as sender_deleted,
    SUM(CASE WHEN is_delete_by_receiver = 1 THEN 1 ELSE 0 END) as receiver_deleted
FROM tb_messages;

-- 5. 确认无误后，可以删除旧字段（可选，建议保留一段时间观察）
-- ALTER TABLE tb_messages DROP COLUMN is_delete;

-- ============================================
-- 迁移完成
-- ============================================
