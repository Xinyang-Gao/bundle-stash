package bundlestash.gui;

import bundlestash.core.Rect;

/**
 * 侧栏绘制所需的全部基本操作。由平台层针对具体版本实现，
 * 这样侧栏的绘制流程本身不依赖任何 Minecraft 类。
 * <p>
 * 样式统一走原版精灵（sprite）：面板边框、槽位、滚动条、输入框全部用 vanilla 自己的贴图，
 * 因此视觉风格与游戏本体一致，也不会因为资源包更换贴图而显得突兀。
 *
 * @param <S> 物品类型
 */
public interface PanelGraphics<S> {

    /** 按九宫格规则绘制原版精灵（自动拉伸/切片）。 */
    void sprite(String spriteId, Rect rect);

    void fill(Rect rect, int argb);

    void drawItem(S stack, int x, int y);

    /** 带数量角标的物品绘制。 */
    void drawItemWithCount(S stack, int x, int y);

    void drawText(String text, int x, int y, int argb, boolean shadow);

    void centeredText(String text, int centerX, int y, int argb);

    int textWidth(String text);

    int lineHeight();

    void showTooltip(S stack, int mouseX, int mouseY);

    /** 用原版样式显示一段文本提示（用于分类名等）。 */
    void showTextTooltip(String text, int mouseX, int mouseY);

    /** 把 id（如 {@code minecraft:diamond}）解析成可用于绘制的物品。 */
    S iconOf(String itemId);

    /** 取物品的本地化名称。 */
    String nameOf(S stack);

    /** 取多语言文本。 */
    String translate(String key);
}
