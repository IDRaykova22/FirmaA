import { getAuthHeaders } from './auth'

const API_URL = import.meta.env.VITE_API_URL ?? ''

// ── Admin endpoints ─────────────────────────────────────────────────

export async function fetchEmployees() {
  const res = await fetch(`${API_URL}/api/admin/employees`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Failed to fetch employees')
  return res.json()
}

export async function createEmployee(data: {
  username: string
  password: string
  jobTitle: string
  address: string
  dateOfBirth: string
  salary: number
  phoneNumber: string
}) {
  const res = await fetch(`${API_URL}/api/admin/employees`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || 'Failed to create employee')
  }
  return res.text()
}

export async function fetchWorkstations() {
  const res = await fetch(`${API_URL}/api/admin/workstations`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Failed to fetch workstations')
  return res.json()
}

export async function createWorkstation(data: {
  title: string
  description?: string
  employeeIds: number[]
}) {
  const res = await fetch(`${API_URL}/api/admin/workstations`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  })
  if (!res.ok) throw new Error('Failed to create workstation')
  return res.text()
}

export async function assignEmployeesToWorkstation(workstationId: number, employeeIds: number[]) {
  const res = await fetch(`${API_URL}/api/admin/workstations/${workstationId}/employees`, {
    method: 'PUT',
    headers: getAuthHeaders(),
    body: JSON.stringify({ employeeIds }),
  })
  if (!res.ok) throw new Error('Failed to assign employees')
  return res.text()
}

export async function fetchProjects() {
  const res = await fetch(`${API_URL}/api/admin/projects`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Failed to fetch projects')
  return res.json()
}

export async function createProject(data: {
  title: string
  description?: string
  dueDate: string
  workstationIds: number[]
}) {
  const res = await fetch(`${API_URL}/api/admin/projects`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: JSON.stringify(data),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || 'Failed to create project')
  }
  return res.text()
}

// ── Employee endpoints ──────────────────────────────────────────────

export async function fetchMyInfo() {
  const res = await fetch(`${API_URL}/api/employee/me`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Failed to fetch user info')
  return res.json()
}

export async function markProjectDone(projectId: number) {
  const res = await fetch(`${API_URL}/api/employee/projects/${projectId}/done`, {
    method: 'POST',
    headers: getAuthHeaders(),
  })
  if (!res.ok) throw new Error('Failed to mark project as done')
  return res.text()
}
