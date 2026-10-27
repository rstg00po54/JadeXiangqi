# 玉弈象棋：国风场景 UI 素材包

与前一张国风游戏效果图对应，已经按用途拆成独立 PNG。素材可以直接复制到 Android `app/src/main/assets/ui/`（建议保留目录结构），或者 `res/drawable-nodpi`（文件名合法）。

## 资源说明

- `backgrounds/scene_background_1080x1920.png`：游戏场景背景，盖在棋盘后面。
- `backgrounds/lobby_background_1080x1920.png`：大厅背景，卡片应由原生 UI 叠加。
- `boards/board_blank_1024x1160.png`：石板棋盘无网格底图，适合当前 BoardView 保留精确 Canvas 网格的方案。
- `boards/board_grid_1024x1160.png`：已经精确绘制 9 列、10 行交点、河界留白与九宫斜线的完整棋盘背景。使用它时应关闭代码绘制网格，以免重复。
- `pieces/piece_blank_256.png`：无字木棋子底片，推荐在 Canvas 根据棋子编码精确叠加中文。
- `pieces/piece_red_*.png`、`piece_black_*.png`：14 个固定文字的棋子 PNG（帅/仕/相/马/车/炮/兵；将/士/象/马/车/炮/卒）。
- `pieces/selection_ring_256.png`、`legal_target_256.png`：棋子选中高亮、合法落点提示。
- `buttons/button_*.png`：透明背景的游戏功能按钮（分析、提示、悔棋、复盘、残局、设置、菜单、表情）。
- `decorations/`：头像框与圆形时钟部件。
- `preview/asset_preview.png`：素材总览。

## 布局和规则

- 棋盘为 **9 条竖线（9 列落点）× 10 条横线（10 行落点）**，**中间楚河汉界横跨一行间距**。
- 标准开局：红棋位于棋盘下方，黑棋位于上方；执黑视角翻转应在代码中处理。
- 不建议把 AI 效果图直接裁成棋盘作为游戏纹理，因为它包含棋子、动态特效及可能有不准确的棋盘线。
- 这个资源包只提供美术素材，不会改变已有 `JadeXiangqi` 仓库代码；原界面仍需接入背景与棋子 Bitmap 绘制。

## 分辨率

- 场景 1080×1920；棋盘 1024×1160；棋子和按钮 256×256。图片有独立透明通道，适合 Android Bitmap 缩放。
