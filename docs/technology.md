---
title: 技术架构
---

# 技术架构

Magisk Pro 继承 Magisk 经典的 **原生层 + 守护进程 + App 壳** 三段式架构，并在原生层引入 Rust 组件。

## 三层架构

```
┌─────────────────────────────────────────────────────────┐
│  App 层（Kotlin + Jetpack Compose）                      │
│  Manager UI · 模块管理 · DenyList · 设置 · DoH 网络层     │
├─────────────────────────────────────────────────────────┤
│  原生层（C++ + Rust）                                    │
│  magiskinit 启动链 · magiskd 守护 · su 授权 · root 注入   │
├─────────────────────────────────────────────────────────┤
│  内核空间（与上游一致）                                   │
│  overlayfs / tmpfs 挂载 · Zygisk 注入（生态兼容）          │
└─────────────────────────────────────────────────────────┘
```

### C++ 启动链

`magiskinit` 在 Android 用户态启动早期接管 init 流程，负责：

- 根据内核命令行（`androidboot.xxx`）探测 rootfs 类型（`rw-root` / `ro-root`）。
- 注入 SELinux 策略，为 `magisk` 域打开必要权限。
- 按需挂载 overlayfs / tmpfs，将 Magisk 核心目录暴露给 `magiskd`。

### Rust 守护进程

`magiskd`（`daemon.rs`）用 Rust 重写核心逻辑，负责 su 授权决策、DenyList 状态同步与 Magisk 核心文件服务。Rust 组件与 C++ 组件通过统一的文件协议与 sockets 通信。

### Kotlin App

Manager App 采用 Jetpack Compose 构建，功能模块包括：

- 模块管理（搜索 / 排序 / 下拉刷新）
- DenyList 管理（全选 / 清除 / 实时状态）
- 设置项（Pro Hide、su 权限组、DoH 服务商等）
- 更新检查（GitHub Releases）

## SELinux 注入

Magisk Pro 沿用上游的 `sepolicy` 注入方式：`magiskinit` 在早期通过 `libsepol` 将 Magisk 所需的 allow 规则直接合并进内核策略，避免依赖厂商的 `sepolicy.rule` 兼容层。

## Root 注入路径

```
App 请求 root
   │
   ▼
magiskd（Rust）→ 查询 policies 表
   │
   ├── 命中策略：按 grp 决定 root/system/shell 身份
   ├── 未命中：弹出授权请求 → 用户确认 → 写回数据库
   │
   ▼
su 进程以指定 uid / SELinux context / gids 运行目标命令
```

- 权限组注入仅在请求方未显式指定身份时生效（详见[功能页](/features)）。
- 所有决策记录在 `policies` 表，附带 `grp` 列持久化权限组。

## tmpfs 挂载方案

Magisk Pro 将 rootfs（`rw-root`）场景的 tmpfs 挂载点从 `/sbin` 迁移到 `/debug_ramdisk`：

```
/sbin            → 不再创建（移除 recreate_sbin）
/debug_ramdisk   → 挂载 tmpfs，暴露 Magisk 核心文件
get_magisk_tmp() → 优先探测 /debug_ramdisk/.magisk
```

- 减少对根目录结构的侵入，与 AOSP 默认目录约定更一致。
- `ro-root` 与 `rw-root` 两条路径行为统一。

## DoH 流量路径

```
App 网络请求
   │
   ▼
Networking（Kotlin）→ DnsResolver（4 选 1）
   │
   ├── Cloudflare / Google / AdGuard / 自定义
   ├── dohCache 按服务商复用
   │
   ▼
DoH 解析失败？── 是 → 回退系统 DNS
   │
   ▼
发起真实连接
```

- 配置项：`DNS_PROVIDER`（0–3）、`DNS_CUSTOM_URL`。
- 失败自动回退，保证可用性优先。

## 二进制兼容

- FIFO 授权协议在策略整型后追加权限组整型，旧客户端无感知。
- 签名策略放宽为警告级，官方 Manager 可共存安装。
