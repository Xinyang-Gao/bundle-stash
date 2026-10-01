package bundlestash.config;

import bundlestash.core.ItemCategory;
import bundlestash.core.PanelLayout;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 侧栏配置。保存在 {@code config/bundle-stash.json}，缺失字段会自动用默认值补齐，
 * 因此新增配置项不会让旧配置文件失效。
 */
public final class BundleConfig {

    /** 是否在容器界面显示侧栏。 */
    public boolean enabled = true;
    /** 面板放在界面的哪一侧。 */
    public String side = PanelLayout.Side.LEFT.name();
    /** 网格列数。 */
    public int columns = 9;
    /** 网格可见行数。 */
    public int rows = 8;
    /** 单格边长（原版槽位是 18）。 */
    public int cellSize = 18;
    /** 面板与界面边缘的间距。 */
    public int screenGap = 4;
    /** 是否启用搜索框。 */
    public boolean searchEnabled = true;
    /** 搜索时是否计算拼音（关闭后只能按物品名/id 搜索）。 */
    public boolean pinyinSearch = true;
    /** 是否在界面角落显示开关按钮。 */
    public boolean toggleButton = true;
    /**
     * 合成书打开时是否自动把面板挪到另一侧。
     * 合成书会占用界面左侧一大片区域，直接隐藏面板太粗暴，挪过去体验更好。
     */
    public boolean avoidRecipeBook = true;
    /** 悬浮面板条目时高亮对应的收纳袋。 */
    public boolean highlightSourceBundle = true;
    /** 上次使用的分类。 */
    public ItemCategory lastCategory = ItemCategory.ALL;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public PanelLayout.Side side() {
        try {
            return PanelLayout.Side.valueOf(side);
        } catch (IllegalArgumentException e) {
            return PanelLayout.Side.LEFT;
        }
    }

    public static BundleConfig load(Path file) {
        if (Files.isRegularFile(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                BundleConfig parsed = GSON.fromJson(reader, BundleConfig.class);
                if (parsed != null) return sanitize(parsed);
            } catch (IOException | RuntimeException e) {
                // 配置损坏时回退到默认值，避免整个模组不可用
            }
        }
        return new BundleConfig();
    }

    public void save(Path file) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException | RuntimeException e) {
            // 保存失败不影响游戏
        }
    }

    private static BundleConfig sanitize(BundleConfig config) {
        config.columns = Math.max(2, Math.min(12, config.columns));
        config.rows = Math.max(1, Math.min(12, config.rows));
        config.cellSize = Math.max(16, Math.min(24, config.cellSize));
        config.screenGap = Math.max(0, Math.min(24, config.screenGap));
        if (config.lastCategory == null) config.lastCategory = ItemCategory.ALL;
        return config;
    }
}
