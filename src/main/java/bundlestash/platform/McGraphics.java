package bundlestash.platform;

import bundlestash.core.Rect;
import bundlestash.gui.PanelGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * {@link PanelGraphics} 在 Minecraft 26.x 上的实现，全部使用原版精灵与物品渲染。
 */
public final class McGraphics implements PanelGraphics<ItemStack> {

    private final GuiGraphicsExtractor graphics;
    private final Minecraft client;

    public McGraphics(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
        this.client = Minecraft.getInstance();
    }

    @Override
    public void sprite(String spriteId, Rect rect) {
        if (rect.width() <= 0 || rect.height() <= 0) return;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.parse(spriteId),
                rect.x(), rect.y(), rect.width(), rect.height());
    }

    @Override
    public void fill(Rect rect, int argb) {
        graphics.fill(rect.x(), rect.y(), rect.right(), rect.bottom(), argb);
    }

    @Override
    public void drawItem(ItemStack stack, int x, int y) {
        graphics.item(stack, x, y);
    }

    @Override
    public void drawItemWithCount(ItemStack stack, int x, int y) {
        graphics.item(stack, x, y);
        graphics.itemDecorations(client.font, stack, x, y);
    }

    @Override
    public void drawText(String text, int x, int y, int argb, boolean shadow) {
        graphics.text(client.font, text, x, y, argb, shadow);
    }

    @Override
    public void centeredText(String text, int centerX, int y, int argb) {
        graphics.centeredText(client.font, text, centerX, y, argb);
    }

    @Override
    public void showTooltip(ItemStack stack, int mouseX, int mouseY) {
        Font font = client.font;
        graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
    }

    @Override
    public void showTextTooltip(String text, int mouseX, int mouseY) {
        graphics.setTooltipForNextFrame(client.font, Component.literal(text), mouseX, mouseY);
    }

    @Override
    public ItemStack iconOf(String itemId) {
        return McAccess.iconOf(itemId);
    }

    @Override
    public String translate(String key) {
        return Component.translatable(key).getString();
    }
}
