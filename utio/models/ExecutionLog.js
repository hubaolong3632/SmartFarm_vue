/**
 * 执行日志模型
 * 对应表：execution_logs
 */

class ExecutionLog {
  constructor(data = {}) {
    this.id = data.id || null
    this.plotId = data.plot_id || data.plotId || null
    this.recipeId = data.recipe_id || data.recipeId || null
    this.executions = data.executions || 1
    this.executedAt = data.executed_at || data.executedAt || null
    this.executionType = data.execution_type || data.executionType || 'manual'
    this.scheduleId = data.schedule_id || data.scheduleId || null
    this.createdAt = data.created_at || data.createdAt || null
  }

  /**
   * 转换为数据库字段格式
   */
  toDbFormat() {
    return {
      id: this.id,
      plot_id: this.plotId,
      recipe_id: this.recipeId,
      executions: this.executions,
      executed_at: this.executedAt,
      execution_type: this.executionType,
      schedule_id: this.scheduleId,
    }
  }

  /**
   * 转换为前端格式
   */
  toFrontendFormat() {
    return {
      id: this.id,
      plot: this.plotId, // 前端使用 plot 字段名
      recipeId: this.recipeId,
      executions: parseInt(this.executions),
      time: this.executedAt ? new Date(this.executedAt).toISOString() : null,
      executionType: this.executionType,
      scheduleId: this.scheduleId,
      createdAt: this.createdAt,
    }
  }

  /**
   * 验证数据有效性
   */
  validate() {
    const errors = []
    if (!this.plotId) errors.push('地块ID不能为空')
    if (!this.recipeId) errors.push('配方ID不能为空')
    if (this.executions < 1) errors.push('执行次数必须大于0')
    if (!['manual', 'scheduled'].includes(this.executionType)) {
      errors.push('执行类型必须是 manual 或 scheduled')
    }
    return errors
  }
}

module.exports = ExecutionLog

