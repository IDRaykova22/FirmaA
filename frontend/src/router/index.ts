import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import UserInfoView from '../views/UserInfoView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/user-info',
      name: 'user-info',
      component: UserInfoView,
    },
  ],
})

export default router
