package shop.view;

/** 各Viewで共通して使うHTML用の小さな道具。 */
final class Html {
    private Html() {
    }

    /**
     * HTMLに埋め込む文字列をエスケープする（XSS対策）。
     * 商品名や検索語など、HTMLに出す文字列は必ずこれを通すこと。
     */
    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** 金額を「¥1,234」形式にする。 */
    static String yen(int amount) {
        return String.format("¥%,d", amount);
    }
}
