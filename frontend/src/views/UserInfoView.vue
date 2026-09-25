<script setup lang="ts">
import UserInfo from '@/components/UserInfo.vue'

const props = defineProps<{
  name: string
  avatar?: string
  firstName: string
  lastName: string
  jobTitle: string
  dateOfBirth: string // YYYYMMDD
  address: string
  phoneNumber: number
  joinDate: string // YYYYMMDD
  salary: number
}>()
</script>

<template>
  <main class="flex min-h-screen flex-col items-center justify-center gap-6 p-6">
    <UUser
        :name="props.name"
        :description="props.jobTitle"
        :avatar="{ src: props.avatar, alt: props.name }"
        size="3xl"
    />

    <UserInfo
        :user="{
          name: props.name,
          avatar: props.avatar,
          firstName: props.firstName,
          lastName: props.lastName,
          jobTitle: props.jobTitle,
          dateOfBirth: props.dateOfBirth,
          address: props.address,
          phoneNumber: props.phoneNumber,
          joinDate: props.joinDate,
          salary: props.salary
        }"
    />
  </main>
</template>

<style scoped>

</style>