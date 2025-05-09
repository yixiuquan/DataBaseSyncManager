/*
 Navicat Premium Data Transfer

 Source Server         : 1-本地
 Source Server Type    : MySQL
 Source Server Version : 50738
 Source Host           : localhost:3306
 Source Schema         : db_sync

 Target Server Type    : MySQL
 Target Server Version : 50738
 File Encoding         : 65001

 Date: 09/05/2025 10:20:37
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for cdc_database_config
-- ----------------------------
DROP TABLE IF EXISTS `cdc_database_config`;
CREATE TABLE `cdc_database_config`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '数据库主机地址',
  `port` int(11) NOT NULL COMMENT '数据库端口',
  `db_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库名',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码（加密存储）',
  `param` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '额外连接参数',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态：0-停用，1-启用',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '数据库连接配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for cdc_sync_exception
-- ----------------------------
DROP TABLE IF EXISTS `cdc_sync_exception`;
CREATE TABLE `cdc_sync_exception`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `execution_id` bigint(20) NOT NULL COMMENT '执行记录ID',
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表名',
  `error_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '错误数据，JSON格式',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '错误信息',
  `error_time` datetime(0) NOT NULL COMMENT '错误发生时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_execution_id`(`execution_id`) USING BTREE,
  INDEX `idx_table_name`(`table_name`) USING BTREE,
  INDEX `idx_error_time`(`error_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '同步异常记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for cdc_sync_task
-- ----------------------------
DROP TABLE IF EXISTS `cdc_sync_task`;
CREATE TABLE `cdc_sync_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务名称',
  `source_db_id` bigint(20) NOT NULL COMMENT '源数据库ID',
  `target_db_id` bigint(20) NOT NULL COMMENT '目标数据库ID',
  `sync_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '同步类型：0-全量同步，1-增量同步',
  `tables` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '同步表配置，JSON格式',
  `startup_options` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '启动选项，JSON格式',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '任务状态：0-停止，1-运行中，2-异常',
  `task_start_time` datetime(0) NULL DEFAULT NULL COMMENT '任务开始时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_source_db`(`source_db_id`) USING BTREE,
  INDEX `idx_target_db`(`target_db_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '同步任务配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for cdc_table_statistics
-- ----------------------------
DROP TABLE IF EXISTS `cdc_table_statistics`;
CREATE TABLE `cdc_table_statistics`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `execution_id` bigint(20) NULL DEFAULT NULL COMMENT '执行记录ID',
  `table_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '表名',
  `insert_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '插入记录数',
  `update_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '更新记录数',
  `delete_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '删除记录数',
  `sync_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '已同步数据量',
  `total_count` bigint(20) NULL DEFAULT -1 COMMENT '总数据量，-1表示未知或持续进行',
  `exception_count` bigint(20) NOT NULL DEFAULT 0 COMMENT '异常记录数',
  `progress` double(255, 0) NULL DEFAULT NULL,
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `start_time` datetime(0) NOT NULL COMMENT '表同步开始时间',
  `last_update_time` datetime(0) NOT NULL COMMENT '最近更新时间',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_task_execution_table`(`task_id`, `execution_id`, `table_name`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_execution_id`(`execution_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 319 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '表同步统计信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for cdc_task_execution
-- ----------------------------
DROP TABLE IF EXISTS `cdc_task_execution`;
CREATE TABLE `cdc_task_execution`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint(20) NOT NULL COMMENT '任务ID',
  `job_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'Flink作业ID',
  `start_time` datetime(0) NOT NULL COMMENT '执行开始时间',
  `end_time` datetime(0) NULL DEFAULT NULL COMMENT '执行结束时间',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '执行状态：0-失败，1-成功，2-运行中',
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '错误信息',
  `create_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `update_time` datetime(0) NOT NULL DEFAULT CURRENT_TIMESTAMP(0) ON UPDATE CURRENT_TIMESTAMP(0) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_task_id`(`task_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '任务执行记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
