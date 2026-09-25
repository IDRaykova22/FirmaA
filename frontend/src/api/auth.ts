// Client for the Spring Boot backend (see backend/). Credentials are checked
// server-side against Postgres via JDBC; the frontend never touches the DB.
const API_URL = import.meta.env.VITE_API_URL ?? ''

export interface LoginCredentials {
  username: string
  password: string
}

export async function login(credentials: LoginCredentials): Promise<string> {
  let response: Response
  try {
    response = await fetch(`${API_URL}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(credentials),
    })
  } catch {
    throw new Error('Could not reach the server')
  }

  if (!response.ok) {
    throw new Error(response.status === 401 ? 'Invalid username or password' : 'Login failed')
  }

  const token = await response.text()
  localStorage.setItem('jwt', token)
  return token
}
