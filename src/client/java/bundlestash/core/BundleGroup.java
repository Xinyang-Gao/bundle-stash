package bundlestash.core;

import java.util.List;

/**
 * 一个收纳袋的可绘制快照。每次重绘前由平台层重建一次。
 *
 * @param containerSlot 该收纳袋所在的容器槽位号（向服务端发送交互包时使用）
 * @param bundleStack   收纳袋本身的物品对象
 * @param items         收纳袋内的物品（以原始堆叠形式展示，不做合并）
 * @param weightUnits   已占用的重量单位
 */
public record BundleGroup<S>(int containerSlot, S bundleStack, List<BundleEntry<S>> items, int weightUnits) {

    public boolean isEmpty() {
        return items.isEmpty();
    }

    /** 剩余容量（重量单位）。 */
    public int remaining() {
        return Math.max(0, BundleWeights.FULL - weightUnits);
    }

    /** 满度百分比，用于绘制进度条。 */
    public int fullnessPercent() {
        return (int) Math.round(BundleWeights.fullness(weightUnits) * 100.0);
    }
}
