-- MySQL dump 10.13  Distrib 8.0.45, for macos26.3 (arm64)
--
-- Host: localhost    Database: db_pix_vision
-- ------------------------------------------------------
-- Server version	8.0.45

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `tb_comments`
--

DROP TABLE IF EXISTS `tb_comments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_comments` (
  `conmment_id` int NOT NULL AUTO_INCREMENT COMMENT '唯一值',
  `user_id` int NOT NULL COMMENT '用于链接到对应的用户数据',
  `work_id` int NOT NULL COMMENT '所属作品id',
  `in_comment_id` int DEFAULT NULL COMMENT '所属一级评论，用于二级评论定位一级评论',
  `parent_comment_id` int DEFAULT NULL COMMENT '所回复的评论id，用于定位被回复方评论',
  `conmment_floor` int NOT NULL COMMENT '评论层级：1 - 作品评论、2 - 二级评论',
  `conmment_text` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '评论内容，限制长度125字',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '数据条目删除标签，0 - 未删除、1 - 已删除',
  `time` timestamp NOT NULL COMMENT '数据条目创建时间戳',
  PRIMARY KEY (`conmment_id`),
  KEY `tb_comments_tb_user_FK` (`user_id`),
  KEY `tb_comments_tb_works_FK` (`work_id`),
  KEY `tb_comments_tb_comments_FK` (`parent_comment_id`),
  KEY `tb_comments_tb_comments_FK_1` (`in_comment_id`),
  CONSTRAINT `tb_comments_tb_comments_FK` FOREIGN KEY (`parent_comment_id`) REFERENCES `tb_comments` (`conmment_id`),
  CONSTRAINT `tb_comments_tb_comments_FK_1` FOREIGN KEY (`in_comment_id`) REFERENCES `tb_comments` (`conmment_id`),
  CONSTRAINT `tb_comments_tb_user_FK` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `tb_comments_tb_works_FK` FOREIGN KEY (`work_id`) REFERENCES `tb_works` (`work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='评论表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_history`
--

DROP TABLE IF EXISTS `tb_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_history` (
  `user_id` int NOT NULL COMMENT '用户id映射',
  `work_id` int NOT NULL COMMENT '作品id映射',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '数据条目删除标签，0 - 未删除、1 - 已删除',
  `time` timestamp NOT NULL COMMENT '数据条目创建时间戳',
  KEY `tb_history_tb_user_FK` (`user_id`),
  KEY `tb_history_tb_works_FK` (`work_id`),
  CONSTRAINT `tb_history_tb_user_FK` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `tb_history_tb_works_FK` FOREIGN KEY (`work_id`) REFERENCES `tb_works` (`work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='历史记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_like`
--

DROP TABLE IF EXISTS `tb_like`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_like` (
  `user_id` int NOT NULL COMMENT '用户id映射',
  `work_id` int NOT NULL COMMENT '作品id映射',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '数据条目删除标签，0 - 未删除、1 - 已删除',
  `time` timestamp NOT NULL COMMENT '数据条目创建时间戳',
  KEY `tb_history_tb_user_FK` (`user_id`) USING BTREE,
  KEY `tb_history_tb_works_FK` (`work_id`) USING BTREE,
  CONSTRAINT `tb_history_tb_user_FK_copy` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `tb_history_tb_works_FK_copy` FOREIGN KEY (`work_id`) REFERENCES `tb_works` (`work_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='赞赏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_pending_reviews`
--

DROP TABLE IF EXISTS `tb_pending_reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_pending_reviews` (
  `pending_reviews_id` int NOT NULL AUTO_INCREMENT COMMENT 'id唯一值',
  `data_type` int NOT NULL COMMENT '数据类型，作品-100、评论-200、头像-300',
  `status` int NOT NULL DEFAULT '20' COMMENT '审核状态，10 - 正常、20 - 待审核、30 - 封禁',
  `work_id` int DEFAULT NULL COMMENT '待审核作品id，用于作品审核记录',
  `conmment_id` int DEFAULT NULL COMMENT '待审核评论id，用于评论审核记录',
  `user_id` int DEFAULT NULL COMMENT '待审核用户id，用于用户信息审核记录',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '数据条目删除标签，0 - 未删除、1 - 已删除',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '数据条目更新时间戳',
  `update_user` int DEFAULT NULL COMMENT '修改者id，系统修改为0',
  `create_time` timestamp NOT NULL COMMENT '数据条目创建时间戳',
  `create_user` int NOT NULL DEFAULT '0' COMMENT '存储创建者id，系统创建为0',
  PRIMARY KEY (`pending_reviews_id`),
  KEY `tb_pending_reviews_tb_user_FK` (`user_id`),
  KEY `tb_pending_reviews_tb_works_FK` (`work_id`),
  KEY `tb_pending_reviews_tb_comments_FK` (`conmment_id`),
  CONSTRAINT `tb_pending_reviews_tb_comments_FK` FOREIGN KEY (`conmment_id`) REFERENCES `tb_comments` (`conmment_id`),
  CONSTRAINT `tb_pending_reviews_tb_user_FK` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`),
  CONSTRAINT `tb_pending_reviews_tb_works_FK` FOREIGN KEY (`work_id`) REFERENCES `tb_works` (`work_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='待审核数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_series`
--

DROP TABLE IF EXISTS `tb_series`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_series` (
  `series_id` int NOT NULL AUTO_INCREMENT COMMENT 'id唯一值',
  `user_id` int NOT NULL COMMENT '用于链接到对应的用户数据',
  `series_title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '系列标题，16个中文长度',
  `about_text` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci DEFAULT NULL COMMENT '系列描述文本，24个中文长度',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '数据条目删除标签，0 - 未删除、1 - 已删除',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '数据条目更新时间戳',
  `update_user` int DEFAULT NULL COMMENT '修改者id，系统修改为0',
  `create_time` timestamp NOT NULL COMMENT '数据条目创建时间戳',
  `create_user` int NOT NULL DEFAULT '0' COMMENT '存储创建者id，系统创建为0',
  PRIMARY KEY (`series_id`),
  KEY `tb_series_tb_user_FK` (`user_id`),
  CONSTRAINT `tb_series_tb_user_FK` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='作品系列';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_star`
--

DROP TABLE IF EXISTS `tb_star`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_star` (
  `user_id` int NOT NULL COMMENT '用户id映射',
  `work_id` int NOT NULL COMMENT '作品id映射',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '数据条目删除标签，0 - 未删除、1 - 已删除',
  `time` timestamp NOT NULL COMMENT '数据条目创建时间戳',
  KEY `tb_history_tb_user_FK` (`user_id`) USING BTREE,
  KEY `tb_history_tb_works_FK` (`work_id`) USING BTREE,
  CONSTRAINT `tb_history_tb_user_FK_copy_copy` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `tb_history_tb_works_FK_copy_copy` FOREIGN KEY (`work_id`) REFERENCES `tb_works` (`work_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='收藏表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_sys_logs`
--

DROP TABLE IF EXISTS `tb_sys_logs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_sys_logs` (
  `sys_log_id` int NOT NULL AUTO_INCREMENT COMMENT '唯一值',
  `user_id` int NOT NULL COMMENT '操作者id',
  `log_datetime` timestamp NOT NULL COMMENT '操作日期时间',
  `log_event` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '操作事件',
  PRIMARY KEY (`sys_log_id`),
  KEY `tb_sys_logs_tb_user_FK` (`user_id`),
  CONSTRAINT `tb_sys_logs_tb_user_FK` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='系统操作日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user`
--

DROP TABLE IF EXISTS `tb_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user` (
  `user_id` int NOT NULL AUTO_INCREMENT COMMENT 'id唯一值',
  `user_uuid` binary(16) NOT NULL COMMENT 'uuid唯一值',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '用户名，仅限英文字符和【-】',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '密码哈希256映射加盐加密',
  `nickname` varchar(48) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '昵称，12位长度的中文、英文、特殊字符',
  `user_role` int NOT NULL DEFAULT '11' COMMENT '用户角色：\r\n普通用户 - 11\r\n创作者 - 22\r\n审核员 - 55\r\n售票管理员 - 66\r\n系统管理员 - 77',
  `avatar_url` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '用户头像路径\n存储：/data/userdata/uuid.*\n数据库映射：uuid.*\n限制文件类型：jpg、jpeg、png\n限制大小：2.5mb',
  `email` varchar(320) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '验证成功后的邮箱地址',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '数据条目删除标签，0 - 未删除、1 - 已删除',
  `status` int NOT NULL DEFAULT '10' COMMENT '账号状态，10 - 正常、20 - 冻结、30 - 封禁',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '数据条目更新时间戳',
  `update_user` int DEFAULT NULL COMMENT '修改者id，系统修改为0',
  `create_time` timestamp NOT NULL COMMENT '数据条目创建时间戳',
  `create_user` int NOT NULL DEFAULT '0' COMMENT '存储创建者id，系统创建为0',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=288 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_user_data`
--

DROP TABLE IF EXISTS `tb_user_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_user_data` (
  `data_id` int NOT NULL AUTO_INCREMENT COMMENT 'id唯一值',
  `user_id` int NOT NULL COMMENT '用于链接用户数据到用户表',
  `user_data_name` varchar(26) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '数据项目名称（电话、邮箱、网站、微信等等）',
  `user_data` varchar(96) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '数据内容（具体的电话号码、邮箱地址、网站url等等）',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '数据条目删除标签，0 - 未删除、1 - 已删除',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '数据条目更新时间戳',
  `update_user` int DEFAULT NULL COMMENT '修改者id，系统修改为0',
  `create_time` timestamp NOT NULL COMMENT '数据条目创建时间戳',
  `create_user` int NOT NULL DEFAULT '0' COMMENT '存储创建者id，系统创建为0',
  PRIMARY KEY (`data_id`),
  KEY `tb_user_data_user_id_IDX` (`user_id`) USING BTREE,
  CONSTRAINT `tb_user_data_tb_user_FK` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='用户数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `tb_works`
--

DROP TABLE IF EXISTS `tb_works`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tb_works` (
  `work_id` int NOT NULL AUTO_INCREMENT COMMENT 'id唯一值',
  `user_id` int NOT NULL COMMENT '用于链接到对应的用户数据',
  `work_title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '作品标题，16个中文长度',
  `img_url` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '存储：/data/work_img/uuid.*\n数据库映射：uuid.*\n限制文件类型：jpg、jpeg、png\n限制大小：12mb',
  `series_id` int DEFAULT NULL COMMENT '用于链接到对应的系列合集',
  `like_count` int NOT NULL DEFAULT '0' COMMENT '点赞数',
  `star_count` int NOT NULL DEFAULT '0' COMMENT '收藏数',
  `view_count` int NOT NULL DEFAULT '0' COMMENT '查看数',
  `is_original_work` tinyint(1) NOT NULL DEFAULT '1' COMMENT '用于标记是否为原创作品，1 - 原创、0 - 转载',
  `out_url` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_german2_ci NOT NULL COMMENT '外部转载链接',
  `is_delete` tinyint(1) NOT NULL DEFAULT '0' COMMENT '数据条目删除标签，0 - 未删除、1 - 已删除',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '数据条目更新时间戳',
  `update_user` int DEFAULT NULL COMMENT '修改者id，系统修改为0',
  `create_time` timestamp NOT NULL COMMENT '数据条目创建时间戳',
  `create_user` int NOT NULL DEFAULT '0' COMMENT '存储创建者id，系统创建为0',
  PRIMARY KEY (`work_id`),
  KEY `tb_works_tb_user_FK` (`user_id`),
  KEY `tb_works_tb_series_FK` (`series_id`),
  CONSTRAINT `tb_works_tb_series_FK` FOREIGN KEY (`series_id`) REFERENCES `tb_series` (`series_id`),
  CONSTRAINT `tb_works_tb_user_FK` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_german2_ci COMMENT='作品表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'db_pix_vision'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-05-12  9:01:39
