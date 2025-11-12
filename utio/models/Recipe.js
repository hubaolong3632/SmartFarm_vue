/**
 * 配方模型
 * 对应表：recipes
 */

class Recipe {
  constructor(data = {}) {
    this.id = data.id || null
    this.name = data.name || ''
    this.waterMl = data.water_ml || data.waterMl || 0
    this.nutrientMl = data.nutrient_ml || data.nutrientMl || 0
    this.rootingPowderMl = data.rooting_powder_ml || data.rootingPowderMl || 0
    this.specialMl = data.special_ml || data.specialMl || 0
    this.createdAt = data.created_at || data.createdAt || null
    this.updatedAt = data.updated_at || data.updatedAt || null
  }

  /**
   * 转换为数据库字段格式
   */
  toDbFormat() {
    return {
      id: this.id,
      name: this.name,
      water_ml: this.waterMl,
      nutrient_ml: this.nutrientMl,
      rooting_powder_ml: this.rootingPowderMl,
      special_ml: this.specialMl,
    }
  }

  /**
   * 转换为前端格式
   */
  toFrontendFormat() {
    return {
      id: this.id,
      name: this.name,
      waterMl: parseInt(this.waterMl),
      nutrientMl: parseInt(this.nutrientMl),
      rootingPowderMl: parseInt(this.rootingPowderMl),
      specialMl: parseInt(this.specialMl),
      createdAt: this.createdAt,
      updatedAt: this.updatedAt,
    }
  }

  /**
   * 验证数据有效性
   */
  validate() {
    const errors = []
    if (!this.id) errors.push('配方ID不能为空')
    if (!this.name || this.name.trim() === '') errors.push('配方名称不能为空')
    if (this.waterMl < 0) errors.push('水量不能为负数')
    if (this.nutrientMl < 0) errors.push('营养液量不能为负数')
    if (this.rootingPowderMl < 0) errors.push('生根粉量不能为负数')
    if (this.specialMl < 0) errors.push('特殊营养量不能为负数')
    return errors
  }
}

module.exports = Recipe

