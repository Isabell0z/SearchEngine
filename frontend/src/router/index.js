import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '../views/Login.vue'
import SearchPage from '../views/Search.vue'
import RegisterPage from '../views/Register.vue'

const routes = [
  { path: '/', redirect: '/search' },
  { path: '/login', component: LoginPage },
  { path: '/search', component: SearchPage },
  { path: '/register', component: RegisterPage }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})


export default router
