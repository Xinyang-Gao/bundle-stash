package bundlestash.platform;

import bundlestash.core.Romanizer;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 基于 pinyin4j 的拼音转换。结果按字符组合缓存，避免每帧重复计算。
 */
public final class PinyinRomanizer implements Romanizer {

    private static volatile PinyinRomanizer instance;

    private final Map<String, Result> cache = new HashMap<>(256);
    private final HanyuPinyinOutputFormat format;

    private PinyinRomanizer() {
        this.format = new HanyuPinyinOutputFormat();
        this.format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        this.format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
    }

    /** 拼音库缺失时返回 {@code null}，由调用方降级为普通搜索。 */
    public static PinyinRomanizer getOrNull() {
        try {
            if (instance == null) {
                instance = new PinyinRomanizer();
            }
            return instance;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public Result romanize(String text) {
        if (text == null || text.isEmpty()) return new Result("", "");
        Result cached = cache.get(text);
        if (cached != null) return cached;

        StringBuilder full = new StringBuilder();
        StringBuilder initials = new StringBuilder();
        for (char c : text.toCharArray()) {
            String syllable = latinOf(c);
            if (syllable == null) continue;
            full.append(syllable);
            initials.append(syllable.charAt(0));
        }
        Result result = new Result(full.toString().toLowerCase(Locale.ROOT), initials.toString().toLowerCase(Locale.ROOT));
        if (cache.size() > 4096) cache.clear();
        cache.put(text, result);
        return result;
    }

    private String latinOf(char c) {
        if (!Character.isIdeographic(c)) {
            // 拉丁字符直接参与匹配，方便混输，例如 "钻石 zuanshi"
            return Character.isLetterOrDigit(c) ? String.valueOf(c) : null;
        }
        try {
            String[] syllables = PinyinHelper.toHanyuPinyinStringArray(c, format);
            return syllables != null && syllables.length > 0 ? syllables[0] : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
