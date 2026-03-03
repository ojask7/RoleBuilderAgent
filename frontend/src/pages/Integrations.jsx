import { useState } from 'react'

const INTEGRATIONS = [
  {
    id: 'azure-ad',
    name: 'Azure AD / Entra ID',
    description: 'Read security groups, memberships, and user attributes via Microsoft Graph API',
    status: 'connected',
    lastSync: '2026-03-03T07:30:00Z',
    stats: { groups: 4500, users: 12400, synced: true },
    config: { tenantId: 'corp.onmicrosoft.com', clientId: 'app-reg-***-***', scope: 'Directory.Read.All, Group.Read.All' },
  },
  {
    id: 'sailpoint',
    name: 'SailPoint IdentityNow',
    description: 'Read entitlements, application services, and push approved roles for provisioning',
    status: 'connected',
    lastSync: '2026-03-03T06:00:00Z',
    stats: { entitlements: 900, applications: 47, sources: 12 },
    config: { tenant: 'corp.identitynow.com', apiVersion: 'v3', mode: 'read-write' },
  },
  {
    id: 'servicenow',
    name: 'ServiceNow',
    description: 'Create catalog items for Business Roles, handle access request workflows',
    status: 'connected',
    lastSync: '2026-03-03T07:45:00Z',
    stats: { catalogItems: 34, pendingRequests: 7, completedThisMonth: 89 },
    config: { instance: 'corp.service-now.com', table: 'sc_cat_item', integration: 'REST API' },
  },
  {
    id: 'on-prem-ad',
    name: 'On-Premises Active Directory',
    description: 'LDAP sync for on-prem AD forests not yet migrated to Azure AD',
    status: 'connected',
    lastSync: '2026-03-03T04:00:00Z',
    stats: { domains: 3, ous: 142, groups: 3200 },
    config: { forests: ['corp.local', 'legacy.corp.local', 'dmz.corp.local'], protocol: 'LDAPS', port: 636 },
  },
  {
    id: 'hr-feed',
    name: 'HR System (Workday)',
    description: 'Org structure, job functions, department hierarchy, and manager chain for role alignment',
    status: 'connected',
    lastSync: '2026-03-03T01:00:00Z',
    stats: { employees: 8500, departments: 42, jobFunctions: 156 },
    config: { integration: 'SCIM 2.0', endpoint: 'workday-scim-proxy', schedule: 'Daily 01:00 UTC' },
  },
  {
    id: 'cmdb',
    name: 'CMDB / Application Inventory',
    description: 'Application service catalog, business application mapping, and criticality tiers',
    status: 'connected',
    lastSync: '2026-03-02T22:00:00Z',
    stats: { appServices: 312, businessApps: 89, tier1: 34 },
    config: { source: 'ServiceNow CMDB', table: 'cmdb_ci_service', refreshSchedule: 'Daily' },
  },
]

export default function Integrations() {
  const [expanded, setExpanded] = useState(null)
  const [syncing, setSyncing] = useState(null)

  function handleSync(id) {
    setSyncing(id)
    setTimeout(() => setSyncing(null), 2000)
  }

  const statusIndicator = {
    connected: { color: 'bg-emerald-500', label: 'Connected', badge: 'badge-green' },
    disconnected: { color: 'bg-red-500', label: 'Disconnected', badge: 'badge-red' },
    error: { color: 'bg-amber-500', label: 'Error', badge: 'badge-yellow' },
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-white">Integrations</h1>
          <p className="text-gray-400 text-sm mt-1">Connected systems for discovery, governance, and provisioning</p>
        </div>
        <button className="btn-primary">Add Connector</button>
      </div>

      {/* Connection status summary */}
      <div className="grid grid-cols-3 gap-4 mb-6">
        <div className="card text-center">
          <div className="text-2xl font-bold text-emerald-400">{INTEGRATIONS.filter(i => i.status === 'connected').length}</div>
          <div className="text-xs text-gray-400">Connected</div>
        </div>
        <div className="card text-center">
          <div className="text-2xl font-bold text-white">{INTEGRATIONS.reduce((a, i) => a + (i.stats.groups || i.stats.entitlements || i.stats.employees || 0), 0).toLocaleString()}</div>
          <div className="text-xs text-gray-400">Objects Synced</div>
        </div>
        <div className="card text-center">
          <div className="text-2xl font-bold text-forge-400">6</div>
          <div className="text-xs text-gray-400">Active Connectors</div>
        </div>
      </div>

      {/* Data flow diagram */}
      <div className="card mb-6">
        <h3 className="text-sm font-medium text-gray-400 mb-4">Data Flow Architecture</h3>
        <div className="flex items-center justify-between text-sm px-4">
          <div className="text-center">
            <div className="w-20 h-20 rounded-xl bg-blue-500/20 border border-blue-500/30 flex items-center justify-center mb-2">
              <span className="text-2xl">🏢</span>
            </div>
            <div className="text-gray-300 font-medium">HR System</div>
            <div className="text-[10px] text-gray-500">Org structure</div>
          </div>
          <div className="text-gray-600">→</div>
          <div className="text-center">
            <div className="w-20 h-20 rounded-xl bg-purple-500/20 border border-purple-500/30 flex items-center justify-center mb-2">
              <span className="text-2xl">📁</span>
            </div>
            <div className="text-gray-300 font-medium">Active Directory</div>
            <div className="text-[10px] text-gray-500">Groups & users</div>
          </div>
          <div className="text-gray-600">→</div>
          <div className="text-center">
            <div className="w-20 h-20 rounded-xl bg-forge-600/30 border border-forge-500/30 flex items-center justify-center mb-2">
              <span className="text-2xl font-bold text-forge-400">F</span>
            </div>
            <div className="text-forge-400 font-bold">AccessForge</div>
            <div className="text-[10px] text-gray-500">AI Role Mining</div>
          </div>
          <div className="text-gray-600">→</div>
          <div className="text-center">
            <div className="w-20 h-20 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center mb-2">
              <span className="text-2xl">⛵</span>
            </div>
            <div className="text-gray-300 font-medium">SailPoint</div>
            <div className="text-[10px] text-gray-500">Governance</div>
          </div>
          <div className="text-gray-600">→</div>
          <div className="text-center">
            <div className="w-20 h-20 rounded-xl bg-amber-500/20 border border-amber-500/30 flex items-center justify-center mb-2">
              <span className="text-2xl">🎫</span>
            </div>
            <div className="text-gray-300 font-medium">ServiceNow</div>
            <div className="text-[10px] text-gray-500">Requests</div>
          </div>
        </div>
      </div>

      {/* Connectors list */}
      <div className="space-y-3">
        {INTEGRATIONS.map(integration => {
          const si = statusIndicator[integration.status]
          return (
            <div key={integration.id} className="card-hover">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-4 flex-1">
                  <div className="flex items-center gap-2">
                    <span className={`w-3 h-3 rounded-full ${si.color} ${integration.status === 'connected' ? 'animate-pulse' : ''}`}></span>
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center gap-3">
                      <h3 className="text-sm font-semibold text-white">{integration.name}</h3>
                      <span className={si.badge}>{si.label}</span>
                    </div>
                    <p className="text-xs text-gray-500 mt-0.5">{integration.description}</p>
                    <div className="flex items-center gap-4 mt-1 text-xs text-gray-400">
                      {Object.entries(integration.stats).map(([key, val]) => (
                        <span key={key}>{key}: <span className="text-gray-300">{typeof val === 'number' ? val.toLocaleString() : String(val)}</span></span>
                      ))}
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs text-gray-500">
                    Last: {new Date(integration.lastSync).toLocaleString()}
                  </span>
                  <button
                    onClick={() => handleSync(integration.id)}
                    disabled={syncing === integration.id}
                    className="btn-secondary text-xs"
                  >
                    {syncing === integration.id ? 'Syncing...' : 'Sync Now'}
                  </button>
                  <button
                    onClick={() => setExpanded(expanded === integration.id ? null : integration.id)}
                    className="btn-secondary text-xs"
                  >
                    Config
                  </button>
                </div>
              </div>

              {expanded === integration.id && (
                <div className="mt-3 pt-3 border-t border-forge-800/50">
                  <h4 className="text-xs font-medium text-gray-400 mb-2">Connection Configuration</h4>
                  <div className="bg-forge-800/30 rounded-lg p-3 font-mono text-xs text-gray-300">
                    {Object.entries(integration.config).map(([key, val]) => (
                      <div key={key} className="flex gap-2">
                        <span className="text-gray-500 w-32">{key}:</span>
                        <span>{Array.isArray(val) ? val.join(', ') : String(val)}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
