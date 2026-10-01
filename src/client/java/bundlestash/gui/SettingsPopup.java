package bundlestash.gui;

import bundlestash.config.BundleConfig;
import bundlestash.core.Rect;

/**
 * 角落设置按钮弹出的小面板，目前只暴露"行数 / 列数"两项。
 * <p>
 * 刻意<b>不用</b>真正的 {@code Screen}：那会顺带关掉底下的容器界面，改完还得重新打开。
 * 这里由侧栏自己绘制、自己处理点击，容器一直开着，加减之后立刻能看到网格变化。
 *
 * @param <S> 物品类型
 */
public final class SettingsPopup<S> {

    private static final String SPRITE_CONTROL = "minecraft:container/slot";

    private static final int WIDTH = 132;
    private static final int HEIGHT = 64;
    private static final int CONTROL = 14;
    private static final int VALUE_W = 26;
    private static final int PAD = 8;
    private static final int ROW_1 = 22;
    private static final int ROW_2 = 42;

    /** 与 {@link BundleConfig#sanitize} 的夹取范围保持一致，避免存档后又被改回去。 */
    private static final int ROWS_MIN = 1;
    private static final int ROWS_MAX = 12;
    private static final int COLS_MIN = 2;
    private static final int COLS_MAX = 12;

    private boolean open;

    private Rect bounds = new Rect(0, 0, WIDTH, HEIGHT);
    private Rect closeButton = new Rect(0, 0, 12, 12);
    private Rect rowsMinus = new Rect(0, 0, CONTROL, CONTROL);
    private Rect rowsPlus = new Rect(0, 0, CONTROL, CONTROL);
    private Rect colsMinus = new Rect(0, 0, CONTROL, CONTROL);
    private Rect colsPlus = new Rect(0, 0, CONTROL, CONTROL);
    private int valueX;

    public boolean isOpen() {
        return open;
    }

    public void open() {
        this.open = true;
    }

    public void close() {
        this.open = false;
    }

    public Rect bounds() {
        return bounds;
    }

    public Rect closeButton() {
        return closeButton;
    }

    /** 相对容器界面居中摆放，并算出各控件的矩形。 */
    public void layout(Rect screen) {
        int x = screen.x() + (screen.width() - WIDTH) / 2;
        int y = screen.y() + (screen.height() - HEIGHT) / 2;
        this.bounds = new Rect(x, y, WIDTH, HEIGHT);
        this.closeButton = new Rect(x + WIDTH - PAD - 12, y + 4, 12, 12);

        int plusX = x + WIDTH - PAD - CONTROL;
        int minusX = plusX - 2 - CONTROL;
        this.valueX = minusX - 2 - VALUE_W;

        this.rowsMinus = new Rect(minusX, y + ROW_1, CONTROL, CONTROL);
        this.rowsPlus = new Rect(plusX, y + ROW_1, CONTROL, CONTROL);
        this.colsMinus = new Rect(minusX, y + ROW_2, CONTROL, CONTROL);
        this.colsPlus = new Rect(plusX, y + ROW_2, CONTROL, CONTROL);
    }

    public void render(PanelGraphics<S> graphics, BundleConfig config, double mouseX, double mouseY) {
        graphics.containerBackground(bounds);

        graphics.centeredText(graphics.translate("bundlestash.settings.title"),
                bounds.x() + bounds.width() / 2, bounds.y() + 5, PanelPalette.TEXT);

        drawControl(graphics, closeButton, "X", mouseX, mouseY);

        drawRow(graphics, "bundlestash.settings.rows", config.rows, rowsMinus, rowsPlus, mouseX, mouseY);
        drawRow(graphics, "bundlestash.settings.columns", config.columns, colsMinus, colsPlus, mouseX, mouseY);
    }

    /**
     * 处理弹窗内部的点击。
     *
     * @return 是否点到了某个加减按钮（调用方据此决定是否保存配置）
     */
    public boolean handleClick(double mouseX, double mouseY, BundleConfig config) {
        if (rowsMinus.contains(mouseX, mouseY)) {
            config.rows = clamp(config.rows - 1, ROWS_MIN, ROWS_MAX);
            return true;
        }
        if (rowsPlus.contains(mouseX, mouseY)) {
            config.rows = clamp(config.rows + 1, ROWS_MIN, ROWS_MAX);
            return true;
        }
        if (colsMinus.contains(mouseX, mouseY)) {
            config.columns = clamp(config.columns - 1, COLS_MIN, COLS_MAX);
            return true;
        }
        if (colsPlus.contains(mouseX, mouseY)) {
            config.columns = clamp(config.columns + 1, COLS_MIN, COLS_MAX);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------ 绘制

    private void drawRow(PanelGraphics<S> graphics, String labelKey, int value,
                         Rect minus, Rect plus, double mouseX, double mouseY) {
        int textY = minus.y() + (CONTROL - 8) / 2;
        graphics.drawText(graphics.translate(labelKey), bounds.x() + PAD, textY, PanelPalette.TEXT, false);
        graphics.centeredText(String.valueOf(value), valueX + VALUE_W / 2, textY, PanelPalette.TEXT);
        drawControl(graphics, minus, "-", mouseX, mouseY);
        drawControl(graphics, plus, "+", mouseX, mouseY);
    }

    private void drawControl(PanelGraphics<S> graphics, Rect rect, String glyph,
                             double mouseX, double mouseY) {
        graphics.sprite(SPRITE_CONTROL, rect);
        int color = rect.contains(mouseX, mouseY) ? PanelPalette.TEXT : PanelPalette.TEXT_DIM;
        graphics.centeredText(glyph, rect.x() + rect.width() / 2, rect.y() + 3, color);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }
}
