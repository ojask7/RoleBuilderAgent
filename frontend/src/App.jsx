import { Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/Layout'
import CISODashboard from './pages/CISODashboard'
import Discovery from './pages/Discovery'
import RoleMining from './pages/RoleMining'
import BusinessRoles from './pages/BusinessRoles'
import AccessBundles from './pages/AccessBundles'
import Integrations from './pages/Integrations'
import Login from './pages/Login'

export default function App() {
  const isAuth = localStorage.getItem('af_user')

  if (!isAuth && window.location.pathname !== '/login') {
    return <Navigate to="/login" replace />
  }

  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route element={<Layout />}>
        <Route path="/" element={<CISODashboard />} />
        <Route path="/discovery" element={<Discovery />} />
        <Route path="/role-mining" element={<RoleMining />} />
        <Route path="/business-roles" element={<BusinessRoles />} />
        <Route path="/access-bundles" element={<AccessBundles />} />
        <Route path="/integrations" element={<Integrations />} />
      </Route>
    </Routes>
  )
}
