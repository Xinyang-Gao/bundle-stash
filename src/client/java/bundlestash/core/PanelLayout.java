package bundlestash.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 侧栏的完整几何布局。
 * <p>
 * 旧版在渲染处与点击判定处各算一遍坐标（还夹杂 {@code +16}、{@code -10}、{@code +24}
 * 之类的魔数），结果就是"看着悬停在第 3 格、实际点到第 5 格"。这里改为一次算好所有矩形，
 * 渲染、点击、滚动、拖拽全部复用同一批矩形。
 */
public final class PanelLayout {

    private final Rect screen;
    private final Side side;
    private final PanelMetrics metrics;
    private final int categorySize;

    private final Rect panel;
    private final Rect categoryBar;
    private final List<Rect> categoryButtons;
    private final Rect searchBar;
    private final Rect scrollbar;
    private final Rect grid;
    private final Rect footer;
    private final Rect toggleButton;

    public enum Side { LEFT, RIGHT }

    /**
     * @param screen        容器屏幕的绘制区域
     * @param side          面板位于屏幕哪一侧
     * @param metrics       尺寸参数
     * @param categoryCount 分类数量
     */
    public PanelLayout(Rect screen, Side side, PanelMetrics metrics, int categoryCount) {
        this.screen = screen;
        this.side = side;
        this.metrics = metrics;

        int gridW = metrics.gridWidth();
        int gridH = metrics.gridHeight();
        int searchBlock = metrics.searchHeight() > 0 ? metrics.searchHeight() + metrics.gap() : 0;

        this.categorySize = categorySizeFor(categoryCount, searchBlock + gridH, metrics);

        int contentTop = metrics.padding() + searchBlock;
        int panelWidth = metrics.padding() + categorySize + metrics.gap()
                + metrics.scrollbarWidth() + metrics.gap() + gridW + metrics.padding();
        // 分类竖条与网格取较高者，保证分类按钮永远不会被裁掉
        int contentHeight = Math.max(gridH, categorySize * categoryCount);
        int panelHeight = contentTop + contentHeight + metrics.gap() + metrics.footerHeight() + metrics.padding();

        int panelX = side == Side.LEFT ? screen.x() - metrics.screenGap() - panelWidth
                : screen.right() + metrics.screenGap();
        this.panel = new Rect(panelX, screen.y(), panelWidth, panelHeight);

        int barX = panelX + metrics.padding();
        int barY = panel.y() + metrics.padding();
        this.categoryBar = new Rect(barX, barY, categorySize, categorySize * categoryCount);
        this.categoryButtons = new ArrayList<>(categoryCount);
        for (int i = 0; i < categoryCount; i++) {
            this.categoryButtons.add(new Rect(barX, barY + i * categorySize, categorySize, categorySize));
        }

        int contentX = barX + categorySize + metrics.gap();
        int contentWidth = panelWidth - metrics.padding() - categorySize - metrics.gap() - metrics.padding();
        this.searchBar = new Rect(contentX, panel.y() + metrics.padding(), contentWidth, metrics.searchHeight());

        int contentY = panel.y() + contentTop;
        this.scrollbar = new Rect(contentX, contentY, metrics.scrollbarWidth(), gridH);
        this.grid = new Rect(contentX + metrics.scrollbarWidth() + metrics.gap(), contentY, gridW, gridH);
        this.footer = new Rect(grid.x(), panel.y() + contentTop + contentHeight + metrics.gap(),
                gridW, metrics.footerHeight());
        this.toggleButton = new Rect(screen.right() + metrics.screenGap(), screen.y() + 2, 20, 20);
    }

    /**
     * 空间不足时自动缩小分类按钮，保证所有分类始终可见。
     * 下限 18：条目图标本身是 16px，再小就会互相压住。
     */
    private static int categorySizeFor(int count, int availableHeight, PanelMetrics metrics) {
        if (count <= 0) return metrics.categorySize();
        int usable = Math.max(18, availableHeight - metrics.padding() * 2);
        return Math.max(18, Math.min(metrics.categorySize(), usable / count));
    }

    public Rect panel() { return panel; }
    public Rect categoryBar() { return categoryBar; }
    public Rect searchBar() { return searchBar; }
    public Rect scrollbar() { return scrollbar; }
    public Rect grid() { return grid; }
    public Rect footer() { return footer; }
    public Rect toggleButton() { return toggleButton; }
    public int categorySize() { return categorySize; }
    public PanelMetrics metrics() { return metrics; }
    public Side side() { return side; }

    public Rect categoryButton(int index) {
        return index < 0 || index >= categoryButtons.size() ? null : categoryButtons.get(index);
    }

    public List<Rect> categoryButtons() {
        return List.copyOf(categoryButtons);
    }

    /** 某个格子（行、列）的矩形。 */
    public Rect cellRect(int row, int col) {
        int step = metrics.cell() + metrics.gap();
        return new Rect(grid.x() + col * step, grid.y() + row * step, metrics.cell(), metrics.cell());
    }

    /** 第 {@code flatIndex} 个格子（考虑滚动偏移后）在屏幕上的矩形。 */
    public Rect cellRectByIndex(int flatIndex, int scrollRow) {
        int row = flatIndex / metrics.columns() - scrollRow;
        int col = flatIndex % metrics.columns();
        return cellRect(row, col);
    }

    /**
     * 命中检测：返回鼠标所在格子的扁平下标。
     *
     * @return 命中的扁平下标；不在网格内返回 {@code -1}
     */
    public int cellIndexAt(double mouseX, double mouseY, int scrollRow) {
        if (!grid.contains(mouseX, mouseY)) return -1;
        int step = metrics.cell() + metrics.gap();
        int col = (int) ((mouseX - grid.x()) / step);
        int row = (int) ((mouseY - grid.y()) / step);
        if (col < 0 || col >= metrics.columns() || row < 0 || row >= metrics.rows()) return -1;
        if (!cellRect(row, col).contains(mouseX, mouseY)) return -1;
        return (scrollRow + row) * metrics.columns() + col;
    }

    /** 命中检测：返回鼠标所在的分类按钮下标，未命中返回 {@code -1}。 */
    public int categoryIndexAt(double mouseX, double mouseY) {
        for (int i = 0; i < categoryButtons.size(); i++) {
            if (categoryButtons.get(i).contains(mouseX, mouseY)) return i;
        }
        return -1;
    }

    public boolean isInsideGrid(double mouseX, double mouseY) {
        return grid.contains(mouseX, mouseY);
    }

    public boolean isInsidePanel(double mouseX, double mouseY) {
        return panel.contains(mouseX, mouseY);
    }

    /** 把滚动行号夹到合法区间。 */
    public int clampScroll(int scrollRow, int totalRows) {
        return Math.max(0, Math.min(scrollRow, Math.max(0, totalRows - metrics.rows())));
    }

    /** 滚动条滑块矩形；不需要滚动时返回 {@code null}。 */
    public Rect scrollbarThumb(int scrollRow, int totalRows) {
        int maxScroll = Math.max(0, totalRows - metrics.rows());
        if (maxScroll <= 0) return null;
        int trackHeight = scrollbar.height();
        int thumb = Math.max(12, trackHeight * metrics.rows() / totalRows);
        int y = scrollbar.y() + (trackHeight - thumb) * clampScroll(scrollRow, maxScroll) / maxScroll;
        return new Rect(scrollbar.x(), y, scrollbar.width(), thumb);
    }

    /** 面板水平方向是否完整落在屏幕内（窗口过窄时应提示或收起）。 */
    public boolean fitsHorizontally(int windowWidth) {
        return panel.x() >= 0 && panel.right() <= windowWidth;
    }
}
