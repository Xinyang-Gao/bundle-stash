package bundlestash;

import bundlestash.config.BundleConfig;
import bundlestash.core.Romanizer;
import bundlestash.gui.BundlePanelController;
import bundlestash.platform.PinyinRomanizer;
//? if fabric {
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
*///?}

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 客户端入口。只负责加载配置并持有侧栏控制器，
 * 具体渲染与交互分别由 {@code gui} 和 {@code mixin} 包承担。
 * Fabric 侧是 ClientModInitializer 入口，NeoForge 侧是 @Mod（仅客户端加载）入口，
 * 两者都汇合到 {@link #init(Path)}。
 */
//? if fabric {
public final class BetterBundleMod implements ClientModInitializer {
//?} elif neoforge {
/*@Mod(value = "bundle_stash", dist = Dist.CLIENT)
public final class BetterBundleMod {
*///?}

    public static final String MOD_ID = "bundle-stash";

    public static final Logger LOGGER = LoggerFactory.getLogger("bundle-stash");

    private static final Set<String> FAILED_STAGES = ConcurrentHashMap.newKeySet();

    private static BetterBundleMod instance;

    private BundleConfig config;
    private BundlePanelController controller;
    private Path configPath;

    //? if fabric {
    @Override
    public void onInitializeClient() {
        init(FabricLoader.getInstance().getConfigDir().resolve("bundle-stash.json"));
    }
    //?} elif neoforge {
    /*public BetterBundleMod() {
        init(FMLPaths.CONFIGDIR.get().resolve("bundle-stash.json"));
    }
    *///?}

    /** 两个加载器共用的初始化逻辑。 */
    private void init(Path path) {
        instance = this;
        configPath = path;
        config = BundleConfig.load(configPath);

        // 拼音库缺失/未启用时降级为普通搜索
        Romanizer romanizer = config.pinyinSearch ? PinyinRomanizer.getOrNull() : null;
        controller = new BundlePanelController(romanizer != null ? romanizer : Romanizer.NONE, config);
    }

    public static BetterBundleMod instance() {
        return instance;
    }

    public BundlePanelController controller() {
        return controller;
    }

    public BundleConfig config() {
        return config;
    }

    /** 把当前分类等状态写回配置文件。 */
    public void saveConfig() {
        if (config == null || configPath == null) return;
        if (controller != null) {
            config.lastCategory = controller.state().category();
        }
        config.save(configPath);
    }

    // ---------------------------------------------------------------- 容错

    /**
     * 侧栏是纯增强型 UI，任何异常都不应该害得游戏本体崩溃。
     * 出错的环节会被单独停用并在日志里打印一次完整堆栈，方便定位。
     */
    public static void runGuarded(String stage, Runnable action) {
        if (FAILED_STAGES.contains(stage)) return;
        try {
            action.run();
        } catch (Throwable t) {
            report(stage, t);
        }
    }

    public static boolean callGuarded(String stage, Supplier<Boolean> action) {
        if (FAILED_STAGES.contains(stage)) return false;
        try {
            return action.get();
        } catch (Throwable t) {
            report(stage, t);
            return false;
        }
    }

    private static void report(String stage, Throwable t) {
        LOGGER.error("[BetterBundle] 执行 {} 时出错，该功能已停用（游戏继续运行）", stage, t);
        FAILED_STAGES.add(stage);
    }
}
