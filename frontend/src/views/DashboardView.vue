<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useToast } from '@nuxt/ui/composables'
import { isAdmin, getUsername } from '@/api/auth'
import AppHeader from '@/components/AppHeader.vue'
import {
  fetchEmployees,
  createEmployee,
  fetchWorkstations,
  createWorkstation,
  fetchProjects,
  createProject,
  updateEmployee,
  deleteEmployee,
  updateWorkstation,
  deleteWorkstation,
  updateProject,
  deleteProject,
  fetchMyInfo,
  markProjectDone,
} from '@/api/dashboard'

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
const emptyWorker = () => ({
  username: '',
  password: '',
  firstName: '',
  lastName: '',
  role: 'ROLE_USER',
  department: '',
  jobTitle: '',
  address: '',
  dateOfBirth: '',
  salary: '' as number | string | null,
  phoneNumber: '',
})
const emptyWorkstation = () => ({ title: '', description: '', computerCount: 0 as number | string, employeeIds: [] as number[] })
const emptyProject = () => ({ title: '', description: '', dueDate: '', projectValue: '' as number | string | null, workstationIds: [] as number[] })

const roleOptions = [
  { label: 'Employee', value: 'ROLE_USER', icon: 'i-lucide-user' },
  { label: 'Manager', value: 'ROLE_ADMIN', icon: 'i-lucide-user-cog' },
]

// Money is stored and shown in EUR
const eurFormat = new Intl.NumberFormat('en-IE', { style: 'currency', currency: 'EUR', maximumFractionDigits: 2 })
function eur(value: number | string | null | undefined) {
  return value === null || value === undefined || value === '' ? '—' : eurFormat.format(Number(value))
}

function fullName(person: any) {
  return `${person.firstName ?? ''} ${person.lastName ?? ''}`.trim() || person.username
}

const isManager = (person: any) => person.role === 'ROLE_ADMIN'

const newWorker = ref(emptyWorker())
const newWorkstation = ref(emptyWorkstation())
const newProject = ref(emptyProject())

// Mirrors the backend checks in AdminController so mistakes surface before submit
const MIN_SALARY = 620.2 // Bulgarian national minimum wage (EUR/month, 2026)
const workerErrors = computed(() => {
  const w = newWorker.value
  const errors: Record<string, string> = {}
  if (w.username && w.username.trim().length < 3) errors.username = 'At least 3 characters'
  if (w.password && !/^(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/.test(w.password)) {
    errors.password = 'At least 8 characters, with a number and a special character'
  }
  if (w.salary !== '' && w.salary !== null && Number(w.salary) < MIN_SALARY) {
    errors.salary = `Cannot be below the minimum wage (${eur(MIN_SALARY)})`
  }
  if (w.phoneNumber) {
    const digits = w.phoneNumber.replace(/\D/g, '')
    if (!(digits.length === 10 || (digits.length === 12 && digits.startsWith('359')))) {
      errors.phoneNumber = 'Must be 10 digits (or +359 followed by 9 digits)'
    }
  }
  if (w.dateOfBirth) {
    const age = Math.floor((Date.now() - new Date(w.dateOfBirth).getTime()) / (365.25 * 24 * 3600 * 1000))
    if (age < 18) errors.dateOfBirth = 'Worker must be at least 18 years old'
  }
  return errors
})

const workerFormIncomplete = computed(() => {
  const w = newWorker.value
  return !w.username || !w.firstName.trim() || !w.lastName.trim()
    || (editingWorkerId.value === null && !w.password)
    || (w.role === 'ROLE_ADMIN' && !w.department.trim())
})

// When set, the matching modal edits that record instead of creating a new one
const editingWorkerId = ref<number | null>(null)
const editingWorkstationId = ref<number | null>(null)
const editingProjectId = ref<number | null>(null)

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

// ── Admin actions ───────────────────────────────────────────────────
function openAddWorker() {
  // Keep an unfinished draft, but don't carry over fields from an edit
  if (editingWorkerId.value !== null) newWorker.value = emptyWorker()
  editingWorkerId.value = null
  showAddWorker.value = true
}

// Renaming yourself would invalidate the username inside your current JWT
const editingSelf = computed(() =>
  editingWorkerId.value !== null &&
  employees.value.find((e: any) => e.id === editingWorkerId.value)?.username === username.value
)

function openEditWorker(emp: any) {
  editingWorkerId.value = emp.id
  newWorker.value = {
    username: emp.username,
    password: '',
    firstName: emp.firstName,
    lastName: emp.lastName,
    role: emp.role,
    department: emp.department,
    jobTitle: emp.jobTitle,
    address: emp.address,
    dateOfBirth: emp.dateOfBirth,
    salary: emp.salary ?? '',
    phoneNumber: emp.phoneNumber,
  }
  showAddWorker.value = true
}

async function submitWorker() {
  const editing = editingWorkerId.value !== null
  try {
    if (editing) {
      await updateEmployee(editingWorkerId.value!, newWorker.value)
    } else {
      await createEmployee(newWorker.value)
    }
    const kind = newWorker.value.role === 'ROLE_ADMIN' ? 'Manager' : 'Employee'
    toast.add({ title: editing ? `${kind} updated!` : `${kind} created successfully!`, color: 'success', icon: 'i-lucide-check' })
    showAddWorker.value = false
    editingWorkerId.value = null
    newWorker.value = emptyWorker()
    await loadAdminData()
  } catch (e: any) {
    toast.add({ title: e.message || `Failed to ${editing ? 'update' : 'create'} worker`, color: 'error', icon: 'i-lucide-circle-x' })
  }
}

function openCreateWorkstation() {
  if (editingWorkstationId.value !== null) newWorkstation.value = emptyWorkstation()
  editingWorkstationId.value = null
  showCreateWorkstation.value = true
}

function openEditWorkstation(ws: any) {
  editingWorkstationId.value = ws.id
  newWorkstation.value = {
    title: ws.title,
    description: ws.description,
    computerCount: ws.computerCount,
    employeeIds: ws.employees.map((e: any) => e.id),
  }
  showCreateWorkstation.value = true
}

async function submitWorkstation() {
  const editing = editingWorkstationId.value !== null
  try {
    if (editing) {
      await updateWorkstation(editingWorkstationId.value!, newWorkstation.value)
    } else {
      await createWorkstation(newWorkstation.value)
    }
    toast.add({ title: editing ? 'Workstation updated!' : 'Workstation created!', color: 'success', icon: 'i-lucide-check' })
    showCreateWorkstation.value = false
    editingWorkstationId.value = null
    newWorkstation.value = emptyWorkstation()
    await loadAdminData()
  } catch (e: any) {
    toast.add({ title: e.message || `Failed to ${editing ? 'update' : 'create'} workstation`, color: 'error', icon: 'i-lucide-circle-x' })
  }
}

function openCreateProject() {
  if (editingProjectId.value !== null) newProject.value = emptyProject()
  editingProjectId.value = null
  showCreateProject.value = true
}

function openEditProject(proj: any) {
  editingProjectId.value = proj.id
  newProject.value = {
    title: proj.title,
    description: proj.description,
    dueDate: proj.dueDate,
    projectValue: proj.projectValue ?? '',
    workstationIds: proj.workstations.map((ws: any) => ws.id),
  }
  showCreateProject.value = true
}

async function submitProject() {
  const editing = editingProjectId.value !== null
  try {
    if (editing) {
      await updateProject(editingProjectId.value!, newProject.value)
    } else {
      await createProject(newProject.value)
    }
    toast.add({ title: editing ? 'Project updated!' : 'Project created!', color: 'success', icon: 'i-lucide-check' })
    showCreateProject.value = false
    editingProjectId.value = null
    newProject.value = emptyProject()
    await loadAdminData()
  } catch (e: any) {
    toast.add({ title: e.message || `Failed to ${editing ? 'update' : 'create'} project`, color: 'error', icon: 'i-lucide-circle-x' })
  }
}

// ── Employee profile panel ──────────────────────────────────────────
const profileId = ref<number | null>(null)

const showProfile = computed({
  get: () => profileId.value !== null,
  set: (open: boolean) => {
    if (!open) profileId.value = null
  },
})

// Looked up by id so the panel reflects fresh data after an edit
const profile = computed(() => employees.value.find((e: any) => e.id === profileId.value) ?? null)

const profileWorkstation = computed(() =>
  profile.value ? workstations.value.find((ws: any) => ws.employees.some((e: any) => e.id === profile.value.id)) ?? null : null
)

const profileProjects = computed(() => {
  const ws = profileWorkstation.value
  if (!ws || !profile.value) return []
  return projects.value
    .filter((p: any) => p.workstations.some((w: any) => w.id === ws.id))
    .map((p: any) => ({ ...p, done: p.completedBy.some((u: any) => u.id === profile.value.id) }))
})

const profileDoneCount = computed(() => profileProjects.value.filter((p: any) => p.done).length)

function ageFrom(dateOfBirth: string) {
  const dob = new Date(dateOfBirth)
  const now = new Date()
  let age = now.getFullYear() - dob.getFullYear()
  if (now.getMonth() < dob.getMonth() || (now.getMonth() === dob.getMonth() && now.getDate() < dob.getDate())) age--
  return age
}

const profileDetails = computed(() => {
  const p = profile.value
  if (!p) return []
  return [
    ...(isManager(p) ? [{ icon: 'i-lucide-building-2', label: 'Department', value: p.department }] : []),
    { icon: 'i-lucide-briefcase', label: 'Job title', value: p.jobTitle },
    { icon: 'i-lucide-phone', label: 'Phone', value: p.phoneNumber },
    { icon: 'i-lucide-map-pin', label: 'Address', value: p.address },
    { icon: 'i-lucide-cake', label: 'Date of birth', value: p.dateOfBirth ? `${p.dateOfBirth} (${ageFrom(p.dateOfBirth)} y.)` : '' },
    { icon: 'i-lucide-banknote', label: 'Salary', value: p.salary != null && Number(p.salary) > 0 ? eur(p.salary) : '' },
    { icon: 'i-lucide-calendar-plus', label: 'Joined', value: p.joinDate },
  ]
})

function openProfile(emp: any) {
  profileId.value = emp.id
}

function editFromProfile() {
  const emp = profile.value
  profileId.value = null
  if (emp) openEditWorker(emp)
}

function initials(name: string) {
  return name.slice(0, 2).toUpperCase()
}

// ── Delete confirmation ─────────────────────────────────────────────
type DeleteKind = 'worker' | 'workstation' | 'project'
const deleteTarget = ref<{ kind: DeleteKind; id: number; name: string } | null>(null)
const deleting = ref(false)

const showDeleteConfirm = computed({
  get: () => deleteTarget.value !== null,
  set: (open: boolean) => {
    if (!open) deleteTarget.value = null
  },
})

const deleteHints: Record<DeleteKind, string> = {
  worker: 'Their project completions will also be removed.',
  workstation: 'Its employees will be unassigned and it will be removed from its projects.',
  project: 'All completion records for this project will be removed.',
}

function askDelete(kind: DeleteKind, id: number, name: string) {
  deleteTarget.value = { kind, id, name }
}

async function confirmDelete() {
  const target = deleteTarget.value
  if (!target) return
  deleting.value = true
  try {
    if (target.kind === 'worker') await deleteEmployee(target.id)
    else if (target.kind === 'workstation') await deleteWorkstation(target.id)
    else await deleteProject(target.id)
    toast.add({ title: `Deleted ${target.name}`, color: 'success', icon: 'i-lucide-check' })
    deleteTarget.value = null
    await loadAdminData()
  } catch (e: any) {
    toast.add({ title: e.message || `Failed to delete ${target.kind}`, color: 'error', icon: 'i-lucide-circle-x' })
  } finally {
    deleting.value = false
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
const managers = computed(() => employees.value.filter(isManager))
const staff = computed(() => employees.value.filter((e: any) => !isManager(e)))

const employeeOptions = computed(() =>
  employees.value
    .filter((e: any) => e.role === 'ROLE_USER')
    .map((e: any) => ({ label: `${fullName(e)} (${e.username})`, value: e.id }))
)

const workstationOptions = computed(() =>
  workstations.value.map((ws: any) => ({ label: ws.title, value: ws.id }))
)
</script>

<template>
  <!-- overflow-x-clip (not hidden) so the sticky header keeps working -->
  <main class="relative min-h-dvh overflow-x-clip bg-default">
    <!-- Soft green glow, same as on the login page -->
    <div
      aria-hidden="true"
      class="pointer-events-none absolute top-0 left-1/2 h-[28rem] w-[56rem] max-w-full -translate-x-1/2 -translate-y-1/2 rounded-full bg-primary/15 blur-3xl"
    />

    <!-- Top navbar -->
    <AppHeader title="Dashboard" icon="i-lucide-layout-dashboard" />

    <div v-if="loading" class="relative flex items-center justify-center py-32">
      <UIcon name="i-lucide-loader-2" class="size-8 animate-spin text-primary" />
    </div>

    <div v-else class="relative mx-auto max-w-7xl space-y-8 p-6">
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
          <UButton block size="lg" icon="i-lucide-user-plus" color="primary" variant="soft" @click="openAddWorker">
            Add Employee / Manager
          </UButton>
          <UButton block size="lg" icon="i-lucide-monitor-plus" color="primary" variant="soft" @click="openCreateWorkstation">
            Create Workstation
          </UButton>
          <UButton block size="lg" icon="i-lucide-folder-plus" color="primary" variant="soft" @click="openCreateProject">
            Create Project
          </UButton>
        </div>

        <!-- Stats cards -->
        <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <UPageCard title="Employees" icon="i-lucide-users">
            <p class="text-3xl font-semibold text-highlighted">{{ staff.length }}</p>
          </UPageCard>
          <UPageCard title="Managers" icon="i-lucide-user-cog">
            <p class="text-3xl font-semibold text-highlighted">{{ managers.length }}</p>
          </UPageCard>
          <UPageCard title="Workstations" icon="i-lucide-monitor">
            <p class="text-3xl font-semibold text-highlighted">{{ workstations.length }}</p>
          </UPageCard>
          <UPageCard title="Projects" icon="i-lucide-folder-kanban">
            <p class="text-3xl font-semibold text-highlighted">{{ projects.length }}</p>
          </UPageCard>
        </div>

        <!-- Managers and employees lists -->
        <section
          v-for="group in [
            { key: 'managers', title: 'Managers', people: managers, empty: 'No managers yet.' },
            { key: 'staff', title: 'Employees', people: staff, empty: 'No employees yet. Add your first one above.' },
          ]"
          :key="group.key"
          class="space-y-4"
        >
          <h3 class="text-xl font-semibold text-highlighted">{{ group.title }}</h3>
          <UPageCard v-if="group.people.length === 0">
            <div class="flex flex-col items-center gap-2 py-6 text-center">
              <UIcon name="i-lucide-user-x" class="size-8 text-muted" />
              <p class="text-muted">{{ group.empty }}</p>
            </div>
          </UPageCard>
          <div v-else class="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            <UCard v-for="emp in group.people" :key="emp.id" class="transition-colors hover:ring-primary/40">
              <div class="flex items-center gap-3">
                <button
                  type="button"
                  class="group flex min-w-0 flex-1 items-center gap-3 rounded-md text-left focus-visible:outline-2 focus-visible:outline-primary"
                  :aria-label="`View profile of ${fullName(emp)}`"
                  @click="openProfile(emp)"
                >
                  <UAvatar :text="initials(fullName(emp))" size="sm" :ui="{ root: 'bg-primary/15', fallback: 'text-primary font-semibold' }" />
                  <div class="min-w-0">
                    <p class="truncate font-medium text-highlighted group-hover:text-primary">{{ fullName(emp) }}</p>
                    <p class="truncate text-sm text-muted">
                      <span class="font-mono text-xs">#{{ emp.id }}</span>
                      · {{ isManager(emp) ? emp.department || 'No department' : emp.jobTitle || 'No title' }}
                    </p>
                  </div>
                </button>
                <div class="flex shrink-0 items-center gap-1">
                  <UButton size="xs" color="neutral" variant="ghost" icon="i-lucide-pencil" :aria-label="`Edit ${fullName(emp)}`" @click="openEditWorker(emp)" />
                  <UButton
                    v-if="emp.username !== username"
                    size="xs"
                    color="error"
                    variant="ghost"
                    icon="i-lucide-trash-2"
                    :aria-label="`Delete ${fullName(emp)}`"
                    @click="askDelete('worker', emp.id, fullName(emp))"
                  />
                </div>
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
            <UCard v-for="ws in workstations" :key="ws.id">
              <div class="space-y-3">
                <div class="flex items-start justify-between gap-2">
                  <div class="flex min-w-0 items-center gap-2">
                    <UIcon name="i-lucide-monitor" class="size-5 shrink-0 text-primary" />
                    <h4 class="truncate font-semibold text-highlighted">{{ ws.title }}</h4>
                    <UBadge color="primary" variant="outline" size="xs" class="shrink-0 font-mono">№{{ ws.id }}</UBadge>
                  </div>
                  <div class="flex shrink-0 items-center gap-1">
                    <UButton size="xs" color="primary" variant="soft" icon="i-lucide-users" @click="openManageEmployees(ws)">
                      Manage
                    </UButton>
                    <UButton size="xs" color="neutral" variant="ghost" icon="i-lucide-pencil" :aria-label="`Edit ${ws.title}`" @click="openEditWorkstation(ws)" />
                    <UButton size="xs" color="error" variant="ghost" icon="i-lucide-trash-2" :aria-label="`Delete ${ws.title}`" @click="askDelete('workstation', ws.id, ws.title)" />
                  </div>
                </div>
                <p v-if="ws.description" class="text-sm text-muted">{{ ws.description }}</p>
                <div class="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted">
                  <span class="flex items-center gap-1.5">
                    <UIcon name="i-lucide-computer" class="size-4" />
                    {{ ws.computerCount }} computer{{ ws.computerCount === 1 ? '' : 's' }}
                  </span>
                  <span class="flex items-center gap-1.5">
                    <UIcon name="i-lucide-users" class="size-4" />
                    {{ ws.employees.length }} employee{{ ws.employees.length === 1 ? '' : 's' }}
                  </span>
                </div>
                <div class="flex flex-wrap gap-2">
                  <UBadge v-for="emp in ws.employees" :key="emp.id" color="neutral" variant="subtle">
                    {{ fullName(emp) }}
                  </UBadge>
                  <span v-if="ws.employees.length === 0" class="text-sm text-muted">No employees assigned</span>
                </div>
              </div>
            </UCard>
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
                <div class="flex items-start justify-between gap-2">
                  <div class="flex min-w-0 items-center gap-2">
                    <UIcon name="i-lucide-folder-kanban" class="size-5 shrink-0 text-primary" />
                    <h4 class="truncate font-semibold text-highlighted">{{ proj.title }}</h4>
                    <UBadge color="primary" variant="outline" size="xs" class="shrink-0 font-mono">#{{ proj.id }}</UBadge>
                  </div>
                  <div class="flex shrink-0 items-center gap-1">
                    <UBadge color="neutral" variant="subtle" size="xs">Due: {{ proj.dueDate }}</UBadge>
                    <UButton size="xs" color="neutral" variant="ghost" icon="i-lucide-pencil" :aria-label="`Edit ${proj.title}`" @click="openEditProject(proj)" />
                    <UButton size="xs" color="error" variant="ghost" icon="i-lucide-trash-2" :aria-label="`Delete ${proj.title}`" @click="askDelete('project', proj.id, proj.title)" />
                  </div>
                </div>
                <p v-if="proj.description" class="text-sm text-muted">{{ proj.description }}</p>
                <div class="flex flex-wrap gap-x-4 gap-y-1 text-sm text-muted">
                  <span class="flex items-center gap-1.5">
                    <UIcon name="i-lucide-euro" class="size-4" />
                    <span class="font-medium text-highlighted tabular-nums">{{ eur(proj.projectValue) }}</span>
                  </span>
                  <span class="flex items-center gap-1.5">
                    <UIcon name="i-lucide-users" class="size-4" />
                    {{ proj.employeeCount }} employee{{ proj.employeeCount === 1 ? '' : 's' }}
                  </span>
                </div>
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
            <p class="flex items-center gap-3 text-sm text-muted">
              <span class="font-mono">№{{ myInfo.workstation.id }}</span>
              <span class="flex items-center gap-1.5">
                <UIcon name="i-lucide-computer" class="size-4" />
                {{ myInfo.workstation.computerCount }} computer{{ myInfo.workstation.computerCount === 1 ? '' : 's' }}
              </span>
            </p>
            <p v-if="myInfo.workstation.description" class="text-muted">{{ myInfo.workstation.description }}</p>

            <div v-if="myInfo.workstation.projects && myInfo.workstation.projects.length > 0" class="grid gap-4 sm:grid-cols-2">
              <UCard v-for="proj in myInfo.workstation.projects" :key="proj.id">
                <div class="space-y-3">
                  <div class="flex items-start justify-between">
                    <div class="flex items-center gap-2">
                      <UIcon name="i-lucide-folder-kanban" class="size-5 text-primary" />
                      <h4 class="font-semibold text-highlighted">{{ proj.title }}</h4>
                      <UBadge color="primary" variant="outline" size="xs" class="font-mono">#{{ proj.id }}</UBadge>
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
        <!-- The modal is capped at the viewport height, so only the fields scroll
             while the title and buttons stay put -->
        <div class="flex min-h-0 flex-1 flex-col">
          <h3 class="border-b border-default px-6 py-4 text-lg font-bold text-highlighted">
            {{ editingWorkerId !== null ? `Edit ${newWorker.role === 'ROLE_ADMIN' ? 'Manager' : 'Employee'}` : 'Add Employee / Manager' }}
          </h3>
          <div class="min-h-0 flex-1 space-y-4 overflow-y-auto px-6 py-5">
            <UFormField label="Role" required :help="editingSelf ? 'You cannot change your own role.' : newWorker.role === 'ROLE_ADMIN' ? 'Managers can log in to this admin dashboard.' : undefined">
              <USelect v-model="newWorker.role" :items="roleOptions" value-key="value" class="w-full" :disabled="editingSelf" />
            </UFormField>
            <div class="grid gap-4 sm:grid-cols-2">
              <UFormField label="First Name" required>
                <UInput v-model="newWorker.firstName" placeholder="First name" class="w-full" />
              </UFormField>
              <UFormField label="Last Name" required>
                <UInput v-model="newWorker.lastName" placeholder="Last name" class="w-full" />
              </UFormField>
            </div>
            <UFormField v-if="newWorker.role === 'ROLE_ADMIN'" label="Department" required>
              <UInput v-model="newWorker.department" placeholder="e.g. Технологично отделение" icon="i-lucide-building-2" class="w-full" />
            </UFormField>
            <UFormField label="Username" required :error="workerErrors.username" :help="editingSelf ? 'You cannot rename the account you are logged in with.' : undefined">
              <UInput v-model="newWorker.username" placeholder="Username" icon="i-lucide-user" class="w-full" :disabled="editingSelf" />
            </UFormField>
            <UFormField label="Password" :required="editingWorkerId === null" :error="workerErrors.password" :help="editingWorkerId !== null ? 'Leave blank to keep the current password.' : undefined">
              <UInput v-model="newWorker.password" type="password" :placeholder="editingWorkerId !== null ? 'New password (optional)' : 'Password'" icon="i-lucide-lock" class="w-full" />
            </UFormField>
            <UFormField label="Job Title">
              <UInput v-model="newWorker.jobTitle" placeholder="e.g. Frontend Developer" icon="i-lucide-briefcase" class="w-full" />
            </UFormField>
            <UFormField label="Address">
              <UInput v-model="newWorker.address" placeholder="Address" icon="i-lucide-map-pin" class="w-full" />
            </UFormField>
            <UFormField label="Date of Birth" :error="workerErrors.dateOfBirth">
              <UInput v-model="newWorker.dateOfBirth" type="date" icon="i-lucide-cake" class="w-full" />
            </UFormField>
            <UFormField label="Monthly Salary (EUR)" :error="workerErrors.salary">
              <UInput v-model.number="newWorker.salary" type="number" min="0" step="0.01" :placeholder="`at least ${MIN_SALARY.toFixed(2)}`" icon="i-lucide-euro" class="w-full" />
            </UFormField>
            <UFormField label="Phone Number" :error="workerErrors.phoneNumber">
              <UInput v-model="newWorker.phoneNumber" placeholder="+359..." icon="i-lucide-phone" class="w-full" />
            </UFormField>
          </div>
          <div class="flex justify-end gap-3 border-t border-default px-6 py-4">
            <UButton color="neutral" variant="ghost" @click="showAddWorker = false">Cancel</UButton>
            <UButton color="primary" @click="submitWorker" :disabled="workerFormIncomplete || Object.keys(workerErrors).length > 0">
              {{ editingWorkerId !== null ? 'Save Changes' : newWorker.role === 'ROLE_ADMIN' ? 'Create Manager' : 'Create Employee' }}
            </UButton>
          </div>
        </div>
      </template>
    </UModal>

    <!-- Create Workstation Modal -->
    <UModal v-model:open="showCreateWorkstation">
      <template #content>
        <div class="min-h-0 space-y-5 overflow-y-auto p-6">
          <h3 class="text-lg font-bold text-highlighted">{{ editingWorkstationId !== null ? 'Edit Workstation' : 'Create Workstation' }}</h3>
          <div class="space-y-4">
            <UFormField label="Title" required>
              <UInput v-model="newWorkstation.title" placeholder="Workstation name" icon="i-lucide-monitor" class="w-full" />
            </UFormField>
            <UFormField label="Number of Computers" required>
              <UInput v-model.number="newWorkstation.computerCount" type="number" min="0" step="1" icon="i-lucide-computer" class="w-full" />
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
            <UButton
              color="primary"
              @click="submitWorkstation"
              :disabled="!newWorkstation.title || newWorkstation.computerCount === '' || Number(newWorkstation.computerCount) < 0 || !Number.isInteger(Number(newWorkstation.computerCount))"
            >
              {{ editingWorkstationId !== null ? 'Save Changes' : 'Create Workstation' }}
            </UButton>
          </div>
        </div>
      </template>
    </UModal>

    <!-- Create Project Modal -->
    <UModal v-model:open="showCreateProject">
      <template #content>
        <div class="min-h-0 space-y-5 overflow-y-auto p-6">
          <h3 class="text-lg font-bold text-highlighted">{{ editingProjectId !== null ? 'Edit Project' : 'Create Project' }}</h3>
          <div class="space-y-4">
            <UFormField label="Title" required>
              <UInput v-model="newProject.title" placeholder="Project name" icon="i-lucide-folder-kanban" class="w-full" />
            </UFormField>
            <div class="grid gap-4 sm:grid-cols-2">
              <UFormField label="Due Date" required>
                <UInput v-model="newProject.dueDate" type="date" icon="i-lucide-calendar" class="w-full" />
              </UFormField>
              <UFormField label="Project Value (EUR)" required>
                <UInput v-model.number="newProject.projectValue" type="number" min="0" step="0.01" placeholder="0.00" icon="i-lucide-euro" class="w-full" />
              </UFormField>
            </div>
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
            <UButton color="primary" @click="submitProject" :disabled="!newProject.title || !newProject.dueDate || newProject.projectValue === '' || newProject.projectValue === null || Number(newProject.projectValue) < 0 || newProject.workstationIds.length === 0">
              {{ editingProjectId !== null ? 'Save Changes' : 'Create Project' }}
            </UButton>
          </div>
        </div>
      </template>
    </UModal>

    <!-- Employee Profile Panel -->
    <USlideover v-model:open="showProfile" :title="profile && isManager(profile) ? 'Manager profile' : 'Employee profile'" :ui="{ content: 'max-w-md' }">
      <template #body>
        <div v-if="profile" class="space-y-6">
          <!-- Identity -->
          <div class="flex items-center gap-4">
            <UAvatar :text="initials(fullName(profile))" size="3xl" :ui="{ root: 'bg-primary/15 ring-1 ring-primary/30', fallback: 'text-primary font-bold' }" />
            <div class="min-w-0 space-y-1">
              <p class="truncate text-xl font-bold text-highlighted">{{ fullName(profile) }}</p>
              <p class="truncate text-sm text-muted">@{{ profile.username }}</p>
              <div class="flex flex-wrap gap-1.5">
                <UBadge color="neutral" variant="outline" size="xs" class="font-mono">
                  {{ isManager(profile) ? 'Manager ID' : 'Employee ID' }} #{{ profile.id }}
                </UBadge>
                <UBadge :color="isManager(profile) ? 'primary' : 'neutral'" variant="subtle" size="xs">
                  {{ isManager(profile) ? 'Manager' : 'Employee' }}
                </UBadge>
              </div>
            </div>
          </div>

          <!-- Personal details -->
          <section class="space-y-2">
            <h4 class="text-xs font-semibold tracking-wide text-muted uppercase">Details</h4>
            <dl class="divide-y divide-default rounded-lg ring ring-default">
              <div v-for="item in profileDetails" :key="item.label" class="flex items-center gap-3 px-3 py-2.5">
                <UIcon :name="item.icon" class="size-4 shrink-0 text-primary" />
                <dt class="w-28 shrink-0 text-sm text-muted">{{ item.label }}</dt>
                <dd class="min-w-0 truncate text-sm text-highlighted" :class="{ 'text-dimmed': !item.value }">
                  {{ item.value || '—' }}
                </dd>
              </div>
            </dl>
          </section>

          <!-- Workstation (managers don't sit at one) -->
          <section v-if="!isManager(profile)" class="space-y-2">
            <h4 class="text-xs font-semibold tracking-wide text-muted uppercase">Workstation</h4>
            <div v-if="profileWorkstation" class="flex items-start gap-3 rounded-lg px-3 py-2.5 ring ring-default">
              <UIcon name="i-lucide-monitor" class="mt-0.5 size-4 shrink-0 text-primary" />
              <div class="min-w-0">
                <p class="text-sm font-medium text-highlighted">
                  {{ profileWorkstation.title }}
                  <span class="font-mono text-xs text-muted">№{{ profileWorkstation.id }}</span>
                </p>
                <p class="text-xs text-muted">{{ profileWorkstation.computerCount }} computer{{ profileWorkstation.computerCount === 1 ? '' : 's' }}</p>
                <p v-if="profileWorkstation.description" class="text-sm text-muted">{{ profileWorkstation.description }}</p>
              </div>
            </div>
            <p v-else class="text-sm text-muted">Not assigned to a workstation.</p>
          </section>

          <!-- Projects -->
          <section v-if="profileWorkstation" class="space-y-2">
            <div class="flex items-center justify-between">
              <h4 class="text-xs font-semibold tracking-wide text-muted uppercase">Projects</h4>
              <span v-if="profileProjects.length" class="text-xs text-muted">
                {{ profileDoneCount }} of {{ profileProjects.length }} completed
              </span>
            </div>
            <UProgress v-if="profileProjects.length" :model-value="profileDoneCount" :max="profileProjects.length" size="sm" />
            <ul v-if="profileProjects.length" class="divide-y divide-default rounded-lg ring ring-default">
              <li v-for="proj in profileProjects" :key="proj.id" class="flex items-center gap-3 px-3 py-2.5">
                <UIcon
                  :name="proj.done ? 'i-lucide-circle-check' : 'i-lucide-circle-dashed'"
                  class="size-4 shrink-0"
                  :class="proj.done ? 'text-success' : 'text-muted'"
                />
                <div class="min-w-0 flex-1">
                  <p class="truncate text-sm font-medium text-highlighted">{{ proj.title }}</p>
                  <p class="text-xs text-muted">Due {{ proj.dueDate }}</p>
                </div>
                <UBadge color="primary" variant="outline" size="xs" class="shrink-0 font-mono">#{{ proj.id }}</UBadge>
              </li>
            </ul>
            <p v-else class="text-sm text-muted">No projects assigned to this workstation.</p>
          </section>
        </div>
      </template>

      <template #footer>
        <div class="flex w-full justify-end gap-3">
          <UButton color="neutral" variant="ghost" @click="showProfile = false">Close</UButton>
          <UButton color="primary" icon="i-lucide-pencil" @click="editFromProfile">Edit</UButton>
        </div>
      </template>
    </USlideover>

    <!-- Delete Confirmation Modal -->
    <UModal v-model:open="showDeleteConfirm">
      <template #content>
        <div v-if="deleteTarget" class="min-h-0 space-y-5 overflow-y-auto p-6">
          <div class="flex items-start gap-3">
            <UIcon name="i-lucide-triangle-alert" class="mt-0.5 size-6 shrink-0 text-error" />
            <div class="space-y-1">
              <h3 class="text-lg font-bold text-highlighted">Delete {{ deleteTarget.kind }}?</h3>
              <p class="text-sm text-muted">
                <span class="font-medium text-highlighted">{{ deleteTarget.name }}</span> will be permanently deleted.
                {{ deleteHints[deleteTarget.kind] }}
              </p>
            </div>
          </div>
          <div class="flex justify-end gap-3 pt-2">
            <UButton color="neutral" variant="ghost" @click="showDeleteConfirm = false">Cancel</UButton>
            <UButton color="error" icon="i-lucide-trash-2" :loading="deleting" @click="confirmDelete">Delete</UButton>
          </div>
        </div>
      </template>
    </UModal>

    <!-- Manage Employees Modal -->
    <UModal v-model:open="showManageEmployees">
      <template #content>
        <div class="min-h-0 space-y-5 overflow-y-auto p-6">
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
