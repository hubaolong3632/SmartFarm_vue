<script setup>
import { ref, onMounted, computed } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
import { ElMessage } from 'element-plus'
import {
  Document as DocumentIcon,
  Plus as PlusIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Refresh as RefreshIcon,
  SuccessFilled as WaterIcon,
  InfoFilled as NutrientIcon,
  WarningFilled as RootingIcon,
  Operation as SpecialIcon
} from '@element-plus/icons-vue'

const store = useGreenhouseStore()

onMounted(() => {
  // 异步加载配方列表，不阻塞界面渲染
  store.loadRecipes().catch(err => console.error('加载配方失败:', err))
})

const form = ref({
  name: '',
  waterMl: 0,
  nutrientMl: 0,
  rootingPowderMl: 0,
  specialMl: 0,
})

const editingId = ref(null)

function reset() {
  form.value = { name: '', waterMl: 0, nutrientMl: 0, rootingPowderMl: 0, specialMl: 0 }
  editingId.value = null
}

function add() {
  if (!form.value.name) {
    ElMessage.warning('请输入配方名称')
    return
  }
  if (editingId.value) {
    store.updateRecipe(editingId.value, { ...form.value })
    ElMessage.success('配方更新成功')
  } else {
    store.addRecipe({ ...form.value })
    ElMessage.success('配方添加成功')
  }
  reset()
}

function edit(recipe) {
  form.value = {
    name: recipe.name,
    waterMl: recipe.waterMl || 0,
    nutrientMl: recipe.nutrientMl || 0,
    rootingPowderMl: recipe.rootingPowderMl || 0,
    specialMl: recipe.specialMl || 0,
  }
  editingId.value = recipe.id
  // 滚动到表单区域
  document.querySelector('.form-card')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function remove(id) {
  store.removeRecipe(id)
  ElMessage.success('配方删除成功')
}

// 计算总配方数和总用量
const stats = computed(() => {
  const recipes = store.recipes || []
  const totalWater = recipes.reduce((sum, r) => sum + (r.waterMl || 0), 0)
  const totalNutrient = recipes.reduce((sum, r) => sum + (r.nutrientMl || 0), 0)
  return {
    count: recipes.length,
    totalWater,
    totalNutrient
  }
})

// 计算配方总量
function getTotalVolume(recipe) {
  return (recipe.waterMl || 0) + (recipe.nutrientMl || 0) + (recipe.rootingPowderMl || 0) + (recipe.specialMl || 0)
}
</script>

<template>
  <div class="recipe-container">
    <!-- 顶部标题和统计 -->
    <div class="recipe-header">
      <div class="header-content">
        <div class="header-title">
          <el-icon class="title-icon"><DocumentIcon /></el-icon>
          <h2>植物营养配方管理</h2>
        </div>
        <div class="header-stats">
          <div class="stat-item">
            <span class="stat-label">配方总数</span>
            <span class="stat-value">{{ stats.count }}</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">总水量</span>
            <span class="stat-value">{{ stats.totalWater }}ml</span>
          </div>
          <div class="stat-item">
            <span class="stat-label">总营养液</span>
            <span class="stat-value">{{ stats.totalNutrient }}ml</span>
          </div>
        </div>
      </div>
    </div>

    <el-row :gutter="20">
      <!-- 左侧：编辑表单 -->
      <el-col :md="12" :xs="24">
        <el-card class="form-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><PlusIcon /></el-icon>
              <span>{{ editingId ? '编辑配方' : '新增配方' }}</span>
            </div>
          </template>
          
          <el-form label-width="140px" class="recipe-form">
            <el-form-item label="配方名称" required>
              <el-input 
                v-model="form.name" 
                placeholder="请输入配方名称，如：基础配方"
                size="large"
                clearable
              />
            </el-form-item>
            
            <el-form-item label="水">
              <div class="input-with-icon">
                <el-icon class="input-icon"><WaterIcon /></el-icon>
                <el-input-number 
                  v-model="form.waterMl" 
                  :min="0" 
                  :step="50"
                  size="large"
                  style="width: 100%;"
                  controls-position="right"
                />
                <span class="input-unit">ml</span>
              </div>
            </el-form-item>
            
            <el-form-item label="营养液">
              <div class="input-with-icon">
                <el-icon class="input-icon"><NutrientIcon /></el-icon>
                <el-input-number 
                  v-model="form.nutrientMl" 
                  :min="0" 
                  :step="10"
                  size="large"
                  style="width: 100%;"
                  controls-position="right"
                />
                <span class="input-unit">ml</span>
              </div>
            </el-form-item>
            
            <el-form-item label="生根粉">
              <div class="input-with-icon">
                <el-icon class="input-icon"><RootingIcon /></el-icon>
                <el-input-number 
                  v-model="form.rootingPowderMl" 
                  :min="0" 
                  :step="5"
                  size="large"
                  style="width: 100%;"
                  controls-position="right"
                />
                <span class="input-unit">ml</span>
              </div>
            </el-form-item>
            
            <el-form-item label="特殊营养">
              <div class="input-with-icon">
                <el-icon class="input-icon"><SpecialIcon /></el-icon>
                <el-input-number 
                  v-model="form.specialMl" 
                  :min="0" 
                  :step="5"
                  size="large"
                  style="width: 100%;"
                  controls-position="right"
                />
                <span class="input-unit">ml</span>
              </div>
            </el-form-item>

            <!-- 总量显示 -->
            <el-form-item label="配方总量">
              <div class="total-volume">
                <span class="total-value">{{ getTotalVolume(form) }} ml</span>
              </div>
            </el-form-item>
            
            <el-form-item>
              <el-button 
                type="primary" 
                size="large"
                :icon="editingId ? EditIcon : PlusIcon"
                @click="add"
                style="width: 100%;"
              >
                {{ editingId ? '更新配方' : '新增配方' }}
              </el-button>
              <el-button 
                size="large"
                :icon="RefreshIcon"
                @click="reset"
                style="width: 100%; margin-top: 12px;"
              >
                重置
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 右侧：配方列表 -->
      <el-col :md="12" :xs="24">
        <el-card class="list-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-icon><DocumentIcon /></el-icon>
              <span>配方列表</span>
              <el-tag type="info" size="small" style="margin-left: auto;">
                共 {{ stats.count }} 个配方
              </el-tag>
            </div>
          </template>

          <div v-if="store.recipes.length === 0" class="empty-state">
            <el-empty description="暂无配方数据" :image-size="100">
              <el-button type="primary" @click="reset">创建第一个配方</el-button>
            </el-empty>
          </div>

          <div v-else class="recipe-list">
            <div 
              v-for="recipe in store.recipes" 
              :key="recipe.id"
              class="recipe-item"
              :class="{ 'editing': editingId === recipe.id }"
            >
              <div class="recipe-header-item">
                <div class="recipe-name">
                  <el-icon class="recipe-icon"><DocumentIcon /></el-icon>
                  <span class="name-text">{{ recipe.name }}</span>
                </div>
                <div class="recipe-actions">
                  <el-button 
                    type="primary" 
                    size="small" 
                    :icon="EditIcon"
                    text
                    @click="edit(recipe)"
                  >
                    编辑
                  </el-button>
                  <el-popconfirm 
                    title="确认删除该配方？" 
                    confirm-button-text="删除" 
                    cancel-button-text="取消" 
                    @confirm="remove(recipe.id)"
                  >
                    <template #reference>
                      <el-button 
                        type="danger" 
                        size="small" 
                        :icon="DeleteIcon"
                        text
                      >
                        删除
                      </el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>

              <div class="recipe-content">
                <div class="ingredient-row">
                  <div class="ingredient-item">
                    <el-icon class="ingredient-icon" style="color: #3b82f6;"><WaterIcon /></el-icon>
                    <div class="ingredient-info">
                      <span class="ingredient-label">水</span>
                      <span class="ingredient-value">{{ recipe.waterMl || 0 }} ml</span>
                    </div>
                  </div>
                  <div class="ingredient-item">
                    <el-icon class="ingredient-icon" style="color: #10b981;"><NutrientIcon /></el-icon>
                    <div class="ingredient-info">
                      <span class="ingredient-label">营养液</span>
                      <span class="ingredient-value">{{ recipe.nutrientMl || 0 }} ml</span>
                    </div>
                  </div>
                </div>
                <div class="ingredient-row">
                  <div class="ingredient-item">
                    <el-icon class="ingredient-icon" style="color: #f59e0b;"><RootingIcon /></el-icon>
                    <div class="ingredient-info">
                      <span class="ingredient-label">生根粉</span>
                      <span class="ingredient-value">{{ recipe.rootingPowderMl || 0 }} ml</span>
                    </div>
                  </div>
                  <div class="ingredient-item">
                    <el-icon class="ingredient-icon" style="color: #8b5cf6;"><SpecialIcon /></el-icon>
                    <div class="ingredient-info">
                      <span class="ingredient-label">特殊营养</span>
                      <span class="ingredient-value">{{ recipe.specialMl || 0 }} ml</span>
                    </div>
                  </div>
                </div>
                <div class="recipe-footer">
                  <div class="total-info">
                    <span class="total-label">总量：</span>
                    <span class="total-amount">{{ getTotalVolume(recipe) }} ml</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.recipe-container {
  padding: 0;
}

.recipe-header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
}

.title-icon {
  font-size: 32px;
}

.header-title h2 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: white;
}

.header-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.3);
}

.stat-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: white;
}

.form-card,
.list-card {
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
}

.form-card:hover,
.list-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1) !important;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.card-header .el-icon {
  font-size: 18px;
  color: #6366f1;
}

.recipe-form {
  padding: 8px 0;
}

.input-with-icon {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}

.input-icon {
  font-size: 20px;
  color: #6366f1;
  flex-shrink: 0;
}

.input-unit {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  min-width: 35px;
}

.total-volume {
  padding: 12px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 8px;
  border: 1px solid #bae6fd;
}

.total-value {
  font-size: 18px;
  font-weight: 700;
  color: #0369a1;
}

.empty-state {
  padding: 40px 0;
  text-align: center;
}

.recipe-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-height: 800px;
  overflow-y: auto;
  padding-right: 4px;
}

.recipe-item {
  padding: 20px;
  background: linear-gradient(to bottom, #ffffff 0%, #f9fafb 100%);
  border-radius: 12px;
  border: 2px solid #e5e7eb;
  transition: all 0.3s ease;
}

.recipe-item:hover {
  border-color: #6366f1;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.1);
  transform: translateY(-2px);
}

.recipe-item.editing {
  border-color: #6366f1;
  background: linear-gradient(to bottom, #eef2ff 0%, #e0e7ff 100%);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.2);
}

.recipe-header-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.recipe-name {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
}

.recipe-icon {
  font-size: 20px;
  color: #6366f1;
}

.name-text {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.recipe-actions {
  display: flex;
  gap: 8px;
}

.recipe-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ingredient-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.ingredient-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: white;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  transition: all 0.2s ease;
}

.ingredient-item:hover {
  border-color: #6366f1;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.1);
}

.ingredient-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.ingredient-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
}

.ingredient-label {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.2;
}

.ingredient-value {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.2;
}

.recipe-footer {
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
}

.total-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #f0f9ff 0%, #e0f2fe 100%);
  border-radius: 20px;
  border: 1px solid #bae6fd;
}

.total-label {
  font-size: 13px;
  color: #0369a1;
  font-weight: 500;
}

.total-amount {
  font-size: 15px;
  font-weight: 700;
  color: #0369a1;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .header-stats {
    width: 100%;
    justify-content: space-around;
  }

  .ingredient-row {
    grid-template-columns: 1fr;
  }

  .recipe-header-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .recipe-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
