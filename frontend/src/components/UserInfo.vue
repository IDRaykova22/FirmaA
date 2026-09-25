<script setup lang="ts">
import { computed } from 'vue'

interface User {
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
}

const props = defineProps<{
  user: User
}>()

const formatDate = (date: string): string =>
    new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
    }).format(new Date(date))

const formatSalary = (value: number): string =>
    new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(value)

const fields = computed(() => [
  { label: 'First Name', value: props.user.firstName, icon: 'i-lucide-user' },
  { label: 'Last Name', value: props.user.lastName, icon: 'i-lucide-user' },
  { label: 'Job Title', value: props.user.jobTitle, icon: 'i-lucide-briefcase' },
  { label: 'Date of Birth', value: formatDate(props.user.dateOfBirth), icon: 'i-lucide-cake' },
  { label: 'Address', value: props.user.address, icon: 'i-lucide-map-pin' },
  { label: 'Phone Number', value: String(props.user.phoneNumber), icon: 'i-lucide-phone' },
  { label: 'Join Date', value: formatDate(props.user.joinDate), icon: 'i-lucide-calendar' },
  { label: 'Salary', value: formatSalary(props.user.salary), icon: 'i-lucide-banknote' }
])
</script>

<template>
  <div class="mx-auto flex max-w-3xl flex-col gap-6 p-6">
    <!-- Info container -->
    <UCard>
      <template #header>
        <h2 class="text-lg font-semibold text-highlighted">
          User Information
        </h2>
      </template>

      <dl class="grid grid-cols-1 gap-6 sm:grid-cols-2">
        <div
            v-for="field in fields"
            :key="field.label"
            class="flex items-start gap-3"
        >
          <UIcon :name="field.icon" class="mt-0.5 size-5 shrink-0 text-primary" />
          <div class="min-w-0">
            <dt class="text-sm text-muted">
              {{ field.label }}
            </dt>
            <dd class="break-words font-medium text-highlighted">
              {{ field.value }}
            </dd>
          </div>
        </div>
      </dl>
    </UCard>
  </div>
</template>
