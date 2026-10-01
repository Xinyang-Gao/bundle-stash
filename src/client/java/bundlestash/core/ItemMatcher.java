package bundlestash.core;

import java.util.Locale;

/**
 * 侧栏搜索：对显示名、物品 id、以及可选的拼音转写做匹配与打分。
 * <p>
 * 旧版的"搜索"只是把命中的项排到前面，未命中的项依然会显示；这里改成真正的过滤，
 * 同时用打分保证最相关的结果排在最前。
 */
public final class ItemMatcher {

    /** 未命中。 */
    public static final int NO_MATCH = -1;

    private static final int SCORE_NAME_PREFIX = 100;
    private static final int SCORE_NAME = 80;
    private static final int SCORE_PINYIN_PREFIX = 60;
    private static final int SCORE_PINYIN = 50;
    private static final int SCORE_INITIALS = 40;
    private static final int SCORE_ID = 20;

    private ItemMatcher() {
    }

    /**
     * @param query     已 {@code trim()} 的查询串，为空表示全部命中
     * @param romanizer 拼音转换实现，可为 {@code null}
     * @return 命中分数，越大越靠前；{@link #NO_MATCH} 表示未命中
     */
    public static <S> int score(String query, BundleEntry<S> entry, Romanizer romanizer) {
        if (query.isEmpty()) return 0;
        String lowered = query.toLowerCase(Locale.ROOT);

        String name = entry.displayName().toLowerCase(Locale.ROOT);
        if (name.startsWith(lowered)) return SCORE_NAME_PREFIX;
        if (name.contains(lowered)) return SCORE_NAME;

        String id = entry.id().toLowerCase(Locale.ROOT);
        if (id.contains(lowered)) return SCORE_ID;

        Romanizer.Result latin = romanizer == null ? null : romanizer.romanize(entry.displayName());
        if (latin != null && !latin.isEmpty()) {
            if (latin.initials().startsWith(lowered)) return SCORE_INITIALS;
            String full = latin.full().toLowerCase(Locale.ROOT);
            if (full.startsWith(lowered)) return SCORE_PINYIN_PREFIX;
            if (full.contains(lowered)) return SCORE_PINYIN;
        }
        return NO_MATCH;
    }

    /** 该物品所属分类是否通过筛选。 */
    public static <S> boolean inCategory(ItemCategory category, BundleEntry<S> entry) {
        if (category == ItemCategory.ALL) return true;
        return category == ItemCategory.classify(entry.id(), entry.traits());
    }
}
