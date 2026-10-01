# 更新日志 / Changelog

本项目的所有重要变更都记录在此文件。
All notable changes to this project are documented in this file.

## [1.0.0] - 2026-10-02

### English

**Fixed**

- The scrollbar thumb never moved while scrolling. `scrollbarThumb` clamped the current row
  against the maximum scroll amount instead of the total row count, so whenever the list fit
  within two screens (the common case) the upper bound collapsed to zero and the thumb stayed
  pinned to the top of the track. The content rows were scrolling all along; only the thumb
  was stuck. The thumb now maps `scrollRow / maxScroll` onto the track.

**Added**

- Witty empty-state hints in the grid, chosen by why it is empty:
  - bundles present but every bundle is empty: "Your bundles are 100% air" /
    "收纳袋里装的都是空气"
  - bundles have contents but the selected category (or search query) matches nothing:
    "The bundles checked. Twice." / "袋子们翻了个遍，啥也没找着"
  - no bundles at all keeps the existing "No bundle in inventory" / "背包里没有收纳袋"
- A code-generated mod icon (`assets/bundle-stash/icon.png`, referenced through the `icon`
  field in `fabric.mod.json`): vanilla textures composed by `.tools/icon/IconGen.java` —
  a 3x3 grid of `container/slot` holding item icons, an enlarged bundle at the center with a
  drop shadow, the container-style frame/bevel and the sidebar accent bar.

### 中文

**修复**

- 滚动时滚动条滑块纹丝不动。`scrollbarThumb` 把"当前滚动行"夹在了"最大滚动量"而不是
  "总行数"上，只要列表不超过两屏（最常见情况）上界就恒为 0，滑块永远停在轨道顶端。
  内容其实一直在滚，卡住的只有滑块。现在按 `scrollRow / maxScroll` 正确映射到轨道位置。

**新增**

- 网格空态的风趣提示，按"为什么空"分别取文案：
  - 有收纳袋但全是空的：「收纳袋里装的都是空气」 / "Your bundles are 100% air"
  - 有内容但当前分类或搜索没命中：「袋子们翻了个遍，啥也没找着」 /
    "The bundles checked. Twice."
  - 背包里没有收纳袋：沿用原文案
- 代码生成的模组图标（`assets/bundle-stash/icon.png`，由 `fabric.mod.json` 的 `icon`
  字段引用）：`.tools/icon/IconGen.java` 用原版贴图合成——3×3 的 `container/slot` 网格
  加物品图标、正中放大的收纳袋（带投影）、原版容器描边与内斜面，以及侧栏强调色横条。

## [0.2.1-beta] - 2026-10-02

### English

**Fixed**

- The settings button did nothing when clicked. Minecraft 26.3 moved mouse input to SDL
  numbering (1 = left, 2 = middle, 3 = right) while the sidebar still checked GLFW numbers,
  so the "left click" check (`button == 0`) could never match. Corner buttons, category tabs
  and popup controls now go through `McAccess.isLeftClick`, matching vanilla's own checks
  (`AbstractContainerScreen#getContainerClickButton` maps 1 and 3 the same way).
- The panel toggle was written for right click but effectively reacted to left click; all
  sidebar buttons now consistently respond to left clicks only.

**GUI**

- Every button now uses the vanilla nine-sliced `widget/button` (with
  `widget/button_highlighted` for hover/selection) instead of the 18x18 `container/slot`
  texture stretched to 20x20, 22x18, 14x14 and 12x12. Buttons stay crisp at any size and
  gain vanilla hover feedback; grid cells keep the slot texture at its native 18x18.
- Category bar height changed from 18 to 20 px, the native height of vanilla buttons;
  icon and label paddings are unchanged.
- Settings popup: the +/- controls grew from 14x14 to 18x18 on proper button textures, and
  the hand-drawn "X" close button was replaced by the native 14x14 `widget/cross_button`
  with its highlighted hover variant.
- The settings button stays highlighted while its popup is open, and the panel toggle gained
  a hover tooltip.

### 中文

**修复**

- 设置按钮点击无效。26.3 起鼠标输入改用 SDL 编号（1 = 左键、2 = 中键、3 = 右键），
  而侧栏仍按 GLFW 编号判断，"左键 = 0"永远不成立。角落按钮、分类按钮与弹窗控件现在统一
  走 `McAccess.isLeftClick`，与 vanilla 的判断方式一致（`AbstractContainerScreen`
  的 `getContainerClickButton` 同样按 1/3 处理）。
- 开关按钮原代码本意是右键触发，实际却响应了左键；现在侧栏所有按钮统一只响应左键。

**界面**

- 所有按钮改用原版九宫格贴图 `widget/button`（悬停/选中用 `widget/button_highlighted`），
  不再把 18x18 的 `container/slot` 分别拉伸成 20x20、22x18、14x14、12x12——任意尺寸都
  清晰锐利，并获得原版悬停反馈；网格格子仍以原生 18x18 使用槽位贴图。
- 分类条高度 18 → 20（原版按钮的原生高度），图标与文字留白不变。
- 设置弹窗：加减控件 14x14 → 18x18 并使用真正的按钮贴图；手写的 "X" 关闭按钮换成原生
  14x14 的 `widget/cross_button`（带悬停高亮变体）。
- 弹窗打开时设置按钮保持高亮态；开关按钮补充悬停提示。

## [0.2.0-beta] - 2026-10-02

### English

**Performance**

- Inventory snapshots are now gated by a new zero-allocation fingerprint of the menu slot
  mapping plus bundle contents: if nothing changed, both the snapshot rebuild and the view
  re-filter/re-sort are skipped entirely (previously both ran every 100 ms unconditionally).
- Item display names are no longer resolved while building the snapshot. Translation lookups
  only happen for entries that actually take part in search scoring.
- Registry ids and classification traits are cached per item, and category classification
  results are memoized by `(item id, traits)`.
- Container slot ids are resolved through a one-time index per snapshot instead of scanning
  every menu slot once per inventory slot.
- Only the selected category label is measured per frame; previously all eight labels were
  measured and translated every frame.
- Hover detection in the grid is merged into the drawing loop, and the category button
  rectangles are no longer copied on every frame.

**Fixed**

- Panel row fitting mixed physical pixel height with GUI-scaled coordinates, which could push
  the panel off-screen on small windows or large GUI scales. It now uses the GUI-scaled
  window height.
- Classification rules referenced the `minecraft:mushrooms`, `minecraft:flowers` and
  `minecraft:crops` tags, but the platform layer never reported them, so those items never
  landed in the Plants category. Item traits now report the item's full tag set.
- A click sent while the world was unloading could throw a null pointer in `sendClick`; the
  failure would permanently disable the sidebar stage through the error guard. The null game
  mode is now handled gracefully.

**Changed**

- Item traits are built from the item's actual tag list instead of a hand-maintained list of
  17 tags, so new versions and modded items classify correctly without code changes.
- Panel colors and the vanilla-style container background are shared through a single palette
  and a `PanelGraphics` default method; the renderer and the settings popup previously
  duplicated both.
- Search scoring lowercases the query once per search instead of once per entry, and
  `ItemMatcher` is now a record that owns the normalized query.
- `BundlePanelController.render` no longer takes an unused `screen` parameter, which also
  removed the casts in both container mixins.

**Removed**

- About 25 unused members were deleted: dead accessors and fields across `PanelLayout`,
  `PanelState`, `BundleModel`, `BundleGroup`, `ItemTraits`, `ItemCategory`, `SearchField`,
  `PanelGraphics`, `PanelMetrics`, `Rect` and `McAccess`, plus the renderer's unused sprite
  constant.

**Build**

- Gradle: Java compilation now relies on `options.release` alone (redundant source/target
  compatibility removed), `-Xlint:deprecation` and `-Xlint:unchecked` are enabled (zero
  warnings), and the pinyin4j version moved to `gradle.properties`.
- The jar's `META-INF` exclusion now applies only to the embedded dependency instead of the
  whole archive, so mod resources are no longer at risk of being stripped.
- Added `README.md` and `.editorconfig`.
- The Gradle configuration cache is intentionally left disabled: storing it forces
  `:test` runtime classpath resolution, which fails for offline builds.

### 中文

**性能**

- 新增零分配的背包指纹（菜单槽位映射 + 收纳袋内容的摘要）。指纹没变时，整份跳过快照
  重建与视图的重过滤/重排序；此前两者每 100ms 都会无条件执行。
- 快照阶段不再解析物品显示名，只有真正参与搜索打分的条目才会走一次翻译。
- 物品注册 id 与分类特征按物品缓存，分类结果按 `(物品 id, 特征)` 记忆化。
- 容器槽位号改为每个快照建一次索引，不再"每个背包格扫一遍全部槽位"。
- 每帧只量选中分类的一个字符串宽度；此前每帧要量并翻译全部 8 个分类名。
- 网格悬停检测并入绘制循环，分类按钮矩形不再每帧复制一份。

**修复**

- 面板行数计算把物理像素高度和 GUI 缩放坐标混在一起用，小窗口或大 GUI 缩放下面板会
  画出屏幕外；现改用 GUI 缩放后的窗口高度。
- 分类规则声明了 `minecraft:mushrooms`、`minecraft:flowers`、`minecraft:crops` 三个标签，
  但平台层从未上报过，这些物品一直进不了"植物"分类；物品特征现在上报完整标签集。
- 世界卸载瞬间发点击包可能在 `sendClick` 抛出空指针，并因错误兜底把整个侧栏永久停用；
  现对空 game mode 做了防护。

**变更**

- 物品特征改从物品的真实标签列表生成，替换掉手维护的 17 条标签清单——版本更新或模组
  物品无需改代码即可正确分类。
- 面板配色与原版风格底板绘制统一到一份调色板和 `PanelGraphics` 默认方法中，渲染器与
  设置弹窗原先各写一份。
- 搜索打分把查询串小写化从"每条一次"降为"每次搜索一次"，`ItemMatcher` 改为持有规范化
  查询的 record。
- `BundlePanelController.render` 移除无用的 `screen` 参数，两个容器 mixin 里的强制类型
  转换随之消失。

**移除**

- 清理约 25 处无引用成员：`PanelLayout`、`PanelState`、`BundleModel`、`BundleGroup`、
  `ItemTraits`、`ItemCategory`、`SearchField`、`PanelGraphics`、`PanelMetrics`、`Rect`、
  `McAccess` 中的死访问器与死字段，以及渲染器里未使用的贴图常量。

**构建与开发**

- Gradle：Java 编译只保留 `options.release`（删掉冗余的 source/target 兼容性设置），启用
  `-Xlint:deprecation` 与 `-Xlint:unchecked`（当前 0 警告），pinyin4j 版本移入
  `gradle.properties`。
- jar 的 `META-INF` 排除只作用于嵌入依赖，不再作用于整个归档，避免误伤模组自身资源。
- 新增 `README.md` 与 `.editorconfig`。
- 有意不启用 Gradle configuration cache：存储缓存会强制解析 `:test` 的运行时类路径，
  离线构建会因此失败。
