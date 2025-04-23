import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import { createPinia } from 'pinia'

const pinia = createPinia()  // 创建 Pinia 实例

// 只需要一次 .use(pinia)
createApp(App)
  .use(pinia)  // 注册 Pinia 实例
  .use(router)
  .use(ElementPlus)
  .mount('#app')