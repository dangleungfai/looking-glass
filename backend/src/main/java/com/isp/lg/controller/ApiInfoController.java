package com.isp.lg.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 预留：对外 API 信息与版本。后续可在此增加 API Key 鉴权、限流与开放接口说明。
 */
@RestController
@RequestMapping("/api/public")
public class ApiInfoController {

    @GetMapping("/info")
    public ResponseEntity<Map<String, String>> info() {
        return ResponseEntity.ok(Map.of(
                "name", "LOOKING GLASS",
                "version", "1.0",
                "docs", "See README for public query API (POST /api/public/query). Future: authenticated API with key."
        ));
    }
}
