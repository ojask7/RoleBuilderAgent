import { useState } from 'react'
import api from '../api/client'

const DEMO_ROLES = [
  {
    id: 1, name: 'Finance-Analyst-EMEA', jobFunction: 'Finance Analyst', department: 'Finance', region: 'EMEA',
    owner: 'alice@corp.com', status: 'ACTIVE',
    itRoles: [
      { id: 1, name: 'SAP-FI-Reader', entitlements: ['CH_SG_SAP_FI_STG_Read', 'CH_SG_SAP_FI_PRD_Read', 'SG_SAP_RFC_FI_ReadOnly'] },
      { id: 2, name: 'Reporting-Viewer', entitlements: ['SG_PowerBI_Finance_View', 'SG_Legacy_Reports'] },
      { id: 3, name: 'ServiceDesk-Basic', entitlements: ['SG_ITSM_SelfService'] },
    ],
    totalEntitlements: 6, assignedUsers: 12,
  },
  {
    id: 2, name: 'HR-Specialist-Global', jobFunction: 'HR Specialist', department: 'HR', region: null,
    owner: 'hr-admin@corp.com', status: 'DRAFT',
    itRoles: [
      { id: 4, name: 'HR-Read', entitlements: ['SG_HR_Workday_Read'] },
      { id: 3, name: 'ServiceDesk-Basic', entitlements: ['SG_ITSM_SelfService'] },
    ],
    totalEntitlements: 2, assignedUsers: 3,
  },
  {
    id: 3, name: 'HR-Admin-Global', jobFunction: 'HR Administrator', department: 'HR', region: null,
    owner: 'hr-admin@corp.com', status: 'DRAFT',
    itRoles: [
      { id: 5, name: 'HR-Admin', entitlements: ['SG_HR_Workday_Read', 'SG_HR_Workday_Write'] },
      { id: 6, name: 'Payroll-Processor', entitlements: ['SG_HR_ADP_Payroll'] },
      { id: 3, name: 'ServiceDesk-Basic', entitlements: ['SG_ITSM_SelfService'] },
    ],
    totalEntitlements: 4, assignedUsers: 2,
  },
]

export default function BusinessRoles() {
  const [roles, setRoles] = useState(DEMO_ROLES)
  const [expanded, setExpanded] = useState(null)
  const [suggesting, setSuggesting] = useState(false)

  async function handleSuggest() {
    setSuggesting(true)
    try {
      await api.suggestBusinessRole({
        jobFunction: 'IT Support',
        department: 'IT Ops',
        region: 'Global',
        userItRoleAssignments: { U030: [3], U031: [3], U032: [3] },
      })
    } catch {}
    // Demo: add suggestion
    setRoles(prev => [...prev, {
      id: prev.length + 1, name: 'IT-Support-Global', jobFunction: 'IT Support', department: 'IT Ops', region: 'Global',
      owner: null, status: 'DRAFT',
      itRoles: [{ id: 3, name: 'ServiceDesk-Basic', entitlements: ['SG_ITSM_SelfService'] }],
      totalEntitlements: 1, assignedUsers: 3,
    }])
    setSuggesting(false)
  }

  const statusBadge = {
    DRAFT: 'badge-gray', REVIEW: 'badge-yellow', APPROVED: 'badge-blue', ACTIVE: 'badge-green', DEPRECATED: 'badge-red',
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-white">Business Roles</h1>
          <p className="text-gray-400 text-sm mt-1">Compose IT Roles into job-function-aligned Business Roles</p>
        </div>
        <div className="flex gap-3">
          <button onClick={handleSuggest} disabled={suggesting} className="btn-secondary">
            {suggesting ? 'Analyzing...' : 'AI Suggest Role'}
          </button>
          <button className="btn-primary">Create Role</button>
        </div>
      </div>

      {/* Summary */}
      <div className="grid grid-cols-4 gap-3 mb-6">
        <div className="card text-center">
          <div className="text-2xl font-bold text-white">{roles.length}</div>
          <div className="text-xs text-gray-400">Total Business Roles</div>
        </div>
        <div className="card text-center">
          <div className="text-2xl font-bold text-emerald-400">{roles.filter(r => r.status === 'ACTIVE').length}</div>
          <div className="text-xs text-gray-400">Active</div>
        </div>
        <div className="card text-center">
          <div className="text-2xl font-bold text-white">{roles.reduce((a, r) => a + r.assignedUsers, 0)}</div>
          <div className="text-xs text-gray-400">Total Users Covered</div>
        </div>
        <div className="card text-center">
          <div className="text-2xl font-bold text-forge-400">{roles.reduce((a, r) => a + r.totalEntitlements, 0)}</div>
          <div className="text-xs text-gray-400">Total Entitlements</div>
        </div>
      </div>

      {/* Roles list */}
      <div className="space-y-4">
        {roles.map(role => (
          <div key={role.id} className="card-hover">
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <div className="flex items-center gap-3">
                  <h3 className="text-lg font-semibold text-white">{role.name}</h3>
                  <span className={statusBadge[role.status]}>{role.status}</span>
                </div>
                <div className="flex flex-wrap items-center gap-3 mt-2 text-sm text-gray-400">
                  <span className="flex items-center gap-1">Job: <span className="text-gray-300">{role.jobFunction}</span></span>
                  <span>|</span>
                  <span className="flex items-center gap-1">Dept: <span className="text-gray-300">{role.department}</span></span>
                  {role.region && <><span>|</span><span>Region: <span className="text-gray-300">{role.region}</span></span></>}
                  <span>|</span>
                  <span>{role.itRoles.length} IT Roles</span>
                  <span>|</span>
                  <span>{role.totalEntitlements} SGs</span>
                  <span>|</span>
                  <span>{role.assignedUsers} users</span>
                </div>
              </div>
              <button
                onClick={() => setExpanded(expanded === role.id ? null : role.id)}
                className="btn-secondary text-sm"
              >
                {expanded === role.id ? 'Collapse' : 'View Hierarchy'}
              </button>
            </div>

            {expanded === role.id && (
              <div className="mt-4 pt-4 border-t border-forge-800/50">
                <h4 className="text-sm font-medium text-gray-400 mb-3">Role Hierarchy: {role.name}</h4>
                <div className="bg-forge-800/30 rounded-lg p-4">
                  <div className="text-sm font-medium text-forge-400 mb-3">
                    Business Role: {role.name}
                  </div>
                  {role.itRoles.map(it => (
                    <div key={it.id} className="ml-6 mb-3">
                      <div className="flex items-center gap-2 text-sm">
                        <span className="text-gray-500">└─</span>
                        <span className="text-blue-400 font-medium">IT Role: {it.name}</span>
                        <span className="badge-blue">{it.entitlements.length} SGs</span>
                      </div>
                      {it.entitlements.map(sg => (
                        <div key={sg} className="ml-10 flex items-center gap-2 text-sm text-gray-400 mt-1">
                          <span className="text-gray-600">└─</span>
                          <span className="font-mono text-xs">{sg}</span>
                        </div>
                      ))}
                    </div>
                  ))}
                </div>
                <div className="flex gap-3 mt-4">
                  <button className="btn-primary text-sm">Create Access Bundle</button>
                  <button className="btn-secondary text-sm">Export to SailPoint</button>
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
