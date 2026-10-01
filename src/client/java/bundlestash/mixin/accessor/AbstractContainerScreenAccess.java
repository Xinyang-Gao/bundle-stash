package bundlestash.mixin.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 读取容器界面中受保护的字段。相比 access widener，accessor 不依赖构建期的访问加宽，
 * 也不需要在 {@code fabric.mod.json} 里声明额外文件。
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccess {

    @Accessor("leftPos")
    int bundlestash$leftPos();

    @Accessor("topPos")
    int bundlestash$topPos();

    @Accessor("imageWidth")
    int bundlestash$imageWidth();

    @Accessor("imageHeight")
    int bundlestash$imageHeight();

    @Accessor("hoveredSlot")
    Slot bundlestash$hoveredSlot();

    @Accessor("menu")
    AbstractContainerMenu bundlestash$menu();
}
