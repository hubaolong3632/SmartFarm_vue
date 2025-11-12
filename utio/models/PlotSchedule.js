/**
 * 地块定时执行计划模型
 * 对应表：plot_schedules
 */

class PlotSchedule {
  constructor(data = {}) {
    this.id = data.id || null
    this.plotId = data.plot_id || data.plotId || null
    this.recipeId = data.recipe_id || data.recipeId || null
    this.scheduleTime = data.schedule_time || data.scheduleTime || null
    this.executions = data.executions || 1
    this.isEnabled = data.is_enabled !== undefined ? data.is_enabled : (data.isEnabled !== undefined ? data.isEnabled : 1)
    this.createdAt = data.created_at || data.createdAt || null
    this.updatedAt = data.updated_at || data.updatedAt || null
  }

  /**
   * 转换为数据库字段格式
   */
  toDbFormat() {
    return {
      id: this.id,
      plot_id: this.plotId,
      recipe_id: this.recipeId,
      schedule_time: this.scheduleTime,
      executions: this.executions,
      is_enabled: this.isEnabled ? 1 : 0,
    }
  }

  /**
   * 转换为前端格式
   */
  toFrontendFormat() {
    return {
      id: this.id,
      plotId: this.plotId,
      recipeId: this.recipeId,
      timeHHmm: this.scheduleTime, // 前端使用 timeHHmm 字段名
      executions: parseInt(this.executions),
      isEnabled: this.isEnabled,
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    }
  }

  /**
   * 验证数据有效性
   */
  validate() {
    const errors = []
    if (!this.plotId) errors.push('地块ID不能为空')
    if (!this.recipeId) errors.push('配方ID不能为空')
    if (!this.scheduleTime) errors.push('执行时间不能为空')
    if (this.executions < 1) errors.push('执行次数必须大于0')
    // 验证时间格式 HH:mm
    if (this.scheduleTime && !/^([0-1][0-9]|2[0-3]):[0-5][0-9]$/.test(this.scheduleTime)) {
      errors.push('执行时间格式错误，应为 HH:mm')
    }
    return errors
  }
}

module.exports = PlotSchedule

