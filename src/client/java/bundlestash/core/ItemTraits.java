package bundlestash.core;

import java.util.Set;

/**
 * 物品的分类特征。由平台层从游戏中采集后交给核心层，核心层只做纯逻辑判断，
 * 因此同一套分类规则可以跨版本复用，也能顺带支持部分模组物品。
 *
 * @param tags 该物品所属的物品标签 id（如 {@code minecraft:swords}）
 */
public record ItemTraits(
        boolean block,
        boolean fullBlock,
        boolean edible,
        boolean stackable,
        Set<String> tags
) {
    public static final ItemTraits NONE = new ItemTraits(false, false, false, true, Set.of());

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
}
