#!/bin/bash

#############################################
# VoiceBox 数据库初始化脚本
# 用于在新环境中快速初始化数据库
#############################################

set -e  # 遇到错误立即退出

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认配置
DB_HOST="localhost"
DB_PORT="3306"
DB_ROOT_USER="root"
DB_ROOT_PASSWORD=""
DB_NAME="voicebox_db"
DB_USER="voicebox"
DB_PASSWORD="voicebox123"
SKIP_CREATE_DB=false
LOAD_TEST_DATA=false

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCHEMA_DIR="${SCRIPT_DIR}/schema"
MIGRATIONS_DIR="${SCRIPT_DIR}/migrations"
TEST_DATA_DIR="${SCRIPT_DIR}/test-data"

#############################################
# 函数定义
#############################################

# 打印信息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# 打印成功
print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

# 打印警告
print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

# 打印错误
print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 显示帮助信息
show_help() {
    cat << EOF
VoiceBox 数据库初始化脚本

用法: $0 [选项]

选项:
  -h, --host HOST           MySQL 主机地址（默认: localhost）
  -P, --port PORT           MySQL 端口（默认: 3306）
  -u, --user USER           MySQL root 用户（默认: root）
  -p, --password PASS       MySQL root 密码
  -d, --database DB         数据库名称（默认: voicebox_db）
  --db-user USER            应用数据库用户（默认: voicebox）
  --db-password PASS        应用数据库密码（默认: voicebox123）
  --skip-create-db          跳过创建数据库步骤
  --test-data               加载测试数据
  --help                    显示此帮助信息

示例:
  # 本地初始化
  $0 -p mypassword

  # 远程服务器初始化
  $0 -h 129.211.180.183 -u root -p mypassword

  # 只创建表结构，不创建数据库
  $0 --skip-create-db

  # 初始化并加载测试数据
  $0 --test-data

EOF
}

# 解析命令行参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -h|--host)
                DB_HOST="$2"
                shift 2
                ;;
            -P|--port)
                DB_PORT="$2"
                shift 2
                ;;
            -u|--user)
                DB_ROOT_USER="$2"
                shift 2
                ;;
            -p|--password)
                DB_ROOT_PASSWORD="$2"
                shift 2
                ;;
            -d|--database)
                DB_NAME="$2"
                shift 2
                ;;
            --db-user)
                DB_USER="$2"
                shift 2
                ;;
            --db-password)
                DB_PASSWORD="$2"
                shift 2
                ;;
            --skip-create-db)
                SKIP_CREATE_DB=true
                shift
                ;;
            --test-data)
                LOAD_TEST_DATA=true
                shift
                ;;
            --help)
                show_help
                exit 0
                ;;
            *)
                print_error "未知选项: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# 检查 MySQL 是否安装
check_mysql() {
    print_info "检查 MySQL 是否安装..."
    if ! command -v mysql &> /dev/null; then
        print_error "MySQL 未安装，请先安装 MySQL"
        exit 1
    fi
    print_success "MySQL 已安装"
}

# 检查 MySQL 连接
check_connection() {
    print_info "检查 MySQL 连接..."
    
    if [ -z "$DB_ROOT_PASSWORD" ]; then
        read -sp "请输入 MySQL root 密码: " DB_ROOT_PASSWORD
        echo
    fi
    
    if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" -e "SELECT 1;" &> /dev/null; then
        print_success "MySQL 连接成功"
    else
        print_error "MySQL 连接失败，请检查主机、用户名和密码"
        exit 1
    fi
}

# 创建数据库和用户
create_database() {
    if [ "$SKIP_CREATE_DB" = true ]; then
        print_info "跳过创建数据库步骤"
        return
    fi
    
    print_info "创建数据库 ${DB_NAME}..."
    
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_ROOT_USER" -p"$DB_ROOT_PASSWORD" << EOF
-- 创建数据库
CREATE DATABASE IF NOT EXISTS ${DB_NAME} 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

-- 创建用户
CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASSWORD}';

-- 授权
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';
GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'%';

-- 刷新权限
FLUSH PRIVILEGES;

-- 使用数据库
USE ${DB_NAME};
EOF
    
    print_success "数据库创建成功"
}

# 执行 SQL 文件
execute_sql_file() {
    local sql_file=$1
    local description=$2
    
    if [ ! -f "$sql_file" ]; then
        print_warning "文件不存在: $sql_file"
        return
    fi
    
    print_info "$description"
    
    if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$sql_file"; then
        print_success "执行成功: $(basename $sql_file)"
    else
        print_error "执行失败: $(basename $sql_file)"
        exit 1
    fi
}

# 创建表结构
create_tables() {
    print_info "开始创建表结构..."
    
    # 按顺序执行 SQL 文件
    execute_sql_file "${SCHEMA_DIR}/01-base-tables.sql" "创建基础表..."
    execute_sql_file "${SCHEMA_DIR}/02-user-tables.sql" "创建用户表..."
    execute_sql_file "${SCHEMA_DIR}/03-personality-tables.sql" "创建个性化分析表..."
    execute_sql_file "${SCHEMA_DIR}/04-indexes.sql" "创建索引..."
    
    print_success "所有表创建完成"
}

# 加载测试数据
load_test_data() {
    if [ "$LOAD_TEST_DATA" = false ]; then
        return
    fi
    
    print_info "加载测试数据..."
    execute_sql_file "${TEST_DATA_DIR}/sample-data.sql" "插入示例数据..."
    print_success "测试数据加载完成"
}

# 验证安装
verify_installation() {
    print_info "验证数据库安装..."
    
    # 检查表数量
    local table_count=$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -sN -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}';")
    
    print_info "数据库中共有 ${table_count} 张表"
    
    # 列出所有表
    print_info "表列表:"
    mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SHOW TABLES;"
    
    print_success "数据库验证完成"
}

# 显示总结
show_summary() {
    cat << EOF

${GREEN}========================================
数据库初始化完成！
========================================${NC}

数据库信息:
  主机: ${DB_HOST}:${DB_PORT}
  数据库: ${DB_NAME}
  用户: ${DB_USER}

连接字符串:
  jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8

配置文件示例 (config.properties):
  spring.datasource.url=jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
  spring.datasource.username=${DB_USER}
  spring.datasource.password=${DB_PASSWORD}

下一步:
  1. 更新应用配置文件中的数据库连接信息
  2. 启动应用: ./start-all.sh
  3. 验证功能: ./status.sh

EOF
}

#############################################
# 主流程
#############################################

main() {
    echo -e "${BLUE}"
    cat << "EOF"
╔═══════════════════════════════════════╗
║   VoiceBox 数据库初始化脚本          ║
╚═══════════════════════════════════════╝
EOF
    echo -e "${NC}"
    
    # 解析参数
    parse_args "$@"
    
    # 检查环境
    check_mysql
    check_connection
    
    # 创建数据库
    create_database
    
    # 创建表结构
    create_tables
    
    # 加载测试数据
    load_test_data
    
    # 验证安装
    verify_installation
    
    # 显示总结
    show_summary
}

# 执行主流程
main "$@"
