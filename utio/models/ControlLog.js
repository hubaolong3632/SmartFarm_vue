/**
 * 控制操作日志模型
 * 对应表：control_logs
 */

class ControlLog {
  constructor(data = {}) {
    this.id = data.id || null
    this.controlType = data.control_type || data.controlType || ''
    this.action = data.action || ''
    this.status = data.status || 'success'
    this.message = data.message || null
    this.createdAt = data.created_at || data.createdAt || null
  }

  /**
   * 转换为数据库字段格式
   */
  toDbFormat() {
    return {
      id: this.id,
      control_type: this.controlType,
      action: this.action,
      status: this.status,
      message: this.message,
    }
  }

  /**
   * 转换为前端格式
   */
  toFrontendFormat() {
    return {
      id: this.id,
      controlType: this.controlType,
      action: this.action,
      status: this.status,
      message: this.message,
      createdAt: this.createdAt,
    }
  }

  /**
   * 验证数据有效性
   */
  validate() {
    const errors = []
    if (!this.controlType || this.controlType.trim() === '') {
      errors.push('控制类型不能为空')
    }
    if (!this.action || this.action.trim() === '') {
      errors.push('操作动作不能为空')
    }
    if (!['success', 'failed'].includes(this.status)) {
      errors.push('操作状态必须是 success 或 failed')
    }
    return errors
  }
}

module.exports = ControlLog

