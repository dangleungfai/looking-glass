import React, { useEffect, useState } from 'react';
import { Card, Form, Select, Input, Button, Spin, Alert, Typography, Divider } from 'antd';
import { getPops, getQueryTypes, submitQuery } from '../services/api';

const { Title, Text } = Typography;

export const PublicHome: React.FC = () => {
  const [pops, setPops] = useState<{ popCode: string; popName: string; country?: string; city?: string }[]>([]);
  const [queryTypes, setQueryTypes] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingMeta, setLoadingMeta] = useState(true);
  const [result, setResult] = useState<Record<string, unknown> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    Promise.all([getPops(), getQueryTypes()])
      .then(([p, t]) => {
        setPops(p);
        setQueryTypes(t);
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoadingMeta(false));
  }, []);

  const onFinish = (values: { popCode: string; queryType: string; target: string; count?: number }) => {
    setError(null);
    setResult(null);
    setLoading(true);
    submitQuery({
      popCode: values.popCode,
      queryType: values.queryType,
      target: values.target.trim(),
      params: values.count ? { count: values.count } : undefined,
    })
      .then(setResult)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  };

  if (loadingMeta) {
    return (
      <div style={{ padding: 48, textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 24 }}>
      <Title level={2}>ISP Looking Glass</Title>
      <Text type="secondary">选择 POP 与查询类型，输入目标 IP/域名/前缀进行网络诊断。</Text>
      <Divider />
      <Card title="查询">
        <Form form={form} layout="vertical" onFinish={onFinish}>
          <Form.Item name="popCode" label="POP 节点" rules={[{ required: true }]}>
            <Select placeholder="选择 POP" options={pops.map((p) => ({ label: `${p.popName} (${p.popCode})`, value: p.popCode }))} />
          </Form.Item>
          <Form.Item name="queryType" label="查询类型" rules={[{ required: true }]}>
            <Select placeholder="选择类型" options={queryTypes.map((t) => ({ label: t, value: t }))} />
          </Form.Item>
          <Form.Item name="target" label="目标 (IP/域名/前缀)" rules={[{ required: true }]}>
            <Input placeholder="例如 8.8.8.8 或 1.1.1.0/24" />
          </Form.Item>
          <Form.Item name="count" label="Ping 次数 (可选)">
            <Input type="number" min={1} max={10} placeholder="5" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={loading}>
              执行查询
            </Button>
          </Form.Item>
        </Form>
      </Card>
      {error && (
        <Alert type="error" message={error} style={{ marginTop: 16 }} />
      )}
      {result && (
        <Card title="查询结果" style={{ marginTop: 16 }}>
          <p><Text strong>状态:</Text> {(result as { status?: string }).status}</p>
          <p><Text strong>POP:</Text> {(result as { pop?: string }).pop} | <Text strong>设备:</Text> {(result as { device?: string }).device}</p>
          <p><Text strong>耗时:</Text> {(result as { durationMs?: number }).durationMs} ms</p>
          {((result as { result?: { rawText?: string } }).result?.rawText) && (
            <pre style={{ background: '#f5f5f5', padding: 12, overflow: 'auto', maxHeight: 400 }}>
              {((result as { result?: { rawText?: string } }).result?.rawText)}
            </pre>
          )}
        </Card>
      )}
    </div>
  );
};
