package com.isp.lg.dto;

import java.util.Map;

public class PublicQueryResponse {

    private String requestId;
    private String status;
    private Long durationMs;
    private String pop;
    private String device;
    private Map<String, Object> result;

    public PublicQueryResponse() {}

    public PublicQueryResponse(String requestId, String status, Long durationMs, String pop, String device, Map<String, Object> result) {
        this.requestId = requestId;
        this.status = status;
        this.durationMs = durationMs;
        this.pop = pop;
        this.device = device;
        this.result = result;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getPop() { return pop; }
    public void setPop(String pop) { this.pop = pop; }
    public String getDevice() { return device; }
    public void setDevice(String device) { this.device = device; }
    public Map<String, Object> getResult() { return result; }
    public void setResult(Map<String, Object> result) { this.result = result; }
}
