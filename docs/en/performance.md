---
title: Performance
---

# Performance

> Note: this page only describes the **structural improvements** Magisk Pro brings with its real implementation. It does not inflate specific numbers. Every point can be reproduced and verified from the source.

## Structural Improvements

### DoH Shortens the DNS Resolution Path

Traditional app DNS requests may traverse carrier recursive resolvers, adding multi-hop latency and cache inconsistency. Pro's `DnsResolver` talks directly to public DoH services (Cloudflare / Google / AdGuard), **reducing the number of recursive hops** and typically yielding more stable resolution latency on the same network.

- `dohCache` is reused within the same provider to avoid repeated handshakes.
- The cache is rebuilt only on provider switch, so day-to-day use costs nothing.

### Streamlined tmpfs Mount Reduces Root-Layout Churn

Upstream rebuilds a `/sbin` symlink tree in the `rw-root` scenario. Pro moves the mount to `/debug_ramdisk` and removes `recreate_sbin`:

- **Fewer filesystem operations**: no per-entry `/sbin` symlink rebuild.
- **Directory layout closer to AOSP defaults**, reducing interference with third-party tools that probe path conventions.
- The mount is still a tmpfs (memory-backed), so read/write performance is unchanged from upstream.

### Pro Hide Reduces the Detection Surface

The randomized process name prevents external tools from matching `magiskd` by a fixed name, **reducing the attack/detection surface** and the operational churn (re-installs, cleanups) that detection can trigger. Disguising the process name itself costs no extra resources.

### Group Injection Avoids Unnecessary Privilege Escalation

When a su request is injected with `system` / `shell` identity per policy, it **does not need to fully switch to the root domain**, narrowing the privilege escalation scope (least privilege) and reducing potential audit noise.

## Suggested Measurements

Latency and battery figures depend heavily on the device, kernel and network environment. We recommend A/B testing Magisk Pro against upstream Magisk on the **same device and network**.

| Dimension | How to compare |
|---|---|
| DNS resolution latency | `dig` / `nslookup` against system DNS vs. DoH |
| Mount layout | `mount` to inspect `/debug_ramdisk` and `/sbin` |
| Process name | `ps -A` / `cat /proc/<pid>/comm` to observe the random magiskd name |
| Authorization path | Compare SELinux contexts on su requests |
