// 网络请求工具类
// 封装 axios 用于 Vue 3 项目，提供统一的 API 调用接口

import axios from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const service = axios.create({
  baseURL: 'http://localhost:10002/api', // 后端 API 基础路径
  // baseURL: 'https://smartfarmservice.00000.work/api', // 后端 API 基础路径
  timeout: 10000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 从 localStorage 获取 token（如果有）
    const token = localStorage.getItem('jwt')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 处理业务错误
    if (res.code < 0) {
      ElMessage.error(res.data || res.message || '请求失败')
      return Promise.reject(new Error(res.data || res.message || '请求失败'))
    }
    
    // Token 失效处理
    if (res.code === 11111) {
      console.log('Token 失效，跳转登录页')
      localStorage.removeItem('jwt')
      return Promise.reject(new Error('Token 失效'))
    }
    
    // 返回业务数据
    return res.data !== undefined ? res.data : res
  },
  error => {
    console.error('响应错误:', error)
    // 对于 400 错误，只在控制台输出，不弹出提示（避免频繁提示）
    if (error.response?.status === 400) {
      console.warn('请求参数错误:', error.response?.data?.message || error.message)
    } else {
      // 其他错误才弹出提示
      const message = error.response?.data?.message || error.message || '请求失败，请重试'
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

// 导出请求方法
export default {
  /**
   * GET 请求
   * @param {string} url - 接口路径
   * @param {object} params - 请求参数
   * @returns {Promise}
   */
  async get(url, params = {}) {
    try {
      return await service.get(url, { params })
    } catch (error) {
      console.error(`GET 请求异常[${url}]:`, error)
      return null
    }
  },

  /**
   * POST 请求
   * @param {string} url - 接口路径
   * @param {object} data - 请求体
   * @returns {Promise}
   */
  async post(url, data = {}) {
    try {
      return await service.post(url, data)
    } catch (error) {
      console.error(`POST 请求异常[${url}]:`, error)
      return null
    }
  },

  /**
   * PUT 请求
   * @param {string} url - 接口路径
   * @param {object} data - 请求体
   * @returns {Promise}
   */
  async put(url, data = {}) {
    try {
      return await service.put(url, data)
    } catch (error) {
      console.error(`PUT 请求异常[${url}]:`, error)
      return null
    }
  },

  /**
   * DELETE 请求
   * @param {string} url - 接口路径
   * @param {object} params - 请求参数
   * @returns {Promise}
   */
  async delete(url, params = {}) {
    try {
      return await service.delete(url, { params })
    } catch (error) {
      console.error(`DELETE 请求异常[${url}]:`, error)
      return null
    }
  }
}

