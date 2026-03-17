package com.isp.lg.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "pops")
public class Pop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pop_code", nullable = false, length = 32, unique = true)
    private String popCode;

    @Column(name = "pop_name", nullable = false, length = 128)
    private String popName;

    @Column(name = "country", length = 64)
    private String country;

    @Column(name = "city", length = 64)
    private String city;

    @Column(name = "is_public", nullable = false)
    private Integer isPublic;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "remark", length = 255)
    private String remark;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPopCode() { return popCode; }
    public void setPopCode(String popCode) { this.popCode = popCode; }
    public String getPopName() { return popName; }
    public void setPopName(String popName) { this.popName = popName; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public Integer getIsPublic() { return isPublic; }
    public void setIsPublic(Integer isPublic) { this.isPublic = isPublic; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
