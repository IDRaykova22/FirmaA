<script setup lang="ts">
import { computed } from 'vue'

interface UserInfoProps {
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
}

const props = defineProps<UserInfoProps>()

const formatDate = (timestamp: number): string =>
    new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    }).format(new Date(timestamp))

const formatSalary = (value: number): string =>
    new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(value)

const fields = computed(() => [
  { label: 'First Name', value: props.firstName, icon: 'i-lucide-user' },
  { label: 'Last Name', value: props.lastName, icon: 'i-lucide-user' },
  { label: 'Job Title', value: props.jobTitle, icon: 'i-lucide-briefcase' },
  { label: 'Date of Birth', value: formatDate(props.dateOfBirth), icon: 'i-lucide-cake' },
  { label: 'Address', value: props.address, icon: 'i-lucide-map-pin' },
  { label: 'Phone Number', value: String(props.phoneNumber), icon: 'i-lucide-phone' },
  { label: 'Join Date', value: formatDate(props.joinDate), icon: 'i-lucide-calendar' },
  { label: 'Salary', value: formatSalary(props.salary), icon: 'i-lucide-banknote' }
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
