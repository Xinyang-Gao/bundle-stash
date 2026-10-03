package bundlestash.mixin;

import bundlestash.BetterBundleMod;
import bundlestash.gui.BundlePanelController;
import bundlestash.platform.SearchField;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.PreeditEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把键盘输入转发给侧栏搜索框。
 * <p>
 * 之所以挂在 {@link KeyboardHandler} 上而不是界面上：各类容器界面覆盖 {@code keyPressed}
 * 的方式并不一致，而 {@code charTyped}/{@code preeditUpdated} 只在接口里有默认实现，
 * 直接在界面类里注入并不可靠。这里是所有键盘事件的源头，最稳定。
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void betterBundle$keyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        if (action == 0) return; // 抬起事件不处理

        // 设置弹窗打开时，Esc 先关弹窗，否则这一下会直接关掉容器界面
        BundlePanelController controller = controller();
        if (controller != null && event.key() == InputConstants.KEY_ESCAPE && controller.onEscape()) {
            ci.cancel();
            return;
        }

        SearchField field = searchField();
        if (field != null && field.onKeyPressed(event)) ci.cancel();
    }

    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
    private void betterBundle$charTyped(long handle, CharacterEvent event, CallbackInfo ci) {
        SearchField field = searchField();
        if (field != null && field.onCharTyped(event)) ci.cancel();
    }

    // 输入法预编辑回调在 26.3 改名为 textEditing（26.1/26.2 叫 preeditCallback），签名一致
    //? if >=26.3 {
    @Inject(method = "textEditing", at = @At("HEAD"), cancellable = true)
    //?} else {
    /*@Inject(method = "preeditCallback", at = @At("HEAD"), cancellable = true)
    *///?}
    private void betterBundle$textEditing(long handle, PreeditEvent event, CallbackInfo ci) {
        SearchField field = searchField();
        if (field != null && field.onPreeditUpdated(event)) ci.cancel();
    }

    private static SearchField searchField() {
        BetterBundleMod mod = BetterBundleMod.instance();
        return mod == null ? null : mod.controller().searchField();
    }

    private static BundlePanelController controller() {
        BetterBundleMod mod = BetterBundleMod.instance();
        return mod == null ? null : mod.controller();
    }
}
