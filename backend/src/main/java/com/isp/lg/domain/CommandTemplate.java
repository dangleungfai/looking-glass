package com.isp.lg.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "command_templates")
public class CommandTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor", nullable = false, length = 64)
    private String vendor;

    @Column(name = "os_type", nullable = false, length = 64)
    private String osType;

    @Column(name = "query_type", nullable = false, length = 64)
    private String queryType;

    @Column(name = "template_name", nullable = false, length = 128)
    private String templateName;

    @Column(name = "command_text", nullable = false, columnDefinition = "TEXT")
    private String commandText;

    @Column(name = "parameter_schema", nullable = false, columnDefinition = "JSON")
    private String parameterSchema;

    @Column(name = "is_public", nullable = false)
    private Integer isPublic;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
