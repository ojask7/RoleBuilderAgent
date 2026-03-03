const API_BASE = '/api/v1'

function authHeaders() {
  const user = localStorage.getItem('af_user') || 'agent'
  const pass = localStorage.getItem('af_pass') || 'agent-secret'
  return {
    Authorization: 'Basic ' + btoa(`${user}:${pass}`),
    'Content-Type': 'application/json',
  }
}

async function request(method, path, body) {
  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers: authHeaders(),
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    const text = await res.text().catch(() => 'Unknown error')
    throw new Error(`${res.status}: ${text}`)
  }
  return res.json()
}

const api = {
  // Health
  health: () => fetch('/actuator/health').then(r => r.json()),

  // Entitlements
  entitlementSummary: () => request('GET', '/entitlements/summary'),
  entitlements: (status, page = 0, size = 50) =>
    request('GET', `/entitlements?status=${status}&page=${page}&size=${size}`),
  discover: (sgNames) => request('POST', '/entitlements/discover', { sgNames }),

  // Role Mining
  mineRoles: (payload) => request('POST', '/roles/mine', payload),
  itRoles: (status) => request('GET', `/roles/it?status=${status || ''}`),
  approveITRole: (id) => request('POST', `/roles/it/${id}/approve`),
  activateITRole: (id) => request('POST', `/roles/it/${id}/activate`),

  // Business Roles
  businessRoles: (department) =>
    request('GET', `/roles/business${department ? `?department=${department}` : ''}`),
  businessRole: (id) => request('GET', `/roles/business/${id}`),
  businessRoleEntitlements: (id) => request('GET', `/roles/business/${id}/entitlements`),
  createBusinessRole: (payload) => request('POST', '/roles/business', payload),
  suggestBusinessRole: (payload) => request('POST', '/roles/business/suggest', payload),

  // Access Bundles
  createBundle: (businessRoleId) => request('POST', '/bundles', { businessRoleId }),
  bundle: (id) => request('GET', `/bundles/${id}`),
  bundleLifecycle: (id, action, performedBy, comment) =>
    request('PATCH', `/bundles/${id}/lifecycle`, { action, performedBy, comment }),
  assessKc27: (id) => request('POST', `/bundles/${id}/assess`),
  bundleEvidence: (id) => request('GET', `/bundles/${id}/evidence`),
  complianceDashboard: () => request('GET', '/bundles/compliance/dashboard'),

  // Integration status
  integrationStatus: () => request('GET', '/integrations/status'),
}

export default api
