import React, { useEffect, useState } from 'react';
import { Button, Modal, Table, Typography, message } from 'antd';
import { admin } from '../../services/api';
import { formatQueryType } from '../../utils/queryType';
import { formatDurationSeconds } from '../../utils/formatDuration';

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
  resultText?: string;
  errorMessage?: string;
  rawCommand?: string;
  durationMs?: number;
  createdAt: string;
}

/** 弹窗内终端风格内容区（与历史「命令/输出」块一致） */
const terminalPreviewPreStyle: React.CSSProperties = {
  margin: 0,
  padding: '12px 14px',
  boxSizing: 'border-box',
  width: '100%',
  minHeight: 120,
  maxHeight: '70vh',
  overflow: 'auto',
  whiteSpace: 'pre',
  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
  fontSize: 12,
  lineHeight: 1.55,
  color: '#e2e8f0',
  background: 'rgba(8, 13, 24, 0.65)',
};

/** 弹窗背景不透明度 65%（alpha = 0.65） */
const terminalShellBg = 'rgba(8, 13, 24, 0.65)';
const terminalHeaderBg = 'rgba(4, 7, 15, 0.65)';
const terminalBorder = '1px solid rgba(51, 65, 85, 0.65)';
const terminalBtnStyle: React.CSSProperties = {
  fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
  fontSize: 12,
  height: 32,
  background: '#1e293b',
  borderColor: '#475569',
  color: '#e2e8f0',
};

/** 表格内缩写行：纯 CSS 省略，不用 Typography（避免再出浏览器 / 组件第二层提示） */
const cellEllipsis: React.CSSProperties = {
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
};

export const QueryLogs: React.FC = () => {
  const [data, setData] = useState<{ content: Log[]; totalElements: number }>({ content: [], totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [detailModalOpen, setDetailModalOpen] = useState(false);
  const [detailModalText, setDetailModalText] = useState('');
  const size = 50;

  useEffect(() => {
    setLoading(true);
    admin.queryLogs.list(page, size).then((r) => {
      setData({ content: r.content ?? r, totalElements: r.totalElements ?? (r.content ?? r).length });
    }).catch(() => setData({ content: [], totalElements: 0 })).finally(() => setLoading(false));
  }, [page]);

  const copyDetailText = async (text: string) => {
    const t = (text || '').trim();
    if (!t) return;
    try {
      await navigator.clipboard.writeText(t);
      message.success('详情已复制');
    } catch {
      message.error('复制失败');
    }
  };

  const openDetailModal = (fullText: string) => {
    const t = (fullText || '').trim();
    if (!t) return;
    setDetailModalText(t);
    setDetailModalOpen(true);
  };

  const parseBrowser = (ua?: string) => {
    if (!ua) return '未知浏览器';
    const text = ua.toLowerCase();
    if (text.includes('edg/')) return 'Microsoft Edge';
    if (text.includes('opr/') || text.includes('opera')) return 'Opera';
    if (text.includes('chrome/') && !text.includes('edg/')) return 'Google Chrome';
    if (text.includes('firefox/')) return 'Mozilla Firefox';
    if (text.includes('safari/') && !text.includes('chrome/')) return 'Safari';
    return '未知浏览器';
  };

  const parseOs = (ua?: string) => {
    if (!ua) return '未知系统';
    const text = ua.toLowerCase();
    if (text.includes('windows nt')) return 'Windows';
    if (text.includes('mac os x') || text.includes('macintosh')) return 'macOS';
    if (text.includes('iphone') || text.includes('ipad') || text.includes('ios')) return 'iOS';
    if (text.includes('android')) return 'Android';
    if (text.includes('linux')) return 'Linux';
    return '未知系统';
  };

  const columns = [
    { title: '请求 ID', dataIndex: 'requestId', key: 'requestId', width: 140 },
    {
      title: '来源 IP',
      dataIndex: 'sourceIp',
      key: 'sourceIp',
      width: 180,
      render: (v: string) => (
        <Typography.Text ellipsis={{ tooltip: v }}>
          {v || '-'}
        </Typography.Text>
      ),
    },
    {
      title: '浏览器/电脑信息',
      dataIndex: 'userAgent',
      key: 'userAgent',
      width: 240,
      render: (ua?: string) => {
        const browser = parseBrowser(ua);
        const os = parseOs(ua);
        const detail = ua || '未记录 User-Agent';
        return (
          <Typography.Text ellipsis={{ tooltip: `${browser} / ${os}\n${detail}` }}>
            {browser} / {os}
          </Typography.Text>
        );
      },
    },
    { title: '类型', dataIndex: 'queryType', key: 'queryType', render: (v: string) => formatQueryType(v) },
    { title: '目标', dataIndex: 'queryTarget', key: 'queryTarget' },
    { title: '状态', dataIndex: 'resultStatus', key: 'resultStatus' },
    {
      title: '详情',
      dataIndex: 'resultText',
      key: 'detail',
      width: 280,
      render: (_: string | undefined, record: Log) => {
        const cmd = record.rawCommand?.trim();
        const cmdBlock = cmd ? `命令：\n${cmd}` : '';
        const detailWrapStyle: React.CSSProperties = {
          maxWidth: 260,
          cursor: 'pointer',
        };

        if (record.resultStatus === 'FAILED') {
          const reason = record.errorMessage
            ? record.errorMessage.replace(/^失败原因[:：]\s*/u, '')
            : '失败原因未记录';
          const copyText = [cmdBlock, reason ? `失败：\n${reason}` : ''].filter(Boolean).join('\n\n');
          const modalText = copyText || reason;
          return (
            <div
              style={detailWrapStyle}
              onClick={(e) => {
                e.stopPropagation();
                openDetailModal(modalText);
              }}
            >
              {cmd ? (
                <div style={{ ...cellEllipsis, fontSize: 12, color: '#64748b', marginBottom: 4 }}>
                  命令：{cmd}
                </div>
              ) : null}
              <div style={{ ...cellEllipsis, color: '#cf1322' }}>{reason}</div>
            </div>
          );
        }
        if (record.resultStatus === 'SUCCESS') {
          const out = record.resultText || '执行成功，但未返回结果内容';
          const copyText = [cmdBlock, `输出：\n${out}`].filter(Boolean).join('\n\n');
          const modalText = copyText || out;
          return (
            <div
              style={detailWrapStyle}
              onClick={(e) => {
                e.stopPropagation();
                openDetailModal(modalText);
              }}
            >
              {cmd ? (
                <div style={{ ...cellEllipsis, fontSize: 12, color: '#64748b', marginBottom: 4 }}>
                  命令：{cmd}
                </div>
              ) : null}
              <div style={{ ...cellEllipsis, color: '#111827' }}>{out}</div>
            </div>
          );
        }
        if (record.resultStatus === 'PENDING') {
          const modalText = cmd ? `${cmdBlock}\n\n状态：执行中`.trim() : '执行中';
          return (
            <div
              style={{
                ...detailWrapStyle,
                cursor: 'pointer',
              }}
              onClick={(e) => {
                e.stopPropagation();
                openDetailModal(modalText);
              }}
            >
              {cmd ? (
                <div style={{ ...cellEllipsis, fontSize: 12, color: '#64748b', marginBottom: 4 }}>
                  命令：{cmd}
                </div>
              ) : null}
              <div style={{ ...cellEllipsis, color: '#8c8c8c' }}>执行中</div>
            </div>
          );
        }
        return (
          <Typography.Text type="secondary">-</Typography.Text>
        );
      },
    },
    {
      title: '耗时',
      dataIndex: 'durationMs',
      key: 'durationMs',
      render: (v: number | undefined) => formatDurationSeconds(v),
    },
    { title: '时间', dataIndex: 'createdAt', key: 'createdAt', render: (v: string) => v && new Date(v).toLocaleString() },
  ];

  return (
    <div>
      <Typography.Title level={5} style={{ marginBottom: 4 }}>
        查询日志
      </Typography.Title>
      <Typography.Text type="secondary" style={{ fontSize: 12 }}>
        查看最近的公网查询请求、来源 IP 与执行详情。
      </Typography.Text>
      <div style={{ height: 16 }} />
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
        scroll={{ x: 1180 }}
      />
      <Modal
        title={(
          <span
            style={{
              color: '#e2e8f0',
              fontFamily: 'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
              fontSize: 13,
              fontWeight: 600,
              letterSpacing: '0.02em',
            }}
          >
            日志详情
          </span>
        )}
        open={detailModalOpen}
        onCancel={() => setDetailModalOpen(false)}
        closable={false}
        footer={(
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 10 }}>
            <Button
              style={{ ...terminalBtnStyle, color: '#7dd3fc', borderColor: '#3b82f6' }}
              onClick={() => void copyDetailText(detailModalText)}
            >
              复制
            </Button>
            <Button style={terminalBtnStyle} onClick={() => setDetailModalOpen(false)}>
              关闭
            </Button>
          </div>
        )}
        width={940}
        centered
        destroyOnHidden
        styles={{
          mask: {
            // 遮罩略加深，弹窗与背景对比更强（整体更不「透」）
            backgroundColor: 'rgba(0, 0, 0, 0.58)',
          },
          container: {
            padding: 0,
            background: terminalShellBg,
            borderRadius: 8,
            border: terminalBorder,
            boxShadow: '0 24px 64px rgba(0,0,0,0.45)',
            overflow: 'hidden',
            backdropFilter: 'blur(14px)',
            WebkitBackdropFilter: 'blur(14px)',
          },
          header: {
            margin: 0,
            padding: '10px 14px',
            background: terminalHeaderBg,
            borderBottom: terminalBorder,
          },
          body: {
            margin: 0,
            padding: 0,
            background: terminalShellBg,
          },
          footer: {
            margin: 0,
            padding: '10px 14px',
            background: terminalHeaderBg,
            borderTop: terminalBorder,
          },
        }}
      >
        <pre style={terminalPreviewPreStyle}>{detailModalText}</pre>
      </Modal>
    </div>
  );
};
