/**
 * 图片模型
 * 对应表：images
 */

class Image {
  constructor(data = {}) {
    this.id = data.id || null
    this.imageUrl = data.image_url || data.imageUrl || ''
    this.recordTime = data.record_time || data.recordTime || null
    this.temperatureC = data.temperature_c !== undefined ? data.temperature_c : (data.temperatureC !== undefined ? data.temperatureC : null)
    this.soilMoisturePct = data.soil_moisture_pct !== undefined ? data.soil_moisture_pct : (data.soilMoisturePct !== undefined ? data.soilMoisturePct : null)
    this.lightLux = data.light_lux !== undefined ? data.light_lux : (data.lightLux !== undefined ? data.lightLux : null)
    this.plotId = data.plot_id !== undefined ? data.plot_id : (data.plotId !== undefined ? data.plotId : null)
    this.isAbnormal = data.is_abnormal !== undefined ? data.is_abnormal : (data.isAbnormal !== undefined ? data.isAbnormal : 0)
    this.abnormalReason = data.abnormal_reason || data.abnormalReason || null
    this.createdAt = data.created_at || data.createdAt || null
  }

  /**
   * 转换为数据库字段格式
   */
  toDbFormat() {
    return {
      id: this.id,
      image_url: this.imageUrl,
      record_time: this.recordTime,
      temperature_c: this.temperatureC,
      soil_moisture_pct: this.soilMoisturePct,
      light_lux: this.lightLux,
      plot_id: this.plotId,
      is_abnormal: this.isAbnormal ? 1 : 0,
      abnormal_reason: this.abnormalReason,
    }
  }

  /**
   * 转换为前端格式
   */
  toFrontendFormat() {
    return {
      id: this.id,
      url: this.imageUrl, // 前端使用 url 字段名
      time: this.recordTime ? new Date(this.recordTime).toISOString() : null,
      temperatureC: this.temperatureC !== null ? parseFloat(this.temperatureC) : null,
      soilMoisturePct: this.soilMoisturePct !== null ? parseFloat(this.soilMoisturePct) : null,
      lightLux: this.lightLux !== null ? parseInt(this.lightLux) : null,
      plotId: this.plotId,
      isAbnormal: this.isAbnormal,
      abnormalReason: this.abnormalReason,
      createdAt: this.createdAt,
    }
  }

  /**
   * 验证数据有效性
   */
  validate() {
    const errors = []
    if (!this.imageUrl || this.imageUrl.trim() === '') {
      errors.push('图片URL不能为空')
    }
    if (!this.recordTime) {
      errors.push('记录时间不能为空')
    }
    return errors
  }
}

module.exports = Image

