<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { userAuthStore } from '@/stores/auth'

const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const email = ref('')
const error = ref('')
const loading = ref(false)
const registered = ref(false)

const isValidEmail = computed(() => {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return regex.test(email.value)
})

const register = async () => {
  error.value = ''
  loading.value = true

  if (!username.value || !email.value || !password.value || !confirmPassword.value) {
    error.value = 'Please fill in all fields'
    loading.value = false
    return
  }

  if (!isValidEmail.value) {
    error.value = 'email not valid'
    loading.value = false
    return
  }

  if (confirmPassword && confirmPassword.value !== password.value) {
    error.value = 'passwords do not match'
    loading.value = false
    return
  }
  try {
    const response = await userAuthStore.register({
      username: username.value,
      email: email.value,
      password: password.value,
      confirmPassword: confirmPassword.value,
    })

    if (response.status === 201) {
      registered.value = true
      router.push('/login')
    }
  } catch (err) {
    if (err.response && err.response.data && err.response.data.message) {
      error.value = err.response.data.message
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
        <div>
          <input
            v-model="email"
            placeholder="email"
            type="email"
            class="bg-[#fff] rounded-lg w-70 h-8 text-[#0A0F1E] text-center"
            @keyup.enter="register"
          />
          <p v-if="!isValidEmail && email" class="text-[#DC0B00] flex justify-center">
            email not valid
          </p>
        </div>
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
          :class="[
            'bg-[#00C853] text-xl font-semibold rounded-lg mt-8 min-w-30 min-h-9 cursor-pointer hover:scale-105 transition-all duration-300',
            loading ? 'animate-pulse' : '',
          ]"
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
