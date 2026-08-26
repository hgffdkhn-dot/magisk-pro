import { defineConfig } from 'vitepress'

export default defineConfig({
  lang: 'zh-CN',
  title: 'Magisk Pro',
  description: '基于 Magisk 深度定制的增强型 Android Root 方案',
  base: '/magisk-pro/',
  cleanUrls: true,
  lastUpdated: true,
  srcExclude: ['upstream/**'],
  head: [
    ['meta', { name: 'theme-color', content: '#1a1a2e' }],
    ['link', { rel: 'icon', href: '/magisk-pro/favicon.svg', type: 'image/svg+xml' }],
    ['meta', { name: 'keywords', content: 'Magisk, Magisk Pro, Root, Android, Zygisk, su, DoH' }],
  ],

  locales: {
    root: {
      label: '简体中文',
      lang: 'zh-CN',
      title: 'Magisk Pro',
      description: '基于 Magisk 深度定制的增强型 Android Root 方案',
      themeConfig: {
        nav: [
          { text: '首页', link: '/' },
          { text: '功能', link: '/features' },
          { text: '技术', link: '/technology' },
          { text: '性能', link: '/performance' },
          { text: 'GitHub', link: 'https://github.com/hgffdkhn-dot/magisk-pro', target: '_blank' },
        ],
        sidebar: [
          {
            text: '项目',
            items: [
              { text: '首页', link: '/' },
              { text: '功能特性', link: '/features' },
              { text: '技术内幕', link: '/technology' },
              { text: '性能与安全', link: '/performance' },
            ],
          },
        ],
        outline: { label: '本页目录', level: [2, 3] },
        docFooter: { prev: '上一篇', next: '下一篇' },
        darkModeSwitchLabel: '外观',
        sidebarMenuLabel: '菜单',
        returnToTopLabel: '回到顶部',
        lastUpdated: { text: '最后更新于', formatOptions: { dateStyle: 'full', timeStyle: 'short' } },
        footer: {
          message: '基于 GPL-3.0 License 发布',
          copyright: 'Copyright © 2026 Magisk Pro Contributors',
        },
      },
    },
    en: {
      label: 'English',
      lang: 'en',
      title: 'Magisk Pro',
      description: 'An enhanced Android root solution built on Magisk',
      themeConfig: {
        nav: [
          { text: 'Home', link: '/en/' },
          { text: 'Features', link: '/en/features' },
          { text: 'Technology', link: '/en/technology' },
          { text: 'Performance', link: '/en/performance' },
          { text: 'GitHub', link: 'https://github.com/hgffdkhn-dot/magisk-pro', target: '_blank' },
        ],
        sidebar: [
          {
            text: 'Project',
            items: [
              { text: 'Home', link: '/en/' },
              { text: 'Features', link: '/en/features' },
              { text: 'Technology', link: '/en/technology' },
              { text: 'Performance', link: '/en/performance' },
            ],
          },
        ],
        outline: { label: 'On this page', level: [2, 3] },
        docFooter: { prev: 'Previous', next: 'Next' },
        darkModeSwitchLabel: 'Appearance',
        sidebarMenuLabel: 'Menu',
        returnToTopLabel: 'Back to top',
        lastUpdated: { text: 'Updated at', formatOptions: { dateStyle: 'full', timeStyle: 'short' } },
        footer: {
          message: 'Released under GPL-3.0 License',
          copyright: 'Copyright © 2026 Magisk Pro Contributors',
        },
      },
    },
  },
})
