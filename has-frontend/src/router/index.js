import DashboardView from '@/views/DashboardView.vue'
import GameDetailView from '@/views/GameDetailView.vue'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import { createRouter, createWebHistory } from 'vue-router'
import { userAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', component: HomeView, meta: { loggedIn: true } },
    { path: '/login', component: LoginView },
    { path: '/register', component: RegisterView },
    { path: '/dashboard', component: DashboardView, meta: { requiresAuth: true } },
    { path: '/game/:id', component: GameDetailView, meta: { requiresAuth: true } },

    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      meta: { catchAll: true },
    },
  ],
})

router.beforeEach(async (to, from, next) => {
  const authStore = userAuthStore()

  if (to.meta.catchAll) {
    await authStore.checkAuth()

    if (authStore.isAuthenticated) {
      next('/dashboard')
    } else {
      next('/login')
    }
  } else if (to.meta.requiresAuth) {
    await authStore.checkAuth()

    if (authStore.isAuthenticated) {
      next()
    } else {
      next('/login')
    }
  } else if (to.meta.loggedIn) {
    await authStore.checkAuth()
    if (authStore.isAuthenticated) {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router
