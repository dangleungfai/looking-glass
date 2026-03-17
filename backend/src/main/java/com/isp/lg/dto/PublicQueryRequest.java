package com.isp.lg.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class PublicQueryRequest {

    @NotBlank(message = "popCode is required")
    private String popCode;

    @NotBlank(message = "queryType is required")
    private String queryType;

    @NotBlank(message = "target is required")
    private String target;

    private Map<String, Object> params;

    private String captchaToken;

    public String getPopCode() { return popCode; }
    public void setPopCode(String popCode) { this.popCode = popCode; }
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public String getCaptchaToken() { return captchaToken; }
    public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
}
