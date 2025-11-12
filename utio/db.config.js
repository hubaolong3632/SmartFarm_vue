/**
 * 数据库配置文件
 * 支持 MySQL/MariaDB 数据库连接配置
 */

const dbConfig = {
  // 开发环境配置
  development: {
    host: 'localhost',
    port: 3306,
    user: 'root',
    password: 'password',
    database: 'greenhouse_db',
    charset: 'utf8mb4',
    timezone: '+08:00',
    connectionLimit: 10,
    acquireTimeout: 60000,
    timeout: 60000,
  },
  
  // 生产环境配置
  production: {
    host: process.env.DB_HOST || 'localhost',
    port: parseInt(process.env.DB_PORT) || 3306,
    user: process.env.DB_USER || 'root',
    password: process.env.DB_PASSWORD || '',
    database: process.env.DB_NAME || 'greenhouse_db',
    charset: 'utf8mb4',
    timezone: '+08:00',
    connectionLimit: 20,
    acquireTimeout: 60000,
    timeout: 60000,
  },
  
  // 测试环境配置
  test: {
    host: 'localhost',
    port: 3306,
    user: 'test',
    password: 'test',
    database: 'greenhouse_db_test',
    charset: 'utf8mb4',
    timezone: '+08:00',
    connectionLimit: 5,
    acquireTimeout: 30000,
    timeout: 30000,
  },
}

// 根据环境变量获取当前配置
const env = process.env.NODE_ENV || 'development'
const currentConfig = dbConfig[env]

module.exports = {
  ...currentConfig,
  // 导出所有环境配置（可选）
  all: dbConfig,
  // 当前环境
  env,
}

