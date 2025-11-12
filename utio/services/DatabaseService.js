/**
 * 数据库服务基类
 * 提供通用的数据库操作方法
 * 需要根据实际使用的数据库驱动（如 mysql2、sequelize 等）进行实现
 */

class DatabaseService {
  constructor(connection) {
    this.connection = connection
  }

  /**
   * 执行查询
   * @param {string} sql - SQL 语句
   * @param {Array} params - 参数数组
   * @returns {Promise<Array>}
   */
  async query(sql, params = []) {
    throw new Error('query method must be implemented')
  }

  /**
   * 执行插入
   * @param {string} table - 表名
   * @param {Object} data - 数据对象
   * @returns {Promise<number>} 插入的ID
   */
  async insert(table, data) {
    throw new Error('insert method must be implemented')
  }

  /**
   * 执行更新
   * @param {string} table - 表名
   * @param {Object} data - 数据对象
   * @param {Object} where - 条件对象
   * @returns {Promise<number>} 影响的行数
   */
  async update(table, data, where) {
    throw new Error('update method must be implemented')
  }

  /**
   * 执行删除
   * @param {string} table - 表名
   * @param {Object} where - 条件对象
   * @returns {Promise<number>} 影响的行数
   */
  async delete(table, where) {
    throw new Error('delete method must be implemented')
  }

  /**
   * 开始事务
   */
  async beginTransaction() {
    throw new Error('beginTransaction method must be implemented')
  }

  /**
   * 提交事务
   */
  async commit() {
    throw new Error('commit method must be implemented')
  }

  /**
   * 回滚事务
   */
  async rollback() {
    throw new Error('rollback method must be implemented')
  }

  /**
   * 关闭连接
   */
  async close() {
    throw new Error('close method must be implemented')
  }
}

module.exports = DatabaseService

