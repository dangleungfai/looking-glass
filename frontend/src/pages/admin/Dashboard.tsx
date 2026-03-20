import React, { useEffect, useMemo, useState } from 'react';
import { Card, Col, Empty, Progress, Row, Spin, Statistic, Tag, Typography } from 'antd';
import { admin } from '../../services/api';
import { formatQueryType } from '../../utils/queryType';

const { Title } = Typography;

interface DashboardViz {
  days: number;
  trend: { bucket: string; total: number; success: number; failed: number }[];
  topTargets: { target: string; count: number }[];
  security: {
    hours: number;
    totalQueries: number;
    failedQueries: number;
    successRate: number;
    uniqueSourceIps: number;
    blacklistActive: number;
    ldapUsers: number;
    localUsers: number;
    topFailSourceIps: { sourceIp: string; count: number }[];
  };
}

const colors = ['#1677ff', '#52c41a', '#faad14', '#f5222d', '#722ed1', '#13c2c2'];

export const Dashboard: React.FC = () => {
  const [loading, setLoading] = useState(true);
  const [pops, setPops] = useState<any[]>([]);
  const [devices, setDevices] = useState<any[]>([]);
  const [templates, setTemplates] = useState<any[]>([]);
  const [logs, setLogs] = useState<any[]>([]);
  const [viz, setViz] = useState<DashboardViz | null>(null);

  useEffect(() => {
    setLoading(true);
    Promise.all([
      admin.pops.list(),
      admin.devices.list(),
      admin.templates.list(),
      admin.dashboard.visualization(30, 24),
      admin.queryLogs.list(0, 200),
    ])
      .then(([p, d, t, v, l]) => {
        setPops(Array.isArray(p) ? p : []);
        setDevices(Array.isArray(d) ? d : []);
        setTemplates(Array.isArray(t) ? t : []);
        setViz(v);
        setLogs(Array.isArray(l?.content) ? l.content : []);
      })
      .finally(() => setLoading(false));
  }, []);

  const queryTypeStats = useMemo(() => {
    const map = new Map<string, number>();
    logs.forEach((l) => {
      const key = (l.queryType || 'UNKNOWN').toUpperCase();
      map.set(key, (map.get(key) || 0) + 1);
    });
    return Array.from(map.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);
  }, [logs]);

  const vendorStats = useMemo(() => {
    const map = new Map<string, number>();
    devices.forEach((d) => {
      const key = (d.vendor || 'UNKNOWN').toUpperCase();
      map.set(key, (map.get(key) || 0) + 1);
    });
    return Array.from(map.entries())
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count);
  }, [devices]);

  const linePoints = useMemo(() => {
    const trend = viz?.trend || [];
    if (!trend.length) return '';
    const width = 640;
    const height = 220;
    const max = Math.max(...trend.map((x) => x.total), 1);
    return trend
      .map((p, i) => {
        const x = (i * (width - 40)) / Math.max(trend.length - 1, 1) + 20;
        const y = height - 20 - ((p.total / max) * (height - 40));
        return `${x},${y}`;
      })
      .join(' ');
  }, [viz]);

  const failLinePoints = useMemo(() => {
    const trend = viz?.trend || [];
    if (!trend.length) return '';
    const width = 640;
    const height = 220;
    const max = Math.max(...trend.map((x) => x.total), 1);
    return trend
      .map((p, i) => {
        const x = (i * (width - 40)) / Math.max(trend.length - 1, 1) + 20;
        const y = height - 20 - ((p.failed / max) * (height - 40));
        return `${x},${y}`;
      })
      .join(' ');
  }, [viz]);

  if (loading) {
    return (
      <div style={{ padding: 24 }}>
        <Spin />
      </div>
    );
  }

  const enabledPops = pops.filter((p) => p.status === 1).length;
  const enabledDevices = devices.filter((d) => d.status === 1).length;
  const enabledTemplates = templates.filter((t) => t.status === 1).length;
  const queryTotal = viz?.security.totalQueries || 0;
  const successRate = viz?.security.successRate ?? 100;
  const metricCardStyle: React.CSSProperties = { height: '100%' };
  const metricBodyStyle: React.CSSProperties = { minHeight: 112 };
  const chartCardStyle: React.CSSProperties = { height: '100%' };
  const chartBodyStyle: React.CSSProperties = { height: 340, overflow: 'hidden' };
  const listBodyStyle: React.CSSProperties = { height: 340, overflowY: 'auto' };

  return (
    <>
      <Title level={4}>概览</Title>
      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} md={12} xl={6}>
          <Card style={metricCardStyle} styles={{ body: metricBodyStyle }}>
            <Statistic title="POP 总数" value={pops.length} suffix={<Tag color="blue">启用 {enabledPops}</Tag>} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card style={metricCardStyle} styles={{ body: metricBodyStyle }}>
            <Statistic title="设备总数" value={devices.length} suffix={<Tag color="green">启用 {enabledDevices}</Tag>} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card style={metricCardStyle} styles={{ body: metricBodyStyle }}>
            <Statistic title="模板总数" value={templates.length} suffix={<Tag color="gold">启用 {enabledTemplates}</Tag>} />
          </Card>
        </Col>
        <Col xs={24} md={12} xl={6}>
          <Card style={metricCardStyle} styles={{ body: metricBodyStyle }}>
            <Statistic title="最近日志样本" value={queryTotal} suffix={<Tag color={successRate >= 95 ? 'green' : 'orange'}>成功率 {successRate}%</Tag>} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24}>
          <Card title="查询趋势看板（近 30 天按小时）" style={chartCardStyle} styles={{ body: chartBodyStyle }}>
            {(viz?.trend?.length || 0) > 0 ? (
              <>
                <svg viewBox="0 0 640 220" style={{ width: '100%', height: 220 }}>
                  <polyline fill="none" stroke="#1677ff" strokeWidth="3" points={linePoints} />
                  <polyline fill="none" stroke="#f5222d" strokeWidth="2" strokeDasharray="4 3" points={failLinePoints} />
                  {(viz?.trend || []).map((p, i) => {
                    const max = Math.max(...(viz?.trend || []).map((x) => x.total), 1);
                    const x = (i * (640 - 40)) / Math.max((viz?.trend || []).length - 1, 1) + 20;
                    const y = 220 - 20 - ((p.total / max) * (220 - 40));
                    return <circle key={`${p.bucket}-${i}`} cx={x} cy={y} r="3" fill="#1677ff" />;
                  })}
                </svg>
                <div style={{ marginTop: 8, color: '#666' }}>蓝线：总请求，红虚线：失败请求</div>
              </>
            ) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无趋势数据" />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} xl={12}>
          <Card title="Top N 热点目标（近 30 天）" style={chartCardStyle} styles={{ body: listBodyStyle }}>
            {(viz?.topTargets?.length || 0) > 0 ? (viz?.topTargets || []).map((item, idx) => {
              const percent = queryTotal > 0 ? Math.round((item.count / queryTotal) * 100) : 0;
              return (
                <div key={`${item.target}-${idx}`} style={{ marginBottom: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span
                      style={{ maxWidth: '70%', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}
                      title={item.target}
                    >
                      {item.target}
                    </span>
                    <span>{item.count} 次</span>
                  </div>
                  <Progress percent={percent} strokeColor={colors[idx % colors.length]} showInfo />
                </div>
              );
            }) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无热点目标数据" />}
          </Card>
        </Col>

        <Col xs={24} xl={12}>
          <Card title="安全与治理面板（最近 24 小时）" style={chartCardStyle} styles={{ body: listBodyStyle }}>
            <Row gutter={[12, 12]} style={{ marginBottom: 8 }}>
              <Col span={12}><Tag color="blue">来源 IP {viz?.security.uniqueSourceIps || 0}</Tag></Col>
              <Col span={12}><Tag color="orange">黑名单 {viz?.security.blacklistActive || 0}</Tag></Col>
              <Col span={12}><Tag color="green">本地用户 {viz?.security.localUsers || 0}</Tag></Col>
              <Col span={12}><Tag color="purple">LDAP 用户 {viz?.security.ldapUsers || 0}</Tag></Col>
            </Row>
            <div style={{ marginBottom: 12 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                <span>日志成功率</span>
                <span>{successRate}%</span>
              </div>
              <Progress percent={successRate} status={successRate >= 95 ? 'success' : 'normal'} />
            </div>
            <div style={{ marginBottom: 8, color: '#666' }}>
              总请求：{viz?.security.totalQueries || 0}，失败：{viz?.security.failedQueries || 0}
            </div>
            <div style={{ fontWeight: 500, marginBottom: 6 }}>失败来源 TOP IP</div>
            {(viz?.security.topFailSourceIps?.length || 0) > 0 ? (viz?.security.topFailSourceIps || []).map((x) => (
              <div key={x.sourceIp} style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 6 }}>
                <span>{x.sourceIp}</span>
                <Tag color="red">{x.count}</Tag>
              </div>
            )) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无失败来源数据" />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} xl={12}>
          <Card title="查询类型占比" style={chartCardStyle} styles={{ body: listBodyStyle }}>
            {queryTypeStats.length > 0 ? queryTypeStats.map((item, idx) => {
              const percent = queryTotal > 0 ? Math.round((item.count / queryTotal) * 100) : 0;
              return (
                <div key={item.name} style={{ marginBottom: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span>{formatQueryType(item.name)}</span><span>{item.count} 次</span>
                  </div>
                  <Progress percent={percent} strokeColor={colors[idx % colors.length]} showInfo />
                </div>
              );
            }) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无类型数据" />}
          </Card>
        </Col>
        <Col xs={24} xl={12}>
          <Card title="设备厂商分布" style={chartCardStyle} styles={{ body: listBodyStyle }}>
            {vendorStats.length > 0 ? vendorStats.map((item, idx) => {
              const percent = devices.length > 0 ? Math.round((item.count / devices.length) * 100) : 0;
              return (
                <div key={item.name} style={{ marginBottom: 12 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <span>{item.name}</span><span>{item.count} 台</span>
                  </div>
                  <Progress percent={percent} strokeColor={colors[idx % colors.length]} showInfo />
                </div>
              );
            }) : <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无厂商数据" />}
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} xl={12}>
          <Card title="日志执行成功率" style={chartCardStyle} styles={{ body: chartBodyStyle }}>
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
              <Progress type="circle" percent={successRate} status={successRate >= 95 ? 'success' : 'normal'} />
            </div>
          </Card>
        </Col>
      </Row>
    </>
  );
};
