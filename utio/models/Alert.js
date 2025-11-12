/**
 * 报警记录模型
 * 对应表：alerts
 */

class Alert {
  constructor(data = {}) {
    this.id = data.id || null
    this.level = data.level || 'info'
    this.message = data.message || ''
    this.alertType = data.alert_type || data.alertType || null
    this.relatedData = data.related_data || data.relatedData || null
    this.isRead = data.is_read !== undefined ? data.is_read : (data.isRead !== undefined ? data.isRead : 0)
    this.createdAt = data.created_at || data.createdAt || null
  }

  /**
   * 转换为数据库字段格式
   */
  toDbFormat() {
    return {
      id: this.id,
      level: this.level,
      message: this.message,
      alert_type: this.alertType,
      related_data: this.relatedData ? JSON.stringify(this.relatedData) : null,
      is_read: this.isRead ? 1 : 0,
    }
  }

  /**
   * 转换为前端格式
   */
  toFrontendFormat() {
    return {
      id: this.id,
      level: this.level,
      message: this.message,
      alertType: this.alertType,
      relatedData: typeof this.relatedData === 'string' ? JSON.parse(this.relatedData) : this.relatedData,
      isRead: this.isRead,
      time: this.createdAt ? new Date(this.createdAt).toISOString() : null,
      createdAt: this.createdAt,
    }
  }

  /**
   * 验证数据有效性
   */
  validate() {
    const errors = []
    if (!['info', 'warning', 'error'].includes(this.level)) {
      errors.push('报警级别必须是 info、warning 或 error')
    }
    if (!this.message || this.message.trim() === '') {
      errors.push('报警消息不能为空')
    }
    return errors
  }
}

module.exports = Alert

