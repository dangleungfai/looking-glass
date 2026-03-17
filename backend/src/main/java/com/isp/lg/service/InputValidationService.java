package com.isp.lg.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class InputValidationService {

    private static final Pattern IPV4 = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    private static final Pattern IPV6 = Pattern.compile(
            "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|^(?:[0-9a-fA-F]{1,4}:){1,7}:$|^(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$");
    private static final Pattern CIDR = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)/(?:[0-9]|[12][0-9]|3[0-2])$");
    private static final Pattern ASN = Pattern.compile("^(?:AS)?([0-9]{1,10})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOMAIN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9.-]{0,252}[a-zA-Z0-9]$");
    private static final int MAX_TARGET_LEN = 256;

    public void validateTargetForQueryType(String target, String queryType) {
        if (target == null || target.length() > MAX_TARGET_LEN) {
            throw new IllegalArgumentException("target length invalid");
        }
        String t = target.trim();
        switch (queryType.toUpperCase()) {
            case "PING":
            case "TRACEROUTE":
            case "ROUTE_LOOKUP":
                if (!isValidIpv4(t) && !isValidIpv6(t) && !isValidDomain(t)) {
                    throw new IllegalArgumentException("target must be IPv4, IPv6 or domain");
                }
                break;
            case "BGP_PREFIX":
                if (!isValidCidr(t) && !isValidIpv4(t) && !isValidIpv6(t)) {
                    throw new IllegalArgumentException("target must be CIDR or IP");
                }
                break;
            case "BGP_ASN":
                if (!isValidAsn(t)) {
                    throw new IllegalArgumentException("target must be ASN (e.g. 9809 or AS9809)");
                }
                break;
            default:
                if (!isValidIpv4(t) && !isValidIpv6(t) && !isValidDomain(t) && !isValidCidr(t) && !isValidAsn(t)) {
                    throw new IllegalArgumentException("target format not allowed");
                }
        }
    }

    public boolean isValidIpv4(String s) {
        return s != null && IPV4.matcher(s).matches();
    }

    public boolean isValidIpv6(String s) {
        return s != null && IPV6.matcher(s).matches();
    }

    public boolean isValidCidr(String s) {
        return s != null && CIDR.matcher(s).matches();
    }

    public boolean isValidAsn(String s) {
        if (s == null) return false;
        return ASN.matcher(s.trim()).matches();
    }

    public boolean isValidDomain(String s) {
        if (s == null || s.length() > 253) return false;
        if (s.contains("..") || s.startsWith(".") || s.endsWith(".")) return false;
        return DOMAIN.matcher(s).matches();
    }

    public int clampCount(Integer count) {
        if (count == null) return 5;
        return Math.max(1, Math.min(10, count));
    }

    public int clampMaxHops(Integer hops) {
        if (hops == null) return 30;
        return Math.max(1, Math.min(30, hops));
    }
}
