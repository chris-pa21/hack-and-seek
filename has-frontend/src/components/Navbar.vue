<script setup>
import { ref } from 'vue'
const menuOpen = ref(false)
function toggleMenu() {
  menuOpen.value = !menuOpen.value
}
</script>

<template>
  <nav
    class="bg-[#1f2937] rounded-2xl pl-4 pr-8 mt-2 sm:mt-4 mx-10 md:mx-20 lg:mx-40 xl:mx-60 2xl:mx-80 font-bold transition-all duration-300"
  >
    <div class="flex justify-between">
      <div class="flex items-center space-x-1 sm:space-x-4">
        <img class="cursor-pointer w-14 h-14" src="@/assets/img/logo.png" />
        <p class="cursor-pointer">Hack&Seek</p>
      </div>
      <div class="hidden items-center space-x-4 sm:flex">
        <button
          @click="$router.push('/login')"
          class="secondary-button"
        >
          Login
        </button>
        <button
          @click="$router.push('/register')"
          class="secondary-button"
        >
          Register
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

    <!-- Mobile Menu Card -->
    <div
      :class="menuOpen ? 'max-h-48 opacity-100 py-4' : 'max-h-0 opacity-0'"
      class="sm:hidden overflow-hidden transition-all duration-300 ease-in-out"
    >
      <div class="flex flex-col items-center space-y-4 border-t border-gray-600 pt-4">
        <p
          @click="($router.push('/login'), toggleMenu())"
          class="cursor-pointer hover:text-[#018EF4] transition-all duration-200"
        >
          Login
        </p>
        <p
          @click="($router.push('/register'), toggleMenu())"
          class="cursor-pointer hover:text-[#018EF4] transition-all duration-200"
        >
          Register
        </p>
      </div>
    </div>
  </nav>
</template>

<style scoped></style>
