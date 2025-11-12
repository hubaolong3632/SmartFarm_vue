/**
 * 地块配方分配模型
 * 对应表：plot_assignments
 */

class PlotAssignment {
  constructor(data = {}) {
    this.id = data.id || null
    this.plotId = data.plot_id || data.plotId || null
    this.recipeId = data.recipe_id || data.recipeId || null
    this.assignedAt = data.assigned_at || data.assignedAt || null
    this.isActive = data.is_active !== undefined ? data.is_active : (data.isActive !== undefined ? data.isActive : 1)
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
      assigned_at: this.assignedAt,
      is_active: this.isActive ? 1 : 0,
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
      assignedAt: this.assignedAt,
      isActive: this.isActive,
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
    return errors
  }
}

module.exports = PlotAssignment

