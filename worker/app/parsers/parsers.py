"""Parse raw device output into structured result for PING, TRACEROUTE, BGP_PREFIX."""

import re
from typing import Any


def parse_ping(raw: str) -> dict[str, Any]:
    summary = {"loss": "0%", "min": "-", "avg": "-", "max": "-"}
    # Common patterns: "5 packets transmitted, 5 received, 0% packet loss"
    loss_m = re.search(r"(\d+)%?\s*packet loss|(\d+)\s*received", raw, re.I)
    if loss_m:
        if "%" in raw:
            pct = re.search(r"(\d+)%?\s*packet loss", raw, re.I)
            if pct:
                summary["loss"] = f"{pct.group(1)}%"
        else:
            recv = re.search(r"(\d+)\s*received", raw, re.I)
            if recv:
                summary["received"] = recv.group(1)
    # min/avg/max: "round-trip min/avg/max = 1.2/2.3/4.5 ms"
    rtt_m = re.search(r"min/avg/max[^=]*=\s*([\d.]+)/([\d.]+)/([\d.]+)\s*ms", raw, re.I)
    if rtt_m:
        summary["min"] = f"{rtt_m.group(1)}ms"
        summary["avg"] = f"{rtt_m.group(2)}ms"
        summary["max"] = f"{rtt_m.group(3)}ms"
    return {"summary": summary, "raw_text": raw}


def parse_traceroute(raw: str) -> dict[str, Any]:
    hops = []
    for line in raw.splitlines():
        # " 1  1.2.3.4  1.2 ms" or " 1  1.2.3.4 (1.2.3.4)  1.2 ms"
        m = re.search(r"^\s*(\d+)\s+([^\s]+)(?:\s+\([^)]+\))?\s+([\d.*]+)\s*ms", line)
        if m:
            hops.append({"hop": int(m.group(1)), "ip": m.group(2).strip("()"), "rtt_ms": m.group(3)})
    return {"hops": hops, "raw_text": raw}


def parse_bgp_prefix(raw: str) -> dict[str, Any]:
    return {"raw_text": raw}


def parse_output(query_type: str, raw: str) -> dict[str, Any]:
    if query_type in ("PING", "IPV4_PING", "IPV6_PING"):
        return parse_ping(raw)
    if query_type in ("TRACEROUTE", "IPV4_TRACEROUTE", "IPV6_TRACEROUTE"):
        return parse_traceroute(raw)
    if query_type in ("BGP_PREFIX", "BGP_ASN", "ROUTE_LOOKUP", "IPV4_BGP_ROUTE", "IPV6_BGP_ROUTE"):
        return parse_bgp_prefix(raw)
    return {"raw_text": raw}
