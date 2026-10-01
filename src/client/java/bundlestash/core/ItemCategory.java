package bundlestash.core;

import java.util.List;
import java.util.Set;

/**
 * 侧栏分类。
 * <p>
 * 与旧版最大的不同：不再硬编码上千个物品 id，而是依据 {@link ItemTraits}（标签 + 物品属性）
 * 做正则/关键字匹配。这样新版本加入的物品、以及部分模组物品都能自动落到合适分类里，
 * 也不会因为版本更新出现"分类表过期"的问题。
 */
public enum ItemCategory {
    ALL("all", "minecraft:compass"),
    BLOCKS("blocks", "minecraft:grass_block"),
    PARTIAL_BLOCKS("partial_blocks", "minecraft:oak_slab"),
    PLANTS("plants", "minecraft:poppy"),
    FOOD("food", "minecraft:bread"),
    TOOLS("tools", "minecraft:diamond_pickaxe"),
    MINERALS("minerals", "minecraft:diamond"),
    MISC("misc", "minecraft:leather");

    private static final Set<String> TOOL_TAGS = Set.of(
            "minecraft:swords", "minecraft:axes", "minecraft:pickaxes", "minecraft:shovels",
            "minecraft:hoes", "minecraft:spears", "minecraft:head_armor", "minecraft:chest_armor",
            "minecraft:leg_armor", "minecraft:foot_armor", "minecraft:boats", "minecraft:chest_boats"
    );
    private static final Set<String> FOOD_TAGS = Set.of(
            "minecraft:meat", "minecraft:fishes"
    );
    private static final Set<String> PLANT_TAGS = Set.of(
            "minecraft:saplings", "minecraft:leaves", "minecraft:mushrooms",
            "minecraft:villager_plantable_seeds", "minecraft:flowers", "minecraft:crops"
    );
    private static final List<String> TOOL_KEYWORDS = List.of(
            "_helmet", "_chestplate", "_leggings", "_boots", "_horse_armor", "_sword", "_axe",
            "_pickaxe", "_shovel", "_hoe", "_spear", "_bucket", "_bundle", "shield", "elytra",
            "totem_of_undying", "crossbow", "fishing_rod", "flint_and_steel", "shears", "spyglass",
            "brush", "saddle", "name_tag", "mace", "trident", "arrow", "bundle"
    );
    private static final List<String> MINERAL_KEYWORDS = List.of(
            "_ingot", "_nugget", "_scrap", "_shard", "raw_", "diamond", "emerald", "coal", "charcoal",
            "redstone", "lapis_lazuli", "quartz", "netherite", "ancient_debris", "nether_star",
            "heart_of_the_sea", "heavy_core", "_gem"
    );
    private static final Set<String> MINERAL_BLOCKS = Set.of(
            "coal_block", "iron_block", "gold_block", "diamond_block", "emerald_block",
            "lapis_block", "redstone_block", "netherite_block", "raw_iron_block", "raw_gold_block",
            "raw_copper_block", "copper_block", "amethyst_block"
    );
    private static final List<String> PLANT_KEYWORDS = List.of(
            "sapling", "leaves", "petals", "bush", "vine", "fern", "roots", "kelp", "seagrass",
            "cactus", "sugar_cane", "bamboo", "berries", "mushroom", "flowering", "blossom",
            "pitcher", "dripleaf", "spore_blossom", "sea_pickle", "wheat", "propagule", "tulip",
            "orchid", "daisy", "poppy", "cornflower", "lily", "seeds", "nether_wart", "flowers"
    );

    private final String id;
    private final String iconId;

    ItemCategory(String id, String iconId) {
        this.id = id;
        this.iconId = iconId;
    }

    public String id() {
        return id;
    }

    /** 分类按钮/标题使用的图标物品 id，由平台层解析为物品。 */
    public String iconItemId() {
        return iconId;
    }

    /** 分类名的翻译键，便于多语言。 */
    public String translationKey() {
        return "bundlestash.category." + id;
    }

    public static ItemCategory of(String id) {
        for (ItemCategory category : values()) {
            if (category.id.equals(id)) return category;
        }
        return ALL;
    }

    /**
     * 判定单个物品属于哪个分类。
     *
     * @param itemId 物品的 registry id
     * @param traits 由平台层采集的特征
     */
    public static ItemCategory classify(String itemId, ItemTraits traits) {
        String path = path(itemId);
        for (String tag : TOOL_TAGS) {
            if (traits.hasTag(tag)) return TOOLS;
        }
        if (matches(path, TOOL_KEYWORDS)) return TOOLS;

        for (String tag : FOOD_TAGS) {
            if (traits.hasTag(tag)) return FOOD;
        }
        if (traits.edible()) return FOOD;

        if (matches(path, MINERAL_KEYWORDS) || MINERAL_BLOCKS.contains(path)) return MINERALS;

        for (String tag : PLANT_TAGS) {
            if (traits.hasTag(tag)) return PLANTS;
        }
        if (matches(path, PLANT_KEYWORDS) && !traits.block()) return PLANTS;

        if (traits.block()) return traits.fullBlock() ? BLOCKS : PARTIAL_BLOCKS;
        return MISC;
    }

    private static boolean matches(String path, List<String> keywords) {
        for (String keyword : keywords) {
            if (path.contains(keyword)) return true;
        }
        return false;
    }

    private static String path(String itemId) {
        int colon = itemId.indexOf(':');
        return colon < 0 ? itemId : itemId.substring(colon + 1);
    }
}
