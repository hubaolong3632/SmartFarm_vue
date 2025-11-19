import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const token = ref(localStorage.getItem('jwt') || null)

  // 设置用户信息
  function setUser(userInfo) {
    user.value = userInfo
  }

  // 设置token
  function setToken(tokenValue) {
    token.value = tokenValue
    if (tokenValue) {
      localStorage.setItem('jwt', tokenValue)
    } else {
      localStorage.removeItem('jwt')
    }
  }

  // 清除用户信息
  function clearUser() {
    user.value = null
    token.value = null
    localStorage.removeItem('jwt')
  }

  // 检查是否已登录
  function isLoggedIn() {
    return !!token.value && !!user.value
  }

  // 初始化用户信息（从localStorage恢复）
  function initUser() {
    const savedToken = localStorage.getItem('jwt')
    if (savedToken) {
      token.value = savedToken
      // 可以在这里调用接口获取用户信息
    }
  }

  return {
    user,
    token,
    setUser,
    setToken,
    clearUser,
    isLoggedIn,
    initUser
  }
})

