package com.isp.lg.worker;

import java.util.Map;

public class WorkerTaskRequest {
    private String requestId;
    private String mgmtIp;
    private int sshPort;
    private String username;
    private String password;
    private String privateKey;
    private String command;
    private String queryType;
    private int timeoutSec;
    private Map<String, Object> resultShape;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getMgmtIp() { return mgmtIp; }
    public void setMgmtIp(String mgmtIp) { this.mgmtIp = mgmtIp; }
    public int getSshPort() { return sshPort; }
    public void setSshPort(int sshPort) { this.sshPort = sshPort; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getQueryType() { return queryType; }
    public void setQueryType(String queryType) { this.queryType = queryType; }
    public int getTimeoutSec() { return timeoutSec; }
    public void setTimeoutSec(int timeoutSec) { this.timeoutSec = timeoutSec; }
    public Map<String, Object> getResultShape() { return resultShape; }
    public void setResultShape(Map<String, Object> resultShape) { this.resultShape = resultShape; }
}
