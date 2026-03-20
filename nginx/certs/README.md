将 HTTPS 证书放到本目录：

- `fullchain.pem`
- `privkey.pem`

例如使用 Let's Encrypt 证书：

```bash
cp /etc/letsencrypt/live/<your-domain>/fullchain.pem ./nginx/certs/fullchain.pem
cp /etc/letsencrypt/live/<your-domain>/privkey.pem  ./nginx/certs/privkey.pem
```

启动后，Nginx 会监听：

- `80`（自动 301 跳转到 HTTPS）
- `443`（TLS）
