import React, { useState } from 'react';
import { Form, Input, Button, Card, message, Typography } from 'antd';
import { useNavigate } from 'react-router-dom';
import { login } from '../services/api';
import { useSystemName } from '../hooks/useSystemName';

const { Title } = Typography;

export const Login: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const systemName = useSystemName();

  const onFinish = (values: { username: string; password: string }) => {
    setLoading(true);
    login(values.username, values.password)
      .then((data) => {
        localStorage.setItem('token', data.token);
        localStorage.setItem('username', data.username);
        if (data.role) localStorage.setItem('lg_role', data.role);
        message.success('登录成功');
        navigate('/admin');
      })
      .catch((e) => message.error(e.message))
      .finally(() => setLoading(false));
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'flex-start',
        justifyContent: 'center',
        background:
          'radial-gradient(circle at top left, #e0f2fe 0, transparent 55%), radial-gradient(circle at bottom right, #e5e7eb 0, transparent 55%), #f5f5f7',
        padding: '20vh 16px 32px',
      }}
    >
      <div style={{ width: '100%', maxWidth: 360 }}>
          <Title level={3} style={{ margin: '0 0 24px', textAlign: 'center' }}>
            {systemName}
          </Title>
          <Card
            bordered={false}
            style={{
              borderRadius: 28,
              boxShadow: '0 18px 45px rgba(15, 23, 42, 0.16)',
              border: '1px solid rgba(148, 163, 184, 0.35)',
              background:
                'radial-gradient(circle at top, rgba(255,255,255,0.9), rgba(248,250,252,0.95))',
            }}
            bodyStyle={{ padding: '28px 28px 24px' }}
          >
            <Form onFinish={onFinish} layout="vertical">
              <Form.Item
                name="username"
                label="用户名"
                rules={[{ required: true, message: '请输入用户名' }]}
                style={{ marginBottom: 16 }}
              >
                <Input size="large" placeholder="admin" autoComplete="username" />
              </Form.Item>
              <Form.Item
                name="password"
                label="密码"
                rules={[{ required: true, message: '请输入密码' }]}
                style={{ marginBottom: 12 }}
              >
                <Input.Password
                  size="large"
                  placeholder="••••••••"
                  autoComplete="current-password"
                />
              </Form.Item>
              <Form.Item style={{ marginBottom: 8 }}>
                <Button
                  type="primary"
                  htmlType="submit"
                  loading={loading}
                  block
                  size="large"
                  style={{
                    borderRadius: 999,
                    fontWeight: 500,
                    background: 'linear-gradient(135deg, #111827, #4b5563)',
                    border: 'none',
                  }}
                >
                  登录
                </Button>
              </Form.Item>
              <div style={{ fontSize: 11, color: '#9ca3af', textAlign: 'center', marginTop: 4 }}>
                若忘记密码，请联系系统管理员重置。
              </div>
            </Form>
          </Card>
      </div>
    </div>
  );
};
