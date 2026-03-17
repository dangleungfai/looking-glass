package com.isp.lg.controller;

import com.isp.lg.domain.CommandTemplate;
import com.isp.lg.repository.CommandTemplateRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/command-templates")
public class AdminCommandTemplateController {

    private final CommandTemplateRepository commandTemplateRepository;

    public AdminCommandTemplateController(CommandTemplateRepository commandTemplateRepository) {
        this.commandTemplateRepository = commandTemplateRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN', 'READONLY_ADMIN', 'AUDITOR')")
    public ResponseEntity<List<CommandTemplate>> list() {
        return ResponseEntity.ok(commandTemplateRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN')")
    public ResponseEntity<CommandTemplate> create(@Valid @RequestBody TemplateBody body) {
        CommandTemplate t = new CommandTemplate();
        t.setVendor(body.getVendor());
        t.setOsType(body.getOsType());
        t.setQueryType(body.getQueryType());
        t.setTemplateName(body.getTemplateName());
        t.setCommandText(body.getCommandText());
        t.setParameterSchema(body.getParameterSchema() != null ? body.getParameterSchema() : "{}");
        t.setIsPublic(body.getIsPublic() != null ? body.getIsPublic() : 1);
        t.setStatus(body.getStatus() != null ? body.getStatus() : 1);
        t.setCreatedAt(Instant.now());
        t.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(commandTemplateRepository.save(t));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN')")
    public ResponseEntity<CommandTemplate> update(@PathVariable Long id, @Valid @RequestBody TemplateBody body) {
        CommandTemplate t = commandTemplateRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Template not found"));
        t.setTemplateName(body.getTemplateName());
        t.setCommandText(body.getCommandText());
        t.setParameterSchema(body.getParameterSchema() != null ? body.getParameterSchema() : t.getParameterSchema());
        t.setIsPublic(body.getIsPublic() != null ? body.getIsPublic() : t.getIsPublic());
        t.setStatus(body.getStatus() != null ? body.getStatus() : t.getStatus());
        t.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(commandTemplateRepository.save(t));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'NETWORK_ADMIN')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        if (!commandTemplateRepository.existsById(id)) throw new IllegalArgumentException("Template not found");
        commandTemplateRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "deleted"));
    }

    public static class TemplateBody {
        @NotBlank
        private String vendor;
        @NotBlank
        private String osType;
        @NotBlank
        private String queryType;
        @NotBlank
        private String templateName;
        @NotBlank
        private String commandText;
        private String parameterSchema;
        private Integer isPublic;
        private Integer status;

        public String getVendor() { return vendor; }
        public void setVendor(String vendor) { this.vendor = vendor; }
        public String getOsType() { return osType; }
        public void setOsType(String osType) { this.osType = osType; }
        public String getQueryType() { return queryType; }
        public void setQueryType(String queryType) { this.queryType = queryType; }
        public String getTemplateName() { return templateName; }
        public void setTemplateName(String templateName) { this.templateName = templateName; }
        public String getCommandText() { return commandText; }
        public void setCommandText(String commandText) { this.commandText = commandText; }
        public String getParameterSchema() { return parameterSchema; }
        public void setParameterSchema(String parameterSchema) { this.parameterSchema = parameterSchema; }
        public Integer getIsPublic() { return isPublic; }
        public void setIsPublic(Integer isPublic) { this.isPublic = isPublic; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}
