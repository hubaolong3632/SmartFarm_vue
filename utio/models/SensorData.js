/**
 * 传感器数据模型
 * 对应表：sensor_data
 */

class SensorData {
  constructor(data = {}) {
    this.id = data.id || null
    this.recordTime = data.record_time || data.recordTime || null
    this.temperatureC = data.temperature_c || data.temperatureC || 0
    this.soilMoisturePct = data.soil_moisture_pct || data.soilMoisturePct || 0
    this.lightLux = data.light_lux || data.lightLux || 0
    this.isRaining = data.is_raining !== undefined ? data.is_raining : (data.isRaining !== undefined ? data.isRaining : 0)
    this.createdAt = data.created_at || data.createdAt || null
    this.updatedAt = data.updated_at || data.updatedAt || null
  }

  /**
   * 转换为数据库字段格式
   */
  toDbFormat() {
    return {
      record_time: this.recordTime,
      temperature_c: this.temperatureC,
      soil_moisture_pct: this.soilMoisturePct,
      light_lux: this.lightLux,
      is_raining: this.isRaining ? 1 : 0,
    }
  }

  /**
   * 转换为前端格式
   */
  toFrontendFormat() {
    return {
      id: this.id,
      time: this.recordTime ? new Date(this.recordTime).toISOString() : null,
      temperatureC: parseFloat(this.temperatureC),
      soilMoisturePct: parseFloat(this.soilMoisturePct),
      lightLux: parseInt(this.lightLux),
      isRaining: this.isRaining ? 1 : 0,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    }
  }

  /**
   * 验证数据有效性
   */
  validate() {
    const errors = []
    if (!this.recordTime) errors.push('记录时间不能为空')
    if (this.temperatureC < -50 || this.temperatureC > 100) errors.push('温度值超出合理范围')
    if (this.soilMoisturePct < 0 || this.soilMoisturePct > 100) errors.push('土壤湿度值超出合理范围')
    if (this.lightLux < 0) errors.push('光照强度不能为负数')
    return errors
  }
}

module.exports = SensorData

