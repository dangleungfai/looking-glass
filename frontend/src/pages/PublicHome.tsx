import React, { useEffect, useState } from 'react';
import { Form, Select, Input, Button, Spin, Alert, Typography } from 'antd';
import { getPops, getPublicCaptcha, getPublicUiConfig, getQueryTypes, submitQuery } from '../services/api';
import { formatQueryType, normalizeQueryType, normalizeQueryTypes } from '../utils/queryType';
import { useSystemName } from '../hooks/useSystemName';
import { useShowPopCode } from '../hooks/useShowPopCode';
import { useFooterText } from '../hooks/useFooterText';
import { useHomeIntroText } from '../hooks/useHomeIntroText';
import { useLogoUrl } from '../hooks/useLogoUrl';
import { useNavigate } from 'react-router-dom';
import { DEFAULT_HOME_INTRO_TEXT } from '../constants/systemContent';

const { Text, Paragraph } = Typography;

export const PublicHome: React.FC = () => {
  const navigate = useNavigate();
  const systemName = useSystemName();
  const showPopCode = useShowPopCode();
  const footerText = useFooterText();
  const homeIntroText = useHomeIntroText();
  const logoUrl = useLogoUrl();
  const [pops, setPops] = useState<{ popCode: string; popName: string; country?: string; city?: string }[]>([]);
  const [queryTypes, setQueryTypes] = useState<string[]>([]);
  const [clientIp, setClientIp] = useState<string>('');
  const [hasCustomLogo, setHasCustomLogo] = useState(false);
  const [captchaEnabled, setCaptchaEnabled] = useState(false);
  const [captchaId, setCaptchaId] = useState('');
  const [captchaQuestion, setCaptchaQuestion] = useState('');
  const [captchaLoading, setCaptchaLoading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [loadingMeta, setLoadingMeta] = useState(true);
  const [result, setResult] = useState<Record<string, unknown> | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [form] = Form.useForm();
  const selectedQueryType = Form.useWatch('queryType', form);
  const targetPlaceholder = (() => {
    const q = normalizeQueryType(selectedQueryType);
    if (q === 'IPV6_PING' || q === 'IPV6_TRACEROUTE') return '例如 2001:4860:4860::8888';
    if (q === 'IPV6_BGP_ROUTE') return '例如 2001:4860:4860::/48';
    if (q.startsWith('IPV6_')) return '例如 2001:4860:4860::8888';
    return '例如 8.8.8.8 或 1.1.1.0/24';
  })();

  useEffect(() => {
    Promise.all([getPops(), getQueryTypes(), getPublicUiConfig()])
      .then(([p, t, ui]) => {
        setPops(p);
        setQueryTypes(normalizeQueryTypes(Array.isArray(t) ? t : []));
        setClientIp((ui?.clientIp || '').trim());
        setHasCustomLogo(Boolean(ui?.hasCustomLogo));
        setCaptchaEnabled(Boolean(ui?.captchaEnabled));
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoadingMeta(false));
  }, []);

  const loadCaptcha = () => {
    setCaptchaLoading(true);
    getPublicCaptcha()
      .then((r) => {
        setCaptchaEnabled(r.enabled);
        if (r.enabled) {
          setCaptchaId(r.captchaId);
          setCaptchaQuestion(r.question);
        } else {
          setCaptchaId('');
          setCaptchaQuestion('');
          form.setFieldValue('captchaAnswer', '');
        }
      })
      .catch(() => {
        setCaptchaId('');
        setCaptchaQuestion('');
      })
      .finally(() => setCaptchaLoading(false));
  };

  useEffect(() => {
    if (!captchaEnabled) return;
    loadCaptcha();
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [captchaEnabled]);

  const onFinish = (values: { popCode: string; queryType: string; target: string; captchaAnswer?: string }) => {
    setError(null);
    setResult(null);
    if (captchaEnabled) {
      const answer = (values.captchaAnswer || '').trim();
      if (!captchaId || !answer) {
        setError('请先完成验证码');
        return;
      }
    }
    setLoading(true);
    submitQuery({
      popCode: values.popCode,
      queryType: values.queryType,
      target: values.target.trim(),
      captchaToken: captchaEnabled ? `${captchaId}:${(values.captchaAnswer || '').trim()}` : undefined,
    })
      .then(setResult)
      .catch((e) => {
        setError(e.message);
        if (captchaEnabled) loadCaptcha();
      })
      .finally(() => setLoading(false));
  };

  if (loadingMeta) {
    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          background: '#f5f5f7',
        }}
      >
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div
      style={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        background: '#f5f5f7',
        color: '#1d1d1f',
        overflowX: 'hidden',
        boxSizing: 'border-box',
      }}
    >
      <style>
        {`
          @keyframes lgGlowBreath {
            0% { transform: scale(1); opacity: 0.35; }
            50% { transform: scale(1.08); opacity: 0.62; }
            100% { transform: scale(1); opacity: 0.35; }
          }
          @keyframes lgParticleFloat {
            0% { transform: translateY(0) translateX(0); opacity: 0.2; }
            50% { transform: translateY(-14px) translateX(3px); opacity: 0.55; }
            100% { transform: translateY(0) translateX(0); opacity: 0.2; }
          }
        `}
      </style>
      {/* 顶部导航条 */}
      <header
        style={{
          width: '100%',
          backdropFilter: 'blur(16px)',
          background: 'rgba(255,255,255,0.86)',
          borderBottom: '1px solid #e5e5e7',
          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
        }}
      >
        <div
          style={{
            width: '100%',
            margin: '0 auto',
            padding: '14px clamp(16px, 4vw, 56px)',
            boxSizing: 'border-box',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: hasCustomLogo ? 8 : 0 }}>
            {hasCustomLogo ? (
              <img
                src={logoUrl}
                alt="logo"
                style={{
                  width: 128,
                  height: 32,
                  borderRadius: 0,
                  objectFit: 'fill',
                  background: 'transparent',
                  display: 'block',
                }}
                onError={() => setHasCustomLogo(false)}
              />
            ) : (
              <span style={{ fontSize: 16, fontWeight: 600, letterSpacing: 0.4 }}>{systemName}</span>
            )}
          </div>
          <Button
            size="small"
            type="primary"
            onClick={() => navigate('/login')}
            style={{
              borderRadius: 999,
              paddingInline: 16,
              background: '#0071e3',
              border: 'none',
              boxShadow: '0 4px 12px rgba(0, 113, 227, 0.28)',
            }}
          >
            登录
          </Button>
        </div>
      </header>

      {/* 查询模块 + 结果 + 文案 */}
      <main
        style={{
          flex: '1 0 auto',
          width: '100%',
          margin: 0,
          padding: '34px clamp(18px, 4vw, 68px) 72px',
          boxSizing: 'border-box',
          display: 'flex',
          flexDirection: 'column',
          gap: 28,
        }}
      >
        {/* 顶部融合 Hero（品牌 + 查询面板） */}
        <section
          style={{
            width: '100%',
            minHeight: '34vh',
          }}
        >
          <div
            style={{
              position: 'relative',
              overflow: 'hidden',
              borderRadius: 0,
              background:
                'linear-gradient(180deg, #ffffff 0%, #fbfbfd 100%)',
              border: 'none',
              boxShadow: 'none',
              padding: '28px clamp(16px, 2.4vw, 28px)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              minHeight: 'clamp(300px, 40vh, 460px)',
            }}
          >
            <div
              style={{
                position: 'absolute',
                inset: 0,
                pointerEvents: 'none',
                zIndex: 0,
              }}
            >
              <div
                style={{
                  position: 'absolute',
                  top: '16%',
                  left: '52%',
                  width: 280,
                  height: 280,
                  borderRadius: '50%',
                  background: 'radial-gradient(circle, rgba(0,113,227,0.14), rgba(0,113,227,0.02))',
                  animation: 'lgGlowBreath 6s ease-in-out infinite',
                }}
              />
              <div
                style={{
                  position: 'absolute',
                  bottom: '8%',
                  left: '8%',
                  width: 220,
                  height: 220,
                  borderRadius: '50%',
                  background: 'radial-gradient(circle, rgba(142,142,147,0.14), rgba(142,142,147,0.02))',
                  animation: 'lgGlowBreath 7.5s ease-in-out infinite',
                }}
              />
              {[12, 28, 42, 58, 73, 86].map((left, idx) => (
                <span
                  key={`particle-${left}`}
                  style={{
                    position: 'absolute',
                    top: `${20 + (idx % 3) * 18}%`,
                    left: `${left}%`,
                    width: idx % 2 === 0 ? 4 : 3,
                    height: idx % 2 === 0 ? 4 : 3,
                    borderRadius: '50%',
                    background: idx % 2 === 0 ? 'rgba(0,113,227,0.55)' : 'rgba(142,142,147,0.5)',
                    boxShadow: '0 0 8px rgba(0,113,227,0.25)',
                    animation: `lgParticleFloat ${3.8 + idx * 0.35}s ease-in-out infinite`,
                    animationDelay: `${idx * 0.25}s`,
                  }}
                />
              ))}
            </div>
            <div
              style={{
                position: 'absolute',
                top: -80,
                right: -60,
                width: 220,
                height: 220,
                borderRadius: '50%',
                background: 'radial-gradient(circle, rgba(0,113,227,0.16), rgba(0,113,227,0.03))',
                filter: 'blur(2px)',
              }}
            />
            <div
              style={{
                position: 'absolute',
                bottom: -90,
                left: -70,
                width: 260,
                height: 260,
                borderRadius: '50%',
                background: 'radial-gradient(circle, rgba(142,142,147,0.14), rgba(142,142,147,0.03))',
              }}
            />
            <div style={{ position: 'relative', zIndex: 1 }}>
              <div
                style={{
                  fontSize: 'clamp(46px, 6.4vw, 74px)',
                  lineHeight: 0.98,
                  fontWeight: 700,
                  letterSpacing: 0.4,
                  marginBottom: 22,
                  background: 'linear-gradient(120deg, #1d1d1f, #3a3a3c 55%, #6e6e73)',
                  WebkitBackgroundClip: 'text',
                  WebkitTextFillColor: 'transparent',
                }}
              >
                    {systemName || 'LOOKING GLASS'}
              </div>
              <Text style={{ color: '#424245', fontSize: 18, lineHeight: 1.65, maxWidth: 900 }}>
                {systemName} 提供从骨干网视角出发的实时诊断能力。你可以快速对路由路径、连通性与时延进行观测，
                获得接近运营商侧的网络可见性。
              </Text>
            </div>
            <div
              style={{
                position: 'relative',
                zIndex: 1,
                marginTop: 34,
                display: 'grid',
                gridTemplateColumns: 'repeat(4, minmax(0, 1fr))',
                gap: 14,
                maxWidth: 980,
              }}
            >
              {[
                { label: 'PING', desc: '时延连通性' },
                { label: 'TRACE', desc: '逐跳追踪' },
                { label: 'BGP', desc: '路由可见性' },
                { label: '来访 IP', desc: clientIp || 'unknown' },
              ].map((item) => (
                <div
                  key={item.label}
                  style={{
                    borderRadius: 14,
                    border: '1px solid #e5e7eb',
                    background: '#ffffff',
                    padding: '14px 14px 13px',
                  }}
                >
                  <div style={{ color: '#1d1d1f', fontSize: 13, fontWeight: 700, marginBottom: 4 }}>{item.label}</div>
                  <div style={{ color: '#6e6e73', fontSize: 12 }}>{item.desc}</div>
                </div>
              ))}
            </div>
            <div
              style={{
                position: 'relative',
                zIndex: 1,
                marginTop: 22,
                borderRadius: 0,
                backdropFilter: 'blur(8px)',
                background: 'linear-gradient(135deg, rgba(255,255,255,0.96), rgba(250,250,252,0.94))',
                border: 'none',
                boxShadow: 'none',
                padding: 22,
              }}
            >
              <div style={{ marginBottom: 12 }}>
                <Text style={{ color: '#0f172a', fontSize: 19, fontWeight: 600 }}>立即发起网络诊断</Text>
                <Paragraph style={{ marginBottom: 0, marginTop: 6, fontSize: 13, color: '#6e6e73' }}>
                  选择 POP 与查询类型后，输入目标 IP / 域名 / 前缀。
                </Paragraph>
              </div>
              <Form form={form} layout="vertical" onFinish={onFinish}>
                <div
                  style={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: 12,
                    alignItems: 'flex-end',
                    marginBottom: 8,
                  }}
                >
                  <Form.Item
                    name="popCode"
                    label="POP 节点"
                    rules={[{ required: true, message: '请选择 POP 节点' }]}
                    style={{ flex: '1 1 240px', marginBottom: 0, minWidth: 0 }}
                  >
                    <Select
                      size="large"
                      placeholder="选择 POP"
                      options={pops.map((p) => ({
                        label: showPopCode ? `${p.popName} (${p.popCode})` : p.popName,
                        value: p.popCode,
                      }))}
                    />
                  </Form.Item>
                  <Form.Item
                    name="queryType"
                    label="查询类型"
                    rules={[{ required: true, message: '请选择查询类型' }]}
                    style={{ flex: '1 1 240px', marginBottom: 0, minWidth: 0 }}
                  >
                    <Select
                      size="large"
                      placeholder="选择查询类型"
                      options={queryTypes.map((t) => ({ label: formatQueryType(t), value: t }))}
                    />
                  </Form.Item>
                  <Form.Item
                    name="target"
                    label="目标 (IP / 域名 / 前缀)"
                    rules={[{ required: true, message: '请输入查询目标' }]}
                    style={{ flex: '1 1 240px', marginBottom: 0, minWidth: 0 }}
                  >
                    <Input size="large" placeholder={targetPlaceholder} />
                  </Form.Item>
                  {captchaEnabled && (
                    <Form.Item
                      name="captchaAnswer"
                      label={captchaQuestion ? `验证码：${captchaQuestion}` : '验证码'}
                      rules={[{ required: true, message: '请输入验证码' }]}
                      style={{ flex: '1 1 220px', marginBottom: 0, minWidth: 0 }}
                    >
                      <Input
                        size="large"
                        placeholder="输入计算结果"
                        addonAfter={(
                          <button
                            type="button"
                            onClick={loadCaptcha}
                            disabled={captchaLoading}
                            style={{ border: 'none', background: 'transparent', cursor: 'pointer' }}
                          >
                            {captchaLoading ? '刷新中' : '刷新'}
                          </button>
                        )}
                      />
                    </Form.Item>
                  )}
                  <Form.Item style={{ flex: '0 0 120px', marginBottom: 0 }}>
                    <Button
                      type="primary"
                      htmlType="submit"
                      loading={loading}
                      block
                      style={{
                        height: 44,
                        borderRadius: 999,
                        fontWeight: 600,
                        background: 'linear-gradient(135deg, #2563eb, #4f46e5)',
                        border: 'none',
                      }}
                    >
                      查询
                    </Button>
                  </Form.Item>
                </div>
              </Form>
              {error && (
                <Alert type="error" message={error} style={{ marginTop: 12 }} />
              )}
            </div>
          </div>
        </section>

        {/* 中间：查询结果卡片（仅有结果时显示） */}
        {result && (
          <section style={{ width: '100%' }}>
            <div
              style={{
                marginTop: 4,
                background: 'linear-gradient(145deg, #ffffff, #fafafc)',
                borderRadius: 0,
                border: 'none',
                color: '#1f2937',
                boxShadow: 'none',
                padding: 18,
              }}
            >
              {((result as { result?: { rawText?: string } }).result?.rawText) && (
                <pre
                  style={{
                    background: 'rgba(248, 250, 252, 0.95)',
                    padding: 10,
                    borderRadius: 12,
                    overflow: 'auto',
                    maxHeight: 420,
                    fontSize: 11,
                    lineHeight: 1.4,
                    color: '#0f172a',
                    border: '1px solid rgba(226, 232, 240, 1)',
                  }}
                >
                  {((result as { result?: { rawText?: string } }).result?.rawText)}
                </pre>
              )}
            </div>
          </section>
        )}

        {/* 底部：文案卡片 */}
        <section style={{ width: '100%' }}>
          <div
            style={{
              background:
                'radial-gradient(120% 130% at 0% 0%, rgba(0,113,227,0.09), rgba(248,248,250,0.98) 45%, rgba(255,255,255,1) 100%)',
              borderRadius: 0,
              border: 'none',
              boxShadow: 'none',
              padding: 24,
            }}
          >
            <Paragraph
              style={{
                color: '#334155',
                fontSize: 14,
                lineHeight: 1.85,
                whiteSpace: 'pre-wrap',
                marginBottom: 0,
              }}
            >
              {homeIntroText || DEFAULT_HOME_INTRO_TEXT}
            </Paragraph>
          </div>
        </section>
      </main>
      {footerText && (
        <footer
          style={{
            width: '100%',
            background: 'linear-gradient(180deg, #f5f5f7, #ececf0)',
            borderTop: '1px solid #e0e0e6',
            marginTop: 8,
            color: '#6e6e73',
            fontSize: 12,
          }}
        >
          <div
            style={{
              width: '100%',
              margin: '0 auto',
              padding: '14px clamp(16px, 4vw, 56px)',
              boxSizing: 'border-box',
              textAlign: 'center',
            }}
          >
            {footerText}
          </div>
        </footer>
      )}
    </div>
  );
};
