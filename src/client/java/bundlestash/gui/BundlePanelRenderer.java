package bundlestash.gui;

import bundlestash.core.BundleEntry;
import bundlestash.core.BundleGroup;
import bundlestash.core.BundleModel;
import bundlestash.core.BundleWeights;
import bundlestash.core.ItemCategory;
import bundlestash.core.PanelLayout;
import bundlestash.core.PanelState;
import bundlestash.core.Rect;

import java.util.List;

/**
 * 侧栏绘制。只依赖 {@link PanelLayout} 算出的矩形与 {@link PanelGraphics} 提供的绘制原语，
 * 不接触任何 Minecraft 类型，因此换版本时这一层不需要改。
 * <p>
 * 视觉上完全照搬原版容器：
 * <ul>
 *   <li>面板底 → 原版容器灰 {@code #C6C6C6} 加 1px 深色描边</li>
 *   <li>格子 → {@code container/slot}（箱子/背包里那套经典凹槽，1:1 像素对齐）</li>
 *   <li>悬停 → {@code container/slot_highlight_back/front}</li>
 *   <li>滚动条 → {@code widget/scroller_background} / {@code widget/scroller}</li>
 *   <li>输入框 → {@code widget/text_field}（聚焦时用 highlighted 版本）</li>
 *   <li>底部 → 原版收纳袋进度条 {@code container/bundle/bundle_progressbar_*}</li>
 * </ul>
 *
 * @param <S> 物品类型
 */
public final class BundlePanelRenderer<S> {

    private static final String SPRITE_SLOT = "minecraft:container/slot";
    private static final String SPRITE_SLOT_HOVER_BACK = "minecraft:container/slot_highlight_back";
    private static final String SPRITE_SLOT_HOVER_FRONT = "minecraft:container/slot_highlight_front";
    private static final String SPRITE_SCROLL_TRACK = "minecraft:widget/scroller_background";
    private static final String SPRITE_SCROLL_THUMB = "minecraft:widget/scroller";
    private static final String SPRITE_TEXT_FIELD = "minecraft:widget/text_field";
    private static final String SPRITE_TEXT_FIELD_FOCUSED = "minecraft:widget/text_field_highlighted";
    private static final String SPRITE_BAR_FILL = "minecraft:container/bundle/bundle_progressbar_fill";
    private static final String SPRITE_BAR_FULL = "minecraft:container/bundle/bundle_progressbar_full";
    private static final String SPRITE_BAR_BORDER = "minecraft:container/bundle/bundle_progressbar_border";

    /** 原版容器面板底色。 */
    private static final int COLOR_PANEL_BG = 0xFFC6C6C6;
    private static final int COLOR_PANEL_BORDER = 0xFF000000;
    /** 浅色面板上用深色文字（和原版容器标题一致）。 */
    private static final int COLOR_TEXT = 0xFF404040;
    private static final int COLOR_TEXT_DIM = 0xFFA0A0A0;
    private static final int COLOR_ACCENT = 0xFF3B6EA5;

    /** 原版槽位贴图的原始边长，缩放时以它为中心对齐。 */
    private static final int SLOT_NATIVE = 18;

    private final ItemCategory[] categories;

    public BundlePanelRenderer(ItemCategory[] categories) {
        this.categories = categories;
    }

    /**
     * 一次绘制请求所需的全部上下文。
     */
    public record Request<S>(
            PanelLayout layout,
            List<BundleEntry<S>> view,
            BundleModel<S> model,
            PanelState state,
            PanelGraphics<S> graphics,
            boolean searchFocused,
            double mouseX,
            double mouseY
    ) {
    }

    /**
     * @return 鼠标悬停到的条目下标，未悬停返回 {@code -1}
     */
    public int render(Request<S> request) {
        PanelLayout layout = request.layout();
        PanelGraphics<S> graphics = request.graphics();
        List<BundleEntry<S>> view = request.view();
        PanelState state = request.state();
        double mouseX = request.mouseX();
        double mouseY = request.mouseY();

        drawPanelBackground(graphics, layout.panel());

        drawSearchField(request);
        drawCategoryButtons(request);
        drawScrollbar(request);

        int columns = layout.metrics().columns();
        int scrollRow = state.scrollRow();
        int firstVisible = scrollRow * columns;
        int visibleCount = Math.max(0, Math.min(columns * layout.metrics().rows(), view.size() - firstVisible));

        int hovered = -1;
        for (int i = 0; i < visibleCount; i++) {
            Rect cell = layout.cellRectByIndex(firstVisible + i, scrollRow);
            if (cell.contains(mouseX, mouseY)) hovered = firstVisible + i;
        }

        for (int i = 0; i < visibleCount; i++) {
            int flatIndex = firstVisible + i;
            Rect cell = layout.cellRectByIndex(flatIndex, scrollRow);
            BundleEntry<S> entry = view.get(flatIndex);
            boolean selected = flatIndex == hovered;

            drawSlot(graphics, cell);
            graphics.drawItemWithCount(entry.stack(), cell.x() + (cell.width() - 16) / 2,
                    cell.y() + (cell.height() - 16) / 2);
            if (selected) highlightSlot(graphics, cell);
        }

        drawFooter(request, hovered);

        if (hovered >= 0) {
            graphics.showTooltip(view.get(hovered).stack(), (int) mouseX, (int) mouseY);
        }
        return hovered;
    }

    /** 界面角落的开关按钮：面板隐藏时也显示，方便随时呼出。 */
    public void drawToggleButton(PanelLayout layout, PanelGraphics<S> graphics,
                                 double mouseX, double mouseY, boolean panelVisible) {
        Rect button = layout.toggleButton();
        boolean hovered = button.contains(mouseX, mouseY);
        drawSlot(graphics, button);
        graphics.drawItem(graphics.iconOf("minecraft:bundle"),
                button.x() + (button.width() - 16) / 2, button.y() + (button.height() - 16) / 2);
        if (hovered) highlightSlot(graphics, button);
        if (panelVisible) {
            graphics.fill(new Rect(button.x() + 3, button.bottom() - 3, button.width() - 6, 2), COLOR_ACCENT);
        }
    }

    // ------------------------------------------------------------ 基础图元

    /** 原版容器式底板：浅灰底 + 深色描边。 */
    private void drawPanelBackground(PanelGraphics<S> graphics, Rect panel) {
        graphics.fill(panel, COLOR_PANEL_BG);
        graphics.fill(new Rect(panel.x(), panel.y(), panel.width(), 1), COLOR_PANEL_BORDER);
        graphics.fill(new Rect(panel.x(), panel.bottom() - 1, panel.width(), 1), COLOR_PANEL_BORDER);
        graphics.fill(new Rect(panel.x(), panel.y(), 1, panel.height()), COLOR_PANEL_BORDER);
        graphics.fill(new Rect(panel.right() - 1, panel.y(), 1, panel.height()), COLOR_PANEL_BORDER);
    }

    /** 画一个槽位（格子/分类按钮/开关按钮共用）。 */
    private void drawSlot(PanelGraphics<S> graphics, Rect rect) {
        graphics.sprite(SPRITE_SLOT, rect);
    }

    private void highlightSlot(PanelGraphics<S> graphics, Rect rect) {
        graphics.sprite(SPRITE_SLOT_HOVER_BACK, rect);
        graphics.sprite(SPRITE_SLOT_HOVER_FRONT, rect);
    }

    // ------------------------------------------------------------ 各区域

    private void drawSearchField(Request<S> request) {
        if (request.layout().metrics().searchHeight() <= 0) return;
        Rect bar = request.layout().searchBar();
        request.graphics().sprite(request.searchFocused() ? SPRITE_TEXT_FIELD_FOCUSED : SPRITE_TEXT_FIELD, bar);

        String query = request.state().query();
        if (query.isEmpty()) {
            String hint = request.graphics().translate("bundlestash.search");
            request.graphics().drawText(hint, bar.x() + 4, bar.y() + (bar.height() - 8) / 2, COLOR_TEXT_DIM, false);
        }
    }

    private void drawCategoryButtons(Request<S> request) {
        PanelLayout layout = request.layout();
        PanelGraphics<S> graphics = request.graphics();
        PanelState state = request.state();
        double mouseX = request.mouseX();
        double mouseY = request.mouseY();

        List<Rect> buttons = layout.categoryButtons();
        for (int i = 0; i < buttons.size() && i < categories.length; i++) {
            Rect button = buttons.get(i);
            ItemCategory category = categories[i];
            boolean selected = state.category() == category;
            boolean hovered = button.contains(mouseX, mouseY);

            drawSlot(graphics, button);

            int iconSize = 16;
            int offset = Math.max(0, (Math.min(button.width(), button.height()) - iconSize) / 2);
            graphics.drawItem(graphics.iconOf(category.iconItemId()), button.x() + offset, button.y() + offset);
            if (selected || hovered) highlightSlot(graphics, button);

            // 分类名走原版 tooltip
            if (hovered) {
                graphics.showTextTooltip(graphics.translate(category.translationKey()), (int) mouseX, (int) mouseY);
            }
        }
    }

    private void drawScrollbar(Request<S> request) {
        PanelLayout layout = request.layout();
        List<BundleEntry<S>> view = request.view();
        int columns = layout.metrics().columns();
        int totalRows = Math.max(1, (view.size() + columns - 1) / columns);

        PanelGraphics<S> graphics = request.graphics();
        graphics.sprite(SPRITE_SCROLL_TRACK, layout.scrollbar());
        Rect thumb = layout.scrollbarThumb(request.state().scrollRow(), totalRows);
        if (thumb != null) graphics.sprite(SPRITE_SCROLL_THUMB, thumb);
    }

    /** 底部：悬停在某个条目上时显示它所属收纳袋的原版进度条，否则显示总量统计。 */
    private void drawFooter(Request<S> request, int hovered) {
        Rect footer = request.layout().footer();
        if (footer.width() <= 8 || footer.height() <= 0) return;

        PanelGraphics<S> graphics = request.graphics();
        BundleGroup<S> group = hovered >= 0 && hovered < request.view().size()
                ? request.model().groups().get(request.view().get(hovered).groupIndex())
                : null;

        float fullness = group != null
                ? Math.min(1f, group.weightUnits() / (float) BundleWeights.FULL)
                : Math.min(1f, request.model().usedUnits() / (float) Math.max(1, request.model().capacityUnits()));

        String label = group != null
                ? group.fullnessPercent() + "%"
                : request.model().itemCount() + " / " + request.model().bundleCount() * BundleWeights.FULL;

        // 原版进度条：先画填充（宽度按比例），再画边框，最后叠文字
        int fillWidth = Math.max(0, Math.round((footer.width() - 2) * fullness));
        String fillSprite = fullness >= 1f ? SPRITE_BAR_FULL : SPRITE_BAR_FILL;
        if (fillWidth > 0) {
            graphics.sprite(fillSprite, new Rect(footer.x() + 1, footer.y(), fillWidth, footer.height()));
        }
        graphics.sprite(SPRITE_BAR_BORDER, footer);
        graphics.centeredText(label, footer.x() + footer.width() / 2,
                footer.y() + (footer.height() - 8) / 2, COLOR_TEXT);
    }
}
