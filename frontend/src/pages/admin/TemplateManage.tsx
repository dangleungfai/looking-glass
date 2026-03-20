import React, { useEffect, useState } from 'react';
import { Table, Button, Space, Modal, Form, Input, Select, Switch, message, Typography } from 'antd';
import { admin, getQueryTypes } from '../../services/api';
import { formatQueryType, normalizeQueryType, normalizeQueryTypes } from '../../utils/queryType';
import { canManageResources, resolveCurrentRole } from '../../utils/role';

interface Template {
  id: number;
  vendor: string;
  templateName: string;
  queryType: string;
  commandText: string;
  parameterSchema: string;
  status: number;
}

export const TemplateManage: React.FC = () => {
  const canWrite = canManageResources(resolveCurrentRole());
  const [list, setList] = useState<Template[]>([]);
  const [queryTypeOptions, setQueryTypeOptions] = useState<{ value: string; label: string }[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const load = () => {
    setLoading(true);
    Promise.all([admin.templates.list(), getQueryTypes()])
      .then(([templates, queryTypes]) => {
        const sortedTemplates = (Array.isArray(templates) ? templates : [])
          .slice()
          .sort((a, b) => String(a.templateName || '').localeCompare(String(b.templateName || ''), 'zh-Hans-CN'));
        setList(sortedTemplates);
        const options = normalizeQueryTypes(Array.isArray(queryTypes) ? queryTypes : [])
          .map((q) => ({ value: q, label: formatQueryType(q) }));
        setQueryTypeOptions(options);
      })
      .catch(() => message.error('加载失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openCreate = () => {
    if (!canWrite) return;
    setEditingId(null);
    form.resetFields();
    form.setFieldsValue({ status: 1 });
    setModalOpen(true);
  };

  const openEdit = (record: Template) => {
    if (!canWrite) return;
    setEditingId(record.id);
    form.setFieldsValue({
      vendor: record.vendor,
      templateName: record.templateName,
      queryType: normalizeQueryType(record.queryType),
      commandText: record.commandText,
      parameterSchema: record.parameterSchema,
      status: record.status,
    });
    setModalOpen(true);
  };

  const handleSubmit = () => {
    if (!canWrite) return;
    form.validateFields().then((values) => {
      const payload = {
        ...values,
        templateName: (values.templateName && String(values.templateName).trim())
          ? String(values.templateName).trim()
          : buildDefaultTemplateName(values.vendor, values.queryType),
      };
      if (editingId) {
        admin.templates.update(editingId, payload).then(() => {
          message.success('更新成功');
          setModalOpen(false);
          load();
        }).catch((e) => message.error(e.message));
      } else {
        admin.templates.create(payload).then(() => {
          message.success('创建成功');
          setModalOpen(false);
          load();
        }).catch((e) => message.error(e.message));
      }
    });
  };

  const handleDelete = (id: number) => {
    if (!canWrite) return;
    Modal.confirm({ title: '确认删除？', onOk: () => admin.templates.delete(id).then(() => { message.success('已删除'); load(); }).catch((e) => message.error(e.message)) });
  };

  const handleCopy = (record: Template) => {
    if (!canWrite) return;
    setEditingId(null);
    form.setFieldsValue({
      vendor: record.vendor,
      templateName: `${record.templateName || record.queryType}-copy`,
      queryType: normalizeQueryType(record.queryType),
      commandText: record.commandText,
      parameterSchema: record.parameterSchema,
      status: 1,
    });
    setModalOpen(true);
  };

  const toggleStatus = (record: Template, enabled: boolean) => {
    if (!canWrite) return;
    admin.templates.update(record.id, {
      vendor: record.vendor,
      templateName: record.templateName,
      queryType: normalizeQueryType(record.queryType),
      commandText: record.commandText,
      parameterSchema: record.parameterSchema,
      status: enabled ? 1 : 0,
    }).then(() => {
      message.success(enabled ? '已启用' : '已禁用');
      load();
    }).catch((e) => message.error(e.message));
  };

  const formatVendor = (vendor: string) => {
    const labels: Record<string, string> = {
      CISCO_IOS_XR: 'Cisco',
      JUNIPER_JUNOS: 'Juniper',
      HUAWEI_VRP: 'Huawei',
    };
    return labels[vendor] || vendor;
  };

  const shortVendor = (vendor?: string) => {
    const labels: Record<string, string> = {
      CISCO_IOS_XR: 'Cisco',
      JUNIPER_JUNOS: 'Juniper',
      HUAWEI_VRP: 'Huawei',
    };
    if (!vendor) return '';
    return labels[vendor] || vendor;
  };

  const buildDefaultTemplateName = (vendor?: string, queryType?: string) => {
    const v = shortVendor((vendor || '').trim());
    const q = formatQueryType(normalizeQueryType(queryType || ''));
    if (v && q && q !== '-') return `${v}_${q}`;
    if (q && q !== '-') return q;
    return v;
  };

  const columns = [
    {
      title: '序号',
      key: 'index',
      width: 70,
      render: (_: unknown, __: Template, index: number) => index + 1,
    },
    { title: '模板名称', dataIndex: 'templateName', key: 'templateName', width: 180, ellipsis: true },
    { title: '厂商', dataIndex: 'vendor', key: 'vendor', width: 100, render: (v: string) => formatVendor(v) },
    { title: '查询类型', dataIndex: 'queryType', key: 'queryType', width: 140, render: (v: string) => formatQueryType(v) },
    { title: '命令文本', dataIndex: 'commandText', key: 'commandText', width: 280, ellipsis: true },
    { title: '参数', dataIndex: 'parameterSchema', key: 'parameterSchema', width: 200, ellipsis: true },
    {
      title: '操作',
      key: 'action',
      width: 280,
      fixed: 'right' as const,
      render: (_: unknown, record: Template) =>
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

  return (
    <div>
      <Typography.Title level={5} style={{ marginBottom: 4 }}>
        命令模板
      </Typography.Title>
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        按厂商维护查询命令模板；禁用的模板不会在公网查询类型中出现。
      </Typography.Text>
      <div style={{ height: 16 }} />
      {canWrite && (
        <div style={{ marginBottom: 16 }}>
          <Button type="primary" onClick={openCreate}>
            新增模板
          </Button>
        </div>
      )}
      <Table
        rowKey="id"
        columns={columns}
        dataSource={list}
        loading={loading}
        pagination={{ pageSize: 50 }}
        scroll={{ x: 1280 }}
      />
      <Modal title={editingId ? '编辑模板' : '新增模板'} open={modalOpen} onOk={handleSubmit} onCancel={() => setModalOpen(false)} width={640}>
        <Form
          form={form}
          layout="vertical"
          onValuesChange={(changed, all) => {
            if (editingId) return;
            if (!('vendor' in changed) && !('queryType' in changed)) return;
            const currentName = String(all.templateName || '').trim();
            if (currentName) return;
            const autoName = buildDefaultTemplateName(all.vendor, all.queryType);
            if (autoName) {
              form.setFieldValue('templateName', autoName);
            }
          }}
        >
          <Form.Item name="vendor" label="厂商" rules={[{ required: true }]}>
            <Select options={[
              { value: 'CISCO_IOS_XR', label: 'Cisco' },
              { value: 'JUNIPER_JUNOS', label: 'Juniper' },
              { value: 'HUAWEI_VRP', label: 'Huawei' },
            ]} />
          </Form.Item>
          <Form.Item name="templateName" label="模板名称">
            <Input placeholder="默认：厂商-查询类型（可手动修改）" />
          </Form.Item>
          <Form.Item name="queryType" label="查询类型" rules={[{ required: true }]}>
            <Select options={queryTypeOptions} />
          </Form.Item>
          <Form.Item name="commandText" label="命令文本 (可用 ${target}, ${count}, ${max_hop}, ${prefix}, ${asn})" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="parameterSchema" label="参数 Schema (JSON)">
            <Input.TextArea rows={2} placeholder='{"target":{"type":"string"},"count":{"type":"integer"}}' />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};
