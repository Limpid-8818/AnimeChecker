# AnimeChecker（本地版重建）

个人追番记录 APP 本地版的全新工程：无后端，Room 本地存储，数据源 AniList 直连，内置 LLM 翻译。
本仓库为从零重建，原实现位于 `misc/AnimeChecker/app-local`（迁移参照）。

## 环境

- Android Studio 2026.2.1（Rabbit）及以上
- JDK 17+
- Android SDK Platform 37、Build-Tools 36.0.0
- AGP 9.4.1（内置 Kotlin 2.2.10，无需 kotlin-android 插件）/ Gradle 9.6.1

## 构建

```bash
./gradlew assembleDebug
```

首次拉取依赖如需代理：`./gradlew assembleDebug -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897`
