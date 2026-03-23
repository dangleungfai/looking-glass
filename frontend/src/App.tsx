import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, theme as antdTheme } from 'antd';
import { PublicHome } from './pages/PublicHome';
import { Login } from './pages/Login';
import { AdminLayout } from './layouts/AdminLayout';
import { Dashboard } from './pages/admin/Dashboard';
import { PopManage } from './pages/admin/PopManage';
import { DeviceManage } from './pages/admin/DeviceManage';
import { TemplateManage } from './pages/admin/TemplateManage';
import { QueryLogs } from './pages/admin/QueryLogs';
import { Settings } from './pages/admin/Settings';
import { useAppearance } from './hooks/useAppearance';

function RequireAuth({ children }: { children: React.ReactNode }) {
  const token = localStorage.getItem('token');
  if (!token) return <Navigate to="/login" replace />;
  return <>{children}</>;
}

export const App: React.FC = () => {
  const appearance = useAppearance();
  const isDark = appearance === 'dark';
  const token =
    appearance === 'techBlue'
      ? {
        colorPrimary: '#1677FF',
        colorInfo: '#1677FF',
        colorSuccess: '#14B8A6',
        colorBgLayout: '#F6FBFF',
        colorBgContainer: '#FFFFFF',
        colorBorder: '#DCEAF5',
        colorText: '#1F2937',
        colorTextSecondary: '#6B7280',
      }
      : isDark
        ? {
          colorPrimary: '#3B82F6',
          colorInfo: '#3B82F6',
          colorSuccess: '#22C55E',
          colorBgLayout: '#0F172A',
          colorBgContainer: '#1E293B',
          colorBorder: '#334155',
          colorText: '#E5E7EB',
          colorTextSecondary: '#94A3B8',
          colorWarning: '#F59E0B',
          colorError: '#EF4444',
        }
        : undefined;

  return (
    <ConfigProvider
      theme={{
        algorithm: isDark ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
        token,
      }}
    >
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
