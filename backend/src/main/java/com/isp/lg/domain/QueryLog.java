package com.isp.lg.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "query_logs")
public class QueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    @Column(name = "source_ip", nullable = false, length = 64)
    private String sourceIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "query_type", nullable = false, length = 64)
    private String queryType;

    @Column(name = "query_target", nullable = false, length = 255)
    private String queryTarget;

    @Column(name = "query_params_json", columnDefinition = "JSON")
    private String queryParamsJson;

    @Column(name = "pop_id")
    private Long popId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "command_template_id")
    private Long commandTemplateId;

    @Column(name = "raw_command", columnDefinition = "TEXT")
    private String rawCommand;

    @Column(name = "result_status", length = 32)
    private String resultStatus;

    @Column(name = "result_text", columnDefinition = "MEDIUMTEXT")
    private String resultText;

    @Column(name = "duration_ms")
    private Integer durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getSourceIp() { return sourceIp; }
    public void setSourceIp(String sourceIp) { this.sourceIp = sourceIp; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    public String getQueryTarget() { return queryTarget; }
    public void setQueryTarget(String queryTarget) { this.queryTarget = queryTarget; }
    public String getQueryParamsJson() { return queryParamsJson; }
    public void setQueryParamsJson(String queryParamsJson) { this.queryParamsJson = queryParamsJson; }
    public Long getPopId() { return popId; }
    public void setPopId(Long popId) { this.popId = popId; }
    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }
    public Long getCommandTemplateId() { return commandTemplateId; }
    public void setCommandTemplateId(Long commandTemplateId) { this.commandTemplateId = commandTemplateId; }
    public String getRawCommand() { return rawCommand; }
    public void setRawCommand(String rawCommand) { this.rawCommand = rawCommand; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getResultText() { return resultText; }
    public void setResultText(String resultText) { this.resultText = resultText; }
    public Integer getDurationMs() { return durationMs; }
    public void setDurationMs(Integer durationMs) { this.durationMs = durationMs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
