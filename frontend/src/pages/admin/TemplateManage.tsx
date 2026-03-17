import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, message } from 'antd';
import { admin } from '../../services/api';

interface Template {
  id: number;
  vendor: string;
  osType: string;
  queryType: string;
  templateName: string;
  commandText: string;
  parameterSchema: string;
  status: number;
}

export const TemplateManage: React.FC = () => {
  const [list, setList] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const load = () => {
    setLoading(true);
    admin.templates.list().then(setList).catch(() => message.error('加载失败')).finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openCreate = () => {
    setEditingId(null);
    form.resetFields();
    setModalOpen(true);
  };

  const openEdit = (record: Template) => {
    setEditingId(record.id);
    form.setFieldsValue({
      vendor: record.vendor,
      osType: record.osType,
      queryType: record.queryType,
      templateName: record.templateName,
      commandText: record.commandText,
      parameterSchema: record.parameterSchema,
      status: record.status,
    });
    setModalOpen(true);
  };

  const handleSubmit = () => {
    form.validateFields().then((values) => {
      if (editingId) {
        admin.templates.update(editingId, values).then(() => {
          message.success('更新成功');
          setModalOpen(false);
          load();
        }).catch((e) => message.error(e.message));
      } else {
        admin.templates.create(values).then(() => {
          message.success('创建成功');
          setModalOpen(false);
          load();
        }).catch((e) => message.error(e.message));
      }
    });
  };

  const handleDelete = (id: number) => {
    Modal.confirm({ title: '确认删除？', onOk: () => admin.templates.delete(id).then(() => { message.success('已删除'); load(); }).catch((e) => message.error(e.message)) });
  };

  const columns = [
    { title: '厂商', dataIndex: 'vendor', key: 'vendor' },
    { title: 'OS', dataIndex: 'osType', key: 'osType' },
    { title: '查询类型', dataIndex: 'queryType', key: 'queryType' },
    { title: '模板名称', dataIndex: 'templateName', key: 'templateName' },
    { title: '命令模板', dataIndex: 'commandText', key: 'commandText', ellipsis: true },
    { title: '状态', dataIndex: 'status', key: 'status', render: (v: number) => (v === 1 ? '启用' : '停用') },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Template) => (
        <Space>
          <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Button size="small" danger onClick={() => handleDelete(record.id)}>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Button type="primary" onClick={openCreate} style={{ marginBottom: 16 }}>新增模板</Button>
      <Table rowKey="id" columns={columns} dataSource={list} loading={loading} pagination={{ pageSize: 10 }} />
      <Modal title={editingId ? '编辑模板' : '新增模板'} open={modalOpen} onOk={handleSubmit} onCancel={() => setModalOpen(false)} width={640}>
        <Form form={form} layout="vertical">
          <Form.Item name="vendor" label="厂商" rules={[{ required: true }]}>
            <Select options={[
              { value: 'CISCO_IOS_XR', label: 'Cisco IOS-XR' },
              { value: 'HUAWEI_VRP', label: 'Huawei VRP' },
              { value: 'MIKROTIK_ROUTEROS', label: 'MikroTik RouterOS' },
            ]} />
          </Form.Item>
          <Form.Item name="osType" label="OS 类型" rules={[{ required: true }]}>
            <Input placeholder="IOS-XR" />
          </Form.Item>
          <Form.Item name="queryType" label="查询类型" rules={[{ required: true }]}>
            <Select options={['PING', 'TRACEROUTE', 'BGP_PREFIX', 'BGP_ASN'].map((t) => ({ value: t, label: t }))} />
          </Form.Item>
          <Form.Item name="templateName" label="模板名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="commandText" label="命令文本 (可用 ${target}, ${count}, ${max_hop}, ${prefix}, ${asn})" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="parameterSchema" label="参数 Schema (JSON)">
            <Input.TextArea rows={2} placeholder='{"target":{"type":"string"},"count":{"type":"integer"}}' />
          </Form.Item>
          <Form.Item name="status" label="状态">
            <Select options={[{ value: 1, label: '启用' }, { value: 0, label: '停用' }]} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};
