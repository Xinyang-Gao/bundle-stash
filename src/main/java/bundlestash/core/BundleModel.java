package bundlestash.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家背包中所有收纳袋的只读快照。每次重绘前由平台层重建一次，
 * 是否真的需要重建由平台层的指纹（{@code McAccess#snapshotFingerprint}）决定。
 *
 * @param groups       收纳袋列表
 * @param includeEmpty 是否包含空收纳袋
 */
public record BundleModel<S>(List<BundleGroup<S>> groups, boolean includeEmpty) {

    public static <S> BundleModel<S> empty() {
        return new BundleModel<>(List.of(), false);
    }

    /** 收纳袋数量。 */
    public int bundleCount() {
        return groups.size();
    }

    /** 收纳袋里物品的总个数。 */
    public int itemCount() {
        int total = 0;
        for (BundleGroup<S> group : groups) {
            for (BundleEntry<S> entry : group.items()) {
                total += entry.count();
            }
        }
        return total;
    }

    /** 收纳袋内容的总行数（一"行"= 一个未合并的堆叠）。 */
    public int stackCount() {
        int total = 0;
        for (BundleGroup<S> group : groups) total += group.items().size();
        return total;
    }

    /** 已占用的总重量单位。 */
    public int usedUnits() {
        int total = 0;
        for (BundleGroup<S> group : groups) total += group.weightUnits();
        return total;
    }

    /** 总容量（重量单位）。 */
    public int capacityUnits() {
        return groups.size() * BundleWeights.FULL;
    }

    /** 按收纳袋顺序展开后的扁平列表，直接对应侧栏格子。 */
    public List<BundleEntry<S>> flattened() {
        List<BundleEntry<S>> result = new ArrayList<>(stackCount());
        for (BundleGroup<S> group : groups) result.addAll(group.items());
        return result;
    }
}
