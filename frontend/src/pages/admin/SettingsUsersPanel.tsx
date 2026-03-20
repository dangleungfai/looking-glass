import React, { useEffect, useState } from 'react';
import { Button, Form, Input, Modal, Select, Space, Table, Tag, message } from 'antd';
import { admin } from '../../services/api';

interface UserRow {
  id: number;
  username: string;
  email?: string;
  mobile?: string;
  userType?: string;
  roleCode: string;
  roleName?: string;
  status: number;
  createdAt?: string;
}

interface RoleOpt {
  id: number;
  roleCode: string;
  roleName: string;
}

const FALLBACK_ROLES: RoleOpt[] = [
  { id: 1, roleCode: 'ADMIN', roleName: '管理员' },
  { id: 2, roleCode: 'OPS', roleName: '运维' },
  { id: 3, roleCode: 'READONLY', roleName: '只读' },
];

const sortUsers = (rows: UserRow[]): UserRow[] =>
  [...rows].sort((a, b) => {
    const aAdmin = a.username?.toLowerCase() === 'admin' ? 0 : 1;
    const bAdmin = b.username?.toLowerCase() === 'admin' ? 0 : 1;
    if (aAdmin !== bAdmin) return aAdmin - bAdmin;
    return (a.username || '').localeCompare(b.username || '', 'zh-CN', { sensitivity: 'base' });
  });

export const SettingsUsersPanel: React.FC = () => {
  const [list, setList] = useState<UserRow[]>([]);
  const [roles, setRoles] = useState<RoleOpt[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [userPasswordVisible, setUserPasswordVisible] = useState(false);
  const [form] = Form.useForm();

  const load = () => {
    setLoading(true);
    Promise.allSettled([admin.users.list(), admin.roles.list()])
      .then(([usersRes, rolesRes]) => {
        if (usersRes.status === 'fulfilled') {
          setList(Array.isArray(usersRes.value) ? sortUsers(usersRes.value) : []);
        } else {
          setList([]);
          message.error(usersRes.reason?.message || '用户列表加载失败');
        }

        if (rolesRes.status === 'fulfilled') {
          setRoles(Array.isArray(rolesRes.value) ? rolesRes.value : FALLBACK_ROLES);
        } else {
          setRoles(FALLBACK_ROLES);
          message.warning('角色接口不可用，已使用内置角色列表');
        }
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openCreate = () => {
    setEditingId(null);
    setUserPasswordVisible(false);
    form.resetFields();
    form.setFieldsValue({ status: 1 });
    setModalOpen(true);
  };

  const openEdit = (row: UserRow) => {
    setEditingId(row.id);
    setUserPasswordVisible(false);
    form.setFieldsValue({
      username: row.username,
      roleCode: row.roleCode,
      email: row.email || '',
      mobile: row.mobile || '',
      status: row.status,
      password: '',
    });
    setModalOpen(true);
  };

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      const body: Record<string, unknown> = {
        roleCode: values.roleCode,
        email: values.email?.trim() || undefined,
        mobile: values.mobile?.trim() || undefined,
        status: values.status,
      };
      if (values.password && String(values.password).trim()) {
        body.password = values.password;
      }
      if (editingId) {
        admin.users
          .update(editingId, body)
          .then(() => {
            message.success('已更新');
            setModalOpen(false);
            load();
          })
          .catch((e) => message.error(e.message));
      } else {
        admin.users
          .create({
            username: values.username.trim(),
            password: values.password,
            roleCode: values.roleCode,
            email: values.email?.trim(),
            mobile: values.mobile?.trim(),
            status: values.status,
          })
          .then(() => {
            message.success('已创建');
            setModalOpen(false);
            load();
          })
          .catch((e) => message.error(e.message));
      }
    });
  };

  const handleDelete = (row: UserRow) => {
    if (row.username?.toLowerCase() === 'admin') {
      message.warning('admin 为系统内置账号，不能删除');
      return;
    }
    Modal.confirm({
      title: `确认删除用户「${row.username}」？`,
      onOk: () =>
        admin.users.delete(row.id).then(() => {
          message.success('已删除');
          load();
        }).catch((e) => message.error(e.message)),
    });
  };

  const columns = [
    { title: '用户名', dataIndex: 'username', key: 'username' },
    {
      title: '用户类型',
      dataIndex: 'userType',
      key: 'userType',
      render: (t?: string) =>
        (t || 'LOCAL').toUpperCase() === 'LDAP' ? <Tag color="blue">LDAP</Tag> : <Tag>本地</Tag>,
    },
    { title: '角色', dataIndex: 'roleName', key: 'roleName', render: (_: string, r: UserRow) => r.roleName || r.roleCode },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (s: number) => (s === 1 ? <Tag color="green">启用</Tag> : <Tag>禁用</Tag>),
    },
    { title: '邮箱', dataIndex: 'email', key: 'email', ellipsis: true },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, row: UserRow) => (
        <Space>
          <Button size="small" onClick={() => openEdit(row)}>
            编辑
          </Button>
          <Button
            size="small"
            danger
            disabled={row.username?.toLowerCase() === 'admin'}
            onClick={() => handleDelete(row)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" onClick={openCreate}>
          新增用户
        </Button>
      </div>
      <Table rowKey="id" columns={columns} dataSource={list} loading={loading} pagination={{ pageSize: 20 }} />
      <Modal
        title={editingId ? '编辑用户' : '新增用户'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        width={520}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          {!editingId && (
            <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
              <Input autoComplete="off" />
            </Form.Item>
          )}
          <Form.Item
            name="password"
            label={editingId ? '新密码（留空不修改）' : '密码'}
            rules={editingId ? [] : [{ required: true, min: 6, message: '至少 6 位' }]}
          >
            <Input
              type={userPasswordVisible ? 'text' : 'password'}
              autoComplete="new-password"
              placeholder={editingId ? '留空不修改' : '至少 6 位'}
              suffix={(
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => setUserPasswordVisible((v) => !v)}
                  aria-label={userPasswordVisible ? '隐藏密码' : '显示密码'}
                  style={{ border: 'none', background: 'transparent', cursor: 'pointer', padding: 0 }}
                >
                  {userPasswordVisible ? '🙈' : '👁'}
                </button>
              )}
            />
          </Form.Item>
          <Form.Item name="roleCode" label="角色" rules={[{ required: true }]}>
            <Select
              options={roles.map((r) => ({
                value: r.roleCode,
                label: `${r.roleName} (${r.roleCode})`,
              }))}
            />
          </Form.Item>
          <Form.Item name="email" label="邮箱">
            <Input />
          </Form.Item>
          <Form.Item name="mobile" label="手机">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="状态" rules={[{ required: true }]}>
            <Select
              options={[
                { value: 1, label: '启用' },
                { value: 0, label: '禁用' },
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};
