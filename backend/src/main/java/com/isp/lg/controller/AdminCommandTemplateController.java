package com.isp.lg.controller;

import com.isp.lg.domain.CommandTemplate;
import com.isp.lg.dto.CommandTemplateDto;
import com.isp.lg.repository.CommandTemplateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/command-templates")
public class AdminCommandTemplateController {

    private final CommandTemplateRepository commandTemplateRepository;

    public AdminCommandTemplateController(CommandTemplateRepository commandTemplateRepository) {
        this.commandTemplateRepository = commandTemplateRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'READONLY')")
    public ResponseEntity<List<CommandTemplateDto>> list() {
        List<CommandTemplateDto> list = commandTemplateRepository.findAll()
                .stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(
                        dto -> dto.getTemplateName() == null ? "" : dto.getTemplateName().toLowerCase(Locale.ROOT)
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<CommandTemplateDto> create(@RequestBody TemplateBody body) {
        CommandTemplate t = new CommandTemplate();
        t.setVendor(body.getVendor());
        t.setOsType(resolveOsType(body.getVendor()));
        t.setQueryType(body.getQueryType());
        t.setTemplateName(resolveTemplateName(body.getTemplateName(), body.getVendor(), body.getQueryType()));
        t.setCommandText(body.getCommandText());
        t.setParameterSchema(body.getParameterSchema() != null ? body.getParameterSchema() : "{}");
        t.setIsPublic(1);
        t.setStatus(body.getStatus() != null ? body.getStatus() : 1);
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(toDto(commandTemplateRepository.save(t)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<CommandTemplateDto> update(@PathVariable Long id, @RequestBody TemplateBody body) {
        CommandTemplate t = commandTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Template not found"));
        t.setVendor(body.getVendor());
        t.setOsType(resolveOsType(body.getVendor()));
        t.setQueryType(body.getQueryType());
        t.setTemplateName(resolveTemplateName(body.getTemplateName(), body.getVendor(), body.getQueryType()));
        t.setCommandText(body.getCommandText());
        t.setParameterSchema(body.getParameterSchema() != null ? body.getParameterSchema() : t.getParameterSchema());
        t.setIsPublic(1);
        t.setStatus(body.getStatus() != null ? body.getStatus() : t.getStatus());
        t.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(toDto(commandTemplateRepository.save(t)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        if (!commandTemplateRepository.existsById(id)) throw new IllegalArgumentException("Template not found");
        commandTemplateRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "deleted"));
    }

    public static class TemplateBody {
        private String vendor;
        private String templateName;
        private String queryType;
        private String commandText;
        private String parameterSchema;
        private Integer status;

        public String getVendor() { return vendor; }
        public void setVendor(String vendor) { this.vendor = vendor; }
        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        public String getCommandText() { return commandText; }
        public void setCommandText(String commandText) { this.commandText = commandText; }
        public String getParameterSchema() { return parameterSchema; }
        public void setParameterSchema(String parameterSchema) { this.parameterSchema = parameterSchema; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    private String resolveOsType(String vendor) {
        if (vendor == null) return "GENERIC";
        switch (vendor) {
            case "CISCO_IOS_XR":
                return "IOS-XR";
            case "JUNIPER_JUNOS":
                return "JUNOS";
            case "HUAWEI_VRP":
                return "VRP";
            case "MIKROTIK_ROUTEROS":
                return "ROUTEROS";
            default:
                return "GENERIC";
        }
    }

    private String resolveTemplateName(String templateName, String vendor, String queryType) {
        if (templateName != null && !templateName.isBlank()) {
            return templateName.trim();
        }
        String v = shortVendorName(vendor);
        String q = displayQueryType(queryType);
        if (!v.isBlank() && !q.isBlank()) {
            return v + "_" + q;
        }
        if (!q.isBlank()) {
            return q;
        }
        if (!v.isBlank()) {
            return v;
        }
        return "template";
    }

    private String shortVendorName(String vendor) {
        if (vendor == null) return "";
        switch (vendor.trim()) {
            case "CISCO_IOS_XR":
                return "Cisco";
            case "JUNIPER_JUNOS":
                return "Juniper";
            case "HUAWEI_VRP":
                return "Huawei";
            default:
                return vendor.trim();
        }
    }

    private String displayQueryType(String queryType) {
        if (queryType == null || queryType.isBlank()) return "";
        String q = queryType.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        switch (q) {
            case "PING":
            case "IPV4PING":
            case "IPV4_PING":
                return "IPv4 Ping";
            case "IPV6PING":
            case "IPV6_PING":
                return "IPv6 Ping";
            case "TRACEROUTE":
            case "IPV4TRACEROUTE":
            case "IPV4_TRACEROUTE":
                return "IPv4 Traceroute";
            case "IPV6TRACEROUTE":
            case "IPV6_TRACEROUTE":
                return "IPv6 Traceroute";
            case "BGP_PREFIX":
            case "BGP_ASN":
            case "ROUTE_LOOKUP":
            case "IPV4BGPROUTE":
            case "IPV4_BGP_ROUTE":
                return "IPv4 BGP Route";
            case "IPV6BGPROUTE":
            case "IPV6_BGP_ROUTE":
                return "IPv6 BGP Route";
            default:
                return queryType.trim();
        }
    }

    private CommandTemplateDto toDto(CommandTemplate t) {
        CommandTemplateDto dto = new CommandTemplateDto();
        dto.setId(t.getId());
        dto.setVendor(t.getVendor());
        dto.setTemplateName(t.getTemplateName());
        dto.setQueryType(t.getQueryType());
        dto.setCommandText(t.getCommandText());
        dto.setParameterSchema(t.getParameterSchema());
        dto.setStatus(t.getStatus());
        return dto;
    }
}
