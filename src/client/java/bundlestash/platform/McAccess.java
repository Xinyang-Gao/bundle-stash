package bundlestash.platform;

import bundlestash.core.BundleEntry;
import bundlestash.core.BundleGroup;
import bundlestash.core.BundleModel;
import bundlestash.core.BundleWeights;
import bundlestash.core.ItemTraits;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * 与 Minecraft 直接打交道的一层：采集收纳袋快照、把物品转成核心层能理解的数据，
 * 以及发送与收纳袋交互所需的网络包。
 * <p>
 * 所有与 Minecraft 版本相关的代码都收敛在这里和 {@code mixin} 包里，
 * 换版本时只需要替换这一层，{@code core} 与 {@code gui} 的逻辑可以复用。
 */
public final class McAccess {

    private McAccess() {
    }

    /** 用于分类判定的物品标签及其字符串形式（与 {@code ItemCategory} 中的规则对应）。 */
    private static final Map<TagKey<Item>, String> CLASSIFIER_TAGS = new LinkedHashMap<>();
    static {
        CLASSIFIER_TAGS.put(ItemTags.SWORDS, "minecraft:swords");
        CLASSIFIER_TAGS.put(ItemTags.AXES, "minecraft:axes");
        CLASSIFIER_TAGS.put(ItemTags.PICKAXES, "minecraft:pickaxes");
        CLASSIFIER_TAGS.put(ItemTags.SHOVELS, "minecraft:shovels");
        CLASSIFIER_TAGS.put(ItemTags.HOES, "minecraft:hoes");
        CLASSIFIER_TAGS.put(ItemTags.SPEARS, "minecraft:spears");
        CLASSIFIER_TAGS.put(ItemTags.HEAD_ARMOR, "minecraft:head_armor");
        CLASSIFIER_TAGS.put(ItemTags.CHEST_ARMOR, "minecraft:chest_armor");
        CLASSIFIER_TAGS.put(ItemTags.LEG_ARMOR, "minecraft:leg_armor");
        CLASSIFIER_TAGS.put(ItemTags.FOOT_ARMOR, "minecraft:foot_armor");
        CLASSIFIER_TAGS.put(ItemTags.BOATS, "minecraft:boats");
        CLASSIFIER_TAGS.put(ItemTags.CHEST_BOATS, "minecraft:chest_boats");
        CLASSIFIER_TAGS.put(ItemTags.MEAT, "minecraft:meat");
        CLASSIFIER_TAGS.put(ItemTags.FISHES, "minecraft:fishes");
        CLASSIFIER_TAGS.put(ItemTags.SAPLINGS, "minecraft:saplings");
        CLASSIFIER_TAGS.put(ItemTags.LEAVES, "minecraft:leaves");
        CLASSIFIER_TAGS.put(ItemTags.VILLAGER_PLANTABLE_SEEDS, "minecraft:villager_plantable_seeds");
    }

    // ------------------------------------------------------------------ 环境

    public static Minecraft client() {
        return Minecraft.getInstance();
    }

    public static Optional<LocalPlayer> player() {
        return Optional.ofNullable(client().player);
    }

    public static Optional<ClientPacketListener> connection() {
        return Optional.ofNullable(client().getConnection());
    }

    public static Optional<AbstractContainerMenu> menu() {
        return player().map(p -> p.containerMenu);
    }

    /** 窗口高度，用于限制面板行数。 */
    public static int windowHeight() {
        return client().getWindow().getHeight();
    }

    // ------------------------------------------------------------ 收纳袋识别

    public static boolean isBundle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return stack.getItem() instanceof BundleItem || stack.is(ItemTags.BUNDLES);
    }

    public static BundleContents contentsOf(ItemStack stack) {
        BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
        return contents == null ? BundleContents.EMPTY : contents;
    }

    // ---------------------------------------------------------------- 快照

    /**
     * 采集玩家背包里的收纳袋。
     * <p>
     * 这里刻意直接读 {@link Inventory}，而不是遍历 {@code menu.slots}：创造模式物品栏、
     * 合成书界面等会动态增删槽位，某些菜单里槽位与背包的对应关系也不直观，
     * 直接读背包可以保证"只要有收纳袋就一定看得见"。槽位号在菜单里查得到时用于交互，
     * 查不到（例如创造模式的某些页签）时标记为 -1，只展示不可点击。
     *
     * @param includeEmpty 是否把空收纳袋也算进去（收东西时需要）
     */
    public static BundleModel<ItemStack> collectBundles(boolean includeEmpty) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return BundleModel.empty();

        Inventory inventory = player.getInventory();
        AbstractContainerMenu menu = player.containerMenu;

        List<BundleGroup<ItemStack>> groups = new ArrayList<>();
        for (int inventoryIndex = 0; inventoryIndex < 36; inventoryIndex++) {
            ItemStack stack = inventory.getItem(inventoryIndex);
            if (!isBundle(stack)) continue;

            BundleContents contents = contentsOf(stack);
            if (contents.isEmpty() && !includeEmpty) continue;

            int slotId = findContainerSlot(menu, inventory, inventoryIndex, stack);
            BundleGroup<ItemStack> group = buildGroup(stack, contents, slotId, groups.size());
            groups.add(group);
        }
        return new BundleModel<>(List.copyOf(groups), includeEmpty);
    }

    private static BundleGroup<ItemStack> buildGroup(ItemStack stack, BundleContents contents, int slotId, int groupIndex) {
        List<ItemStack> copies = contents.itemCopies().toList();
        List<BundleEntry<ItemStack>> items = new ArrayList<>(copies.size());
        for (int i = 0; i < copies.size(); i++) {
            ItemStack copy = copies.get(i);
            items.add(new BundleEntry<>(
                    copy,
                    idOf(copy),
                    nameOf(copy),
                    groupIndex,
                    i,
                    copy.getCount(),
                    BundleWeights.unitsOf(copy.getCount(), copy.getMaxStackSize()),
                    traitsOf(copy)
            ));
        }
        return new BundleGroup<>(slotId, stack, List.copyOf(items), unitsOf(contents));
    }

    /** 在菜单里找到背包第 {@code inventoryIndex} 格对应的容器槽位号，找不到返回 -1。 */
    private static int findContainerSlot(AbstractContainerMenu menu, Inventory inventory, int inventoryIndex, ItemStack stack) {
        for (Slot slot : menu.slots) {
            if (slot.getItem() == stack) return slot.index;
        }
        for (Slot slot : menu.slots) {
            if (slot.container == inventory && slot.getContainerSlot() == inventoryIndex) return slot.index;
        }
        return -1;
    }

    /** 把 vanilla 的 Fraction 重量换算成 1/64 单位。 */
    public static int unitsOf(BundleContents contents) {
        return contents.weight().result().map(McAccess::toUnits).orElse(0);
    }

    private static int toUnits(Fraction fraction) {
        return BundleWeights.unitsOf(fraction.doubleValue());
    }

    // -------------------------------------------------------------- 物品信息

    public static String idOf(ItemStack stack) {
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id == null ? "minecraft:air" : id.toString();
    }

    public static String nameOf(ItemStack stack) {
        return stack.getDisplayName().getString();
    }

    public static ItemTraits traitsOf(ItemStack stack) {
        Item item = stack.getItem();
        boolean block = item instanceof BlockItem;
        boolean fullBlock = block && ((BlockItem) item).getBlock().defaultBlockState().canOcclude();
        boolean edible = stack.get(DataComponents.FOOD) != null;

        Set<String> tags = new HashSet<>();
        for (Map.Entry<TagKey<Item>, String> entry : CLASSIFIER_TAGS.entrySet()) {
            if (stack.is(entry.getKey())) tags.add(entry.getValue());
        }
        return new ItemTraits(block, fullBlock, edible, stack.getMaxStackSize() > 1, tags);
    }

    /** 把物品 id 解析成用于显示图标的 {@link ItemStack}。 */
    public static ItemStack iconOf(String itemId) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    // ------------------------------------------------------------ 容纳判定

    /**
     * 预测某个收纳袋还能不能放下这个物品。直接复用 vanilla 的 {@code tryInsert}，
     * 免得自己重算"袋子套袋子""特殊物品"之类的重量规则。
     */
    public static boolean canAccept(ItemStack bundleStack, ItemStack stack) {
        if (bundleStack == null || stack == null || stack.isEmpty()) return false;
        BundleContents contents = contentsOf(bundleStack);
        return contents.asMutable().tryInsert(stack.copy()) > 0;
    }

    // ------------------------------------------------------------ 网络交互

    /** 选中（或取消选中，index 为 -1）某个收纳袋里的第 index 项。 */
    public static void selectBundleItem(BundleGroup<ItemStack> group, int index) {
        if (group.containerSlot() < 0) return;
        connection().ifPresent(connection -> {
            BundleItem.toggleSelectedItem(group.bundleStack(), index);
            connection.send(new ServerboundSelectBundleItemPacket(group.containerSlot(), index));
        });
    }

    /**
     * 向服务端发送一次容器点击，等价于玩家真的点了一下某个槽位。
     *
     * @param action 点击语义：{@code PICKUP} / {@code QUICK_MOVE} 等
     * @param button 0 = 主键（左键），1 = 副键（右键）
     */
    public static void sendClick(int slotId, int button, ContainerInput action) {
        if (slotId < 0) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        Minecraft.getInstance().gameMode.handleContainerInput(player.containerMenu.containerId, slotId, button, action, player);
    }

    /** 从 {@link MouseButtonEvent} 换算 vanilla 的点击按钮编号。 */
    public static int clickButton(MouseButtonEvent event) {
        return event.button() == 3 ? 1 : 0;
    }

    // ------------------------------------------------------------ 目标槽位

    /**
     * 找一个空格子来放置取出来的物品。
     *
     * @param preferContainer 是否优先选择当前打开容器（而非玩家背包）中的格子
     */
    public static OptionalInt findFreeSlot(boolean preferContainer) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return OptionalInt.empty();
        AbstractContainerMenu menu = player.containerMenu;

        List<Slot> candidates = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (!slot.hasItem() && slot.mayPickup(player)) candidates.add(slot);
        }
        if (candidates.isEmpty()) return OptionalInt.empty();

        if (preferContainer) {
            for (Slot slot : candidates) {
                if (slot.container != player.getInventory()) return OptionalInt.of(slot.index);
            }
        }
        for (Slot slot : candidates) {
            if (slot.container == player.getInventory() && slot.getContainerSlot() >= 9) return OptionalInt.of(slot.index);
        }
        for (Slot slot : candidates) {
            if (slot.container == player.getInventory()) return OptionalInt.of(slot.index);
        }
        return OptionalInt.of(candidates.get(0).index);
    }

    /** 找一个能装下指定物品的收纳袋（只考虑当前能交互的那些）。 */
    public static Optional<BundleGroup<ItemStack>> findBundleFor(BundleModel<ItemStack> model, ItemStack stack) {
        for (BundleGroup<ItemStack> group : model.groups()) {
            if (group.containerSlot() < 0) continue;
            if (canAccept(group.bundleStack(), stack)) return Optional.of(group);
        }
        return Optional.empty();
    }
}
