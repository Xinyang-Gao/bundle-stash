package bundlestash.gui;

import bundlestash.core.BundleEntry;
import bundlestash.core.BundleGroup;
import bundlestash.core.BundleModel;
import bundlestash.platform.McAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * 侧栏能触发的全部动作。
 * <p>
 * 收纳袋的服务端语义（26.3）：
 * <ul>
 *   <li>主键点击 + 鼠标上有东西 → 整堆塞进收纳袋</li>
 *   <li>副键点击 + 鼠标空 → 取出一个物品到鼠标</li>
 *   <li>主键点击 + 鼠标空 → 正常拿起整个收纳袋</li>
 * </ul>
 * 因此"取出一项"必须先选中目标内容下标，再模拟一次副键点击；
 * 想直接放进背包则在后面补一次主键点击到空格子上。
 */
public final class BundleActions {

    /** vanilla 的鼠标按钮编号：0 = 主键（左键），1 = 副键（右键）。 */
    public static final int PRIMARY = 0;
    public static final int SECONDARY = 1;

    private BundleActions() {
    }

    /**
     * 取出一项到鼠标上。
     *
     * @return 是否真的发出了交互请求
     */
    public static boolean takeToCursor(BundleModel<ItemStack> model, BundleEntry<ItemStack> entry) {
        BundleGroup<ItemStack> group = groupOf(model, entry);
        if (group == null || group.containerSlot() < 0 || McAccess.connection().isEmpty()) return false;
        McAccess.selectBundleItem(group, entry.contentIndex());
        McAccess.sendClick(group.containerSlot(), SECONDARY, ContainerInput.PICKUP);
        return true;
    }

    /**
     * 取出一项并直接放进一个空格子。
     *
     * @param preferContainer 优先放进当前打开的容器（而不是玩家背包）
     */
    public static boolean takeToSlot(BundleModel<ItemStack> model, BundleEntry<ItemStack> entry, boolean preferContainer) {
        BundleGroup<ItemStack> group = groupOf(model, entry);
        if (group == null || group.containerSlot() < 0 || McAccess.connection().isEmpty()) return false;
        OptionalInt free = McAccess.findFreeSlot(preferContainer);
        if (free.isEmpty()) return takeToCursor(model, entry);

        McAccess.selectBundleItem(group, entry.contentIndex());
        McAccess.sendClick(group.containerSlot(), SECONDARY, ContainerInput.PICKUP);
        McAccess.sendClick(free.getAsInt(), PRIMARY, ContainerInput.PICKUP);
        return true;
    }

    /** 把鼠标上拿着的物品塞进第一个放得下的收纳袋。 */
    public static boolean depositCarried(BundleModel<ItemStack> model) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return false;
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) return false;

        Optional<BundleGroup<ItemStack>> target = McAccess.findBundleFor(model, carried);
        if (target.isEmpty()) return false;
        McAccess.sendClick(target.get().containerSlot(), PRIMARY, ContainerInput.PICKUP);
        return true;
    }

    /**
     * 把某个槽位的整堆物品塞进收纳袋，塞不下的部分放回原槽。
     * 用于"空格 + 点击/拖拽"的批量收纳。
     */
    public static boolean stashSlot(BundleModel<ItemStack> model, Slot slot) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || slot == null || !slot.hasItem()) return false;

        ItemStack stack = slot.getItem();
        if (McAccess.isBundle(stack)) return false;

        Optional<BundleGroup<ItemStack>> target = McAccess.findBundleFor(model, stack);
        if (target.isEmpty()) return false;

        if (!player.containerMenu.getCarried().isEmpty()) {
            // 鼠标上还有东西时先把手上的收纳袋填掉，避免覆盖
            return depositCarried(model);
        }

        int sourceSlot = slot.index;
        McAccess.sendClick(sourceSlot, PRIMARY, ContainerInput.PICKUP);
        McAccess.sendClick(target.get().containerSlot(), PRIMARY, ContainerInput.PICKUP);
        // 有剩余就放回原槽（服务端会按顺序处理，没剩余时这是一次无害的空点击）
        McAccess.sendClick(sourceSlot, PRIMARY, ContainerInput.PICKUP);
        return true;
    }

    private static BundleGroup<ItemStack> groupOf(BundleModel<ItemStack> model, BundleEntry<ItemStack> entry) {
        if (entry.groupIndex() < 0 || entry.groupIndex() >= model.groups().size()) return null;
        return model.groups().get(entry.groupIndex());
    }
}
