<div align="center">
  <img src="docs/images/icon.png" width="110" alt="AnimeChecker 应用图标" />

  <h1>AnimeChecker</h1>

  <p>轻量级追番记录应用。</p>

  <p>
    <a href="LICENSE"><img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-yellow.svg" /></a>
    <img alt="Platform: Android 8.0+" src="https://img.shields.io/badge/Platform-Android_8.0%2B-3DDC84?logo=android&logoColor=white" />
    <img alt="Kotlin 2.2.10" src="https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?logo=kotlin&logoColor=white" />
    <img alt="Jetpack Compose / Material 3" src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white" />
    <img alt="Gradle 9.6.1" src="https://img.shields.io/badge/Gradle-9.6.1-02303A?logo=gradle&logoColor=white" />
  </p>
</div>

---

## 功能

- **追番清单**：在看、想看、看完、搁置、抛弃五种状态筛选，参考 Bangumi 的追番功能
- **放送表**：按每周日期分组的每日放送表，已放送集数实时推算
- **番库**：提供季度番剧目录和关键词检索
- **LLM 翻译**：针对数据源为英文的问题，可以自行配置 OpenAI 兼容 LLM 接口进行番剧条目的翻译
- **离线优先**：本地数据库是界面唯一数据源，离线可用，网络拉取数据自动合入数据库
- **简洁界面**：基于 Jetpack Compose Material 3 构建的界面，支持深浅双主题，随系统自动切换

## 技术栈

| 分类 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| 存储 | Room 2.8.5 / DataStore 1.2.1 |
| 网络 | Ktor Client 3.6 |
| 图片 | Coil 3.4 |
| 构建 | Gradle 9.6.1 / AGP 9.4.1 |

## 构建

环境要求：JDK 17+、Android SDK Platform 37 与 Build-Tools 36.0.0。

```bash
./gradlew assembleDebug
```

首次构建需要拉取依赖，或将花费更多的时间，如需使用代理：

```bash
./gradlew assembleDebug -Dhttps.proxyHost=<ProxyHost> -Dhttps.proxyPort=<ProxyPort>
```

release 构建将从根目录的 `secret-sign.conf` 读取签名信息（storeFile / storePass / keyAlias / keyPass），请自行使用签名工具 `apksigner` 生成签名文件并编辑配置文件，文件不存在时将回退到 debug 构建。

```bash
./gradlew assembleRelease
```

## 说明

- 番剧数据与封面来自 [AniList](https://anilist.co) 公开 API，放送日程仅供参考，以实际播出为准。
- 翻译内容由用户配置的 LLM 服务生成，准确性不作保证，可自行编辑修改翻译不准确的条目。

## 许可证

[MIT](LICENSE) © 2026 Limpid-8818
