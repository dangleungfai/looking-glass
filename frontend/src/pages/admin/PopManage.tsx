import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, InputNumber, Select, message } from 'antd';
import { admin } from '../../services/api';

interface Pop {
  id: number;
  popCode: string;
  popName: string;
  country?: string;
  city?: string;
  isPublic: number;
  status: number;
  sortOrder: number;
  remark?: string;
}

export const PopManage: React.FC = () => {
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
    setEditingId(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (record: Pop) => {
    setEditingId(record.id);
    form.setFieldsValue({
      popCode: record.popCode,
      popName: record.popName,
      country: record.country,
      city: record.city,
      isPublic: record.isPublic,
      status: record.status,
      sortOrder: record.sortOrder,
      remark: record.remark,
    });
    setModalOpen(true);
  };

  const handleSubmit = () => {
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
    Modal.confirm({ title: '确认删除？', onOk: () => admin.pops.delete(id).then(() => { message.success('已删除'); load(); }).catch((e) => message.error(e.message)) });
  };

  const columns = [
    { title: '编码', dataIndex: 'popCode', key: 'popCode' },
    { title: '名称', dataIndex: 'popName', key: 'popName' },
    { title: '国家', dataIndex: 'country', key: 'country' },
    { title: '城市', dataIndex: 'city', key: 'city' },
    { title: '公网可见', dataIndex: 'isPublic', key: 'isPublic', render: (v: number) => (v === 1 ? '是' : '否') },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: number) => (v === 1 ? '启用' : '停用') },
    { title: '排序', dataIndex: 'sortOrder', key: 'sortOrder' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Pop) => (
        <Space>
          <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Button size="small" danger onClick={() => handleDelete(record.id)}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Button type="primary" onClick={openCreate} style={{ marginBottom: 16 }}>新增 POP</Button>
      <Table rowKey="id" columns={columns} dataSource={list} loading={loading} pagination={{ pageSize: 10 }} />
      <Modal title={editingId ? '编辑 POP' : '新增 POP'} open={modalOpen} onOk={handleSubmit} onCancel={() => setModalOpen(false)}>
        <Form form={form} layout="vertical">
          <Form.Item name="popCode" label="POP 编码" rules={[{ required: true }]}>
            <Input disabled={!!editingId} />
          </Form.Item>
          <Form.Item name="popName" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="country" label="国家"><Input /></Form.Item>
          <Form.Item name="city" label="城市"><Input /></Form.Item>
          <Form.Item name="isPublic" label="公网可见">
            <Select options={[{ value: 1, label: '是' }, { value: 0, label: '否' }]} />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]} />
          </Form.Item>
          <Form.Item name="sortOrder" label="排序"><InputNumber min={0} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="remark" label="备注"><Input.TextArea /></Form.Item>
        </Form>
      </Modal>
    </>
  );
};
