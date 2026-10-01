package bundlestash.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 侧栏的完整几何布局。
 * <p>
 * 旧版在渲染处与点击判定处各算一遍坐标（还夹杂 {@code +16}、{@code -10}、{@code +24}
 * 之类的魔数），结果就是"看着悬停在第 3 格、实际点到第 5 格"。这里改为一次算好所有矩形，
 * 渲染、点击、滚动、拖拽全部复用同一批矩形。
 * <p>
 * 结构上分三块，互相独立：
 * <ul>
 *   <li>{@link #categoryButtons()} —— 横向排在容器界面<b>上方</b>的分类按钮条</li>
 *   <li>{@link #panel()} —— 贴在容器界面一侧的面板（搜索框 + 网格 + 底部统计）</li>
 *   <li>{@link #toggleButton()} / {@link #settingsButton()} —— 界面角落的开关与设置按钮</li>
 * </ul>
 */
public final class PanelLayout {

    /** 分类按钮里小图标的边长。 */
    public static final int CATEGORY_ICON = 16;
    /** 分类按钮高度（= 原版按钮贴图 {@code widget/button} 的原生高度 20，图标上下各留 2px）。 */
    public static final int CATEGORY_BUTTON_H = 20;
    /** 分类按钮内部的水平留白。 */
    public static final int CATEGORY_PAD = 3;
    /** 分类按钮之间的间距。 */
    public static final int CATEGORY_GAP = 2;
    /** 分类条与容器界面上边缘的间距。 */
    public static final int CATEGORY_BAR_GAP = 4;

    private final PanelMetrics metrics;

    private final Rect panel;
    private final List<Rect> categoryButtons;
    private final List<Rect> categoryIcons;
    private final List<Integer> categoryLabelStarts;
    private final Rect searchBar;
    private final Rect scrollbar;
    private final Rect grid;
    private final Rect footer;
    private final Rect toggleButton;
    private final Rect settingsButton;

    public enum Side { LEFT, RIGHT }

    /**
     * 顶部分类条的布局参数。
     * 分类名的文字宽度由上层（拿得到字体）算好传进来，这一层保持纯几何、不依赖字体 API。
     * 只需要当前选中项的宽度——其余按钮只画小图标，不画文字。
     *
     * @param count              分类数量
     * @param selectedIndex      当前选中的下标（该按钮会展开显示分类名），-1 表示无
     * @param selectedLabelWidth 选中分类名的文字宽度（px）
     */
    public record CategoryTabs(int count, int selectedIndex, int selectedLabelWidth) {
    }

    /**
     * @param screen      容器屏幕的绘制区域
     * @param side        面板位于屏幕哪一侧
     * @param metrics     尺寸参数
     * @param windowWidth 窗口宽度，用于保证分类条不会被挤出屏幕
     * @param tabs        顶部分类条参数
     */
    public PanelLayout(Rect screen, Side side, PanelMetrics metrics, int windowWidth, CategoryTabs tabs) {
        this.metrics = metrics;

        int gridW = metrics.gridWidth();
        int gridH = metrics.gridHeight();
        int searchBlock = metrics.searchHeight() > 0 ? metrics.searchHeight() + metrics.gap() : 0;

        // ---------------------------------------------------------- 顶部横向分类条
        int count = Math.max(0, tabs.count());
        int[] widths = new int[count];
        int totalWidth = 0;
        for (int i = 0; i < count; i++) {
            // 选中的按钮多出一段分类名，其余按钮只显示小图标
            int label = i == tabs.selectedIndex() ? Math.max(0, tabs.selectedLabelWidth()) : 0;
            widths[i] = CATEGORY_PAD + CATEGORY_ICON + CATEGORY_PAD + (label > 0 ? CATEGORY_PAD + label : 0);
            totalWidth += widths[i];
        }
        totalWidth += Math.max(0, count - 1) * CATEGORY_GAP;

        // 相对界面横向居中，并保证整条完整落在窗口内
        int maxX = Math.max(2, windowWidth - totalWidth - 2);
        int barX = Math.max(2, Math.min(screen.x() + (screen.width() - totalWidth) / 2, maxX));
        // 界面上方空间不足时贴到窗口顶部（此时可能与界面重叠，属于极端窄窗口的兜底）
        int barY = Math.max(2, screen.y() - CATEGORY_BAR_GAP - CATEGORY_BUTTON_H);

        List<Rect> buttons = new ArrayList<>(count);
        List<Rect> icons = new ArrayList<>(count);
        List<Integer> labelStarts = new ArrayList<>(count);
        int cursor = barX;
        for (int i = 0; i < count; i++) {
            buttons.add(new Rect(cursor, barY, widths[i], CATEGORY_BUTTON_H));
            icons.add(new Rect(cursor + CATEGORY_PAD,
                    barY + (CATEGORY_BUTTON_H - CATEGORY_ICON) / 2, CATEGORY_ICON, CATEGORY_ICON));
            labelStarts.add(cursor + CATEGORY_PAD + CATEGORY_ICON + CATEGORY_PAD);
            cursor += widths[i] + CATEGORY_GAP;
        }
        this.categoryButtons = List.copyOf(buttons);
        this.categoryIcons = List.copyOf(icons);
        this.categoryLabelStarts = List.copyOf(labelStarts);

        // ---------------------------------------------------------- 侧栏面板
        int contentTop = metrics.padding() + searchBlock;
        int panelWidth = metrics.padding() * 2 + metrics.scrollbarWidth() + metrics.gap() + gridW;
        int panelHeight = contentTop + gridH + metrics.gap() + metrics.footerHeight() + metrics.padding();

        int panelX = side == Side.LEFT ? screen.x() - metrics.screenGap() - panelWidth
                : screen.right() + metrics.screenGap();
        this.panel = new Rect(panelX, screen.y(), panelWidth, panelHeight);

        int contentX = panelX + metrics.padding();
        int contentWidth = panelWidth - metrics.padding() * 2;
        this.searchBar = new Rect(contentX, panel.y() + metrics.padding(), contentWidth, metrics.searchHeight());

        int contentY = panel.y() + contentTop;
        this.scrollbar = new Rect(contentX, contentY, metrics.scrollbarWidth(), gridH);
        this.grid = new Rect(contentX + metrics.scrollbarWidth() + metrics.gap(), contentY, gridW, gridH);
        this.footer = new Rect(grid.x(), panel.y() + contentTop + gridH + metrics.gap(),
                gridW, metrics.footerHeight());

        // ---------------------------------------------------------- 角落按钮
        // 放在面板的**另一侧**，避免面板挪到右边时按钮被压在面板下面
        int buttonX = side == Side.LEFT
                ? screen.right() + metrics.screenGap()
                : Math.max(2, screen.x() - metrics.screenGap() - 20);
        this.toggleButton = new Rect(buttonX, screen.y() + 2, 20, 20);
        this.settingsButton = new Rect(buttonX, screen.y() + 24, 20, 20);
    }

    public Rect panel() { return panel; }
    public Rect searchBar() { return searchBar; }
    public Rect scrollbar() { return scrollbar; }
    public Rect grid() { return grid; }
    public Rect footer() { return footer; }
    public Rect toggleButton() { return toggleButton; }
    public Rect settingsButton() { return settingsButton; }
    public PanelMetrics metrics() { return metrics; }

    /** 顶部分类按钮的矩形列表（构造时已定型，可直接读）。 */
    public List<Rect> categoryButtons() {
        return categoryButtons;
    }

    /** 分类按钮里小图标的矩形（16×16）。 */
    public Rect categoryIconRect(int index) {
        return index < 0 || index >= categoryIcons.size() ? null : categoryIcons.get(index);
    }

    /** 分类名文字的起始 x（紧跟在小图标右侧）。 */
    public int categoryLabelStart(int index) {
        return index < 0 || index >= categoryLabelStarts.size() ? 0 : categoryLabelStarts.get(index);
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

    public boolean isInsidePanel(double mouseX, double mouseY) {
        return panel.contains(mouseX, mouseY);
    }

    /**
     * 滚动条滑块矩形；不需要滚动时返回 {@code null}。
     * <p>
     * 滑块位置 = 轨道行程 ×（当前滚动行 / 最大滚动量）。当前行必须夹到
     * {@code [0, maxScroll]}——历史上这里错把 {@code maxScroll} 当"总行数"传给了
     * 另一个夹取函数，导致总行数不足两屏时上界恒为 0、滑块永远钉在顶端。
     */
    public Rect scrollbarThumb(int scrollRow, int totalRows) {
        int maxScroll = Math.max(0, totalRows - metrics.rows());
        if (maxScroll <= 0) return null;
        int trackHeight = scrollbar.height();
        int thumb = Math.max(12, trackHeight * metrics.rows() / totalRows);
        int clamped = Math.max(0, Math.min(scrollRow, maxScroll));
        int y = scrollbar.y() + (trackHeight - thumb) * clamped / maxScroll;
        return new Rect(scrollbar.x(), y, scrollbar.width(), thumb);
    }

    /** 面板水平方向是否完整落在屏幕内（窗口过窄时应提示或收起）。 */
    public boolean fitsHorizontally(int windowWidth) {
        return panel.x() >= 0 && panel.right() <= windowWidth;
    }
}
