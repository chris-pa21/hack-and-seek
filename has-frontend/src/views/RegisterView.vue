<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { userAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = userAuthStore() // ✅ Anderer Name!

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const error = ref('')
const loading = ref(false)
const registered = ref(false)

const register = async () => {
  error.value = ''
  loading.value = true

  if (!username.value || !password.value || !confirmPassword.value) {
    error.value = 'Please fill in all fields'
    loading.value = false
    return
  }

  if (confirmPassword && confirmPassword.value !== password.value) {
    error.value = 'passwords do not match'
    loading.value = false
    return
  }

  try {
    const response = await authStore.register({
      username: username.value,
      password: password.value,
      confirmPassword: confirmPassword.value,
    })

    if (response.status === 201) {
      registered.value = true
      setTimeout(() => {
        router.push('/login')
      }, 3000)
    }
  } catch (err) {
    if (err.response?.status === 400) {
      error.value = 'Invalid input data'
    } else if (err.response?.status === 409) {
      error.value = 'User already exists'
    } else {
      error.value = 'Registration failed'
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="flex justify-center pt-5">
    <h1 class="flex justify-center text-center font-semibold text-6xl">Create Account</h1>
  </div>

  <div v-if="registered" class="flex justify-center mt-4 pt-4">
    <div class="bg-[#00C853] text-white px-4 py-2 rounded-lg w-[280px] text-center">
      Registration successfull. You will be redirected in 3 seconds.
    </div>
  </div>
  <div v-if="error" class="flex justify-center mt-4 pt-4">
    <div class="bg-[#DC0B00] text-white px-4 py-2 rounded-lg w-[280px] text-center">
      {{ error }}
    </div>
  </div>

  <div class="flex justify-center mt-8">
    <div class="flex justify-center bg-[#1f2937] rounded-2xl py-10 w-[450px] mx-4">
      <form
        @submit.prevent="register()"
        class="flex justify-items-center justify-center items-center flex-col gap-5"
      >
        <input
          v-model="username"
          placeholder="username"
          type="username"
          class="bg-[#fff] rounded-lg w-70 h-8 text-[#0A0F1E] text-center"
          @keyup.enter="register"
        />
        <input
          v-model="password"
          placeholder="password"
          type="password"
          class="bg-[#fff] rounded-lg w-70 h-8 text-[#0A0F1E] text-center"
          @keydown.enter="register"
        />
        <input
          v-model="confirmPassword"
          placeholder="confirm password"
          type="password"
          class="bg-[#fff] rounded-lg w-70 h-8 text-[#0A0F1E] text-center"
          @keydown.enter="register"
        />
        <p v-if="confirmPassword && password !== confirmPassword" class="text-red-600">
          Passwords do not match
        </p>

        <button
          type="button"
          @click="register()"
          :class="['primary-button mt-8', loading ? 'animate-pulse' : '']"
        >
          register
        </button>
        <p @click="router.push('/login')" class="text-[#9CA3AF] underline text-sm cursor-pointer">
          already have an account?
        </p>
      </form>
    </div>
  </div>
</template>

<style scoped></style>
