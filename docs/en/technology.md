---
title: Technology
---

# Technology

Magisk Pro inherits the classic **native layer + daemon + app shell** three-tier architecture of Magisk, and introduces Rust components at the native layer.

## Three-Tier Architecture

```
┌─────────────────────────────────────────────────────────┐
│  App layer (Kotlin + Jetpack Compose)                    │
│  Manager UI · Module manager · DenyList · Settings · DoH │
├─────────────────────────────────────────────────────────┤
│  Native layer (C++ + Rust)                              │
│  magiskinit boot chain · magiskd daemon · su · root      │
├─────────────────────────────────────────────────────────┤
│  Kernel space (same as upstream)                        │
│  overlayfs / tmpfs mounts · Zygisk injection (compat)    │
└─────────────────────────────────────────────────────────┘
```

### C++ Boot Chain

`magiskinit` takes over the init flow early in Android user-space:

- Detects the rootfs type (`rw-root` / `ro-root`) from the kernel cmdline (`androidboot.xxx`).
- Injects SELinux policy to open the necessary permissions for the `magisk` domain.
- Mounts overlayfs / tmpfs as needed to expose the Magisk core directories to `magiskd`.

### Rust Daemon

`magiskd` (`daemon.rs`) rewrites core logic in Rust: su authorization decisions, DenyList state sync, and Magisk core file serving. Rust and C++ components communicate over a unified file protocol and sockets.

### Kotlin App

The Manager app is built with Jetpack Compose:

- Module management (search / sort / pull-to-refresh)
- DenyList management (select-all / clear / live state)
- Settings (Pro Hide, su permission groups, DoH provider, etc.)
- Update checks (GitHub Releases)

## SELinux Injection

Magisk Pro follows the upstream `sepolicy` injection approach: `magiskinit` merges the required allow rules directly into the kernel policy via `libsepol` early in boot, avoiding reliance on vendor `sepolicy.rule` compatibility layers.

## Root Injection Path

```
App requests root
   │
   ▼
magiskd (Rust) → queries the policies table
   │
   ├── policy hit: identity decided by grp (root/system/shell)
   ├── no policy: show authorization dialog → user confirms → write back
   │
   ▼
su process runs the target command with the chosen uid / SELinux context / gids
```

- Permission group injection only applies when the requester did not explicitly specify an identity (see [Features](/en/features)).
- Every decision is recorded in the `policies` table, with the `grp` column persisting the group.

## tmpfs Mount Layout

Magisk Pro relocates the `rw-root` tmpfs mount from `/sbin` to `/debug_ramdisk`:

```
/sbin            → no longer created (recreate_sbin removed)
/debug_ramdisk   → tmpfs mount exposing Magisk core files
get_magisk_tmp() → probes /debug_ramdisk/.magisk first
```

- Reduces intrusion into the system root layout, closer to the AOSP default directory conventions.
- `ro-root` and `rw-root` paths now behave consistently.

## DoH Traffic Path

```
App network request
   │
   ▼
Networking (Kotlin) → DnsResolver (4-way choice)
   │
   ├── Cloudflare / Google / AdGuard / custom
   ├── dohCache reused per provider
   │
   ▼
DoH lookup failed? ── yes → fall back to system DNS
   │
   ▼
Open the real connection
```

- Settings: `DNS_PROVIDER` (0–3), `DNS_CUSTOM_URL`.
- Automatic fallback keeps availability first.

## Binary Compatibility

- The FIFO authorization protocol appends a group integer after the policy integer; old clients are unaffected.
- The relaxed signing policy lets the official Manager coexist on the same device.
