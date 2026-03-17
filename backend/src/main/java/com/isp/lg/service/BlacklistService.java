package com.isp.lg.service;

import com.isp.lg.domain.IpBlacklist;
import com.isp.lg.repository.IpBlacklistRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BlacklistService {

    private final IpBlacklistRepository ipBlacklistRepository;

    public BlacklistService(IpBlacklistRepository ipBlacklistRepository) {
        this.ipBlacklistRepository = ipBlacklistRepository;
    }

    public boolean isBlacklisted(String clientIp) {
        List<IpBlacklist> entries = ipBlacklistRepository.findByStatus(1);
        for (IpBlacklist e : entries) {
            if (match(clientIp, e.getIpOrCidr())) {
                return true;
            }
        }
        return false;
    }

    private boolean match(String ip, String ipOrCidr) {
        if (ipOrCidr == null) return false;
        if (ipOrCidr.contains("/")) {
            return matchCidr(ip, ipOrCidr);
        }
        return ip.equals(ipOrCidr);
    }

    private boolean matchCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) return false;
            int prefixLen = Integer.parseInt(parts[1]);
            long ipLong = ipToLong(parts[0]);
            long cidrBase = ipToLong(parts[0]);
            long mask = prefixLen == 0 ? 0 : -1 << (32 - prefixLen);
            long ipVal = ipToLong(ip);
            return (ipVal & mask) == (cidrBase & mask);
        } catch (Exception e) {
            return false;
        }
    }

    private long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        long r = 0;
        for (int i = 0; i < 4; i++) {
            r = r << 8 | Integer.parseInt(octets[i]);
        }
        return r;
    }
}
