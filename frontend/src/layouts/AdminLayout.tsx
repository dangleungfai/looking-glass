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
  { key: '/home', label: <a href="/" target="_blank" rel="noopener noreferrer" style={linkStyle}>首页</a> },
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
          background: 'var(--lg-page-bg)',
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
            background: 'var(--lg-header-bg)',
            backdropFilter: 'blur(10px)',
            borderBottom: '1px solid var(--lg-border)',
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
              gap: hasCustomLogo ? 12 : 0,
              flexShrink: 0,
            }}
          >
            {hasCustomLogo ? (
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
                onError={() => setHasCustomLogo(false)}
              />
            ) : (
              <span
                style={{
                  fontSize: 15,
                  fontWeight: 600,
                  color: 'var(--lg-text)',
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
                color: 'var(--lg-text-secondary)',
                maxWidth: 200,
                overflow: 'hidden',
                textOverflow: 'ellipsis',
                whiteSpace: 'nowrap',
              }}
              title={username || undefined}
            >
              {username || '-'}
            </span>
            <Button type="text" onClick={logout} style={{ color: 'var(--lg-text)', fontSize: 12, padding: '0 8px' }}>
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
            background: 'var(--lg-card-bg)',
            borderRadius: 32,
            boxShadow: '0 16px 44px rgba(15,23,42,0.16)',
            border: '1px solid var(--lg-border)',
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
            background: 'var(--lg-footer-bg)',
            borderTop: '1px solid var(--lg-border)',
            color: 'var(--lg-text-secondary)',
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
