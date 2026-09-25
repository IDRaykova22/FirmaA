<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@nuxt/ui/composables'
import { logout as doLogout, isAdmin, getUsername } from '@/api/auth'
import {
  fetchEmployees,
  createEmployee,
  fetchWorkstations,
  createWorkstation,
  fetchProjects,
  createProject,
  fetchMyInfo,
  markProjectDone,
} from '@/api/dashboard'

const router = useRouter()
const toast = useToast()

const admin = computed(() => isAdmin())
const username = computed(() => getUsername())

// ── Shared state ────────────────────────────────────────────────────
const loading = ref(true)

// ── Admin state ─────────────────────────────────────────────────────
const employees = ref<any[]>([])
const workstations = ref<any[]>([])
const projects = ref<any[]>([])

// ── Worker state ────────────────────────────────────────────────────
const myInfo = ref<any>(null)

// ── Modals ──────────────────────────────────────────────────────────
const showAddWorker = ref(false)
const showCreateWorkstation = ref(false)
const showCreateProject = ref(false)

// ── Form state ──────────────────────────────────────────────────────
const newWorker = ref({ username: '', password: '', jobTitle: '', address: '', dateOfBirth: '', salary: 0, phoneNumber: '' })
const newWorkstation = ref({ title: '', description: '', employeeIds: [] as number[] })
const newProject = ref({ title: '', description: '', dueDate: '', workstationIds: [] as number[] })

async function loadAdminData() {
  try {
    const [emps, wss, projs] = await Promise.all([fetchEmployees(), fetchWorkstations(), fetchProjects()])
    employees.value = emps
    workstations.value = wss
    projects.value = projs
  } catch (e) {
    toast.add({ title: 'Failed to load admin data', color: 'error', icon: 'i-lucide-circle-x' })
  }
}

async function loadWorkerData() {
  try {
    myInfo.value = await fetchMyInfo()
  } catch (e) {
    toast.add({ title: 'Failed to load your data', color: 'error', icon: 'i-lucide-circle-x' })
  }
}

onMounted(async () => {
  loading.value = true
  if (admin.value) {
    await loadAdminData()
  }
  await loadWorkerData()
  loading.value = false
})

function logout() {
  doLogout()
  router.push('/')
}

// ── Admin actions ───────────────────────────────────────────────────
async function submitWorker() {
  try {
    await createEmployee(newWorker.value)
    toast.add({ title: 'Worker created successfully!', color: 'success', icon: 'i-lucide-check' })
    showAddWorker.value = false
    newWorker.value = { username: '', password: '', jobTitle: '', address: '', dateOfBirth: '', salary: 0, phoneNumber: '' }
    await loadAdminData()
  } catch (e: any) {
    toast.add({ title: e.message || 'Failed to create worker', color: 'error', icon: 'i-lucide-circle-x' })
  }
}

async function submitWorkstation() {
  try {
    await createWorkstation(newWorkstation.value)
    toast.add({ title: 'Workstation created!', color: 'success', icon: 'i-lucide-check' })
    showCreateWorkstation.value = false
    newWorkstation.value = { title: '', description: '', employeeIds: [] }
    await loadAdminData()
  } catch (e: any) {
    toast.add({ title: e.message || 'Failed to create workstation', color: 'error', icon: 'i-lucide-circle-x' })
  }
}

async function submitProject() {
  try {
    await createProject(newProject.value)
    toast.add({ title: 'Project created!', color: 'success', icon: 'i-lucide-check' })
    showCreateProject.value = false
    newProject.value = { title: '', description: '', dueDate: '', workstationIds: [] }
    await loadAdminData()
  } catch (e: any) {
    toast.add({ title: e.message || 'Failed to create project', color: 'error', icon: 'i-lucide-circle-x' })
  }
}

async function handleMarkDone(projectId: number) {
  try {
    await markProjectDone(projectId)
    toast.add({ title: 'Project marked as done!', color: 'success', icon: 'i-lucide-check' })
    await loadWorkerData()
  } catch (e: any) {
    toast.add({ title: e.message || 'Failed', color: 'error', icon: 'i-lucide-circle-x' })
  }
}

// ── Manage Workstation Employees ────────────────────────────────────
const showManageEmployees = ref(false)
const manageWorkstationData = ref({ id: 0, employeeIds: [] as number[] })

function openManageEmployees(ws: any) {
  manageWorkstationData.value = {
    id: ws.id,
    employeeIds: ws.employees.map((e: any) => e.id)
  }
  showManageEmployees.value = true
}

import { assignEmployeesToWorkstation } from '@/api/dashboard'

async function submitManageEmployees() {
  try {
    await assignEmployeesToWorkstation(manageWorkstationData.value.id, manageWorkstationData.value.employeeIds)
    toast.add({ title: 'Employees updated!', color: 'success', icon: 'i-lucide-check' })
    showManageEmployees.value = false
    await loadAdminData()
  } catch (e: any) {
    toast.add({ title: e.message || 'Failed to update employees', color: 'error', icon: 'i-lucide-circle-x' })
  }
}

// Employee options for multi-selects
const employeeOptions = computed(() =>
  employees.value
    .filter((e: any) => e.role === 'ROLE_USER')
    .map((e: any) => ({ label: e.username, value: e.id }))
)

const workstationOptions = computed(() =>
  workstations.value.map((ws: any) => ({ label: ws.title, value: ws.id }))
)
</script>

<template>
  <main class="min-h-dvh bg-default">
    <!-- Top navbar -->
    <header class="sticky top-0 z-50 flex items-center justify-between border-b border-default bg-default/80 px-6 py-3 backdrop-blur-lg">
      <div class="flex items-center gap-3">
        <UIcon name="i-lucide-layout-dashboard" class="size-6 text-primary" />
        <h1 class="text-lg font-bold text-highlighted">TechDept Dashboard</h1>
        <UBadge v-if="admin" color="primary" variant="subtle" size="sm">Admin</UBadge>
      </div>
      <div class="flex items-center gap-4">
        <span class="text-sm text-muted">{{ username }}</span>
        <UButton color="error" variant="soft" icon="i-lucide-log-out" size="sm" @click="logout">
          Logout
        </UButton>
      </div>
    </header>

    <div v-if="loading" class="flex items-center justify-center py-32">
      <UIcon name="i-lucide-loader-2" class="size-8 animate-spin text-primary" />
    </div>

    <div v-else class="mx-auto max-w-7xl space-y-8 p-6">
      <!-- Welcome section -->
      <div>
        <p class="text-sm font-medium text-primary">
          {{ admin ? 'Administration overview' : 'Your workspace' }}
        </p>
        <h2 class="mt-1 text-3xl font-bold tracking-tight text-highlighted">
          Welcome, {{ username }}
        </h2>
      </div>

      <!-- ══ ADMIN SECTION ══════════════════════════════════════════════ -->
      <template v-if="admin">
        <!-- Quick actions -->
        <div class="grid gap-4 sm:grid-cols-3">
          <UButton block size="lg" icon="i-lucide-user-plus" color="primary" variant="soft" @click="showAddWorker = true">
            Add a Worker
          </UButton>
          <UButton block size="lg" icon="i-lucide-monitor-plus" color="primary" variant="soft" @click="showCreateWorkstation = true">
            Create Workstation
          </UButton>
          <UButton block size="lg" icon="i-lucide-folder-plus" color="primary" variant="soft" @click="showCreateProject = true">
            Create Project
          </UButton>
        </div>

        <!-- Stats cards -->
        <div class="grid gap-4 sm:grid-cols-3">
          <UPageCard title="Employees" icon="i-lucide-users">
            <p class="text-3xl font-semibold text-highlighted">{{ employees.length }}</p>
          </UPageCard>
          <UPageCard title="Workstations" icon="i-lucide-monitor">
            <p class="text-3xl font-semibold text-highlighted">{{ workstations.length }}</p>
          </UPageCard>
          <UPageCard title="Projects" icon="i-lucide-folder-kanban">
            <p class="text-3xl font-semibold text-highlighted">{{ projects.length }}</p>
          </UPageCard>
        </div>

        <!-- Employees list -->
        <section class="space-y-4">
          <h3 class="text-xl font-semibold text-highlighted">Employees</h3>
          <UPageCard v-if="employees.length === 0">
            <div class="flex flex-col items-center gap-2 py-6 text-center">
              <UIcon name="i-lucide-user-x" class="size-8 text-muted" />
              <p class="text-muted">No employees yet. Add your first worker above.</p>
            </div>
          </UPageCard>
          <div v-else class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <UCard v-for="emp in employees" :key="emp.id">
              <div class="flex items-center gap-3">
                <UIcon name="i-lucide-user" class="size-5 text-primary" />
                <div>
                  <p class="font-medium text-highlighted">{{ emp.username }}</p>
                  <p class="text-sm text-muted">{{ emp.jobTitle || 'No title' }}</p>
                </div>
                <UBadge v-if="emp.role === 'ROLE_ADMIN'" color="primary" variant="subtle" size="xs" class="ml-auto">Admin</UBadge>
              </div>
            </UCard>
          </div>
        </section>

        <!-- Workstations list -->
        <section class="space-y-4">
          <h3 class="text-xl font-semibold text-highlighted">Workstations</h3>
          <UPageCard v-if="workstations.length === 0">
            <div class="flex flex-col items-center gap-2 py-6 text-center">
              <UIcon name="i-lucide-monitor-x" class="size-8 text-muted" />
              <p class="text-muted">No workstations yet.</p>
            </div>
          </UPageCard>
          <div v-else class="grid gap-4 sm:grid-cols-2">
            <UPageCard v-for="ws in workstations" :key="ws.id" :title="ws.title" icon="i-lucide-monitor">
              <template #header-right>
                <UButton size="xs" color="primary" variant="soft" icon="i-lucide-users" @click="openManageEmployees(ws)">
                  Manage
                </UButton>
              </template>
              <p v-if="ws.description" class="mb-3 text-sm text-muted">{{ ws.description }}</p>
              <div class="flex flex-wrap gap-2">
                <UBadge v-for="emp in ws.employees" :key="emp.id" color="neutral" variant="subtle">
                  {{ emp.username }}
                </UBadge>
                <span v-if="ws.employees.length === 0" class="text-sm text-muted">No employees assigned</span>
              </div>
            </UPageCard>
          </div>
        </section>

        <!-- Projects list -->
        <section class="space-y-4">
          <h3 class="text-xl font-semibold text-highlighted">Projects</h3>
          <UPageCard v-if="projects.length === 0">
            <div class="flex flex-col items-center gap-2 py-6 text-center">
              <UIcon name="i-lucide-folder-x" class="size-8 text-muted" />
              <p class="text-muted">No projects yet.</p>
            </div>
          </UPageCard>
          <div v-else class="grid gap-4 sm:grid-cols-2">
            <UCard v-for="proj in projects" :key="proj.id">
              <div class="space-y-2">
                <div class="flex items-start justify-between">
                  <div class="flex items-center gap-2">
                    <UIcon name="i-lucide-folder-kanban" class="size-5 text-primary" />
                    <h4 class="font-semibold text-highlighted">{{ proj.title }}</h4>
                  </div>
                  <UBadge color="neutral" variant="subtle" size="xs">Due: {{ proj.dueDate }}</UBadge>
                </div>
                <p v-if="proj.description" class="text-sm text-muted">{{ proj.description }}</p>
                <div class="flex flex-wrap gap-1">
                  <UBadge v-for="ws in proj.workstations" :key="ws.id" color="primary" variant="subtle" size="xs">
                    {{ ws.title }}
                  </UBadge>
                </div>
                
                <div v-if="proj.completedBy && proj.completedBy.length > 0" class="pt-2 border-t border-default/50 mt-3">
                  <p class="text-xs font-semibold text-highlighted mb-1">Completed by:</p>
                  <div class="flex flex-col gap-1">
                    <div v-for="user in proj.completedBy" :key="user.id" class="flex items-center gap-1.5 text-xs text-success">
                      <UIcon name="i-lucide-check-circle-2" class="size-3.5" />
                      <span>{{ user.username }} е готов</span>
                    </div>
                  </div>
                </div>
              </div>
            </UCard>
          </div>
        </section>
      </template>

      <!-- ══ WORKER SECTION ═════════════════════════════════════════════ -->
      <template v-if="!admin && myInfo">
        <template v-if="myInfo.workstation">
          <section class="space-y-4">
            <h3 class="text-xl font-semibold text-highlighted">
              <UIcon name="i-lucide-monitor" class="inline size-5" />
              {{ myInfo.workstation.title }}
            </h3>
            <p v-if="myInfo.workstation.description" class="text-muted">{{ myInfo.workstation.description }}</p>

            <div v-if="myInfo.workstation.projects && myInfo.workstation.projects.length > 0" class="grid gap-4 sm:grid-cols-2">
              <UCard v-for="proj in myInfo.workstation.projects" :key="proj.id">
                <div class="space-y-3">
                  <div class="flex items-start justify-between">
                    <div class="flex items-center gap-2">
                      <UIcon name="i-lucide-folder-kanban" class="size-5 text-primary" />
                      <h4 class="font-semibold text-highlighted">{{ proj.title }}</h4>
                    </div>
                    <UBadge color="neutral" variant="subtle" size="xs">Due: {{ proj.dueDate }}</UBadge>
                  </div>
                  <p v-if="proj.description" class="text-sm text-muted">{{ proj.description }}</p>
                  <UButton
                    v-if="!proj.completedByMe"
                    color="success"
                    variant="soft"
                    icon="i-lucide-check-circle"
                    block
                    @click="handleMarkDone(proj.id)"
                  >
                    Done
                  </UButton>
                  <UBadge v-else color="success" variant="subtle" class="w-full justify-center py-1.5">
                    <UIcon name="i-lucide-check" class="size-4" /> Completed
                  </UBadge>
                </div>
              </UCard>
            </div>

            <UPageCard v-else>
              <div class="flex flex-col items-center gap-2 py-6 text-center">
                <UIcon name="i-lucide-folder-x" class="size-8 text-muted" />
                <p class="text-muted">No projects assigned to your workstation yet.</p>
              </div>
            </UPageCard>
          </section>
        </template>

        <UPageCard v-else>
          <div class="flex flex-col items-center gap-2 py-8 text-center">
            <UIcon name="i-lucide-monitor-x" class="size-8 text-muted" />
            <p class="font-medium text-highlighted">No workstation assigned</p>
            <p class="text-sm text-muted">Your workstation and projects will appear here once an admin assigns you.</p>
          </div>
        </UPageCard>
      </template>
    </div>

    <!-- ══ MODALS ══════════════════════════════════════════════════════ -->

    <!-- Add Worker Modal -->
    <UModal v-model:open="showAddWorker">
      <template #content>
        <div class="p-6 space-y-5">
          <h3 class="text-lg font-bold text-highlighted">Add a Worker</h3>
          <div class="space-y-4">
            <UFormField label="Username" required>
              <UInput v-model="newWorker.username" placeholder="Username" icon="i-lucide-user" class="w-full" />
            </UFormField>
            <UFormField label="Password" required>
              <UInput v-model="newWorker.password" type="password" placeholder="Password" icon="i-lucide-lock" class="w-full" />
            </UFormField>
            <UFormField label="Job Title">
              <UInput v-model="newWorker.jobTitle" placeholder="e.g. Frontend Developer" icon="i-lucide-briefcase" class="w-full" />
            </UFormField>
            <UFormField label="Address">
              <UInput v-model="newWorker.address" placeholder="Address" icon="i-lucide-map-pin" class="w-full" />
            </UFormField>
            <UFormField label="Date of Birth">
              <UInput v-model="newWorker.dateOfBirth" type="date" icon="i-lucide-cake" class="w-full" />
            </UFormField>
            <UFormField label="Salary">
              <UInput v-model.number="newWorker.salary" type="number" placeholder="0" icon="i-lucide-banknote" class="w-full" />
            </UFormField>
            <UFormField label="Phone Number">
              <UInput v-model="newWorker.phoneNumber" placeholder="+359..." icon="i-lucide-phone" class="w-full" />
            </UFormField>
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <UButton color="neutral" variant="ghost" @click="showAddWorker = false">Cancel</UButton>
            <UButton color="primary" @click="submitWorker" :disabled="!newWorker.username || !newWorker.password">Create Worker</UButton>
          </div>
        </div>
      </template>
    </UModal>

    <!-- Create Workstation Modal -->
    <UModal v-model:open="showCreateWorkstation">
      <template #content>
        <div class="p-6 space-y-5">
          <h3 class="text-lg font-bold text-highlighted">Create Workstation</h3>
          <div class="space-y-4">
            <UFormField label="Title" required>
              <UInput v-model="newWorkstation.title" placeholder="Workstation name" icon="i-lucide-monitor" class="w-full" />
            </UFormField>
            <UFormField label="Description">
              <UTextarea v-model="newWorkstation.description" placeholder="Optional description..." class="w-full" />
            </UFormField>
            <UFormField label="Employees">
              <USelectMenu
                v-model="newWorkstation.employeeIds"
                :items="employeeOptions"
                value-key="value"
                multiple
                placeholder="Select employees..."
                class="w-full"
              />
            </UFormField>
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <UButton color="neutral" variant="ghost" @click="showCreateWorkstation = false">Cancel</UButton>
            <UButton color="primary" @click="submitWorkstation" :disabled="!newWorkstation.title">Create Workstation</UButton>
          </div>
        </div>
      </template>
    </UModal>

    <!-- Create Project Modal -->
    <UModal v-model:open="showCreateProject">
      <template #content>
        <div class="p-6 space-y-5">
          <h3 class="text-lg font-bold text-highlighted">Create Project</h3>
          <div class="space-y-4">
            <UFormField label="Title" required>
              <UInput v-model="newProject.title" placeholder="Project name" icon="i-lucide-folder-kanban" class="w-full" />
            </UFormField>
            <UFormField label="Due Date" required>
              <UInput v-model="newProject.dueDate" type="date" icon="i-lucide-calendar" class="w-full" />
            </UFormField>
            <UFormField label="Description">
              <UTextarea v-model="newProject.description" placeholder="Optional description..." class="w-full" />
            </UFormField>
            <UFormField label="Workstations" required>
              <USelectMenu
                v-model="newProject.workstationIds"
                :items="workstationOptions"
                value-key="value"
                multiple
                placeholder="Select at least one workstation..."
                class="w-full"
              />
            </UFormField>
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <UButton color="neutral" variant="ghost" @click="showCreateProject = false">Cancel</UButton>
            <UButton color="primary" @click="submitProject" :disabled="!newProject.title || !newProject.dueDate || newProject.workstationIds.length === 0">Create Project</UButton>
          </div>
        </div>
      </template>
    </UModal>

    <!-- Manage Employees Modal -->
    <UModal v-model:open="showManageEmployees">
      <template #content>
        <div class="p-6 space-y-5">
          <h3 class="text-lg font-bold text-highlighted">Manage Workstation Employees</h3>
          <div class="space-y-4">
            <UFormField label="Employees">
              <USelectMenu
                v-model="manageWorkstationData.employeeIds"
                :items="employeeOptions"
                value-key="value"
                multiple
                placeholder="Select employees..."
                class="w-full"
              />
            </UFormField>
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <UButton color="neutral" variant="ghost" @click="showManageEmployees = false">Cancel</UButton>
            <UButton color="primary" @click="submitManageEmployees">Save Changes</UButton>
          </div>
        </div>
      </template>
    </UModal>
  </main>
</template>
