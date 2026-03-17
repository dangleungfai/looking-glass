import React, { useEffect, useState } from 'react';
import { Table, Button, Input, Form, Modal, message } from 'antd';
import { admin } from '../../services/api';

interface Setting {
  id: number;
  settingKey: string;
  settingValue: string;
  description?: string;
}

export const Settings: React.FC = () => {
  const [list, setList] = useState<Setting[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingKey, setEditingKey] = useState<string | null>(null);
  const [editValue, setEditValue] = useState('');

  const load = () => {
    setLoading(true);
    admin.settings.list().then(setList).catch(() => message.error('加载失败')).finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openEdit = (record: Setting) => {
    setEditingKey(record.settingKey);
    setEditValue(record.settingValue);
  };

  const saveEdit = () => {
    if (!editingKey) return;
    admin.settings.update(editingKey, editValue).then(() => {
      message.success('已保存');
      setEditingKey(null);
      load();
    }).catch((e) => message.error(e.message));
  };

  const columns = [
    { title: '键', dataIndex: 'settingKey', key: 'settingKey' },
    { title: '值', dataIndex: 'settingValue', key: 'settingValue' },
    { title: '说明', dataIndex: 'description', key: 'description' },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: Setting) => (
        <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
      ),
    },
  ];

  return (
    <>
      <Table rowKey="id" columns={columns} dataSource={list} loading={loading} pagination={false} />
      <Modal title="编辑设置" open={!!editingKey} onOk={saveEdit} onCancel={() => setEditingKey(null)}>
        <Form layout="vertical">
          <Form.Item label="值">
            <Input value={editValue} onChange={(e) => setEditValue(e.target.value)} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
};
