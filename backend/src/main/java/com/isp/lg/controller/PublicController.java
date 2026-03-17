package com.isp.lg.controller;

import com.isp.lg.dto.PopDto;
import com.isp.lg.dto.PublicQueryRequest;
import com.isp.lg.dto.PublicQueryResponse;
import com.isp.lg.service.QueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public")
public class PublicController {

    private final QueryService queryService;

    public PublicController(QueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping("/pops")
    public ResponseEntity<List<PopDto>> listPops() {
        return ResponseEntity.ok(queryService.listPublicPops());
    }

    @GetMapping("/query-types")
    public ResponseEntity<List<String>> listQueryTypes() {
        return ResponseEntity.ok(queryService.listPublicQueryTypes());
    }

    @PostMapping("/query")
    public ResponseEntity<PublicQueryResponse> query(@Valid @RequestBody PublicQueryRequest request,
                                                      HttpServletRequest httpRequest) {
        String sourceIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        PublicQueryResponse response = queryService.executeQuery(request, sourceIp, userAgent != null ? userAgent : "");
        return ResponseEntity.ok(response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
