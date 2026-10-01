package bundlestash.gui;

/**
 * 侧栏用到的颜色，全部取自原版的配色，集中一处避免各绘制类各写一份魔数。
 */
public final class PanelPalette {

    /** 原版容器面板底色。 */
    public static final int PANEL_BG = 0xFFC6C6C6;
    /** 面板/弹窗描边。 */
    public static final int BORDER = 0xFF000000;
    /** 浅色面板上用深色文字（和原版容器标题一致）。 */
    public static final int TEXT = 0xFF404040;
    /** 次要文字（占位提示、未悬停的控件字形）。 */
    public static final int TEXT_DIM = 0xFFA0A0A0;
    /** 强调色（开关按钮的"已开启"标记）。 */
    public static final int ACCENT = 0xFF3B6EA5;
    /** 悬停条目时高亮其来源收纳袋的描边色。 */
    public static final int SOURCE_HIGHLIGHT = 0xFFFFC864;

    private PanelPalette() {
    }
}
