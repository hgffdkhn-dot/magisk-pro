---
layout: home

hero:
  name: Magisk Pro
  text: 基于 Magisk 深度定制的增强型 Root 方案
  tagline: 进程伪装 · 自定义权限组 · 安全 DNS · 更顺手的模块管理
  image:
    src: /magisk-pro/logo.svg
    alt: Magisk Pro
  actions:
    - theme: brand
      text: 开始阅读
      link: /features
    - theme: alt
      text: 在 GitHub 查看
      link: https://github.com/hgffdkhn-dot/magisk-pro

features:
  - icon: 🕶️
    title: Pro Hide 进程伪装
    details: magiskd 守护进程以随机内核线程名运行，大幅降低被安全软件与检测工具识别的暴露面。
  - icon: 🎭
    title: 自定义 su 权限组
    details: 为每个 root 请求注入 system / shell / root 三档身份与对应 SELinux 上下文，权限按需下发。
  - icon: 🛡️
    title: DoH 安全 DNS 通道
    details: 内置 Cloudflare / Google / AdGuard / 自定义四家 DoH 服务商，规避污染与劫持，失败自动回退系统 DNS。
  - icon: ⚡
    title: DenyList 增强
    details: 一键全选 / 一键清除隔离名单，App 内状态与 magiskd 实时同步，操作零往返。
  - icon: 🧩
    title: 模块管理升级
    details: 模块列表支持关键词搜索、按名称 / 版本 / 作者排序、下拉刷新，海量模块一屏掌控。
  - icon: 📁
    title: tmpfs 精简挂载
    details: rootfs 场景下 tmpfs 改挂 /debug_ramdisk，精简启动路径，减少符号链接重建。
---

<HomeContent>
Magisk Pro 是构建在 [Magisk](https://github.com/topjohnwu/Magisk) 官方源码之上的增强分支。在保持上游安全模型与 Zygisk 生态完整兼容的同时，我们补强了进程隐蔽性、权限粒度、网络安全与日常可操作性四个方向，全部改动均有源码与可复现构建支撑。
</HomeContent>
