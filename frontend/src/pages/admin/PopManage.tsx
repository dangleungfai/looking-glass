import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, Switch, message } from 'antd';
import { admin } from '../../services/api';
import { canManageResources, resolveCurrentRole } from '../../utils/role';

interface Pop {
  id: number;
  popCode: string;
  popName: string;
  country?: string;
  city?: string;
  isPublic: number;
  status: number;
  remark?: string;
}

export const PopManage: React.FC = () => {
  const canWrite = canManageResources(resolveCurrentRole());
  const [list, setList] = useState<Pop[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const load = () => {
    setLoading(true);
    admin.pops.list().then(setList).catch(() => message.error('加载失败')).finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openCreate = () => {
    if (!canWrite) return;
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({
      country: '中国',
      isPublic: 1,
      status: 1,
    });
    setModalOpen(true);
  };

  const openEdit = (record: Pop) => {
    if (!canWrite) return;
    setEditingId(record.id);
    form.setFieldsValue({
      popCode: record.popCode,
      popName: record.popName,
      country: record.country,
      city: record.city,
      isPublic: record.isPublic,
      status: record.status,
      remark: record.remark,
    });
    setModalOpen(true);
  };

  const handleSubmit = () => {
    if (!canWrite) return;
    form.validateFields().then((values) => {
      if (editingId) {
        admin.pops.update(editingId, values).then(() => {
          message.success('更新成功');
          setModalOpen(false);
          load();
        }).catch((e) => message.error(e.message));
      } else {
        admin.pops.create(values).then(() => {
          message.success('创建成功');
          setModalOpen(false);
          load();
        }).catch((e) => message.error(e.message));
      }
    });
  };

  const handleDelete = (id: number) => {
    if (!canWrite) return;
    Modal.confirm({ title: '确认删除？', onOk: () => admin.pops.delete(id).then(() => { message.success('已删除'); load(); }).catch((e) => message.error(e.message)) });
  };

  const toggleStatus = (record: Pop, enabled: boolean) => {
    if (!canWrite) return;
    admin.pops.update(record.id, {
      popCode: record.popCode,
      popName: record.popName,
      country: record.country,
      city: record.city,
      isPublic: record.isPublic,
      status: enabled ? 1 : 0,
      remark: record.remark,
    }).then(() => {
      message.success(enabled ? '已启用' : '已禁用');
      load();
    }).catch((e) => message.error(e.message));
  };

  const columns = [
    {
      title: '序号',
      key: 'index',
      width: 70,
      render: (_: unknown, __: Pop, index: number) => index + 1,
    },
    { title: '名称', dataIndex: 'popName', key: 'popName' },
    { title: '编码', dataIndex: 'popCode', key: 'popCode' },
    { title: '国家', dataIndex: 'country', key: 'country' },
    { title: '城市', dataIndex: 'city', key: 'city' },
    { title: '公网可见', dataIndex: 'isPublic', key: 'isPublic', render: (v: number) => (v === 1 ? '是' : '否') },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Pop) =>
        canWrite ? (
          <Space>
            <Switch
              size="small"
              checked={record.status === 1}
              onChange={(checked) => toggleStatus(record, checked)}
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

  const handleCopy = (record: Pop) => {
    setEditingId(null);
    const suffix = '-copy';
    form.setFieldsValue({
      popCode: `${record.popCode}${suffix}`,
      popName: record.popName,
      country: record.country || '中国',
      city: record.city,
      isPublic: record.isPublic,
      status: record.status,
      remark: record.remark,
    });
    setModalOpen(true);
  };

  return (
    <>
      {canWrite && <Button type="primary" onClick={openCreate} style={{ marginBottom: 16 }}>新增 POP</Button>}
      <Table rowKey="id" columns={columns} dataSource={list} loading={loading} pagination={{ pageSize: 50 }} />
      <Modal title={editingId ? '编辑 POP' : '新增 POP'} open={modalOpen} onOk={handleSubmit} onCancel={() => setModalOpen(false)} destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="popCode" label="POP 编码" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="popName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="country" label="国家"><Input placeholder="默认：中国" /></Form.Item>
          <Form.Item name="city" label="城市"><Input /></Form.Item>
          <Form.Item name="isPublic" label="公网可见">
            <Select options={[{ value: 1, label: '是' }, { value: 0, label: '否' }]} placeholder="默认：是" />
          </Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};
