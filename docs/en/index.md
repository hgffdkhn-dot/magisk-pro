---
layout: home

hero:
  name: "Magisk Pro"
  text: "An enhanced root solution built on Magisk"
  tagline: "Process disguise · Custom su permission groups · Secure DNS · Refined module management"
  image:
    src: /magisk-pro/logo.svg
    alt: Magisk Pro
  actions:
    - theme: brand
      text: Get Started
      link: /en/features
    - theme: alt
      text: View on GitHub
      link: https://github.com/hgffdkhn-dot/magisk-pro

features:
  - icon: 🕶️
    title: Pro Hide Process Disguise
    details: magiskd starts with a randomized kernel-thread-style process name, hiding from detection tools that match fixed names.
  - icon: 🎭
    title: Custom su Permission Groups
    details: Grant su as root, system, or shell identity with fine-grained SELinux contexts and gids per policy.
  - icon: 🛡️
    title: DoH Secure DNS
    details: DnsResolver backed by Cloudflare / Google / AdGuard / custom endpoints, with automatic system-DNS fallback.
  - icon: ⚡
    title: Enhanced DenyList
    details: One-tap select-all / clear-all plus live state sync with magiskd every time the settings page resumes.
  - icon: 🧩
    title: Upgraded Module Management
    details: Search, sort and pull-to-refresh over your installed modules with reactive filtering and zero extra requests.
  - icon: 📁
    title: Streamlined tmpfs Mount
    details: rw-root tmpfs relocated from /sbin to /debug_ramdisk, removing the legacy recreate_sbin symlink tree.

# HomeContent block: parsed as regular Markdown below the features grid
---

Magisk Pro is an independent fork built on top of the official Magisk source tree. It keeps the upstream security model and the Zygisk ecosystem compatible, while adding practical enhancements: randomized daemon process names, per-policy su identity injection, encrypted DNS transport, a more usable DenyList and module manager, and a cleaner tmpfs layout.

Every feature listed here is backed by real source code and reproducible CI builds. See [Features](/en/features), [Technology](/en/technology) and [Performance](/en/performance) for details.
