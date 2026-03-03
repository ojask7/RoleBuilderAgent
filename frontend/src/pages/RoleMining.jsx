import { useState } from 'react'
import api from '../api/client'

const DEMO_SUGGESTIONS = [
  {
    id: 1, name: 'SAP-FI-Reader', applicationId: 'SAP-Finance', status: 'ACTIVE',
    entitlements: ['CH_SG_SAP_FI_STG_Read', 'CH_SG_SAP_FI_PRD_Read', 'SG_SAP_RFC_FI_ReadOnly'],
    confidence: 0.94, usersMatched: 10, totalUsers: 12,
    reasoning: 'These 3 SGs co-occur in 83% of Finance department users. All map to SAP-Finance in CMDB. Naming convention CH_SG_SAP_FI_*_Read confirms read-level access pattern.'
  },
  {
    id: 2, name: 'Reporting-Viewer', applicationId: 'Reporting', status: 'ACTIVE',
    entitlements: ['SG_PowerBI_Finance_View', 'SG_Legacy_Reports'],
    confidence: 0.87, usersMatched: 8, totalUsers: 12,
    reasoning: 'PowerBI and Legacy Reports co-occur in 67% of Finance users. Both map to Reporting platform. SG_Legacy_Reports lacks SailPoint governance — flag for remediation.'
  },
  {
    id: 3, name: 'ServiceDesk-Basic', applicationId: 'ITSM', status: 'ACTIVE',
    entitlements: ['SG_ITSM_SelfService'],
    confidence: 0.92, usersMatched: 18, totalUsers: 20,
    reasoning: 'ITSM self-service is near-universal (90% of all users). Single-entitlement role for baseline access.'
  },
  {
    id: 4, name: 'HR-ReadWrite', applicationId: 'HR-Suite', status: 'SUGGESTED',
    entitlements: ['SG_HR_Workday_Read', 'SG_HR_Workday_Write'],
    confidence: 0.88, usersMatched: 2, totalUsers: 5,
    reasoning: 'Workday read+write co-occur in 100% of HR Administrator users. Both governed in SailPoint with owner hr-admin@corp.com.'
  },
  {
    id: 5, name: 'Payroll-Access', applicationId: 'Payroll', status: 'SUGGESTED',
    entitlements: ['SG_HR_ADP_Payroll'],
    confidence: 0.75, usersMatched: 2, totalUsers: 5,
    reasoning: 'ADP Payroll access limited to HR Administrators. High-risk entitlement — recommend separate role for SOD controls.'
  },
]

export default function RoleMining() {
  const [suggestions, setSuggestions] = useState(DEMO_SUGGESTIONS)
  const [mining, setMining] = useState(false)
  const [miningDept, setMiningDept] = useState('Finance')
  const [expanded, setExpanded] = useState(null)

  async function handleMine() {
    setMining(true)
    try {
      const result = await api.mineRoles({
        userSgAssignments: {
          U001: ['CH_SG_SAP_FI_STG_Read', 'CH_SG_SAP_FI_PRD_Read', 'SG_PowerBI_Finance_View'],
          U002: ['CH_SG_SAP_FI_STG_Read', 'CH_SG_SAP_FI_PRD_Read', 'SG_PowerBI_Finance_View'],
          U003: ['CH_SG_SAP_FI_STG_Read', 'CH_SG_SAP_FI_PRD_Read'],
        },
        userDepartments: { U001: miningDept, U002: miningDept, U003: miningDept },
        department: miningDept,
        minClusterSize: 2,
        minConfidence: 0.7,
      })
      if (result?.suggestedRoles) setSuggestions(result.suggestedRoles)
    } catch {
      // demo mode — keep existing suggestions
    } finally {
      setMining(false)
    }
  }

  async function handleApprove(id) {
    try {
      await api.approveITRole(id)
    } catch {}
    setSuggestions(prev => prev.map(s => s.id === id ? { ...s, status: 'APPROVED' } : s))
  }

  async function handleActivate(id) {
    try {
      await api.activateITRole(id)
    } catch {}
    setSuggestions(prev => prev.map(s => s.id === id ? { ...s, status: 'ACTIVE' } : s))
  }

  const statusBadge = {
    SUGGESTED: 'badge-yellow',
    APPROVED: 'badge-blue',
    ACTIVE: 'badge-green',
    DEPRECATED: 'badge-gray',
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-white">AI Role Mining</h1>
          <p className="text-gray-400 text-sm mt-1">Discover IT Role clusters from SG co-occurrence patterns</p>
        </div>
      </div>

      {/* Mining controls */}
      <div className="card mb-6">
        <h3 className="text-sm font-medium text-gray-400 mb-3">Run Role Mining</h3>
        <div className="flex gap-3 items-end">
          <div>
            <label className="block text-xs text-gray-500 mb-1">Department</label>
            <select
              value={miningDept}
              onChange={e => setMiningDept(e.target.value)}
              className="bg-forge-800 border border-forge-700 rounded-lg px-4 py-2.5 text-white text-sm focus:outline-none"
            >
              <option>Finance</option>
              <option>HR</option>
              <option>IT Ops</option>
              <option>All Departments</option>
            </select>
          </div>
          <button onClick={handleMine} disabled={mining} className="btn-primary">
            {mining ? 'Mining...' : 'Start Role Mining'}
          </button>
        </div>
        <p className="text-xs text-gray-600 mt-2">
          Analyzes user-SG assignment patterns to identify natural IT Role groupings. Uses AI to validate cluster quality and generate reasoning.
        </p>
      </div>

      {/* Results summary */}
      <div className="grid grid-cols-4 gap-3 mb-6">
        <div className="card text-center">
          <div className="text-2xl font-bold text-white">{suggestions.length}</div>
          <div className="text-xs text-gray-400">IT Roles Found</div>
        </div>
        <div className="card text-center">
          <div className="text-2xl font-bold text-emerald-400">{suggestions.filter(s => s.status === 'ACTIVE').length}</div>
          <div className="text-xs text-gray-400">Active</div>
        </div>
        <div className="card text-center">
          <div className="text-2xl font-bold text-amber-400">{suggestions.filter(s => s.status === 'SUGGESTED').length}</div>
          <div className="text-xs text-gray-400">Pending Review</div>
        </div>
        <div className="card text-center">
          <div className="text-2xl font-bold text-forge-400">{(suggestions.reduce((a, s) => a + s.confidence, 0) / suggestions.length * 100).toFixed(0)}%</div>
          <div className="text-xs text-gray-400">Avg Confidence</div>
        </div>
      </div>

      {/* Suggestions list */}
      <div className="space-y-4">
        {suggestions.map(role => (
          <div key={role.id} className="card-hover">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-3">
                  <h3 className="text-lg font-semibold text-white">{role.name}</h3>
                  <span className={statusBadge[role.status]}>{role.status}</span>
                  <span className="badge-purple">{role.applicationId}</span>
                </div>
                <div className="flex items-center gap-4 mt-2 text-sm text-gray-400">
                  <span>{role.entitlements.length} entitlements</span>
                  <span>|</span>
                  <span>{role.usersMatched}/{role.totalUsers} users matched</span>
                  <span>|</span>
                  <span>Confidence: {(role.confidence * 100).toFixed(0)}%</span>
                </div>
              </div>
              <div className="flex gap-2">
                {role.status === 'SUGGESTED' && (
                  <button onClick={() => handleApprove(role.id)} className="btn-primary text-sm">Approve</button>
                )}
                {role.status === 'APPROVED' && (
                  <button onClick={() => handleActivate(role.id)} className="btn-primary text-sm">Activate</button>
                )}
                <button
                  onClick={() => setExpanded(expanded === role.id ? null : role.id)}
                  className="btn-secondary text-sm"
                >
                  {expanded === role.id ? 'Collapse' : 'Details'}
                </button>
              </div>
            </div>

            {expanded === role.id && (
              <div className="mt-4 pt-4 border-t border-forge-800/50">
                <div className="grid grid-cols-2 gap-6">
                  <div>
                    <h4 className="text-sm font-medium text-gray-400 mb-2">Member Entitlements (SGs)</h4>
                    <div className="space-y-1">
                      {role.entitlements.map(sg => (
                        <div key={sg} className="flex items-center gap-2 text-sm">
                          <span className="w-2 h-2 rounded-full bg-forge-500"></span>
                          <span className="font-mono text-gray-300">{sg}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                  <div>
                    <h4 className="text-sm font-medium text-gray-400 mb-2">AI Reasoning</h4>
                    <p className="text-sm text-gray-300 bg-forge-800/50 rounded-lg p-3">{role.reasoning}</p>
                    <div className="mt-3">
                      <div className="text-xs text-gray-500 mb-1">Confidence</div>
                      <div className="progress-bar">
                        <div
                          className={`progress-fill ${role.confidence >= 0.8 ? 'bg-emerald-500' : 'bg-amber-500'}`}
                          style={{ width: `${role.confidence * 100}%` }}
                        />
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
