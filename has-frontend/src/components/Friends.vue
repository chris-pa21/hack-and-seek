<script setup>
import FriendService from '@/services/FriendService'
import { ref, onMounted } from 'vue'

const friends = ref([])
const loadingRefresh = ref(false)

const reloadRequests = async () => {
  loadingRefresh.value = true
  try {
    const response = await FriendService.getAllFriends()
    friends.value = response.data
  } catch (error) {
    console.error('Error loading friends:', error)
  } finally {
    loadingRefresh.value = false
  }
}

const deleteFriend = async (username) => {
  try {
    await FriendService.deleteFriend(username)
  } catch (error) {
    console.error('Error deleting friend:', error)
  } finally {
    await reloadRequests()
  }
}

onMounted(() => {
  reloadRequests()
})
</script>

<template>
  <div
    class="bg-[#1F2937] px-3 pt-2 pb-5 flex flex-col gap-y-4 items-center w-full max-w-[400px] lg:max-w-[600px] max-h-[600px] rounded-xl text-2xl"
  >
    <div class="relative flex items-center justify-center w-full">
      <h2 class="font-semibold">My Friends</h2>
      <button
        @click="reloadRequests"
        :disabled="loadingRefresh"
        :class="[
          'absolute right-0.5 w-10 h-10 text-[#E0E6F0] px-2 pl-0.5 pr-1 rounded-lg text-2xl font-bold hover:bg-[#018EF4] transition-colors cursor-pointer',
          loadingRefresh ? 'animate-spin' : '',
        ]"
      >
        &#10227;
      </button>
    </div>

    <div class="w-full overflow-y-auto flex flex-col gap-y-4">
      <div
        v-for="friend in friends"
        :key="friend.username"
        class="flex items-center gap-x-4 px-4 py-2 border-b border-gray-700 w-full"
      >
        <span class="font-black text-[#E0E6F0] text-lg w-32 truncate">{{ friend.username }}</span>
        <span class="hidden md:block text-[#9CA3AF] text-base w-20 text-right">
          {{ friend.points }}
        </span>
        <div class="flex items-center gap-x-2 ml-auto">
          <button
            @click="deleteFriend(friend.username)"
            class="danger-button-sm"
          >
            &#10005;
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped></style>
