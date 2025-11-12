/**
 * 地块模型
 * 对应表：plots
 */

class Plot {
  constructor(data = {}) {
    this.id = data.id || null
    this.plotNumber = data.plot_number || data.plotNumber || null
    this.name = data.name || null
    this.status = data.status !== undefined ? data.status : 1
    this.createdAt = data.created_at || data.createdAt || null
    this.updatedAt = data.updated_at || data.updatedAt || null
  }

  /**
   * 转换为数据库字段格式
   */
  toDbFormat() {
    return {
      id: this.id,
      plot_number: this.plotNumber,
      name: this.name,
      status: this.status ? 1 : 0,
    }
  }

  /**
   * 转换为前端格式
   */
  toFrontendFormat() {
    return {
      id: this.id,
      plotNumber: this.plotNumber,
      name: this.name,
      status: this.status,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    }
  }

  /**
   * 验证数据有效性
   */
  validate() {
    const errors = []
    if (!this.plotNumber || this.plotNumber < 1) errors.push('地块编号必须大于0')
    return errors
  }
}

module.exports = Plot

