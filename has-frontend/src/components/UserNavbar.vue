<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { userAuthStore } from '@/stores/auth'
import UserService from '@/services/UserService'

const router = useRouter()
const auth = userAuthStore()
const menuOpen = ref(false)
const points = ref(0)

async function fetchPoints() {
  try {
    const response = await UserService.getMe()
    points.value = response.data.points ?? 0
  } catch (error) {
    console.error('Failed to fetch points:', error)
  }
}

onMounted(() => {
  fetchPoints()
})

function toggleMenu() {
  menuOpen.value = !menuOpen.value
  if (menuOpen.value) fetchPoints()
}

async function logout() {
  await auth.logout()
  router.push('/')
}
</script>

<template>
  <nav
    class="bg-[#1f2937] rounded-2xl pl-4 pr-8 mt-2 sm:mt-4 mx-10 md:mx-20 lg:mx-40 xl:mx-60 2xl:mx-80 font-bold transition-all duration-300"
  >
    <div class="flex justify-between items-center">
      <div class="flex items-center space-x-1 sm:space-x-4">
        <img class="cursor-pointer w-14 h-14" src="@/assets/img/logo.png" />
        <p class="cursor-pointer">Hack&Seek</p>
      </div>
      <div class="hidden sm:flex items-center space-x-4">
        <p>{{ auth.currentUser?.username }}</p>
        <p class="text-[#018EF4]">{{ points }} pts</p>
      </div>
      <div class="hidden sm:flex items-center">
        <button @click="logout" class="secondary-button ml-4">
          Logout
        </button>
      </div>
      <div @click="toggleMenu" class="cursor-pointer flex items-center sm:hidden">
        <div class="relative w-[25px] h-[25px] flex items-center justify-center">
          <div
            :class="menuOpen ? 'rotate-45' : '-translate-y-2'"
            class="w-[25px] h-[3px] bg-white absolute transition-all duration-300 rounded-md"
          ></div>
          <div
            :class="menuOpen ? 'opacity-0' : ''"
            class="w-[25px] h-[3px] bg-white absolute transition-all duration-300 rounded-md"
          ></div>
          <div
            :class="menuOpen ? '-rotate-45' : 'translate-y-2'"
            class="w-[25px] h-[3px] bg-white absolute transition-all duration-300 rounded-md"
          ></div>
        </div>
      </div>
    </div>

    <!-- Mobile Menu -->
    <div
      :class="menuOpen ? 'max-h-48 opacity-100 py-4' : 'max-h-0 opacity-0'"
      class="sm:hidden overflow-hidden transition-all duration-300 ease-in-out"
    >
      <div class="flex flex-col items-center space-y-4 border-t border-gray-600 pt-4">
        <p>
          {{ auth.currentUser?.username }} — <span class="text-[#018EF4]">{{ points }} pts</span>
        </p>
        <button
          @click="(logout(), toggleMenu())"
          class="secondary-button w-full"
        >
          Logout
        </button>
      </div>
    </div>
  </nav>
</template>

<style scoped></style>
