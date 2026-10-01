package bundlestash.gui;

import bundlestash.BetterBundleMod;
import bundlestash.config.BundleConfig;
import bundlestash.core.BundleEntry;
import bundlestash.core.BundleModel;
import bundlestash.core.ItemCategory;
import bundlestash.core.PanelLayout;
import bundlestash.core.PanelMetrics;
import bundlestash.core.PanelState;
import bundlestash.core.Rect;
import bundlestash.core.Romanizer;
import bundlestash.mixin.accessor.AbstractRecipeBookScreenAccess;
import bundlestash.platform.McAccess;
import bundlestash.platform.McGraphics;
import bundlestash.platform.SearchField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 侧栏控制器：把"数据模型 + 布局 + 状态"编排成一次完整的交互周期。
 * 渲染与输入都从这里取同一个 {@link PanelLayout}，两者永远不会算出不同的坐标。
 */
public final class BundlePanelController {

    private static final ItemCategory[] CATEGORIES = ItemCategory.values();

    /** 快照刷新间隔：服务端每次同步都要上百毫秒，本地没必要每帧重算。 */
    private static final long MODEL_REFRESH_MS = 100L;

    private final PanelState state;
    private final BundlePanelRenderer<ItemStack> renderer = new BundlePanelRenderer<>(CATEGORIES);
    private final SearchField searchField;

    private BundleConfig config = new BundleConfig();
    private BundleModel<ItemStack> model = BundleModel.empty();
    private List<BundleEntry<ItemStack>> view = List.of();
    private PanelLayout layout;
    private int hoveredBundleSlot = -1;
    private long lastModelUpdateMs = -1L;
    private boolean viewDirty = true;

    public BundlePanelController(Romanizer romanizer, BundleConfig config) {
        this.state = new PanelState(romanizer);
        this.config = config;
        this.state.setCategory(config.lastCategory);
        this.searchField = new SearchField(query -> {
            state.setQuery(query);
            viewDirty = true;
        });
    }

    public PanelState state() {
        return state;
    }

    public BundleConfig config() {
        return config;
    }

    public SearchField searchField() {
        return searchField;
    }

    public ItemCategory[] categories() {
        return CATEGORIES;
    }

    public void setConfig(BundleConfig config) {
        this.config = config;
    }

    // ------------------------------------------------------------ 布局

    public PanelLayout layoutFor(Rect screenRect) {
        PanelMetrics metrics = baseMetrics().withRowsFitting(McAccess.windowHeight() - screenRect.y() - 4);
        PanelLayout.Side side = preferredSide();
        PanelLayout layout = new PanelLayout(screenRect, side, metrics, CATEGORIES.length);
        if (!layout.fitsHorizontally(windowWidth())) {
            // 界面靠边时会画到屏幕外，自动换到另一侧
            layout = new PanelLayout(screenRect, opposite(side), metrics, CATEGORIES.length);
        }
        return layout;
    }

    /** 合成书打开时占用界面左侧，把面板让到右边去。 */
    private PanelLayout.Side preferredSide() {
        PanelLayout.Side side = config.side();
        if (config.avoidRecipeBook && side == PanelLayout.Side.LEFT && isRecipeBookOpen()) {
            return PanelLayout.Side.RIGHT;
        }
        return side;
    }

    private static PanelLayout.Side opposite(PanelLayout.Side side) {
        return side == PanelLayout.Side.LEFT ? PanelLayout.Side.RIGHT : PanelLayout.Side.LEFT;
    }

    private PanelMetrics baseMetrics() {
        // gap 取 1：格子之间留一线，经典槽位贴图才看得出是一个个格子而不是一整块
        return new PanelMetrics(config.columns, config.rows, config.cellSize, 1, 6, config.screenGap,
                22, 6, config.searchEnabled ? 16 : 0, 15);
    }

    private static int windowWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    /** 最近一次算出的布局，未渲染过则为 {@code null}。 */
    public PanelLayout layout() {
        return layout;
    }

    // ------------------------------------------------------------ 显示状态

    private boolean panelActive() {
        return config.enabled && state.isVisible();
    }

    private boolean isRecipeBookOpen() {
        Screen screen = Minecraft.getInstance().gui.screen();
        return screen instanceof AbstractRecipeBookScreenAccess access
                && access.bundlestash$recipeBookComponent().isVisible();
    }

    // ------------------------------------------------------------ 渲染

    public void render(AbstractContainerScreen<?> screen, Rect screenRect,
                       GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        if (now - lastModelUpdateMs >= MODEL_REFRESH_MS) {
            this.model = McAccess.collectBundles(false);
            this.lastModelUpdateMs = now;
            this.viewDirty = true;
        }
        if (viewDirty) {
            this.view = state.computeView(model);
            this.viewDirty = false;
        }
        this.layout = layoutFor(screenRect);

        McGraphics mcGraphics = new McGraphics(graphics);
        if (config.toggleButton) {
            renderer.drawToggleButton(layout, mcGraphics, mouseX, mouseY, panelActive());
        }

        if (!panelActive()) {
            searchField.setVisible(false);
            hoveredBundleSlot = -1;
            return;
        }

        PanelMetrics metrics = layout.metrics();
        state.clampScroll(rowsOf(view.size(), metrics.columns()), metrics);

        searchField.setVisible(config.searchEnabled);
        if (config.searchEnabled) searchField.applyRect(layout.searchBar());

        int hovered = renderer.render(new BundlePanelRenderer.Request<>(
                layout, view, model, state, mcGraphics, searchField.isFocused(), mouseX, mouseY));

        // 输入框自己画，顺序可控（画在面板背景之上），不依赖界面的子控件列表
        if (config.searchEnabled) {
            searchField.render(graphics, mouseX, mouseY, partialTick);
        }

        state.setHoveredIndex(hovered);
        hoveredBundleSlot = hovered >= 0 && hovered < view.size()
                ? model.groups().get(view.get(hovered).groupIndex()).containerSlot()
                : -1;
    }

    /** 悬停条目所属收纳袋的容器槽位，用于高亮；无法交互时为 -1。 */
    public int hoveredBundleSlot() {
        return hoveredBundleSlot;
    }

    private static int rowsOf(int items, int columns) {
        return Math.max(1, (items + columns - 1) / columns);
    }

    // ------------------------------------------------------------ 输入

    /**
     * @return {@code true} 表示点击已被侧栏消费
     */
    public boolean mouseClicked(MouseButtonEvent event, Rect screenRect, boolean doubleClick) {
        if (layout == null) return false;
        double mouseX = event.x();
        double mouseY = event.y();

        if (config.toggleButton && layout.toggleButton().contains(mouseX, mouseY)) {
            if (event.button() == 1) {
                state.toggleVisible();
                BetterBundleMod.instance().saveConfig();
            }
            return true;
        }

        if (!panelActive()) return false;

        // 输入框自己接管焦点与光标（键盘事件由 KeyboardHandlerMixin 转发给它）
        if (config.searchEnabled && layout.searchBar().contains(mouseX, mouseY)) {
            searchField.onMouseClicked(event, doubleClick);
            return true;
        }

        int categoryIndex = layout.categoryIndexAt(mouseX, mouseY);
        if (categoryIndex >= 0) {
            state.setCategory(CATEGORIES[categoryIndex]);
            viewDirty = true;
            searchField.unfocus();
            BetterBundleMod.instance().saveConfig();
            return true;
        }

        if (!layout.isInsidePanel(mouseX, mouseY)) return false;

        searchField.unfocus();

        if (carriedIsNotEmpty()) return BundleActions.depositCarried(McAccess.collectBundles(true));

        int pressed = layout.cellIndexAt(mouseX, mouseY, state.scrollRow());
        if (pressed < 0 || pressed >= view.size()) return true;

        BundleEntry<ItemStack> entry = view.get(pressed);
        boolean preferContainer = !(Minecraft.getInstance().gui.screen() instanceof InventoryScreen);
        if (event.hasShiftDown()) {
            if (!BundleActions.takeToSlot(model, entry, preferContainer)) {
                BundleActions.takeToCursor(model, entry);
            }
        } else {
            BundleActions.takeToCursor(model, entry);
        }
        return true;
    }

    /** 侧栏区域内的抬起事件一律吞掉，避免 vanilla 把鼠标上的东西丢到地上。 */
    public boolean mouseReleased(MouseButtonEvent event) {
        return layout != null && panelActive() && layout.panel().contains(event.x(), event.y());
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (layout == null || !panelActive()) return false;
        if (!layout.isInsidePanel(mouseX, mouseY)) return false;
        int rows = rowsOf(view.size(), layout.metrics().columns());
        state.scrollBy(scrollDelta > 0 ? -1 : 1, rows, layout.metrics());
        return true;
    }

    /** 空格 + 点击/拖拽：把槽位里的东西收进袋子。 */
    public boolean handleBulkInsert(Slot slot) {
        if (!panelActive()) return false;
        return BundleActions.stashSlot(McAccess.collectBundles(true), slot);
    }

    public List<BundleEntry<ItemStack>> view() {
        return view;
    }

    public BundleModel<ItemStack> model() {
        return model;
    }

    private boolean carriedIsNotEmpty() {
        return McAccess.menu().map(menu -> !menu.getCarried().isEmpty()).orElse(false);
    }
}
