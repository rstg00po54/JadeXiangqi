# JadeXiangqi 首页素材包

这包使用已确认的**高清茶室背景**；其他素材分别保存为透明 PNG，方便 Android 组合使用。没有将 UI 文字烘焙进按钮图。

## 目录

- `backgrounds/lobby_background.png`：已经确认的竖屏背景（813×1934）。
- `panels/panel_title.png`：顶部「玉弈象棋」标题底板（1170×285）。
- `panels/panel_today.png`：「今日来一局」绿底金边介绍板（1145×347）。
- `buttons/time_5min.png` / `time_10min.png` / `time_15min.png`：三个时限按钮（356×160）。
- `buttons/time_selected.png`：时限按钮选中态（356×160）。
- `buttons/button_mode_red.png` / `button_mode_black.png` / `button_mode_ai.png`：三种金色对弈入口按钮（1140×216）。
- `navigation/nav_background.png`：无图标导航底板（1260×246）。
- `navigation/nav_selected_background.png`：底部导航选中背景（238×225）。
- `icons/nav_{home,match,record,settings}_{normal,selected}.png`：4×2 个独立透明图标（128×128）。
- `icons/icon_{scroll,pawn,rook}.png`：额外图标。
- `preview/home_preview_1260x2800.png`：组装预览，不要当作实际 UI 素材使用。
- `asset_manifest.json`：1260×2800 示例画布的参考坐标。

## Android 集成建议

请分别用 ImageView 放置底板 / 按钮，用 TextView 绘制中文；按钮点击和状态切换由 Android 程序控制。底栏图标单独放置，不要直接用预览图做交互页面。适配不同屏幕时，请依父容器缩放坐标（或使用 ConstraintLayout / 比例布局），预览中的位置是参考值，**不要写死全机型像素坐标**。

说明：装饰面板和金色卡片依据截图风格进行了独立重制，不是原 App 的官方素材；和原截图不会像素级一致。
