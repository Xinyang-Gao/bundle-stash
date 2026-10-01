# Bundle Stash（探囊取物）

在容器界面（箱子、背包、熔炉……）侧边显示玩家背包里**所有收纳袋的内容**，
带分类、搜索与一键存取的 Fabric 客户端模组。

- 分类浏览：全部 / 方块 / 植物 / 食物 / 工具与装备 / 矿物 / 杂物，依据物品标签与属性自动归类，版本更新不会让分类表过期
- 搜索：物品名、注册 id、拼音（含首字母，pinyin4j 提供），按相关度排序
- 一键存取：点击取出到鼠标、Shift+点击取出到空格子、点击面板把鼠标上的物品存入
- 空格 + 点击/拖拽槽位：整堆收纳
- 悬停条目时在背包里高亮它所属的收纳袋
- 面板可换边（合成书打开时自动让开）、可调行数列数、可整体隐藏
- 界面视觉完全使用原版贴图与配色

仅客户端（`"environment": "client"`），不依赖 Fabric API。

## 构建

要求 [JDK 25](https://adoptium.net/)（或更高）；Gradle 由 wrapper 自带，无需安装。

```bash
./gradlew build        # Windows: gradlew.bat build
./gradlew runClient    # 启动开发客户端试玩
```

产物在 `build/libs/`：

- `bundle-stash-<版本>.jar` —— 发布用模组 jar（已内嵌 pinyin4j）
- `bundle-stash-<版本>-sources.jar` —— 源码包

## 配置

首次运行后生成 `config/bundle-stash.json`，缺字段自动补默认值，改版本不会让旧配置失效。
也可通过面板角落的设置按钮直接改行数/列数。

## 代码结构

| 包 | 职责 |
| --- | --- |
| `bundlestash.core` | 纯逻辑：数据快照、布局几何、分类、搜索打分。**不 import 任何 Minecraft 类型** |
| `bundlestash.gui` | 绘制与交互编排：控制器、渲染器、设置弹窗。只依赖 `core` 与绘制接口 |
| `bundlestash.platform` | Minecraft 适配层：采集收纳袋快照、绘制原语、搜索框、发包 |
| `bundlestash.mixin` | 容器界面/键盘事件的注入点与 accessor |
| `bundlestash.config` | JSON 配置的读写与纠错 |

换 Minecraft 版本时，理论上只需改 `platform` 与 `mixin` 两层；
`core`/`gui` 的几何与规则可以原样复用。

性能上的两条约定（改动时请保持）：

1. 快照每 100ms 最多重建一次，且先过 `McAccess.snapshotFingerprint()`——背包没动就整份跳过；
2. 显示名只在真正参与搜索打分时才解析（`PanelState.computeView` 的 `nameOf` 参数），
   快照里不保存，避免每 100ms 对每个条目跑一遍翻译。

## 更新日志

见 [CHANGELOG.md](CHANGELOG.md)（中英双语）。

## 许可

[MIT](LICENSE)
