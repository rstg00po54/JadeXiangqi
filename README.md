# 玉弈象棋 Jade Xiangqi — Android 第一版

一款独立于围棋 App 的中国象棋 Android 项目。包名 `com.badukai.xiangqi`，Java + Android SDK，无第三方运行库。

## 已实现

- 中国象棋 9×10 棋盘、楚河汉界、九宫与玉石质感棋子（Canvas 矢量绘制）
- 车、马（蹩马腿）、炮（隔子打）、象（塞象眼、不过河）、士、将帅（九宫与照面）、兵卒（过河横走）
- 禁止自陷被将；将死或困毙判负；红方先走
- 人机对弈（红/黑）、双人同屏、双 AI 自动对弈
- 内置 Java Alpha-Beta CPU AI，三个难度（不是皮卡鱼）
- 走法提示、AI 推荐、悔棋、逐手复盘、复制棋谱、自动恢复对局

## 构建

Android Studio 打开项目，Build > Build APK(s)。
AGP 8.2.2、Gradle 8.5、Java 17、compileSdk 35、minSdk 26。

通过 GitHub Actions 的 Build Jade Xiangqi Android APK 工作流可自动生成 debug APK Artifact。
在安装了 JDK 17、Android SDK 35 的机器执行（已内置 Gradle Wrapper 8.5，首次构建会自动下载 Gradle）：

```bash
bash build.sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

本地纯 Java 引擎测试：

```bash
bash scripts/test_core.sh
```

## 下一步

建议接入 [Pikafish](https://github.com/official-pikafish/Pikafish) 的 CPU + NNUE 象棋引擎。此版本尚未集成 Pikafish、GPU 或 NPU。
Pikafish 源码使用 GPLv3，官方 NNUE 权重另有非商业许可限制。

## 已知限制

- 内置搜索 AI 较弱，深度有限。
- 尚未实现赛事级长将、长捉和复杂重复裁决。
- 当前棋谱是内部坐标而非传统纵线记谱。
- 核心 Java 逻辑已测试；Android APK 构建需要 GitHub Actions 或本机 SDK 验证。
