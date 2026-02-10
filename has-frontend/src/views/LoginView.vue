<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = userAuthStore()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

const login = async () => {
  error.value = ''
  loading.value = true

  if (!username.value || !password.value) {
    error.value = 'Please fill in all fields'
    loading.value = false
    return
  }

  try {
    const response = await authStore.login({
      username: username.value,
      password: password.value,
    })

    if (response.status === 200) {
      router.push('/dashboard')
    }
  } catch (err) {
    console.error('Login error:', err)
    if (err.response?.status === 401) {
      error.value = 'Wrong username and/or password'
    } else {
      error.value = 'An error occurred'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex justify-center pt-5">
    <h1 class="font-semibold text-6xl">Login</h1>
  </div>

  <div v-if="error" class="flex justify-center mt-4 pt-4">
    <div class="bg-[#DC0B00] text-white px-4 py-2 rounded-lg w-[280px] text-center">
      {{ error }}
    </div>
  </div>

  <div class="flex justify-center mt-8">
    <div class="flex justify-center bg-[#1f2937] rounded-2xl py-10 w-[450px] mx-4">
      <form
        @submit.prevent="login()"
        class="flex justify-items-center justify-center items-center flex-col gap-5"
      >
        <input
          v-model="username"
          placeholder="username"
          class="bg-[#fff] rounded-lg w-70 h-8 text-[#0A0F1E] text-center"
          @keyup.enter="login"
        />
        <input
          v-model="password"
          placeholder="password"
          type="password"
          class="bg-[#fff] rounded-lg w-70 h-8 text-[#0A0F1E] text-center"
          @keydown.enter="login"
        />
        <button
          type="button"
          @click="login()"
          :class="[
            'bg-[#018EF4] text-xl font-semibold rounded-lg mt-8 min-w-30 min-h-9 cursor-pointer hover:scale-105 transition-all duration-300',
            loading ? 'animate-pulse' : '',
          ]"
        >
          login
        </button>
        <p
          @click="router.push('/register')"
          class="text-[#9CA3AF] underline text-sm cursor-pointer"
        >
          create account
        </p>
      </form>
    </div>
  </div>
</template>

<style scoped></style>
