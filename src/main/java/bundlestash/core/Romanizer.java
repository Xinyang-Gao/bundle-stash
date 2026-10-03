package bundlestash.core;

/**
 * 把非拉丁文本转成拉丁字母的抽象，用于"用拼音搜中文物品"。
 * <p>
 * 核心层只依赖这个接口，是否真的接入拼音转换（例如 pinyin4j）由平台层决定，
 * 未接入时 {@link #NONE} 会直接返回空结果，搜索自动退化为"按名称/id 搜索"。
 */
public interface Romanizer {

    Romanizer NONE = text -> new Result("", "");

    /**
     * @return {@code full} 为完整拼写（"钻石" → "zuanshi"），{@code initials} 为首字母（"钻石" → "zs"）
     */
    Result romanize(String text);

    record Result(String full, String initials) {
        public boolean isEmpty() {
            return full.isEmpty() && initials.isEmpty();
        }
    }
}
