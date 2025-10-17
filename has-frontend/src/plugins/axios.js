// plugins/axios.js
import axios from 'axios'

axios.defaults.baseURL = 'http://localhost:8080' 
axios.defaults.withCredentials = true 
axios.defaults.headers.common['Content-Type'] = 'application/json'

axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.user = null
      authStore.isAuthenticated = false
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default axios