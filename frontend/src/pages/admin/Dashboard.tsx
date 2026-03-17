import React from 'react';
import { Card, Typography } from 'antd';

const { Title } = Typography;

export const Dashboard: React.FC = () => {
  return (
    <>
      <Title level={4}>概览</Title>
      <Card>
        <p>欢迎使用 ISP Looking Glass 管理后台。请从左侧菜单进入 POP 管理、设备管理、命令模板、查询日志或系统设置。</p>
      </Card>
    </>
  );
};
