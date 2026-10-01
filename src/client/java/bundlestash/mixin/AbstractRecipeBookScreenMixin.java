package bundlestash.mixin;

import bundlestash.BetterBundleMod;
import bundlestash.core.Rect;
import bundlestash.gui.BundleInput;
import bundlestash.gui.BundlePanelController;
import bundlestash.mixin.accessor.AbstractContainerScreenAccess;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 带合成书的界面（背包、工作台、熔炉等）既覆盖了 {@code mouseClicked}/{@code mouseDragged}，
 * 又**不会**调用 {@code super.extractRenderState}（它直接调 {@code super.extractContents}），
 * 所以渲染与输入都得在这里单独转发一次。
 */
@Mixin(AbstractRecipeBookScreen.class)
public abstract class AbstractRecipeBookScreenMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void betterBundle$onRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
                                           float partialTick, CallbackInfo ci) {
        AbstractContainerScreenAccess access = (AbstractContainerScreenAccess) this;
        Rect screenRect = BundleInput.screenRect(access);
        BundlePanelController controller = BetterBundleMod.instance().controller();
        BetterBundleMod.runGuarded("recipe-render", () ->
                controller.render(screenRect, graphics, mouseX, mouseY, partialTick));
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void betterBundle$onMouseClicked(MouseButtonEvent event, boolean doubleClick,
                                             CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreenAccess access = (AbstractContainerScreenAccess) this;
        if (BetterBundleMod.callGuarded("recipe-mouse-clicked",
                () -> BundleInput.onMouseClicked(access, event, doubleClick))) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void betterBundle$onMouseDragged(MouseButtonEvent event, double dx, double dy,
                                             CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreenAccess access = (AbstractContainerScreenAccess) this;
        if (BetterBundleMod.callGuarded("recipe-mouse-dragged", () -> BundleInput.onMouseDragged(access, event))) {
            cir.setReturnValue(true);
        }
    }
}
