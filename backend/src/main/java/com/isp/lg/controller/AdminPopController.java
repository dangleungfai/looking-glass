package com.isp.lg.controller;

import com.isp.lg.domain.Pop;
import com.isp.lg.repository.DeviceRepository;
import com.isp.lg.repository.PopRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/pops")
public class AdminPopController {

    private final PopRepository popRepository;
    private final DeviceRepository deviceRepository;

    public AdminPopController(PopRepository popRepository, DeviceRepository deviceRepository) {
        this.popRepository = popRepository;
        this.deviceRepository = deviceRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS', 'READONLY')")
    public ResponseEntity<List<Pop>> list() {
        return ResponseEntity.ok(popRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<Pop> create(@Valid @RequestBody PopBody body) {
        Pop p = new Pop();
        p.setPopCode(body.getPopCode());
        p.setPopName(body.getPopName());
        p.setCountry(body.getCountry() != null && !body.getCountry().isBlank() ? body.getCountry() : "中国");
        p.setCity(body.getCity());
        p.setIsPublic(body.getIsPublic() != null ? body.getIsPublic() : 1);
        p.setStatus(body.getStatus() != null ? body.getStatus() : 1);
        p.setSortOrder(body.getSortOrder() != null ? body.getSortOrder() : 0);
        p.setRemark(body.getRemark());
        p.setCreatedAt(Instant.now());
        p.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(popRepository.save(p));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<Pop> update(@PathVariable Long id, @Valid @RequestBody PopBody body) {
        Pop p = popRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("POP not found"));
        if (body.getPopCode() != null && !body.getPopCode().isBlank()) {
            p.setPopCode(body.getPopCode().trim());
        }
        p.setPopName(body.getPopName());
        p.setCountry(body.getCountry());
        p.setCity(body.getCity());
        p.setIsPublic(body.getIsPublic() != null ? body.getIsPublic() : p.getIsPublic());
        p.setStatus(body.getStatus() != null ? body.getStatus() : p.getStatus());
        p.setSortOrder(body.getSortOrder() != null ? body.getSortOrder() : p.getSortOrder());
        p.setRemark(body.getRemark());
        p.setUpdatedAt(Instant.now());
        return ResponseEntity.ok(popRepository.save(p));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPS')")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id) {
        Pop pop = popRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("POP not found"));
        long boundDevices = deviceRepository.countByPopId(pop.getId());
        if (boundDevices > 0) {
            throw new IllegalArgumentException("该 POP 下仍有关联设备，无法删除，请先迁移或删除设备。");
        }
        popRepository.delete(pop);
        return ResponseEntity.ok(Map.of("message", "deleted"));
    }

    public static class PopBody {
        private String popCode;
        @NotBlank
        private String popName;
        private String country;
        private String city;
        private Integer isPublic;
        private Integer status;
        private Integer sortOrder;
        private String remark;

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
    }
}
