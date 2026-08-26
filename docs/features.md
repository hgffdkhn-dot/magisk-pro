---
title: 功能特性
---

# 功能特性

Magisk Pro 在保持 Magisk 上游安全模型与 Zygisk 生态兼容的前提下，新增了以下核心能力。每一项都有源码与可复现构建支撑。

## 🕶️ Pro Hide 进程伪装

Magisk 的守护进程 `magiskd` 在运行时进程名固定为 `magiskd`，容易被安全软件、检测工具与日志审计直接命中。

**Pro 的改动：**

- `magiskd` 启动时，从 10 个预置的内核线程风格名称（如 `kworker/u16:2`、`ksoftirqd/0`、`jbd2/sda1-8` 等）中随机挑选一个作为新进程名。
- 随机种子 = 当前时间纳秒异或 `PID << 20`，确保每次启动都不同、不可预测。
- 通过 `set_nice_name` 同时清空 `argv[0]` 并调用 `prctl(PR_SET_NAME)`，保证 `/proc/self/cmdline` 与线程名全部一致伪装。
- 所有候选名均 ≤ 15 字节，符合内核 `TASK_COMM_LEN` 限制，避免截断导致名称异常。
- 开关为数据库设置项 `pro_hide`，默认关闭，修改后重启守护进程生效。

**涉及文件：** `native/src/daemon/daemon.rs`、`native/src/core/app.cpp`（`base.cpp` 所在模块）

## 🎭 自定义 su 权限组

上游 Magisk 的 su 授权后默认以 root 身份运行。Pro 允许按需将请求降权为 `system` 或 `shell` 用户，权限粒度更细。

**三种身份：**

| 组 | uid | SELinux 上下文 | gids |
|---|---|---|---|
| root | 0 | `u:r:magisk:s0` | 0 |
| system | 1000 | `u:r:system_app:s0` | 1000 + 3003 |
| shell | 2000 | `u:r:shell:s0` | 2000 + 3003 |

**实现要点：**

- `RootSettings` 新增 `group` 字段，持久化在 policies 表的 `grp` 列。
- 仅当请求方**未显式**指定 target_uid / gids / SELinux context 时注入自定义身份，避免覆盖 App 自身的精细请求。
- 修复了 `uid=0` 的 root 请求也走策略数据库查询的问题，保证统一授权路径。
- FIFO 通信协议在策略整型之后追加一个权限组整型，保持二进制兼容。
- 由数据库开关 `su_group_enabled` 控制，独立于单条策略。

**涉及文件：** `native/src/core/su/daemon.rs`、`native/src/connect.rs`、`native/src/core/policy.rs`

## 🛡️ DoH 安全 DNS 通道

App 内的网络请求默认走系统 DNS，存在被运营商 / 路由器污染与劫持的风险。

**Pro 的改动：**

- `Networking` 层将 `DnsResolver` 扩展为 **4 选 1**：Cloudflare / Google / AdGuard / 自定义 URL。
- `dohCache` 只在切换服务商时重建，同一服务商下复用连接，避免频繁重建缓存。
- 新增配置项 `DNS_PROVIDER`（0–3）与 `DNS_CUSTOM_URL`，可在设置页直接切换。
- DoH 请求失败时自动回退系统 DNS，保证可用性优先。

**涉及文件：** `app/shared/src/main/kotlin/.../Networking.kt`、`app/core/.../Config.kt`

## ⚡ DenyList 增强

上游 DenyList（隔离名单）需要逐条勾选，且 App 内开关状态与 magiskd 实际状态可能出现不一致。

**Pro 的改动：**

- 新增**一键全选**与**一键清除**，复用 `toggle()` / `toggleAll()` 逻辑批量写入。
- Settings 页在进入时通过 `magisk --denylist status` 实时刷新开关状态。
- 通过 `LifecycleResumedEffect` 在页面每次回到前台时重新同步，杜绝"开了但没生效"的假象。

**涉及文件：** `app/apk-ng/.../DenyListViewModel.kt`、`app/apk-ng/.../SettingsViewModel.kt`

## 🧩 模块管理升级

上游模块列表仅支持翻页浏览。Pro 补上了日常管理最常用的三个操作。

**Pro 的改动：**

- **搜索**：按模块名、ID、作者、描述四个字段模糊匹配。
- **排序**：支持按名称、版本、作者排序，并可一键反转排序方向。
- **下拉刷新**：`PullToRefreshBox` 手势刷新，无需重启 App。
- 状态流用 `combine(uiState, query, sortBy, sortReverse)` + `stateIn(Eagerly)` 聚合，搜索与排序零额外请求、响应式即时更新。

**涉及文件：** `app/apk-ng/.../ModuleViewModel.kt`

## 📁 tmpfs 精简挂载

上游在 rootfs 场景（`rw-root`）会把 tmpfs 挂载到 `/sbin` 并重建符号链接树。Pro 将挂载点迁移到 `/debug_ramdisk`。

**Pro 的改动：**

- rw-root 场景下 tmpfs 挂载点从 `/sbin` 迁移到 `/debug_ramdisk`，减少对系统根目录结构的侵入。
- 移除 `recreate_sbin` 逻辑，不再重建 `/sbin` 符号链接树。
- `get_magisk_tmp()` 优先探测 `/debug_ramdisk/.magisk`，兼顾旧路径兼容。
- ro-root 场景原本已走 `/debug_ramdisk` 分支，两条路径现在保持一致。

**涉及文件：** `native/src/init/rootdir.cpp`

## 其他改进

- **签名策略放宽**：APK 签名校验失败从硬失败降级为警告，允许官方 Manager 与本项目共存安装。
- **更新通道**：内置 GitHub 仓库已切换到本项目源，可直接在 App 内检查更新。
- **版本方案**：`20.001` / `200010`，独立于上游版本号演进。
- **CI**：构建流水线包含调试签名 keystore 恢复，保证产物签名稳定可复现。
