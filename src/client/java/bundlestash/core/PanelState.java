package bundlestash.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 侧栏的可变状态：显示与否、当前分类、搜索词、滚动位置、悬停项。
 * 与 {@link BundleModel}（只读快照）分离，避免在绘制过程中直接修改数据来源。
 */
public final class PanelState {

    private final Romanizer romanizer;

    private boolean visible = true;
    private ItemCategory category = ItemCategory.ALL;
    private String query = "";
    private int scrollRow;
    private int hoveredIndex = -1;

    public PanelState(Romanizer romanizer) {
        this.romanizer = romanizer;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean toggleVisible() {
        this.visible = !this.visible;
        return this.visible;
    }

    public ItemCategory category() {
        return category;
    }

    public void setCategory(ItemCategory category) {
        if (this.category != category) {
            this.category = category;
            this.scrollRow = 0;
        }
    }

    public String query() {
        return query;
    }

    public void setQuery(String query) {
        String normalized = query == null ? "" : query.trim();
        if (!this.query.equals(normalized)) {
            this.query = normalized;
            this.scrollRow = 0;
        }
    }

    public void clearQuery() {
        setQuery("");
    }

    public int scrollRow() {
        return scrollRow;
    }

    public void scrollBy(int rows, int totalRows, PanelMetrics metrics) {
        scrollRow = clamp(scrollRow + rows, totalRows, metrics);
    }

    public void scrollToTop() {
        scrollRow = 0;
    }

    public void clampScroll(int totalRows, PanelMetrics metrics) {
        scrollRow = clamp(scrollRow, totalRows, metrics);
    }

    public void setHoveredIndex(int index) {
        this.hoveredIndex = index;
    }

    public int hoveredIndex() {
        return hoveredIndex;
    }

    private static int clamp(int value, int totalRows, PanelMetrics metrics) {
        int max = Math.max(0, totalRows - metrics.rows());
        return Math.max(0, Math.min(value, max));
    }

    /**
     * 根据当前分类与搜索词，从模型里挑出要在网格里展示的条目并按相关度排序。
     * 搜索词为空时保持原始顺序（先按收纳袋分组），和 vanilla 收纳袋的展示顺序一致。
     */
    private record Scored<S>(BundleEntry<S> entry, int score) {
    }

    public <S> List<BundleEntry<S>> computeView(BundleModel<S> model) {
        List<BundleEntry<S>> source = model.flattened();
        if (query.isEmpty()) {
            List<BundleEntry<S>> filtered = new ArrayList<>(source.size());
            for (BundleEntry<S> entry : source) {
                if (ItemMatcher.inCategory(category, entry)) filtered.add(entry);
            }
            return filtered;
        }

        List<Scored<S>> scored = new ArrayList<>(source.size());
        for (BundleEntry<S> entry : source) {
            if (!ItemMatcher.inCategory(category, entry)) continue;
            int score = ItemMatcher.score(query, entry, romanizer);
            if (score != ItemMatcher.NO_MATCH) scored.add(new Scored<>(entry, score));
        }
        // List#sort 是稳定排序，同分时保留原本的收纳袋顺序
        scored.sort((first, second) -> Integer.compare(second.score(), first.score()));
        List<BundleEntry<S>> result = new ArrayList<>(scored.size());
        for (Scored<S> item : scored) result.add(item.entry());
        return result;
    }
}
