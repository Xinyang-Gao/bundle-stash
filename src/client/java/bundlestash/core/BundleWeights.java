package bundlestash.core;

/**
 * 收纳袋重量体系的最小单位换算。
 * <p>
 * 收纳袋按"重量"计数：一整格（{@code FULL}）等于 1.0，堆叠上限为 64 的物品每个占 1/64，
 * 上限 16 的物品每个占 4/64，不可堆叠的物品占 64/64。因此统一以 1/64 为计重单位即可用整数表达，
 * 无需引入外部分数库，也不会有浮点误差。
 */
public final class BundleWeights {

    /** 一个收纳袋的总容量（重量 1.0）。 */
    public static final int FULL = 64;

    private BundleWeights() {
    }

    /**
     * 计算若干物品所占的重量单位数。
     *
     * @param count     物品数量
     * @param maxStack  物品堆叠上限
     * @return 所占的 1/64 单位数
     */
    public static int unitsOf(int count, int maxStack) {
        if (count <= 0) return 0;
        int per = perItem(maxStack);
        return count * per;
    }

    /** 单个物品所占的重量单位数（向上取整，保证非 1/16/64 堆叠不会因为取整而被判为 0）。 */
    private static int perItem(int maxStack) {
        if (maxStack <= 0) return FULL;
        if (maxStack >= FULL) return 1;
        if (FULL % maxStack == 0) return FULL / maxStack;
        return (FULL + maxStack - 1) / maxStack;
    }

    /** 把 0~1 的浮点占比换算成重量单位。 */
    public static int unitsOf(double fullness) {
        return Math.max(0, Math.min(FULL, (int) Math.ceil(fullness * FULL)));
    }

    /** 把重量单位换算回 0~1 的占比。 */
    public static double fullness(int units) {
        return units / (double) FULL;
    }
}
