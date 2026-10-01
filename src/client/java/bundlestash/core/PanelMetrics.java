package bundlestash.core;

/**
 * 侧栏尺寸参数。渲染与命中检测共用同一份参数，二者不会脱钩。
 * <p>
 * 默认值按原版风格选取：18×18 的密集格子（和箱子/背包的槽位一致），
 * 因此"一屏"能看到的东西更多，超出部分靠滚动查看。
 *
 * @param columns        网格列数
 * @param rows           可见行数
 * @param cell           单格边长
 * @param gap            格间距（原版槽位是紧挨着的，所以默认 0）
 * @param padding        面板内边距
 * @param screenGap      面板与界面边缘的间距
 * @param categorySize   分类按钮边长上限（空间不足时自动缩小）
 * @param scrollbarWidth 滚动条宽度
 * @param searchHeight   搜索框高度，为 0 时不绘制搜索框
 * @param footerHeight   底部统计/进度条高度
 */
public record PanelMetrics(
        int columns,
        int rows,
        int cell,
        int gap,
        int padding,
        int screenGap,
        int categorySize,
        int scrollbarWidth,
        int searchHeight,
        int footerHeight
) {
    public static final PanelMetrics DEFAULT = new PanelMetrics(9, 8, 18, 0, 6, 4, 22, 6, 16, 15);

    public int gridWidth() {
        return columns * cell + (columns - 1) * gap;
    }

    public int gridHeight() {
        return rows * cell + (rows - 1) * gap;
    }

    public PanelMetrics withRows(int newRows) {
        return new PanelMetrics(columns, newRows, cell, gap, padding, screenGap, categorySize, scrollbarWidth,
                searchHeight, footerHeight);
    }

    /** 面板竖向可用空间不足时，自动减少可见行数。 */
    public PanelMetrics withRowsFitting(int availableHeight) {
        int searchBlock = searchHeight > 0 ? searchHeight + gap : 0;
        int usable = availableHeight - padding * 2 - searchBlock - gap - footerHeight;
        if (usable < cell) return withRows(1);
        int maxRows = (usable + gap) / (cell + gap);
        return withRows(Math.max(1, Math.min(rows, maxRows)));
    }
}
