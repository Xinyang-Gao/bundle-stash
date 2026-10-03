package bundlestash.gui;

import bundlestash.BetterBundleMod;
import bundlestash.core.Rect;
import bundlestash.mixin.accessor.AbstractContainerScreenAccess;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.Slot;

/**
 * 容器界面输入事件的统一处理入口。
 * <p>
 * 之所以抽出来：合成书类界面会覆盖 {@code mouseClicked}/{@code mouseDragged}，
 * 父类里的注入对它们不生效，必须由各自的 mixin 转发；共用同一份实现才能保证行为一致。
 */
public final class BundleInput {

    private static int lastDraggedSlot = -1;

    private BundleInput() {
    }

    public static boolean onMouseClicked(AbstractContainerScreenAccess access, MouseButtonEvent event,
                                        boolean doubleClick) {
        BundlePanelController controller = BetterBundleMod.instance().controller();

        //? if >=26.3 {
        if (InputConstants.isKeyDown(InputConstants.KEY_SPACE)) {
        //?} else {
        /*if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), InputConstants.KEY_SPACE)) {
        *///?}
            Slot hovered = access.bundlestash$hoveredSlot();
            if (hovered != null && hovered.hasItem() && controller.handleBulkInsert(hovered)) {
                lastDraggedSlot = hovered.index;
                return true;
            }
        }

        return controller.mouseClicked(event, screenRect(access), doubleClick);
    }

    public static boolean onMouseDragged(AbstractContainerScreenAccess access, MouseButtonEvent event) {
        //? if >=26.3 {
        if (!InputConstants.isKeyDown(InputConstants.KEY_SPACE)) return false;
        //?} else {
        /*if (!InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), InputConstants.KEY_SPACE)) return false;
        *///?}
        Slot hovered = access.bundlestash$hoveredSlot();
        if (hovered == null || !hovered.hasItem() || hovered.index == lastDraggedSlot) return false;

        lastDraggedSlot = hovered.index;
        return BetterBundleMod.instance().controller().handleBulkInsert(hovered);
    }

    public static boolean onMouseReleased(MouseButtonEvent event) {
        lastDraggedSlot = -1;
        return BetterBundleMod.instance().controller().mouseReleased(event);
    }

    public static boolean onMouseScrolled(double mouseX, double mouseY, double scrollY) {
        return BetterBundleMod.instance().controller().mouseScrolled(mouseX, mouseY, scrollY);
    }

    /** 界面绘制区域的屏幕坐标，是面板布局的锚点。 */
    public static Rect screenRect(AbstractContainerScreenAccess access) {
        return new Rect(access.bundlestash$leftPos(), access.bundlestash$topPos(),
                access.bundlestash$imageWidth(), access.bundlestash$imageHeight());
    }
}
