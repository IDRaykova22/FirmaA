// Client for the Spring Boot backend (see backend/). Credentials are checked
// server-side against Postgres via JDBC; the frontend never touches the DB.
const API_URL = import.meta.env.VITE_API_URL ?? ''

export interface LoginCredentials {
  username: string
  password: string
}

export interface AuthResponse {
  token: string
  username: string
  role: string
}

export async function login(credentials: LoginCredentials): Promise<AuthResponse> {
  let response: Response
  try {
    response = await fetch(`${API_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(credentials),
    })
  } catch {
    throw new Error('Could not reach the server')
  }

  if (!response.ok) {
    throw new Error(response.status === 401 ? 'Invalid username or password' : 'Login failed')
  }

  const data: AuthResponse = await response.json()
  localStorage.setItem('jwt', data.token)
  localStorage.setItem('role', data.role)
  localStorage.setItem('username', data.username)
  return data
}

export function getAuthHeaders(): Record<string, string> {
  const token = localStorage.getItem('jwt')
  return token ? { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json' }
}

export function logout() {
  localStorage.removeItem('jwt')
  localStorage.removeItem('role')
  localStorage.removeItem('username')
}

export function isAdmin(): boolean {
  return localStorage.getItem('role') === 'ROLE_ADMIN'
}

export function getUsername(): string {
  return localStorage.getItem('username') ?? ''
}
