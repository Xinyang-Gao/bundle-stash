# 更新日志 / Changelog

本项目的所有重要变更都记录在此文件。
All notable changes to this project are documented in this file.

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
