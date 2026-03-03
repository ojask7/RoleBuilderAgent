import { Outlet, NavLink, useNavigate } from 'react-router-dom'

const nav = [
  { to: '/',               icon: '📊', label: 'Executive Dashboard' },
  { to: '/discovery',      icon: '🔍', label: 'Entitlement Discovery' },
  { to: '/role-mining',    icon: '⛏️',  label: 'Role Mining' },
  { to: '/business-roles', icon: '🏗️',  label: 'Business Roles' },
  { to: '/access-bundles', icon: '📦', label: 'Access Bundles' },
  { to: '/integrations',   icon: '🔗', label: 'Integrations' },
]

export default function Layout() {
  const navigate = useNavigate()

  function handleLogout() {
    localStorage.removeItem('af_user')
    localStorage.removeItem('af_pass')
    navigate('/login')
  }

  return (
    <div className="flex h-screen overflow-hidden">
      {/* Sidebar */}
      <aside className="w-64 bg-forge-900 border-r border-forge-800/60 flex flex-col">
        {/* Brand */}
        <div className="p-5 border-b border-forge-800/60">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 bg-forge-600 rounded-lg flex items-center justify-center text-white font-bold text-lg">
              F
            </div>
            <div>
              <h1 className="text-lg font-bold text-white tracking-tight">AccessForge</h1>
              <p className="text-[10px] text-forge-400 uppercase tracking-widest">Role Governance</p>
            </div>
          </div>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {nav.map(({ to, icon, label }) => (
            <NavLink
              key={to}
              to={to}
              end={to === '/'}
              className={({ isActive }) => isActive ? 'sidebar-link-active' : 'sidebar-link'}
            >
              <span className="text-base">{icon}</span>
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>

        {/* Tenant info */}
        <div className="p-4 border-t border-forge-800/60">
          <div className="text-xs text-gray-500 mb-2">Connected Tenant</div>
          <div className="text-sm font-medium text-gray-300">corp.onmicrosoft.com</div>
          <div className="flex items-center gap-1.5 mt-1">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
            <span className="text-xs text-emerald-400">All systems connected</span>
          </div>
          <button onClick={handleLogout} className="mt-3 text-xs text-gray-500 hover:text-gray-300">
            Sign out
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-y-auto bg-forge-950">
        <div className="p-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
