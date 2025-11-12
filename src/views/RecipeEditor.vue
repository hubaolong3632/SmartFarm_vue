<script setup>
import { ref, onMounted } from 'vue'
import { useGreenhouseStore } from '../stores/greenhouse'
const store = useGreenhouseStore()

onMounted(async () => {
  // 加载配方列表
  await store.loadRecipes()
})

const form = ref({
  name: '',
  waterMl: 0,
  nutrientMl: 0,
  rootingPowderMl: 0,
  specialMl: 0,
})

function reset() {
  form.value = { name: '', waterMl: 0, nutrientMl: 0, rootingPowderMl: 0, specialMl: 0 }
}

function add() {
  if (!form.value.name) return
  store.addRecipe({ ...form.value })
  reset()
}
</script>

<template>
  <el-row :gutter="16">
    <el-col :md="12" :xs="24">
      <el-card shadow="never">
        <template #header>编辑植物配方</template>
        <el-form label-width="120px">
          <el-form-item label="名称">
            <el-input v-model="form.name" placeholder="如：基础配方" />
          </el-form-item>
          <el-form-item label="水(ml)">
            <el-input-number v-model="form.waterMl" :min="0" :step="50" />
          </el-form-item>
          <el-form-item label="营养液(ml)">
            <el-input-number v-model="form.nutrientMl" :min="0" :step="10" />
          </el-form-item>
          <el-form-item label="生根粉(ml)">
            <el-input-number v-model="form.rootingPowderMl" :min="0" :step="5" />
          </el-form-item>
          <el-form-item label="特殊营养(ml)">
            <el-input-number v-model="form.specialMl" :min="0" :step="5" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="add">新增配方</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </el-col>
    <el-col :md="12" :xs="24">
      <el-card shadow="never">
        <template #header>配方列表</template>
        <el-table :data="store.recipes" border>
          <el-table-column prop="name" label="名称" />
          <el-table-column prop="waterMl" label="水(ml)" />
          <el-table-column prop="nutrientMl" label="营养液(ml)" />
          <el-table-column prop="rootingPowderMl" label="生根粉(ml)" />
          <el-table-column prop="specialMl" label="特殊营养(ml)" />
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-popconfirm title="确认删除该配方？" confirm-button-text="删除" cancel-button-text="取消" @confirm="store.removeRecipe(row.id)">
                <template #reference>
                  <el-button type="danger" size="small">删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped></style>


