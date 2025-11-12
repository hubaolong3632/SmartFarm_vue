/**
 * 配方服务
 * 提供配方的 CRUD 操作
 */

const Recipe = require('../models/Recipe')

class RecipeService {
  constructor(dbService) {
    this.db = dbService
  }

  /**
   * 创建配方
   * @param {Object} data - 配方数据
   * @returns {Promise<Recipe>}
   */
  async create(data) {
    const recipe = new Recipe(data)
    const errors = recipe.validate()
    if (errors.length > 0) {
      throw new Error(`数据验证失败: ${errors.join(', ')}`)
    }

    const dbData = recipe.toDbFormat()
    const sql = `INSERT INTO recipes (id, name, water_ml, nutrient_ml, rooting_powder_ml, special_ml) 
                 VALUES (?, ?, ?, ?, ?, ?)`
    const params = [
      dbData.id,
      dbData.name,
      dbData.water_ml,
      dbData.nutrient_ml,
      dbData.rooting_powder_ml,
      dbData.special_ml,
    ]

    await this.db.query(sql, params)
    return recipe
  }

  /**
   * 根据ID查询
   * @param {string} id - 配方ID
   * @returns {Promise<Recipe|null>}
   */
  async findById(id) {
    const sql = 'SELECT * FROM recipes WHERE id = ?'
    const rows = await this.db.query(sql, [id])
    if (rows.length === 0) return null
    return new Recipe(rows[0])
  }

  /**
   * 查询所有配方
   * @returns {Promise<Array<Recipe>>}
   */
  async findAll() {
    const sql = 'SELECT * FROM recipes ORDER BY created_at DESC'
    const rows = await this.db.query(sql)
    return rows.map(row => new Recipe(row))
  }

  /**
   * 更新配方
   * @param {string} id - 配方ID
   * @param {Object} data - 更新的数据
   * @returns {Promise<Recipe>}
   */
  async update(id, data) {
    const existing = await this.findById(id)
    if (!existing) {
      throw new Error('配方不存在')
    }

    const recipe = new Recipe({ ...existing.toDbFormat(), ...data })
    const errors = recipe.validate()
    if (errors.length > 0) {
      throw new Error(`数据验证失败: ${errors.join(', ')}`)
    }

    const dbData = recipe.toDbFormat()
    const sql = `UPDATE recipes 
                 SET name = ?, water_ml = ?, nutrient_ml = ?, rooting_powder_ml = ?, special_ml = ? 
                 WHERE id = ?`
    const params = [
      dbData.name,
      dbData.water_ml,
      dbData.nutrient_ml,
      dbData.rooting_powder_ml,
      dbData.special_ml,
      id,
    ]

    await this.db.query(sql, params)
    return recipe
  }

  /**
   * 删除配方
   * @param {string} id - 配方ID
   * @returns {Promise<boolean>}
   */
  async delete(id) {
    // 检查是否有地块正在使用此配方
    const checkSql = 'SELECT COUNT(*) as count FROM plot_assignments WHERE recipe_id = ? AND is_active = 1'
    const checkRows = await this.db.query(checkSql, [id])
    if (checkRows[0].count > 0) {
      throw new Error('该配方正在被使用，无法删除')
    }

    const sql = 'DELETE FROM recipes WHERE id = ?'
    const result = await this.db.query(sql, [id])
    return result.affectedRows > 0
  }
}

module.exports = RecipeService

