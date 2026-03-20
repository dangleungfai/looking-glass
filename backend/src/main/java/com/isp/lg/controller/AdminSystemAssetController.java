package com.isp.lg.controller;

import com.isp.lg.service.SystemAssetService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/system-assets")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSystemAssetController {

    private final SystemAssetService systemAssetService;

    public AdminSystemAssetController(SystemAssetService systemAssetService) {
        this.systemAssetService = systemAssetService;
    }

    @PostMapping("/logo")
    public ResponseEntity<Map<String, Object>> uploadLogo(@RequestParam("file") MultipartFile file) throws Exception {
        systemAssetService.saveLogo(file);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "logo 已更新"
        ));
    }

    @DeleteMapping("/logo")
    public ResponseEntity<Map<String, Object>> resetLogo() throws Exception {
        systemAssetService.resetLogo();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "已恢复默认 logo"
        ));
    }

    @PostMapping("/nginx-ssl/upload")
    public ResponseEntity<Map<String, Object>> uploadNginxSsl(@RequestParam("fullchain") MultipartFile fullchain,
                                                              @RequestParam("privkey") MultipartFile privkey) throws Exception {
        systemAssetService.replaceNginxSslCert(fullchain, privkey);
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Nginx 证书已替换，请重载 Nginx 生效"
        ));
    }

    @PostMapping("/nginx-ssl/reset-self-signed")
    public ResponseEntity<Map<String, Object>> resetSelfSigned() throws Exception {
        systemAssetService.resetSelfSignedNginxCert();
        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "已重置为自签名证书（最长年限），请重载 Nginx 生效"
        ));
    }

}
