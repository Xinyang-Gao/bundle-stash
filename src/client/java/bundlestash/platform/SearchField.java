package bundlestash.platform;

import bundlestash.core.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * 侧栏搜索框。
 * <p>
 * 实现上完全接管 vanilla {@link EditBox} 的绘制与输入转发，而不把它注册成界面的子控件：
 * 各类容器界面（合成书类、创造物品栏等）会覆盖 {@code mouseClicked}/{@code keyPressed}，
 * 子控件的事件分派链路并不一致，靠它来拿焦点并不可靠。
 * 自己转发 keyPressed / charTyped / preeditUpdated 反而能保证任何界面里都能正常输入，
 * 中文输入法的候选词与预编辑也照常工作。
 */
public final class SearchField {

    private static final int MAX_LENGTH = 64;

    private final Consumer<String> responder;

    private EditBox editBox;
    private String value = "";
    private boolean visible;

    public SearchField(Consumer<String> responder) {
        this.responder = responder;
    }

    /** 字体尚未就绪时返回 {@code null}（客户端入口阶段就可能发生）。 */
    public EditBox widget() {
        return ensure();
    }

    private EditBox ensure() {
        if (editBox != null) return editBox;
        Font font = Minecraft.getInstance().font;
        if (font == null) return null;

        EditBox box = new EditBox(font, 0, 0, 10, 10, Component.translatable("bundlestash.search"));
        box.setMaxLength(MAX_LENGTH);
        box.setBordered(false);
        box.setCanLoseFocus(true);
        box.setTextColor(0xFFFFFF);
        box.setTextColorUneditable(0x888888);
        box.setResponder(responder);
        box.setValue(value);
        box.setVisible(visible);
        this.editBox = box;
        return box;
    }

    // ------------------------------------------------------------ 布局与显示

    /**
     * 把输入框摆到布局算出来的矩形上。
     * 外框（{@code widget/text_field} 精灵）由渲染器绘制，这里给文字留出边距，
     * 否则无边框模式下文字会贴着精灵的左边线。
     */
    public void applyRect(Rect rect) {
        EditBox box = ensure();
        if (box == null) return;
        int x = rect.x() + 4;
        int y = rect.y() + 2;
        int width = Math.max(8, rect.width() - 8);
        int height = Math.max(8, rect.height() - 4);

        if (width != box.getWidth()) box.setWidth(width);
        if (height != box.getHeight()) box.setHeight(height);
        box.setX(x);
        box.setY(y);
    }

    public void setVisible(boolean newVisible) {
        this.visible = newVisible;
        EditBox box = ensure();
        if (box == null) return;
        if (!newVisible && box.isFocused()) box.setFocused(false);
        box.setVisible(newVisible);
    }

    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        EditBox box = ensure();
        if (box == null || !box.isVisible()) return;
        box.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
    }

    // ------------------------------------------------------------ 值

    public String value() {
        return editBox == null ? value : editBox.getValue();
    }

    public void setValue(String newValue) {
        this.value = newValue == null ? "" : newValue;
        if (editBox != null && !editBox.getValue().equals(this.value)) editBox.setValue(this.value);
    }

    // ------------------------------------------------------------ 输入

    public boolean isFocused() {
        return editBox != null && editBox.isFocused();
    }

    /** 取得焦点（内部自己维护，不依赖界面的子控件分派）。 */
    public void focus() {
        EditBox box = ensure();
        if (box == null) return;
        box.setFocused(true);
        box.moveCursorToEnd(false);
    }

    public void unfocus() {
        EditBox box = editBox;
        if (box != null) box.setFocused(false);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return editBox != null && editBox.isVisible() && editBox.isMouseOver(mouseX, mouseY);
    }

    public void onMouseClicked(MouseButtonEvent event, boolean doubleClick) {
        EditBox box = ensure();
        if (box == null) return;
        focus();
        box.mouseClicked(event, doubleClick);
    }

    public boolean onKeyPressed(KeyEvent event) {
        EditBox box = editBox;
        return box != null && box.isFocused() && box.keyPressed(event);
    }

    public boolean onCharTyped(CharacterEvent event) {
        EditBox box = editBox;
        return box != null && box.isFocused() && box.charTyped(event);
    }

    public boolean onPreeditUpdated(PreeditEvent event) {
        EditBox box = editBox;
        return box != null && box.isFocused() && box.preeditUpdated(event);
    }
}
