package bundlestash.core;

import java.util.Set;

/**
 * 物品的分类特征。由平台层从游戏中采集后交给核心层，核心层只做纯逻辑判断，
 * 因此同一套分类规则可以跨版本复用，也能顺带支持部分模组物品。
 *
 * @param tags      该物品所属的全部物品标签 id（如 {@code minecraft:swords}），平台层按物品缓存
 * @param block     是否是方块物品
 * @param fullBlock 是否是完整不透明方块
 * @param edible    是否可食用
 */
public record ItemTraits(
        boolean block,
        boolean fullBlock,
        boolean edible,
        Set<String> tags
) {
    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }
}
