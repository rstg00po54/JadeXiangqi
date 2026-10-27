# 玉弈象棋 Jade Xiangqi — Android 第一版

一款独立于围棋 App 的中国象棋 Android 项目。包名 `com.badukai.xiangqi`，Java + Android SDK，无第三方运行库。

## 已实现

- 中国象棋 9×10 棋盘、楚河汉界、九宫与玉石质感棋子（Canvas 矢量绘制）
- 车、马（蹩马腿）、炮（隔子打）、象（塞象眼、不过河）、士、将帅（九宫与照面）、兵卒（过河横走）
- 禁止自陷被将；将死或困毙判负；红方先走
- 人机对弈（红/黑）、双人同屏、双 AI 自动对弈
- 内置 Java Alpha-Beta CPU AI，三个难度（不是皮卡鱼）
- 走法提示、AI 推荐、悔棋、逐手复盘、复制棋谱、自动恢复对局
- 棋子移动缓动动画（190～340ms）、吃子淡出、悔棋返回动画、棋谱复盘逐手动画
- 选黑时棋盘自动翻转，点击位置与走子动画都正确映射
- 参考传统象棋游戏大厅的单机主页，提供 5/10/15 分钟对局、红黑双方倒计时
- 四关原创“一步将军”入门训练（车、炮、马、兵），可提示、重试及进入下一关

## 竖屏 UI 与场景动画

- 首页使用 `app/src/main/assets/ui_tearoom_x300pro/backgrounds/tea_room_lobby_background_1260x2800.png`，基准比例 9:20；其他比例居中裁切，不拉伸。
- 对局和残局使用 `app/src/main/assets/ui/backgrounds/scene_background_1080x1920.png` 夜景庭院；棋盘使用 `ui_tearoom_x300pro/boards/board_warm_stone_1024x1190.png`，棋子使用相同素材包中的红黑独立透明 PNG。
- `SceneBackgroundView` 是独立于触摸控件的 Canvas 动效层，有低频灯火光晕、漂浮光点、飘叶及轻微背景平移；进入后台或离开场景会取消计时器，目标最大约 30fps。
- 对局页提供“动效：开/关”，自动保存设置；关闭动效时背景保持静态。棋子移动、吃子淡出、撤销和复盘由 `BoardView` 单独处理。
- 本项目的动效是原创实现，不包含 JJ 游戏的动画或美术文件。

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

## 当前定位

当前是单机版的 JJ 风格象棋游戏原型，不含 JJ 原版素材、账号、联机匹配或线上排行榜。内置搜索 AI 不等同于 Pikafish。移动动画已通过 Android APK 构建，但仍建议在手机上验收帧率和触控手感。

## 下一步

建议接入 [Pikafish](https://github.com/official-pikafish/Pikafish) 的 CPU + NNUE 象棋引擎。此版本尚未集成 Pikafish、GPU 或 NPU。
Pikafish 源码使用 GPLv3，官方 NNUE 权重另有非商业许可限制。

## 已知限制

- 内置搜索 AI 较弱，深度有限。
- 尚未实现赛事级长将、长捉和复杂重复裁决。
- 当前棋谱是内部坐标而非传统纵线记谱。
- 核心 Java 逻辑已测试；Android APK 构建需要 GitHub Actions 或本机 SDK 验证。
