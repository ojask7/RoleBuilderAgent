import { useState } from 'react'
import api from '../api/client'

const LIFECYCLE = ['DRAFT', 'PENDING_REVIEW', 'APPROVED', 'ACTIVE', 'RECERTIFICATION_DUE', 'DEPRECATED']

const DEMO_BUNDLES = [
  {
    id: 1, businessRole: 'Finance-Analyst-EMEA', version: 2, status: 'ACTIVE',
    approvedBy: 'iam-governance@corp.com', approvedAt: '2026-02-01T10:00:00Z',
    kc27Status: 'PARTIALLY_COMPLIANT', totalEntitlements: 6,
    evidenceHash: 'sha256:bf5c3a2d91e8',
    itRoles: ['SAP-FI-Reader', 'Reporting-Viewer', 'ServiceDesk-Basic'],
    gaps: [
      { entitlement: 'SG_Legacy_Reports', issues: ['No recertification history', 'Owner inferred from MDT (not confirmed)'] },
      { entitlement: 'SG_SAP_RFC_FI_ReadOnly', issues: ['Missing SailPoint governance'] },
    ]
  },
  {
    id: 2, businessRole: 'HR-Specialist-Global', version: 1, status: 'DRAFT',
    approvedBy: null, approvedAt: null,
    kc27Status: 'NOT_ASSESSED', totalEntitlements: 2,
    evidenceHash: null,
    itRoles: ['HR-Read', 'ServiceDesk-Basic'],
    gaps: []
  },
  {
    id: 3, businessRole: 'HR-Admin-Global', version: 1, status: 'DRAFT',
    approvedBy: null, approvedAt: null,
    kc27Status: 'NOT_ASSESSED', totalEntitlements: 4,
    evidenceHash: null,
    itRoles: ['HR-Admin', 'Payroll-Processor', 'ServiceDesk-Basic'],
    gaps: []
  },
]

export default function AccessBundles() {
  const [bundles, setBundles] = useState(DEMO_BUNDLES)
  const [expanded, setExpanded] = useState(null)
  const [assessing, setAssessing] = useState(null)

  const statusBadge = {
    DRAFT: 'badge-gray', PENDING_REVIEW: 'badge-yellow', APPROVED: 'badge-blue',
    ACTIVE: 'badge-green', RECERTIFICATION_DUE: 'badge-red', DEPRECATED: 'badge-gray',
  }
  const kc27Badge = {
    COMPLIANT: 'badge-green', PARTIALLY_COMPLIANT: 'badge-yellow',
    NON_COMPLIANT: 'badge-red', NOT_ASSESSED: 'badge-gray',
  }

  function nextAction(status) {
    switch (status) {
      case 'DRAFT': return { action: 'SUBMIT_FOR_REVIEW', label: 'Submit for Review' }
      case 'PENDING_REVIEW': return { action: 'APPROVE', label: 'Approve' }
      case 'APPROVED': return { action: 'ACTIVATE', label: 'Activate' }
      case 'ACTIVE': return { action: 'FLAG_RECERTIFICATION', label: 'Flag Recertification' }
      default: return null
    }
  }

  async function handleTransition(id, action) {
    try {
      await api.bundleLifecycle(id, action, 'current-user@corp.com', 'Via AccessForge UI')
    } catch {}
    setBundles(prev => prev.map(b => {
      if (b.id !== id) return b
      const idx = LIFECYCLE.indexOf(b.status)
      return { ...b, status: LIFECYCLE[Math.min(idx + 1, LIFECYCLE.length - 1)] }
    }))
  }

  async function handleAssess(id) {
    setAssessing(id)
    try {
      const result = await api.assessKc27(id)
      if (result) {
        setBundles(prev => prev.map(b => b.id === id ? { ...b, kc27Status: result.kc27Status, gaps: result.gaps || [] } : b))
      }
    } catch {
      // Demo mode
      setBundles(prev => prev.map(b => b.id === id ? { ...b, kc27Status: 'PARTIALLY_COMPLIANT' } : b))
    } finally {
      setAssessing(null)
    }
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-8">
        <div>
          <h1 className="text-2xl font-bold text-white">Access Bundles</h1>
          <p className="text-gray-400 text-sm mt-1">Lifecycle-managed, audit-ready access packages with KC27 compliance</p>
        </div>
      </div>

      {/* Lifecycle pipeline visualization */}
      <div className="card mb-6">
        <h3 className="text-sm font-medium text-gray-400 mb-4">Bundle Lifecycle Pipeline</h3>
        <div className="flex items-center justify-between">
          {LIFECYCLE.map((stage, i) => {
            const count = bundles.filter(b => b.status === stage).length
            return (
              <div key={stage} className="flex items-center">
                <div className={`flex flex-col items-center ${count > 0 ? 'opacity-100' : 'opacity-40'}`}>
                  <div className={`w-10 h-10 rounded-full flex items-center justify-center text-sm font-bold ${
                    count > 0 ? 'bg-forge-600 text-white' : 'bg-forge-800 text-gray-500'
                  }`}>
                    {count}
                  </div>
                  <span className="text-[10px] text-gray-500 mt-1 text-center max-w-[80px]">{stage.replace('_', ' ')}</span>
                </div>
                {i < LIFECYCLE.length - 1 && (
                  <div className="w-12 h-0.5 bg-forge-800 mx-1"></div>
                )}
              </div>
            )
          })}
        </div>
      </div>

      {/* Bundles list */}
      <div className="space-y-4">
        {bundles.map(bundle => {
          const next = nextAction(bundle.status)
          return (
            <div key={bundle.id} className="card-hover">
              <div className="flex items-start justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-3">
                    <h3 className="text-lg font-semibold text-white">{bundle.businessRole}</h3>
                    <span className={statusBadge[bundle.status]}>{bundle.status.replace('_', ' ')}</span>
                    <span className={kc27Badge[bundle.kc27Status]}>KC27: {bundle.kc27Status.replace('_', ' ')}</span>
                    <span className="text-xs text-gray-500">v{bundle.version}</span>
                  </div>
                  <div className="flex items-center gap-4 mt-2 text-sm text-gray-400">
                    <span>{bundle.totalEntitlements} entitlements</span>
                    <span>|</span>
                    <span>{bundle.itRoles.length} IT Roles</span>
                    {bundle.approvedBy && <><span>|</span><span>Approved by {bundle.approvedBy}</span></>}
                    {bundle.evidenceHash && <><span>|</span><span className="font-mono text-xs">{bundle.evidenceHash}</span></>}
                  </div>
                </div>
                <div className="flex gap-2">
                  {next && (
                    <button onClick={() => handleTransition(bundle.id, next.action)} className="btn-primary text-sm">
                      {next.label}
                    </button>
                  )}
                  <button
                    onClick={() => handleAssess(bundle.id)}
                    disabled={assessing === bundle.id}
                    className="btn-secondary text-sm"
                  >
                    {assessing === bundle.id ? 'Assessing...' : 'Run KC27'}
                  </button>
                  <button onClick={() => setExpanded(expanded === bundle.id ? null : bundle.id)} className="btn-secondary text-sm">
                    {expanded === bundle.id ? 'Collapse' : 'Details'}
                  </button>
                </div>
              </div>

              {expanded === bundle.id && (
                <div className="mt-4 pt-4 border-t border-forge-800/50">
                  <div className="grid grid-cols-2 gap-6">
                    <div>
                      <h4 className="text-sm font-medium text-gray-400 mb-2">IT Roles in Bundle</h4>
                      <div className="space-y-2">
                        {bundle.itRoles.map(name => (
                          <div key={name} className="flex items-center gap-2 text-sm">
                            <span className="w-2 h-2 rounded-full bg-blue-500"></span>
                            <span className="text-gray-300">{name}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                    <div>
                      <h4 className="text-sm font-medium text-gray-400 mb-2">KC27 Compliance Gaps</h4>
                      {bundle.gaps.length === 0 ? (
                        <p className="text-sm text-gray-500">No gaps identified (or not yet assessed)</p>
                      ) : (
                        <div className="space-y-2">
                          {bundle.gaps.map((gap, i) => (
                            <div key={i} className="bg-red-500/10 border border-red-500/20 rounded-lg p-3">
                              <div className="font-mono text-sm text-red-400">{gap.entitlement}</div>
                              <ul className="mt-1 text-xs text-gray-400">
                                {gap.issues.map((issue, j) => (
                                  <li key={j} className="flex items-center gap-1">
                                    <span className="text-red-500">!</span> {issue}
                                  </li>
                                ))}
                              </ul>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                  <div className="flex gap-3 mt-4">
                    <button className="btn-secondary text-sm">Download Evidence Pack</button>
                    <button className="btn-secondary text-sm">Push to SailPoint</button>
                    <button className="btn-secondary text-sm">Create SNOW Catalog Item</button>
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
