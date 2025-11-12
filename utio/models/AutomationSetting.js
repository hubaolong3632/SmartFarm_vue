/**
 * 自动化设置模型
 * 对应表：automation_settings
 */

class AutomationSetting {
  constructor(data = {}) {
    this.id = data.id || null
    this.settingKey = data.setting_key || data.settingKey || ''
    this.settingValue = data.setting_value || data.settingValue || ''
    this.description = data.description || null
    this.createdAt = data.created_at || data.createdAt || null
    this.updatedAt = data.updated_at || data.updatedAt || null
  }

  /**
   * 转换为数据库字段格式
   */
  toDbFormat() {
    return {
      id: this.id,
      setting_key: this.settingKey,
      setting_value: this.settingValue,
      description: this.description,
    }
  }

  /**
   * 转换为前端格式
   */
  toFrontendFormat() {
    // 尝试解析 JSON 值
    let parsedValue = this.settingValue
    try {
      parsedValue = JSON.parse(this.settingValue)
    } catch (e) {
      // 如果不是 JSON，保持原值
    }

    return {
      id: this.id,
      key: this.settingKey,
      value: parsedValue,
      description: this.description,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    }
  }

  /**
   * 验证数据有效性
   */
  validate() {
    const errors = []
    if (!this.settingKey || this.settingKey.trim() === '') {
      errors.push('设置键名不能为空')
    }
    if (this.settingValue === null || this.settingValue === undefined) {
      errors.push('设置值不能为空')
    }
    return errors
  }
}

module.exports = AutomationSetting

