// plugins/axios.js
import axios from 'axios'

axios.defaults.baseURL = 'http://10.10.10.122:8080'
axios.defaults.withCredentials = true
axios.defaults.headers.common['Content-Type'] = 'application/json'

export default axios
