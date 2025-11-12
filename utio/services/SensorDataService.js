/**
 * 传感器数据服务
 * 提供传感器数据的 CRUD 操作
 */

const SensorData = require('../models/SensorData')

class SensorDataService {
  constructor(dbService) {
    this.db = dbService
  }

  /**
   * 创建传感器数据记录
   * @param {Object} data - 传感器数据
   * @returns {Promise<SensorData>}
   */
  async create(data) {
    const sensorData = new SensorData(data)
    const errors = sensorData.validate()
    if (errors.length > 0) {
      throw new Error(`数据验证失败: ${errors.join(', ')}`)
    }

    const dbData = sensorData.toDbFormat()
    const sql = `INSERT INTO sensor_data (record_time, temperature_c, soil_moisture_pct, light_lux, is_raining) 
                 VALUES (?, ?, ?, ?, ?)`
    const params = [
      dbData.record_time,
      dbData.temperature_c,
      dbData.soil_moisture_pct,
      dbData.light_lux,
      dbData.is_raining,
    ]

    const result = await this.db.query(sql, params)
    sensorData.id = result.insertId
    return sensorData
  }

  /**
   * 根据ID查询
   * @param {number} id - 记录ID
   * @returns {Promise<SensorData|null>}
   */
  async findById(id) {
    const sql = 'SELECT * FROM sensor_data WHERE id = ?'
    const rows = await this.db.query(sql, [id])
    if (rows.length === 0) return null
    return new SensorData(rows[0])
  }

  /**
   * 查询最近24小时的数据
   * @param {Date} endTime - 结束时间（默认当前时间）
   * @returns {Promise<Array<SensorData>>}
   */
  async findLast24Hours(endTime = new Date()) {
    const startTime = new Date(endTime.getTime() - 23 * 3600 * 1000)
    const sql = `SELECT * FROM sensor_data 
                 WHERE record_time >= ? AND record_time <= ? 
                 ORDER BY record_time ASC`
    const rows = await this.db.query(sql, [startTime, endTime])
    return rows.map(row => new SensorData(row))
  }

  /**
   * 查询指定时间范围的数据
   * @param {Date} startTime - 开始时间
   * @param {Date} endTime - 结束时间
   * @returns {Promise<Array<SensorData>>}
   */
  async findByTimeRange(startTime, endTime) {
    const sql = `SELECT * FROM sensor_data 
                 WHERE record_time >= ? AND record_time <= ? 
                 ORDER BY record_time ASC`
    const rows = await this.db.query(sql, [startTime, endTime])
    return rows.map(row => new SensorData(row))
  }

  /**
   * 获取最新的一条记录
   * @returns {Promise<SensorData|null>}
   */
  async findLatest() {
    const sql = 'SELECT * FROM sensor_data ORDER BY record_time DESC LIMIT 1'
    const rows = await this.db.query(sql)
    if (rows.length === 0) return null
    return new SensorData(rows[0])
  }

  /**
   * 批量插入传感器数据
   * @param {Array<Object>} dataList - 数据列表
   * @returns {Promise<number>} 插入的记录数
   */
  async batchCreate(dataList) {
    if (!Array.isArray(dataList) || dataList.length === 0) {
      return 0
    }

    const values = []
    const params = []
    dataList.forEach(data => {
      const sensorData = new SensorData(data)
      const errors = sensorData.validate()
      if (errors.length > 0) {
        throw new Error(`数据验证失败: ${errors.join(', ')}`)
      }
      const dbData = sensorData.toDbFormat()
      values.push('(?, ?, ?, ?, ?)')
      params.push(
        dbData.record_time,
        dbData.temperature_c,
        dbData.soil_moisture_pct,
        dbData.light_lux,
        dbData.is_raining
      )
    })

    const sql = `INSERT INTO sensor_data (record_time, temperature_c, soil_moisture_pct, light_lux, is_raining) 
                 VALUES ${values.join(', ')}`
    const result = await this.db.query(sql, params)
    return result.affectedRows
  }

  /**
   * 删除指定时间之前的数据（用于数据清理）
   * @param {Date} beforeTime - 删除此时间之前的数据
   * @returns {Promise<number>} 删除的记录数
   */
  async deleteBefore(beforeTime) {
    const sql = 'DELETE FROM sensor_data WHERE record_time < ?'
    const result = await this.db.query(sql, [beforeTime])
    return result.affectedRows
  }
}

module.exports = SensorDataService

