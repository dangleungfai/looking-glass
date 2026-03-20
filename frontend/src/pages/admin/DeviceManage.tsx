import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, InputNumber, Select, Switch, message } from 'antd';
import { admin, systemSettings, type SystemSettingsDto } from '../../services/api';
import { useShowPopCode } from '../../hooks/useShowPopCode';
import { canManageResources, resolveCurrentRole } from '../../utils/role';

interface Device {
  id: number;
  deviceName: string;
  vendor: string;
  popCode?: string;
  mgmtIp: string;
  sshPort: number;
  authType: string;
  username?: string;
  status: number;
}

interface PopOption {
  id: number;
  popCode: string;
  popName: string;
}

export const DeviceManage: React.FC = () => {
  const canWrite = canManageResources(resolveCurrentRole());
  const showPopCode = useShowPopCode();
  const [list, setList] = useState<Device[]>([]);
  const [pops, setPops] = useState<PopOption[]>([]);
  const [defaults, setDefaults] = useState<SystemSettingsDto['deviceDefaults'] | null>(null);
  const [vendors, setVendors] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [deviceLoginPasswordVisible, setDeviceLoginPasswordVisible] = useState(false);
  const [form] = Form.useForm();

  const normalizeAuthType = (value?: string) => {
    const v = (value || '').toUpperCase();
    if (v === 'TELNET') return 'TELNET';
    return 'SSH';
  };

  const getDefaultPort = (d: SystemSettingsDto['deviceDefaults'] | null, authType: string) => {
    if (!d) return authType === 'TELNET' ? 23 : 22;
    return authType === 'TELNET' ? (d.telnetPort || 23) : (d.sshPort || 22);
  };

  const formatVendor = (vendor: string) => {
    const labels: Record<string, string> = {
      CISCO_IOS_XR: 'Cisco',
      JUNIPER_JUNOS: 'Juniper',
      HUAWEI_VRP: 'Huawei',
    };
    return labels[vendor] || vendor;
  };

  const load = () => {
    setLoading(true);
    Promise.all([admin.devices.list(), systemSettings.get(), admin.templates.list(), admin.pops.list()])
      .then(([devices, s, templates, popList]) => {
        setList(Array.isArray(devices) ? devices : []);
        setPops(Array.isArray(popList) ? popList : []);
        setDefaults({
          ...s.deviceDefaults,
          authType: normalizeAuthType(s.deviceDefaults?.authType),
          sshPort: s.deviceDefaults?.sshPort || 22,
          telnetPort: s.deviceDefaults?.telnetPort || 23,
          username: s.deviceDefaults?.username || 'admin',
          password: s.deviceDefaults?.password || '',
          timeoutSec: s.deviceDefaults?.timeoutSec || 15,
          maxConcurrency: s.deviceDefaults?.maxConcurrency || 10,
        });
        const vendorSet = new Set<string>();
        if (Array.isArray(templates)) {
          templates.forEach((t) => {
            const v = (t?.vendor || '').toString().trim();
            if (v) vendorSet.add(v);
          });
        }
        setVendors(Array.from(vendorSet));
      })
      .catch((e) => {
        message.error(e?.message || '加载失败');
        setList([]);
        setPops([]);
        setDefaults(null);
        setVendors([]);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openCreate = () => {
    if (!canWrite) return;
    setEditingId(null);
    setDeviceLoginPasswordVisible(false);
    form.resetFields();
    if (defaults) {
      const authType = normalizeAuthType(defaults.authType);
      form.setFieldsValue({
        popId: pops.length > 0 ? pops[0].id : undefined,
        sshPort: getDefaultPort(defaults, authType),
        authType,
        username: '',
        password: '',
        status: 1,
      });
    } else {
      form.setFieldsValue({
        popId: pops.length > 0 ? pops[0].id : undefined,
        sshPort: 22,
        authType: 'SSH',
        username: '',
        password: '',
        status: 1,
      });
    }
    setModalOpen(true);
  };

  const openEdit = (record: Device) => {
    if (!canWrite) return;
    setEditingId(record.id);
    setDeviceLoginPasswordVisible(false);
    const popId = pops.find((p) => p.popCode === record.popCode)?.id;
    form.setFieldsValue({
      deviceName: record.deviceName,
      popId,
      vendor: record.vendor,
      mgmtIp: record.mgmtIp,
      sshPort: record.sshPort,
      authType: normalizeAuthType(record.authType),
      username: '',
      password: '',
      status: record.status,
    });
    setModalOpen(true);
  };

  const handleCopy = (record: Device) => {
    if (!canWrite) return;
    setEditingId(null);
    setDeviceLoginPasswordVisible(false);
    const popId = pops.find((p) => p.popCode === record.popCode)?.id;
    form.setFieldsValue({
      deviceName: `${record.deviceName}-copy`,
      popId,
      vendor: record.vendor,
      mgmtIp: record.mgmtIp,
      sshPort: record.sshPort,
      authType: normalizeAuthType(record.authType),
      username: '',
      password: '',
      status: 1,
    });
    setModalOpen(true);
  };

  const handleSubmit = () => {
    if (!canWrite) return;
    form.validateFields().then((values) => {
      const body: Record<string, unknown> = { ...values, authType: normalizeAuthType(values.authType) };
      if (!editingId) {
        body.status = 1;
      }
      if (editingId) {
        admin.devices.update(editingId, body).then(() => {
          message.success('更新成功');
          setModalOpen(false);
          load();
        }).catch((e) => message.error(e.message));
      } else {
        admin.devices.create(body).then(() => {
          message.success('创建成功');
          setModalOpen(false);
          load();
        }).catch((e) => message.error(e.message));
      }
    });
  };

  const handleDelete = (id: number) => {
    if (!canWrite) return;
    Modal.confirm({ title: '确认删除？', onOk: () => admin.devices.delete(id).then(() => { message.success('已删除'); load(); }).catch((e) => message.error(e.message)) });
  };

  const toggleStatus = (id: number, enabled: boolean) => {
    if (!canWrite) return;
    admin.devices.updateStatus(id, enabled ? 1 : 0)
      .then(() => {
        message.success(enabled ? '已启用' : '已禁用');
        load();
      })
      .catch((e) => message.error(e.message));
  };

  const columns = [
    {
      title: '序号',
      key: 'index',
      width: 70,
      render: (_: unknown, __: Device, index: number) => index + 1,
    },
    { title: '设备名称', dataIndex: 'deviceName', key: 'deviceName' },
    { title: 'POP', dataIndex: 'popCode', key: 'popCode' },
    { title: '管理 IP', dataIndex: 'mgmtIp', key: 'mgmtIp' },
    { title: '厂商', dataIndex: 'vendor', key: 'vendor', render: (v: string) => formatVendor(v) },
    { title: '登录方式', dataIndex: 'authType', key: 'authType', render: (v: string) => normalizeAuthType(v) },
    { title: '连接端口', dataIndex: 'sshPort', key: 'sshPort' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Device) =>
        canWrite ? (
          <Space>
            <Switch
              size="small"
              checked={record.status === 1}
              onChange={(checked) => toggleStatus(record.id, checked)}
              checkedChildren="启用"
              unCheckedChildren="禁用"
            />
            <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
            <Button size="small" onClick={() => handleCopy(record)}>复制</Button>
            <Button size="small" danger onClick={() => handleDelete(record.id)}>删除</Button>
          </Space>
        ) : (
          '-'
        ),
    },
  ];

  return (
    <>
      {canWrite && <Button type="primary" onClick={openCreate} style={{ marginBottom: 16 }}>新增设备</Button>}
      <Table rowKey="id" columns={columns} dataSource={list} loading={loading} pagination={{ pageSize: 50 }} />
      <Modal
        title={editingId ? '编辑设备' : '新增设备'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => {
          setDeviceLoginPasswordVisible(false);
          setModalOpen(false);
        }}
        width={560}
      >
        <Form
          form={form}
          layout="vertical"
          onValuesChange={(changedValues) => {
            if (!editingId && changedValues.authType) {
              const mode = normalizeAuthType(changedValues.authType);
              form.setFieldValue('sshPort', getDefaultPort(defaults, mode));
            }
          }}
        >
          <Form.Item name="deviceName" label="设备名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="popId" label="关联 POP 节点" rules={[{ required: true, message: '请选择 POP 节点' }]}>
            <Select
              placeholder="请选择 POP 节点"
              options={pops.map((p) => ({ value: p.id, label: showPopCode ? `${p.popName} (${p.popCode})` : p.popName }))}
            />
          </Form.Item>
          <Form.Item name="mgmtIp" label="管理 IP" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="vendor" label="厂商" rules={[{ required: true }]}>
            <Select options={vendors.map((v) => ({ value: v, label: formatVendor(v) }))} />
          </Form.Item>
          <Form.Item name="authType" label="登录方式" rules={[{ required: true }]}>
            <Select options={[{ value: 'SSH', label: 'SSH' }, { value: 'TELNET', label: 'TELNET' }]} />
          </Form.Item>
          <Form.Item name="sshPort" label="连接端口" rules={[{ required: true }]}>
            <InputNumber min={1} max={65535} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="username" label="登录用户名">
            <Input placeholder="留空则使用系统默认用户名" />
          </Form.Item>
          <Form.Item
            name="password"
            label="登录密码"
          >
            <Input
              type={deviceLoginPasswordVisible ? 'text' : 'password'}
              placeholder="留空则使用系统默认密码"
              suffix={(
                <button
                  type="button"
                  onMouseDown={(e) => e.preventDefault()}
                  onClick={() => setDeviceLoginPasswordVisible((v) => !v)}
                  aria-label={deviceLoginPasswordVisible ? '隐藏密码' : '显示密码'}
                  style={{ border: 'none', background: 'transparent', cursor: 'pointer', padding: 0 }}
                >
                  {deviceLoginPasswordVisible ? '🙈' : '👁'}
                </button>
              )}
            />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};
