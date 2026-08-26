---
title: Features
---

# Features

Magisk Pro keeps the upstream Magisk security model and Zygisk ecosystem compatibility, while adding the following core capabilities. Every item is backed by source code and reproducible builds.

## 🕶️ Pro Hide Process Disguise

The upstream Magisk daemon `magiskd` runs with a fixed process name, which makes it easy for security software, detection tools and log audits to hit directly.

**What Pro changes:**

- On startup, `magiskd` picks a random name from 10 preset kernel-thread-style names (e.g. `kworker/u16:2`, `ksoftirqd/0`, `jbd2/sda1-8`).
- Random seed = current time in nanoseconds XOR `PID << 20`, so every boot yields a different, unpredictable name.
- `set_nice_name` clears `argv[0]` and calls `prctl(PR_SET_NAME)`, so `/proc/self/cmdline` and the thread name are disguised consistently.
- All candidate names are ≤ 15 bytes, within the kernel `TASK_COMM_LEN` limit, to avoid truncation.
- Controlled by the database setting `pro_hide`; disabled by default, effective after a daemon restart.

**Files:** `native/src/daemon/daemon.rs`, `native/src/core/app.cpp`

## 🎭 Custom su Permission Groups

Upstream Magisk grants su as root by default. Pro lets you downgrade a request to `system` or `shell` identity for finer-grained privileges.

**Three identities:**

| Group | uid | SELinux context | gids |
|---|---|---|---|
| root | 0 | `u:r:magisk:s0` | 0 |
| system | 1000 | `u:r:system_app:s0` | 1000 + 3003 |
| shell | 2000 | `u:r:shell:s0` | 2000 + 3003 |

**Implementation notes:**

- `RootSettings` gains a `group` field, persisted in the `grp` column of the policies table.
- The custom identity is injected **only** when the requester did not explicitly specify target_uid / gids / SELinux context, so fine-grained app requests are never overridden.
- Fixed the path where a `uid=0` root request also went through the policy database, unifying the authorization flow.
- The FIFO protocol appends a group integer after the policy integer, keeping binary compatibility.
- Controlled by the database switch `su_group_enabled`, independent of individual policies.

**Files:** `native/src/core/su/daemon.rs`, `native/src/connect.rs`, `native/src/core/policy.rs`

## 🛡️ DoH Secure DNS Channel

Network requests in the app default to the system DNS, which can be polluted or hijacked by carriers and routers.

**What Pro changes:**

- The `Networking` layer extends `DnsResolver` to a **4-way choice**: Cloudflare / Google / AdGuard / custom URL.
- `dohCache` is rebuilt only when switching providers; connections are reused within the same provider.
- New settings `DNS_PROVIDER` (0–3) and `DNS_CUSTOM_URL` are exposed in the settings screen.
- Falls back to the system DNS on failure to keep availability first.

**Files:** `app/shared/.../Networking.kt`, `app/core/.../Config.kt`

## ⚡ Enhanced DenyList

Upstream DenyList requires checking entries one by one, and the in-app switch state may drift from the actual magiskd state.

**What Pro changes:**

- Adds **one-tap select-all** and **one-tap clear-all**, reusing `toggle()` / `toggleAll()` for batched writes.
- The settings page refreshes the switch state via `magisk --denylist status` on entry.
- `LifecycleResumedEffect` re-syncs the state every time the page returns to the foreground, eliminating the "turned on but not active" illusion.

**Files:** `app/apk-ng/.../DenyListViewModel.kt`, `app/apk-ng/.../SettingsViewModel.kt`

## 🧩 Upgraded Module Management

Upstream module list only supports paging. Pro adds the three most common daily operations.

**What Pro changes:**

- **Search**: fuzzy match across module name, ID, author and description.
- **Sort**: by name, version or author, with one-tap reverse ordering.
- **Pull-to-refresh**: `PullToRefreshBox` gesture refresh without restarting the app.
- State flow aggregated with `combine(uiState, query, sortBy, sortReverse)` + `stateIn(Eagerly)`: search and sorting are fully reactive with zero extra network requests.

**Files:** `app/apk-ng/.../ModuleViewModel.kt`

## 📁 Streamlined tmpfs Mount

Upstream mounts a tmpfs at `/sbin` and rebuilds a symlink tree in the `rw-root` scenario. Pro relocates the mount point to `/debug_ramdisk`.

**What Pro changes:**

- In `rw-root`, the tmpfs mount point moves from `/sbin` to `/debug_ramdisk`, reducing intrusion into the system root layout.
- Removes `recreate_sbin`; the `/sbin` symlink tree is no longer rebuilt.
- `get_magisk_tmp()` probes `/debug_ramdisk/.magisk` first, keeping compatibility with the legacy path.
- The `ro-root` path already used `/debug_ramdisk`; both paths now behave consistently.

**Files:** `native/src/init/rootdir.cpp`

## Other Improvements

- **Relaxed signing policy**: APK signature verification failures degrade to a warning, allowing the official Manager and this project to coexist.
- **Update channel**: the built-in GitHub repository points to this project for in-app update checks.
- **Version scheme**: `20.001` / `200010`, evolving independently from upstream.
- **CI**: the build pipeline restores a debug signing keystore so artifacts are signed reproducibly.
