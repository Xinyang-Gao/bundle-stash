package bundlestash.platform;

import bundlestash.core.BundleEntry;
import bundlestash.core.BundleGroup;
import bundlestash.core.BundleModel;
import bundlestash.core.BundleWeights;
import bundlestash.core.ItemTraits;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import org.apache.commons.lang3.math.Fraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 与 Minecraft 直接打交道的一层：采集收纳袋快照、把物品转成核心层能理解的数据，
 * 以及发送与收纳袋交互所需的网络包。
 * <p>
 * 所有与 Minecraft 版本相关的代码都收敛在这里和 {@code mixin} 包里，
 * 换版本时只需要替换这一层，{@code core} 与 {@code gui} 的逻辑可以复用。
 * <p>
 * 快照最多每 100ms 重建一次，且先由 {@link #snapshotFingerprint()} 判断值不值得重建，
 * 因此这里的每一步都按"会被反复执行"来写：槽位索引一次建好、物品 id 与分类特征按物品缓存、
 * 显示名干脆不进快照（见 {@link BundleEntry}），快照本体只做指针搬运。
 */
public final class McAccess {

    private McAccess() {
    }

    /** 物品 id 按物品缓存：registry id 是物品固有属性，无需每个条目查一次注册表再 toString。 */
    private static final Map<Item, String> ID_CACHE = new IdentityHashMap<>();

    /**
     * 分类特征按物品缓存：方块/可食用/标签对同一物品的每个堆叠都一样，
     * 按物品算一次即可。（个别堆叠被改写组件的极少数情况以先见到的堆叠为准，
     * 影响仅限于分类归类，不影响任何交互。）
     */
    private static final Map<Item, ItemTraits> TRAIT_CACHE = new IdentityHashMap<>();

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

    /** 窗口（GUI 缩放后）高度，用于限制面板行数。 */
    public static int windowHeight() {
        return client().getWindow().getGuiScaledHeight();
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
     * 背包状态的轻量指纹：菜单槽位映射 + 收纳袋内容的摘要。
     * <p>
     * 只做整数运算、不分配对象：空闲（背包没动）时每 100ms 只需遍历一次，
     * 就能跳过整份快照的重建——而重建要物化袋内每个堆叠。见
     * {@code BundlePanelController} 里对它的使用。
     */
    public static int snapshotFingerprint() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;

        AbstractContainerMenu menu = player.containerMenu;
        int hash = menu.containerId;

        // 槽位映射进指纹：切页/换界面时 slot id 会重排，交互包必须用新的
        hash = hash * 31 + menu.slots.size();
        for (Slot slot : menu.slots) {
            hash = hash * 31 + slot.index;
            hash = hash * 31 + slot.getContainerSlot();
            hash = hash * 31 + System.identityHashCode(slot.container);
        }

        Inventory inventory = player.getInventory();
        for (int inventoryIndex = 0; inventoryIndex < 36; inventoryIndex++) {
            ItemStack stack = inventory.getItem(inventoryIndex);
            if (!isBundle(stack)) continue;
            hash = hash * 31 + inventoryIndex;
            // 袋子本体的引用也要进指纹：toggleSelectedItem 会就地改这个对象
            hash = hash * 31 + System.identityHashCode(stack);
            for (ItemStackTemplate template : contentsOf(stack).items()) {
                hash = hash * 31 + template.hashCode();
            }
        }
        return hash;
    }

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
        SlotLookup slots = SlotLookup.of(menu, inventory);

        List<BundleGroup<ItemStack>> groups = new ArrayList<>();
        for (int inventoryIndex = 0; inventoryIndex < 36; inventoryIndex++) {
            ItemStack stack = inventory.getItem(inventoryIndex);
            if (!isBundle(stack)) continue;

            BundleContents contents = contentsOf(stack);
            if (contents.isEmpty() && !includeEmpty) continue;

            groups.add(buildGroup(stack, contents, slots.find(inventoryIndex, stack), groups.size()));
        }
        return new BundleModel<>(List.copyOf(groups), includeEmpty);
    }

    private static BundleGroup<ItemStack> buildGroup(ItemStack bundleStack, BundleContents contents,
                                                     int slotId, int groupIndex) {
        // 26.3 起袋内物品以 ItemStackTemplate 存储，绘制要的还是实体堆叠，这里照旧物化一份
        List<ItemStack> stacks = contents.itemCopies().toList();
        List<BundleEntry<ItemStack>> items = new ArrayList<>(stacks.size());
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            items.add(new BundleEntry<>(
                    stack,
                    idOf(stack),
                    groupIndex,
                    i,
                    stack.getCount(),
                    BundleWeights.unitsOf(stack.getCount(), stack.getMaxStackSize()),
                    traitsOf(stack)
            ));
        }
        return new BundleGroup<>(slotId, bundleStack, List.copyOf(items), unitsOf(contents));
    }

    /** 菜单槽位的一次性索引，把"36 个背包格 × 全部槽位"的双向线性查找降到 O(1)。 */
    private static final class SlotLookup {

        private final Map<ItemStack, Integer> byStack = new IdentityHashMap<>();
        private final Map<Integer, Integer> byInventorySlot = new HashMap<>();

        static SlotLookup of(AbstractContainerMenu menu, Inventory inventory) {
            SlotLookup index = new SlotLookup();
            for (Slot slot : menu.slots) {
                index.byStack.putIfAbsent(slot.getItem(), slot.index);
                if (slot.container == inventory) {
                    index.byInventorySlot.putIfAbsent(slot.getContainerSlot(), slot.index);
                }
            }
            return index;
        }

        /** 背包第 {@code inventoryIndex} 格对应的容器槽位号，找不到返回 -1。 */
        int find(int inventoryIndex, ItemStack stack) {
            Integer slot = byStack.get(stack);
            if (slot != null) return slot;
            Integer inventorySlot = byInventorySlot.get(inventoryIndex);
            return inventorySlot == null ? -1 : inventorySlot;
        }
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
        return ID_CACHE.computeIfAbsent(stack.getItem(), item -> {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            return id == null ? "minecraft:air" : id.toString();
        });
    }

    public static String nameOf(ItemStack stack) {
        return stack.getDisplayName().getString();
    }

    public static ItemTraits traitsOf(ItemStack stack) {
        Item item = stack.getItem();
        ItemTraits cached = TRAIT_CACHE.get(item);
        if (cached != null) return cached;

        boolean block = item instanceof BlockItem;
        boolean fullBlock = block && ((BlockItem) item).getBlock().defaultBlockState().canOcclude();
        boolean edible = stack.get(DataComponents.FOOD) != null;

        // 取物品的全部标签而不是维护一份"关心的标签"清单：
        // 分类规则（ItemCategory）按标签 id 匹配，物品侧只需如实上报
        Set<String> tags;
        try (Stream<TagKey<Item>> stream = BuiltInRegistries.ITEM.wrapAsHolder(item).tags()) {
            tags = stream.map(tag -> tag.location().toString()).collect(Collectors.toUnmodifiableSet());
        }

        ItemTraits traits = new ItemTraits(block, fullBlock, edible, tags);
        TRAIT_CACHE.put(item, traits);
        return traits;
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
        Minecraft client = Minecraft.getInstance();
        // 世界卸载/切换的瞬间 gameMode 会为 null，此时不发包（否则 NPE 会把整个侧栏停用）
        if (client.player == null || client.gameMode == null) return;
        client.gameMode.handleContainerInput(client.player.containerMenu.containerId, slotId, button, action,
                client.player);
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
