import React, { useEffect, useState } from 'react';
import { Table, Typography } from 'antd';
import { admin } from '../../services/api';

interface Log {
  id: number;
  requestId: string;
  sourceIp: string;
  userAgent?: string;
  queryType: string;
  queryTarget: string;
  popId: number;
  deviceId: number;
  resultStatus: string;
  durationMs?: number;
  createdAt: string;
}

export const QueryLogs: React.FC = () => {
  const [data, setData] = useState<{ content: Log[]; totalElements: number }>({ content: [], totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const size = 20;

  useEffect(() => {
    setLoading(true);
    admin.queryLogs.list(page, size).then((r) => {
      setData({ content: r.content ?? r, totalElements: r.totalElements ?? (r.content ?? r).length });
    }).catch(() => setData({ content: [], totalElements: 0 })).finally(() => setLoading(false));
  }, [page]);

  const columns = [
    { title: '请求 ID', dataIndex: 'requestId', key: 'requestId', width: 140 },
    { title: '来源 IP', dataIndex: 'sourceIp', key: 'sourceIp' },
    { title: '类型', dataIndex: 'queryType', key: 'queryType' },
    { title: '目标', dataIndex: 'queryTarget', key: 'queryTarget' },
    { title: '状态', dataIndex: 'resultStatus', key: 'resultStatus' },
    { title: '耗时(ms)', dataIndex: 'durationMs', key: 'durationMs' },
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => v && new Date(v).toLocaleString() },
  ];

  return (
    <>
      <Typography.Title level={5}>查询日志</Typography.Title>
      <Table
        rowKey="id"
        columns={columns}
        dataSource={data.content}
        loading={loading}
        pagination={{
          current: page + 1,
          pageSize: size,
          total: data.totalElements,
          onChange: (p) => setPage(p - 1),
        }}
      />
    </>
  );
};
