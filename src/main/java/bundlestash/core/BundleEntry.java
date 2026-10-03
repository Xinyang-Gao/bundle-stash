package bundlestash.core;

/**
 * 侧栏里的一格：某个收纳袋内容中的一项。
 * <p>
 * 名称不在快照里保存：显示名只在搜索打分时才需要，而"取显示名"要走一次完整的
 * 翻译解析（每 100ms 对每个条目做一遍太浪费），因此由调用方按需解析后传给
 * {@link ItemMatcher#score}。
 *
 * @param stack        原始物品对象，由平台层提供（Minecraft 中为 {@code ItemStack}）
 * @param id           注册名，如 {@code minecraft:diamond}
 * @param groupIndex   所属收纳袋在 {@link BundleModel} 中的下标
 * @param contentIndex 这一项在收纳袋内容中的下标，用于向服务端发送"选择第几项"
 * @param count        数量
 * @param weightUnits  重量（1/64 单位）
 * @param traits       用于分类的特征
 */
public record BundleEntry<S>(
        S stack,
        String id,
        int groupIndex,
        int contentIndex,
        int count,
        int weightUnits,
        ItemTraits traits
) {
}
