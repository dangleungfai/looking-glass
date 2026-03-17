package com.isp.lg.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "ip_blacklist")
public class IpBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_or_cidr", nullable = false, length = 64)
    private String ipOrCidr;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIpOrCidr() { return ipOrCidr; }
    public void setIpOrCidr(String ipOrCidr) { this.ipOrCidr = ipOrCidr; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
