<script setup>
import Navbar from '@/components/Navbar.vue'
</script>

<template>
  <div class="min-h-screen flex flex-col justify-between">
    <Navbar />

    <div
      class="flex flex-col items-center text-center space-y-6 transition-all duration-300 flex-grow justify-center mt-[40px]"
    >
      <h1 class="text-5xl font-semibold sm:text-6xl transition-all duration-300">
        Capture The Flag
      </h1>
      <h2 class="text-4xl text-[#9CA3AF] sm:text-5xl transition-all duration-300">
        On another level
      </h2>

      <img
        src="@/assets/img/logo.png"
        class="max-w-[250px] sm:max-w-[300px] md:max-w-[400px] mt-6 select-none"
        draggable="false"
      />
    </div>

    <div
      class="hidden justify-center md:flex mx-[40px] md:space-x-20 lg:space-x-40 xl:space-x-50 2xl:space-x-60 transition-all mb-[4vh]"
    >
      <a
        href="#what"
        class="bg-[#1f2937] w-[275px] h-[130px] rounded-xl font-medium text-lg hover:scale-110 duration-300 hover:cursor-pointer flex items-center justify-center"
      >
        What is a CTF?
      </a>
      <a
        href="#how"
        class="bg-[#1f2937] w-[275px] h-[130px] text-center leading-tight px-4 rounded-xl font-medium text-lg hover:scale-110 duration-300 hover:cursor-pointer flex items-center justify-center"
      >
        How does Hack&Seek work?
      </a>
      <a
        @click="$router.push('/login')"
        class="bg-[#1f2937] w-[275px] h-[130px] rounded-xl font-medium text-lg hover:scale-110 duration-300 hover:cursor-pointer flex items-center justify-center"
      >
        GET STARTED
      </a>
    </div>
  </div>

  <div
    id="what"
    class="bg-[#1f2937] mt-[25px] flex items-center justify-center flex-col min-h-[40vh] p-10 gap-10 lg:flex-row sm:gap-20 lg:gap-40 xl:gap-50 2xl:gap-100 transition-all duration-300"
  >
    <h1 class="text-center font-semibold text-5xl flex justify-center">What is a CTF?</h1>
    <p class="text-[#9CA3AF] text-center lg:text-right max-w-125 text-md">
      A Capture The Flag (CTF) is a cybersecurity competition where players solve real-world hacking
      challenges to find hidden “flags.” These challenges test skills in cryptography, web
      exploitation, reverse engineering, and more. Participants earn points for each solved task,
      competing individually or in teams to learn, improve, and showcase their technical expertise.
    </p>
  </div>

  <div
    id="how"
    class="bg-[#0A0F1E] flex items-center justify-center flex-col-reverse min-h-[40vh] p-10 gap-10 lg:flex-row sm:gap-20 lg:gap-40 xl:gap-50 2xl:gap-100 transition-all duration-300"
  >
    <p class="text-[#9CA3AF] text-center lg:text-left max-w-125 text-md">
      A Capture The Flag (CTF) is a cybersecurity competition where players solve real-world hacking
      challenges to find hidden “flags.” These challenges test skills in cryptography, web
      exploitation, reverse engineering, and more. Participants earn points for each solved task,
      competing individually or in teams to learn, improve, and showcase their technical expertise.
    </p>
    <h1 class="text-center font-semibold text-5xl flex justify-center">How does Hack&Seek work?</h1>
  </div>
</template>

<style scoped></style>
