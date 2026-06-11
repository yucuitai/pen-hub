import axios from 'axios'
import { message } from 'ant-design-vue'

// 区分开发和生产环境
const isDev = import.meta.env.MODE === 'development'
const DEV_BASE_URL = 'http://localhost:8567/api'
const PROD_BASE_URL = 'http://118.190.107.1/api' // 或者你的域名

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: isDev ? DEV_BASE_URL : PROD_BASE_URL,
  timeout: 60000,
  withCredentials: true,
})

// //创建axios实例
// const myAxios = axios.create({
//   baseURL: 'http://localhost:8567/api',
//   timeout: 60000,
//   withCredentials: true, // 跨域请求时是否需要使用凭证cookie
// })

//全局响应拦截器
// 添加请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // 在发送请求之前做些什么
    return config
  },
  function (error) {
    // 对请求错误做些什么
    return Promise.reject(error)
  },
)

/**
 * 添加响应拦截器
 * 作用：统一处理后端返回的业务异常，尤其是登录状态校验
 */
myAxios.interceptors.response.use(
  function (response) {
    // 2xx 范围内的状态码都会触发该函数（HTTP层面成功）

    // 解构获取后端返回的业务数据
    const { data } = response

    // 检查业务状态码：40100 表示用户未登录或登录已失效
    if (data.code === 40100) {
      /**
       * 双重条件判断，防止无限跳转死循环：
       *
       * 1. !response.request.responseURL.includes('user/get/login')
       *    - 检查当前请求的URL是否是登录接口
       *    - 如果是登录接口本身返回401，不进行跳转（否则会重复调用登录接口）
       *
       * 2. !window.location.pathname.includes('login')
       *    - 检查当前页面是否已经在登录页
       *    - 如果已在登录页，不进行跳转（否则会无限循环跳转）
       *
       * 只有两个条件都满足时，才执行登录跳转
       */
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('login')
      ) {
        // 显示提示消息
        message.warning('请重新登录')
        // 跳转到登录页，并携带当前页面路径作为重定向参数
        // 登录成功后可根据 redirect 参数跳回原页面
        window.location.href = `/user/login?redirect=${window.location.pathname}`
      }
    }

    // 将响应原样返回，供后续业务逻辑使用
    return response
  },
  function (error) {
    // 超出 2xx 范围的状态码都会触发该函数（HTTP层面失败）
    // 对响应错误做些什么（如网络错误、服务器错误等）
    return Promise.reject(error)
  },
)

export default myAxios
