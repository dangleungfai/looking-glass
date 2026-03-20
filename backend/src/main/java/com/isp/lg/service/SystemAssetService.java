package com.isp.lg.service;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class SystemAssetService {

    private static final Path ASSET_DIR = Path.of("runtime", "assets");
    private static final Path LOGO_PATH = ASSET_DIR.resolve("logo");
    private static final long MAX_LOGO_SIZE = 2 * 1024 * 1024;
    private static final long MAX_CERT_SIZE = 10 * 1024 * 1024;
    private static final int SELF_SIGNED_DAYS = 36500;

    private final Path nginxCertDir;

    public SystemAssetService(@Value("${looking-glass.nginx.cert-dir:../nginx/certs}") String certDir) {
        this.nginxCertDir = Path.of(certDir);
    }

    public record AssetData(byte[] bytes, String contentType) {}

    public String saveLogo(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的 logo 文件");
        }
        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new IllegalArgumentException("logo 文件不能超过 2MB");
        }
        String type = file.getContentType();
        if (type == null) type = "";
        if (!type.startsWith("image/")) {
            throw new IllegalArgumentException("仅支持图片文件");
        }
        Files.createDirectories(ASSET_DIR);
        Files.copy(file.getInputStream(), LOGO_PATH, StandardCopyOption.REPLACE_EXISTING);
        return type;
    }

    public void resetLogo() throws IOException {
        Files.deleteIfExists(LOGO_PATH);
    }

    public AssetData loadLogo() throws IOException {
        if (Files.exists(LOGO_PATH)) {
            byte[] bytes = Files.readAllBytes(LOGO_PATH);
            String type = Files.probeContentType(LOGO_PATH);
            if (type == null || type.isBlank()) type = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            return new AssetData(bytes, type);
        }
        ClassPathResource resource = new ClassPathResource("defaults/logo-default.svg");
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new AssetData(bytes, "image/svg+xml");
    }

    public boolean hasCustomLogo() {
        return Files.exists(LOGO_PATH);
    }

    public void replaceNginxSslCert(MultipartFile fullchain, MultipartFile privkey) throws IOException {
        if (fullchain == null || fullchain.isEmpty()) {
            throw new IllegalArgumentException("请上传 fullchain.pem");
        }
        if (privkey == null || privkey.isEmpty()) {
            throw new IllegalArgumentException("请上传 privkey.pem");
        }
        if (fullchain.getSize() > MAX_CERT_SIZE || privkey.getSize() > MAX_CERT_SIZE) {
            throw new IllegalArgumentException("证书文件不能超过 10MB");
        }
        String certText = new String(fullchain.getBytes(), StandardCharsets.UTF_8);
        String keyText = new String(privkey.getBytes(), StandardCharsets.UTF_8);
        if (!certText.contains("BEGIN CERTIFICATE")) {
            throw new IllegalArgumentException("fullchain.pem 内容无效");
        }
        if (!keyText.contains("BEGIN") || !keyText.contains("PRIVATE KEY")) {
            throw new IllegalArgumentException("privkey.pem 内容无效");
        }
        Files.createDirectories(nginxCertDir);
        Path certTmp = Files.createTempFile("fullchain-", ".pem");
        Path keyTmp = Files.createTempFile("privkey-", ".pem");
        try {
            Files.writeString(certTmp, certText, StandardOpenOption.TRUNCATE_EXISTING);
            Files.writeString(keyTmp, keyText, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(certTmp, nginxCertDir.resolve("fullchain.pem"), StandardCopyOption.REPLACE_EXISTING);
            Files.move(keyTmp, nginxCertDir.resolve("privkey.pem"), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(certTmp);
            Files.deleteIfExists(keyTmp);
        }
    }

    public void resetSelfSignedNginxCert() throws Exception {
        Files.createDirectories(nginxCertDir);
        Path cert = nginxCertDir.resolve("fullchain.pem");
        Path key = nginxCertDir.resolve("privkey.pem");
        List<String> withSan = List.of(
                "openssl", "req", "-x509", "-nodes", "-newkey", "rsa:2048",
                "-keyout", key.toString(),
                "-out", cert.toString(),
                "-days", String.valueOf(SELF_SIGNED_DAYS),
                "-subj", "/CN=localhost",
                "-addext", "subjectAltName=DNS:localhost,IP:127.0.0.1,IP:::1"
        );
        int code = runCommand(withSan);
        if (code != 0) {
            List<String> fallback = List.of(
                    "openssl", "req", "-x509", "-nodes", "-newkey", "rsa:2048",
                    "-keyout", key.toString(),
                    "-out", cert.toString(),
                    "-days", String.valueOf(SELF_SIGNED_DAYS),
                    "-subj", "/CN=localhost"
            );
            int fallbackCode = runCommand(fallback);
            if (fallbackCode != 0) {
                throw new IllegalStateException("生成自签名证书失败，请确认已安装 openssl");
            }
        }
    }

    private int runCommand(List<String> cmd) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        p.getInputStream().readAllBytes();
        return p.waitFor();
    }
}
