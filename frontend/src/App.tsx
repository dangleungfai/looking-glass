import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import { PublicHome } from './pages/PublicHome';
import { Login } from './pages/Login';
import { AdminLayout } from './layouts/AdminLayout';
import { Dashboard } from './pages/admin/Dashboard';
import { PopManage } from './pages/admin/PopManage';
import { DeviceManage } from './pages/admin/DeviceManage';
import { TemplateManage } from './pages/admin/TemplateManage';
import { QueryLogs } from './pages/admin/QueryLogs';
import { Settings } from './pages/admin/Settings';

function RequireAuth({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('token');
  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export const App: React.FC = () => {
  return (
    <ConfigProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<PublicHome />} />
          <Route path="/login" element={<Login />} />
          <Route
            path="/admin"
            element={
              <RequireAuth>
                <AdminLayout />
              </RequireAuth>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="pops" element={<PopManage />} />
            <Route path="devices" element={<DeviceManage />} />
            <Route path="templates" element={<TemplateManage />} />
            <Route path="logs" element={<QueryLogs />} />
            <Route path="settings" element={<Settings />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
};
