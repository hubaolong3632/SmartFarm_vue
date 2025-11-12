// 应用入口：挂载 Vue、注册路由、状态管理和 UI 框架
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 全局状态管理（Pinia）
app.use(createPinia())
// 路由（页面导航）
app.use(router)
// UI 组件库（Element Plus）
app.use(ElementPlus)

// 挂载到 DOM
app.mount('#app')
