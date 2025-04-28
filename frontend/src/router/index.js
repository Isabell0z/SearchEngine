import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '../views/Login.vue'
import SearchPage from '../views/Search.vue'
import RegisterPage from '../views/Register.vue'
import StartPage from '../views/Start.vue'

const routes = [
  { path: '/', redirect: '/start' },
  { path: '/login', component: LoginPage },
  { path: '/search', component: SearchPage },
  { path: '/register', component: RegisterPage },
  { path: '/start', component: StartPage }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})


export default router
