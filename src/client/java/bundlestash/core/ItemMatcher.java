package bundlestash.core;

import java.util.Locale;
import java.util.Objects;

/**
 * 侧栏搜索：对显示名、物品 id、以及可选的拼音转写做匹配与打分。
 * <p>
 * 旧版的"搜索"只是把命中的项排到前面，未命中的项依然会显示；这里改成真正的过滤，
 * 同时用打分保证最相关的结果排在最前。
 * <p>
 * 作为记录持有"已转小写的查询串"，一次搜索里每个条目都打分，
 * 查询串只在构造时规范化一次，而不是每条打分都 {@code toLowerCase} 一遍。
 *
 * @param query     已转小写的查询串，空串表示全部命中
 * @param romanizer 拼音转换实现（非空；未接入拼音时传 {@link Romanizer#NONE}）
 */
public record ItemMatcher(String query, Romanizer romanizer) {

    /** 未命中。 */
    public static final int NO_MATCH = -1;

    private static final int SCORE_NAME_PREFIX = 100;
    private static final int SCORE_NAME = 80;
    private static final int SCORE_PINYIN_PREFIX = 60;
    private static final int SCORE_PINYIN = 50;
    private static final int SCORE_INITIALS = 40;
    private static final int SCORE_ID = 20;

    public ItemMatcher {
        query = query == null ? "" : query.toLowerCase(Locale.ROOT);
        Objects.requireNonNull(romanizer, "romanizer");
    }

    /** 查询串为空：不做过滤，也不用打分。 */
    public boolean isEmpty() {
        return query.isEmpty();
    }

    /**
     * @param displayName 条目的本地化显示名（调用方按需解析后传入）
     * @return 命中分数，越大越靠前；{@link #NO_MATCH} 表示未命中
     */
    public int score(BundleEntry<?> entry, String displayName) {
        if (query.isEmpty()) return 0;

        String name = displayName.toLowerCase(Locale.ROOT);
        if (name.startsWith(query)) return SCORE_NAME_PREFIX;
        if (name.contains(query)) return SCORE_NAME;

        String id = entry.id().toLowerCase(Locale.ROOT);
        if (id.contains(query)) return SCORE_ID;

        Romanizer.Result latin = romanizer.romanize(displayName);
        if (!latin.isEmpty()) {
            if (latin.initials().startsWith(query)) return SCORE_INITIALS;
            String full = latin.full().toLowerCase(Locale.ROOT);
            if (full.startsWith(query)) return SCORE_PINYIN_PREFIX;
            if (full.contains(query)) return SCORE_PINYIN;
        }
        return NO_MATCH;
    }

    /** 该物品所属分类是否通过筛选。 */
    public static <S> boolean inCategory(ItemCategory category, BundleEntry<S> entry) {
        if (category == ItemCategory.ALL) return true;
        return category == ItemCategory.classify(entry.id(), entry.traits());
    }
}
