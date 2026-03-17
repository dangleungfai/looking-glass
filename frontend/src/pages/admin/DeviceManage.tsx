import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, InputNumber, Select, message } from 'antd';
import { admin } from '../../services/api';

interface Device {
  id: number;
  deviceCode: string;
  deviceName: string;
  vendor: string;
  osType: string;
  mgmtIp: string;
  sshPort: number;
  username: string;
  status: number;
  priority: number;
  timeoutSec: number;
  supportedQueryTypes?: string;
  pop?: { id: number; popCode: string };
}

export const DeviceManage: React.FC = () => {
  const [list, setList] = useState<Device[]>([]);
  const [pops, setPops] = useState<{ id: number; popCode: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const load = () => {
    setLoading(true);
    Promise.all([admin.devices.list(), admin.pops.list()])
      .then(([devices, p]) => {
        setList(Array.isArray(devices) ? devices : []);
        setPops(Array.isArray(p) ? p : []);
      })
      .catch((e) => {
        message.error(e?.message || '加载失败');
        setList([]);
        setPops([]);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openCreate = () => {
    setEditingId(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (record: Device) => {
    setEditingId(record.id);
    form.setFieldsValue({
      deviceCode: record.deviceCode,
      deviceName: record.deviceName,
      vendor: record.vendor,
      osType: record.osType,
      mgmtIp: record.mgmtIp,
      sshPort: record.sshPort,
      username: record.username,
      password: '',
      popId: record.pop?.id,
      status: record.status,
      priority: record.priority,
      timeoutSec: record.timeoutSec,
      supportedQueryTypes: record.supportedQueryTypes,
      remark: (record as { remark?: string }).remark,
    });
    setModalOpen(true);
  };

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      const body = { ...values };
      if (!body.password) delete body.password;
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
    Modal.confirm({ title: '确认删除？', onOk: () => admin.devices.delete(id).then(() => { message.success('已删除'); load(); }).catch((e) => message.error(e.message)) });
  };

  const columns = [
    { title: '设备编码', dataIndex: 'deviceCode', key: 'deviceCode' },
    { title: '设备名称', dataIndex: 'deviceName', key: 'deviceName' },
    { title: '厂商', dataIndex: 'vendor', key: 'vendor' },
    { title: 'OS', dataIndex: 'osType', key: 'osType' },
    { title: '管理 IP', dataIndex: 'mgmtIp', key: 'mgmtIp' },
    { title: 'SSH 端口', dataIndex: 'sshPort', key: 'sshPort' },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: number) => (v === 1 ? '启用' : '停用') },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Device) => (
        <Space>
          <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Button size="small" danger onClick={() => handleDelete(record.id)}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Button type="primary" onClick={openCreate} style={{ marginBottom: 16 }}>新增设备</Button>
      <Table rowKey="id" columns={columns} dataSource={list} loading={loading} pagination={{ pageSize: 10 }} />
      <Modal title={editingId ? '编辑设备' : '新增设备'} open={modalOpen} onOk={handleSubmit} onCancel={() => setModalOpen(false)} width={560}>
        <Form form={form} layout="vertical">
          <Form.Item name="deviceCode" label="设备编码" rules={[{ required: true }]}>
            <Input placeholder="如 HKG1.R1" />
          </Form.Item>
          <Form.Item name="deviceName" label="设备名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="vendor" label="厂商" rules={[{ required: true }]}>
            <Select options={[
              { value: 'CISCO_IOS_XR', label: 'Cisco IOS-XR' },
              { value: 'HUAWEI_VRP', label: 'Huawei VRP' },
              { value: 'MIKROTIK_ROUTEROS', label: 'MikroTik RouterOS' },
            ]} />
          </Form.Item>
          <Form.Item name="osType" label="OS 类型" rules={[{ required: true }]}>
            <Input placeholder="如 IOS-XR" />
          </Form.Item>
          <Form.Item name="mgmtIp" label="管理 IP" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="sshPort" label="SSH 端口">
            <InputNumber min={1} max={65535} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="username" label="用户名" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="密码" help={editingId ? '留空则不修改' : ''}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="popId" label="所属 POP" rules={[{ required: true }]}>
            <Select options={pops.map((p) => ({ value: p.id, label: p.popCode }))} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]} />
          </Form.Item>
          <Form.Item name="priority" label="优先级"><InputNumber style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="timeoutSec" label="超时(秒)"><InputNumber min={5} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="supportedQueryTypes" label="支持的查询类型(逗号分隔)">
            <Input placeholder="PING,TRACEROUTE,BGP_PREFIX" />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};
