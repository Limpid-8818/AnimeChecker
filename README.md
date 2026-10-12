# AnimeChecker

个人追番记录 APP。无后端，数据以 Room 存储在设备本地；番剧数据来自 AniList，标题与简介翻译通过用户自配的 OpenAI 兼容接口完成。

## 技术栈

- Kotlin + Jetpack Compose（Material 3）
- AGP 9.4.1 / Gradle 9.6.1（AGP 内置 Kotlin 2.2.10，无需 kotlin-android 插件）
- minSdk 26 / targetSdk 37

## 环境

- Android Studio 2026.2.1（Rabbit）及以上
- JDK 17+
- Android SDK Platform 37、Build-Tools 36.0.0

## 构建

```bash
./gradlew assembleDebug
```

首次拉取依赖如需代理：

```bash
./gradlew assembleDebug -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897
```
