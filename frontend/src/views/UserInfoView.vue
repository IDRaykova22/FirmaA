<script setup lang="ts">
import UserInfo from '@/components/UserInfo.vue'

const props = defineProps<{
  name: string
  avatar?: string
  firstName: string
  lastName: string
  jobTitle: string
  dateOfBirth: number // Unix timestamp
  address: string
  phoneNumber: number
  joinDate: number // Unix timestamp
  salary: number
}>()

</script>

<template>
  <div class="flex justify-center">
    <UUser
        :name="props.name"
        :description="props.jobTitle"
        :avatar="{ src: props.avatar, alt: props.name }"
        size="3xl"
    />
  </div>

  <UserInfo
      name="John Doe"
      avatar="https://github.com/benjamincanac.png"
      first-name="John"
      last-name="Doe"
      job-title="Software Engineer"
      :date-of-birth="631152000000"
      address="123 Main St, Springfield"
      :phone-number="5551234567"
      :join-date="1672531200000"
      :salary="85000.5"
  />

</template>

<style scoped>

</style>