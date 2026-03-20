import React, { useEffect, useMemo, useState } from 'react';
import { Layout, Menu, Button } from 'antd';
import { Link, Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useSystemName } from '../hooks/useSystemName';
import { useFooterText } from '../hooks/useFooterText';
import { useLogoUrl } from '../hooks/useLogoUrl';
import { getPublicUiConfig } from '../services/api';

const { Header, Content, Footer } = Layout;

const linkStyle: React.CSSProperties = { textDecoration: 'none' };

const adminMenus = [
  { key: '/admin', label: <Link to="/admin" style={linkStyle}>概览</Link> },
  { key: '/admin/pops', label: <Link to="/admin/pops" style={linkStyle}>POP 管理</Link> },
  { key: '/admin/devices', label: <Link to="/admin/devices" style={linkStyle}>设备管理</Link> },
  { key: '/admin/templates', label: <Link to="/admin/templates" style={linkStyle}>命令模板</Link> },
  { key: '/admin/logs', label: <Link to="/admin/logs" style={linkStyle}>查询日志</Link> },
  { key: '/admin/settings', label: <Link to="/admin/settings" style={linkStyle}>系统设置</Link> },
];

export const AdminLayout: React.FC = () => {
  const systemName = useSystemName();
  const footerText = useFooterText();
  const logoUrl = useLogoUrl();
  const [hasCustomLogo, setHasCustomLogo] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const username = useMemo(() => (localStorage.getItem('username') || '').trim(), [location.pathname]);

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('lg_role');
    navigate('/login');
  };

  useEffect(() => {
    let mounted = true;
    getPublicUiConfig()
      .then((cfg) => {
        if (mounted) setHasCustomLogo(Boolean(cfg?.hasCustomLogo));
      })
      .catch(() => {
        if (mounted) setHasCustomLogo(false);
      });
    const onLogoChanged = () => {
      getPublicUiConfig()
        .then((cfg) => {
          if (mounted) setHasCustomLogo(Boolean(cfg?.hasCustomLogo));
        })
        .catch(() => {
          if (mounted) setHasCustomLogo(false);
        });
    };
    window.addEventListener('lg-logo-changed', onLogoChanged);
    return () => {
      mounted = false;
      window.removeEventListener('lg-logo-changed', onLogoChanged);
    };
  }, []);

  return (
    <>
      <style>{`
        .admin-top-menu.ant-menu-horizontal > .ant-menu-item::after,
        .admin-top-menu.ant-menu-horizontal > .ant-menu-submenu::after {
          border-bottom: none !important;
        }
      `}</style>
      <Layout
        style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          background:
            'radial-gradient(circle at top left, #e0f2fe 0, transparent 55%), radial-gradient(circle at bottom right, #e5e7eb 0, transparent 55%), #f5f5f7',
        }}
      >
        <Header
          style={{
            display: 'flex',
            alignItems: 'center',
            height: 64,
            lineHeight: 1.2,
            padding: 0,
            paddingInline: 32,
            background: 'rgba(248,250,252,0.86)',
            backdropFilter: 'blur(10px)',
            borderBottom: '1px solid rgba(148,163,184,0.35)',
          }}
        >
        {/* 与下方主内容区同宽（maxWidth 1200 + 居中），左右与 Content 的 32px 内边距一致 */}
        <div
          style={{
            maxWidth: 1200,
            margin: '0 auto',
            width: '100%',
            height: '100%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: 16,
          }}
        >
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 12,
              flexShrink: 0,
            }}
          >
            <img
              src={logoUrl}
              alt="logo"
              style={{
                width: 128,
                height: 32,
                borderRadius: 0,
                objectFit: 'fill',
                background: 'transparent',
                flexShrink: 0,
                display: 'block',
              }}
              onError={(e) => {
                (e.currentTarget as HTMLImageElement).style.display = 'none';
              }}
            />
            {!hasCustomLogo && (
              <span
                style={{
                  fontSize: 15,
                  fontWeight: 600,
                  color: '#111827',
                  lineHeight: '20px',
                }}
              >
                {systemName}
              </span>
            )}
          </div>
            <Menu
              mode="horizontal"
              selectedKeys={[location.pathname]}
              items={adminMenus}
              className="admin-top-menu"
              style={{
                flex: 1,
                minWidth: 0,
                marginInline: 8,
                borderBottom: 'none',
                background: 'transparent',
                ['--ant-menu-active-bar-height' as string]: '0px',
              }}
            />
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 4,
              flexShrink: 0,
            }}
          >
            <span
              style={{
                fontSize: 13,
                color: '#4b5563',
                maxWidth: 200,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
              title={username || undefined}
            >
              {username || '-'}
            </span>
            <Button type="text" onClick={logout} style={{ color: '#111827', fontSize: 12, padding: '0 8px' }}>
              退出
            </Button>
          </div>
        </div>
        </Header>
        <Content style={{ flex: 1, padding: '24px 32px 32px', minHeight: 0 }}>
        <div
          style={{
            maxWidth: 1200,
            margin: '0 auto',
            background: 'rgba(255,255,255,0.94)',
            borderRadius: 32,
            boxShadow: '0 24px 80px rgba(15,23,42,0.18)',
            border: '1px solid rgba(148,163,184,0.45)',
            padding: 24,
          }}
        >
          <Outlet />
        </div>
        </Content>
        <Footer
          style={{
            margin: 0,
            padding: 0,
            flexShrink: 0,
            background: 'transparent',
          }}
        >
        <div
          style={{
            width: '100%',
            background: '#eef2ff',
            borderTop: '1px solid #dbeafe',
            color: '#64748b',
            fontSize: 12,
          }}
        >
          <div
            style={{
              maxWidth: 1200,
              margin: '0 auto',
              padding: '14px 32px',
              textAlign: 'center',
              minHeight: footerText ? undefined : 8,
            }}
          >
            {footerText}
          </div>
        </div>
        </Footer>
      </Layout>
    </>
  );
};
