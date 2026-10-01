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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 侧栏控制器：把"数据模型 + 布局 + 状态"编排成一次完整的交互周期。
 * 渲染与输入都从这里取同一个 {@link PanelLayout}，两者永远不会算出不同的坐标。
 */
public final class BundlePanelController {

    private static final List<ItemCategory> CATEGORIES = List.of(ItemCategory.values());

    /** 快照刷新间隔：服务端每次同步都要上百毫秒，本地没必要每帧重算。 */
    private static final long MODEL_REFRESH_MS = 100L;

    private final PanelState state;
    private final BundlePanelRenderer<ItemStack> renderer = new BundlePanelRenderer<>(CATEGORIES);
    private final SearchField searchField;
    private final SettingsPopup<ItemStack> settingsPopup = new SettingsPopup<>();

    private final BundleConfig config;
    private BundleModel<ItemStack> model = BundleModel.empty();
    private List<BundleEntry<ItemStack>> view = List.of();
    private PanelLayout layout;
    private int hoveredBundleSlot = -1;
    private long lastModelUpdateMs = -1L;
    private int lastFingerprint;
    private boolean fingerprintValid;
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

    // ------------------------------------------------------------ 布局

    private PanelLayout layoutFor(Rect screenRect) {
        PanelMetrics metrics = baseMetrics().withRowsFitting(McAccess.windowHeight() - screenRect.y() - 4);
        PanelLayout.Side side = preferredSide();
        int windowWidth = windowWidth();
        PanelLayout.CategoryTabs tabs = categoryTabs();
        PanelLayout layout = new PanelLayout(screenRect, side, metrics, windowWidth, tabs);
        if (!layout.fitsHorizontally(windowWidth)) {
            // 界面靠边时会画到屏幕外，自动换到另一侧
            layout = new PanelLayout(screenRect, opposite(side), metrics, windowWidth, tabs);
        }
        return layout;
    }

    /**
     * 顶部分类条的参数：分类数量、当前选中下标、以及选中分类名的文字宽度。
     * 只有选中的按钮会画文字，其余只画小图标，所以只量一个字符串；
     * 文字宽度要量字体，只能在拿得到 Minecraft 的这一层算，再传给纯几何的 {@link PanelLayout}。
     */
    private PanelLayout.CategoryTabs categoryTabs() {
        Font font = Minecraft.getInstance().font;
        int selected = -1;
        for (int i = 0; i < CATEGORIES.size(); i++) {
            if (CATEGORIES.get(i) == state.category()) {
                selected = i;
                break;
            }
        }
        int labelWidth = 0;
        if (selected >= 0 && font != null) {
            String label = Component.translatable(CATEGORIES.get(selected).translationKey()).getString();
            labelWidth = font.width(label);
        }
        return new PanelLayout.CategoryTabs(CATEGORIES.size(), selected, labelWidth);
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
                6, config.searchEnabled ? 16 : 0, 15);
    }

    private static int windowWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
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

    public void render(Rect screenRect, GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        if (now - lastModelUpdateMs >= MODEL_REFRESH_MS) {
            this.lastModelUpdateMs = now;
            // 先用无分配的指纹问一句"背包动过吗"：没动就整份跳过快照重建与视图重算，
            // 动了才采集（含空收纳袋：bundleCount() 才能如实反映"背包里有没有收纳袋"）
            int fingerprint = McAccess.snapshotFingerprint();
            if (!fingerprintValid || fingerprint != lastFingerprint) {
                this.fingerprintValid = true;
                this.lastFingerprint = fingerprint;
                this.model = McAccess.collectBundles(true);
                this.viewDirty = true;
            }
        }
        if (viewDirty) {
            this.view = state.computeView(model, McAccess::nameOf);
            this.viewDirty = false;
        }
        this.layout = layoutFor(screenRect);

        McGraphics mcGraphics = new McGraphics(graphics);
        if (config.toggleButton) {
            renderer.drawToggleButton(layout, mcGraphics, mouseX, mouseY, panelActive());
            renderer.drawSettingsButton(layout, mcGraphics, mouseX, mouseY);
        }

        if (panelActive()) {
            renderPanel(screenRect, graphics, mcGraphics, mouseX, mouseY, partialTick);
        } else {
            searchField.setVisible(false);
            hoveredBundleSlot = -1;
        }

        // 弹窗画在最上层，面板隐藏时也要能设置
        if (settingsPopup.isOpen()) {
            settingsPopup.layout(screenRect);
            settingsPopup.render(mcGraphics, config, mouseX, mouseY);
        }
    }

    /** 面板本体（搜索框、网格、底部统计）以及面板上方的浮动物品补画。 */
    private void renderPanel(Rect screenRect, GuiGraphicsExtractor graphics, McGraphics mcGraphics,
                             int mouseX, int mouseY, float partialTick) {
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

        // 把鼠标上正在拖动的物品画在面板之上，否则面板背景会把它挡住，
        // 导致往面板里放/从面板里取时看不清具体是什么
        drawCarriedOnTop(graphics, mouseX, mouseY);

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

    /**
     * 把玩家手上正在拖动的物品画在面板之上。原版虽然也会画这个"浮动物品"，
     * 但它可能在面板之前被绘制而被面板背景盖住，这里在面板之后补画一次保证可见。
     */
    private void drawCarriedOnTop(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        ItemStack carried = client.player.containerMenu.getCarried();
        if (carried.isEmpty()) return;
        if (layout == null || !layout.panel().contains(mouseX, mouseY)) return;
        int x = mouseX - 8;
        int y = mouseY - 8;
        graphics.item(carried, x, y);
        graphics.itemDecorations(client.font, carried, x, y);
    }

    // ------------------------------------------------------------ 输入

    /**
     * @return {@code true} 表示点击已被侧栏消费
     */
    public boolean mouseClicked(MouseButtonEvent event, Rect screenRect, boolean doubleClick) {
        if (layout == null) return false;
        double mouseX = event.x();
        double mouseY = event.y();

        // 设置弹窗打开时按模态处理：点内部操作控件，点外部关闭，一律吞掉事件
        if (settingsPopup.isOpen()) {
            settingsPopup.layout(screenRect);
            if (settingsPopup.bounds().contains(mouseX, mouseY)) {
                if (settingsPopup.closeButton().contains(mouseX, mouseY)) {
                    settingsPopup.close();
                } else if (settingsPopup.handleClick(mouseX, mouseY, config)) {
                    BetterBundleMod.instance().saveConfig();
                    viewDirty = true;
                }
            } else {
                settingsPopup.close();
            }
            return true;
        }

        if (config.toggleButton && layout.toggleButton().contains(mouseX, mouseY)) {
            if (event.button() == 1) {
                state.toggleVisible();
                BetterBundleMod.instance().saveConfig();
            }
            return true;
        }

        if (config.toggleButton && layout.settingsButton().contains(mouseX, mouseY)) {
            if (event.button() == 0) settingsPopup.open();
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
            state.setCategory(CATEGORIES.get(categoryIndex));
            viewDirty = true;
            searchField.unfocus();
            BetterBundleMod.instance().saveConfig();
            return true;
        }

        if (!layout.isInsidePanel(mouseX, mouseY)) return false;

        searchField.unfocus();

        if (carriedIsNotEmpty()) {
            // 面板内点击一律吞掉事件，避免 vanilla 把鼠标上的物品丢到地上。
            // 没有可用收纳袋时 depositCarried 是空操作，物品会留在鼠标上。
            BundleActions.depositCarried(McAccess.collectBundles(true));
            return true;
        }

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
        if (settingsPopup.isOpen()) return true;
        return layout != null && panelActive() && layout.panel().contains(event.x(), event.y());
    }

    /**
     * Esc 键：设置弹窗打开时先关弹窗，避免这一下直接关掉容器界面。
     *
     * @return 是否已消费这次按键
     */
    public boolean onEscape() {
        if (!settingsPopup.isOpen()) return false;
        settingsPopup.close();
        return true;
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

    private boolean carriedIsNotEmpty() {
        return McAccess.menu().map(menu -> !menu.getCarried().isEmpty()).orElse(false);
    }
}
