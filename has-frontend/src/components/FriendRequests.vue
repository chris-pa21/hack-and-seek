<script setup>
import FriendService from '@/services/FriendService'
import { ref, onMounted } from 'vue'

const friendRequests = ref([])
const loadingRefresh = ref(null)

const reloadRequests = async () => {
  loadingRefresh.value = true
  try {
    const response = await FriendService.getFriendRequests()
    friendRequests.value = response.data
  } catch (error) {
    // TODO
  } finally {
    loadingRefresh.value = false
  }
}

const acceptFriendRequest = async (requester) => {
  try {
    const response = await FriendService.acceptFriendRequest(requester)
  } catch (error) {
    // TODO
  } finally {
    reloadRequests()
  }
}

const refuseFriendRequest = async (requester) => {
  try {
    const response = await FriendService.refuseFriendRequest(requester)
    reloadRequests()
  } catch (error) {
    // TODO
  } finally {
    reloadRequests()
  }
}

onMounted(() => {
  reloadRequests()
})
</script>
<template>
  <div
    class="bg-[#1F2937] px-3 pt-2 pb-5 flex flex-col gap-y-4 items-center w-full min-w-[300px] max-w-[600px] max-h-[400px] rounded-xl text-2xl"
  >
    <div class="relative flex items-center justify-center w-full">
      <h2 class="font-semibold">Incoming Requests</h2>
      <button
        @click="reloadRequests"
        :class="[
          'absolute right-0.5 w-10 h-10 text-[#E0E6F0] px-2 pl-0.5 pr-1 rounded-lg text-2xl font-bold hover:bg-[#018EF4] transition-colors cursor-pointer',
          loadingRefresh ? 'animate-pulse' : '',
        ]"
      >
        &#10227;
      </button>
    </div>

    <div class="w-full overflow-y-auto flex flex-col gap-y-4">
      <div
        v-for="request in friendRequests"
        :key="request.username"
        class="flex items-center gap-x-4 px-4 py-2 border-b border-gray-700 w-full"
      >
        <span class="font-black text-[#E0E6F0] text-lg w-32 truncate">{{ request.username }}</span>
        <span class="hidden md:block text-[#9CA3AF] text-base w-20 text-right">{{
          request.points
        }}</span>
        <div class="flex items-center gap-x-2 ml-auto">
          <button
            @click="acceptFriendRequest(request.username)"
            class="bg-[#00C853] disabled:bg-[#005724] disabled:cursor-not-allowed cursor-pointer text-[#E0E6F0] px-4 py-1 rounded-lg text-sm font-semibold"
          >
            &check;
          </button>
          <button
            @click="refuseFriendRequest(request.username)"
            class="bg-[#DC0B00] disabled:bg-[#005724] disabled:cursor-not-allowed cursor-pointer text-[#E0E6F0] px-4 py-1 rounded-lg text-sm font-semibold"
          >
            &#10005;
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
<style scoped></style>
