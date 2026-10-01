package bundlestash.mixin;

import bundlestash.BetterBundleMod;
import bundlestash.core.Rect;
import bundlestash.gui.BundleInput;
import bundlestash.gui.BundlePanelController;
import bundlestash.gui.PanelPalette;
import bundlestash.mixin.accessor.AbstractContainerScreenAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 容器界面的注入点，只负责把事件转交给 {@link BundlePanelController}：
 * <ul>
 *   <li>界面初始化时把搜索框挂成子控件</li>
 *   <li>绘制阶段调用侧栏渲染</li>
 *   <li>输入事件先给侧栏一次机会，被消费就不再传给 vanilla</li>
 * </ul>
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    /**
     * 面板在界面的最后一步绘制，保证不会被界面自身的背景遮挡。
     * 注意：合成书类界面不会调用 {@code super.extractRenderState}，因此那边还有一份注入
     * （见 {@link AbstractRecipeBookScreenMixin}），两者不会重复执行。
     */
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void betterBundle$onRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                           float partialTick, CallbackInfo ci) {
        AbstractContainerScreenAccess access = (AbstractContainerScreenAccess) this;
        Rect screenRect = BundleInput.screenRect(access);
        BundlePanelController controller = BetterBundleMod.instance().controller();
        BetterBundleMod.runGuarded("render", () -> {
            controller.render(screenRect, graphics, mouseX, mouseY, partialTick);
            betterBundle$highlightSource(controller, graphics, access);
        });
    }

    /** 悬停某个条目时，把它所在的收纳袋在背包里描出来。 */
    @Unique
    private void betterBundle$highlightSource(BundlePanelController controller, GuiGraphicsExtractor graphics,
                                             AbstractContainerScreenAccess access) {
        if (!controller.config().highlightSourceBundle) return;
        int bundleSlot = controller.hoveredBundleSlot();
        if (bundleSlot < 0) return;

        Slot slot = access.bundlestash$menu().getSlot(bundleSlot);
        if (slot == null || !slot.hasItem()) return;

        int left = access.bundlestash$leftPos() + slot.x;
        int top = access.bundlestash$topPos() + slot.y;
        int color = PanelPalette.SOURCE_HIGHLIGHT;
        graphics.fill(left - 1, top - 1, left + 17, top, color);
        graphics.fill(left - 1, top + 16, left + 17, top + 17, color);
        graphics.fill(left - 1, top, left, top + 16, color);
        graphics.fill(left + 16, top, left + 17, top + 16, color);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void betterBundle$onMouseClicked(MouseButtonEvent event, boolean doubleClick,
                                             CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreenAccess access = (AbstractContainerScreenAccess) this;
        if (BetterBundleMod.callGuarded("mouse-clicked",
                () -> BundleInput.onMouseClicked(access, event, doubleClick))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void betterBundle$onMouseReleased(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (BetterBundleMod.callGuarded("mouse-released", () -> BundleInput.onMouseReleased(event))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void betterBundle$onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (BetterBundleMod.callGuarded("mouse-scrolled", () -> BundleInput.onMouseScrolled(mouseX, mouseY, scrollY))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void betterBundle$onMouseDragged(MouseButtonEvent event, double dx, double dy,
                                             CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreenAccess access = (AbstractContainerScreenAccess) this;
        if (BetterBundleMod.callGuarded("mouse-dragged", () -> BundleInput.onMouseDragged(access, event))) {
            cir.setReturnValue(true);
        }
    }
}
