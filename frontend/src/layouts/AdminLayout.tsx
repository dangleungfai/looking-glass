import React from 'react';
import { Layout, Menu, Button } from 'antd';
import { Link, Outlet, useNavigate, useLocation } from 'react-router-dom';

const { Header, Content } = Layout;

const adminMenus = [
  { key: '/admin', label: <Link to="/admin">概览</Link> },
  { key: '/admin/pops', label: <Link to="/admin/pops">POP 管理</Link> },
  { key: '/admin/devices', label: <Link to="/admin/devices">设备管理</Link> },
  { key: '/admin/templates', label: <Link to="/admin/templates">命令模板</Link> },
  { key: '/admin/logs', label: <Link to="/admin/logs">查询日志</Link> },
  { key: '/admin/settings', label: <Link to="/admin/settings">系统设置</Link> },
];

export const AdminLayout: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    navigate('/login');
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        <div style={{ color: '#fff', marginRight: 24 }}>Looking Glass 后台</div>
        <Menu theme="dark" mode="horizontal" selectedKeys={[location.pathname]} items={adminMenus} style={{ flex: 1 }} />
        <Button type="link" onClick={logout} style={{ color: '#fff' }}>
          退出
        </Button>
      </Header>
      <Content style={{ padding: 24 }}>
        <Outlet />
      </Content>
    </Layout>
  );
};
