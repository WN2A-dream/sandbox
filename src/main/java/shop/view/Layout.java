package shop.view;

import static shop.view.Html.esc;

/** 全ページ共通の枠（ヘッダー・検索ボックス・スタイル）を作る。 */
public final class Layout {
    private Layout() {
    }

    private static final String CSS = """
            body{margin:0;font-family:sans-serif;background:#f3f3f3;color:#111}
            header{background:#232f3e;color:#fff;display:flex;gap:16px;align-items:center;padding:10px 20px}
            header a{color:#fff;text-decoration:none}.logo{font-size:20px;font-weight:bold}
            header form{flex:1;display:flex}header input{flex:1;padding:8px;border:0;border-radius:4px 0 0 4px}
            header button{border-radius:0 4px 4px 0}
            main{max-width:960px;margin:20px auto;padding:0 16px}
            .grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:16px}
            .card{background:#fff;padding:16px;text-align:center;color:inherit;text-decoration:none;border-radius:4px}
            .card:hover{box-shadow:0 2px 8px #0003}.emoji{font-size:56px}.big{font-size:120px}
            .price{color:#b12704;font-weight:bold}
            .detail{display:flex;gap:32px;background:#fff;padding:24px;border-radius:4px}
            .notice{background:#e6f4ea;border:1px solid #9bd3ab;padding:12px;border-radius:4px;margin-bottom:16px}
            table{width:100%;background:#fff;border-collapse:collapse}
            th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left}
            button{background:#ffd814;border:1px solid #fcd200;border-radius:4px;padding:8px 14px;cursor:pointer}
            button.sub{background:#eee;border-color:#ccc}
            """;

    /**
     * ページ全体のHTMLを組み立てる。
     *
     * @param title     ページタイトル（見出しと<title>に使う）
     * @param keyword   検索ボックスに表示しておく検索語
     * @param cartCount カート内の個数。0ならバッジは出さない
     * @param body      ページ固有の本文HTML
     */
    public static String page(String title, String keyword, int cartCount, String body) {
        String badge = cartCount > 0 ? " (" + cartCount + ")" : "";
        return "<!doctype html><html lang='ja'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>" + esc(title) + " - Mini Shop</title><style>" + CSS + "</style></head><body>"
                + "<header><a class='logo' href='/'>Mini Shop</a>"
                + "<form action='/'><input name='q' placeholder='商品を検索' value='" + esc(keyword) + "'>"
                + "<button>検索</button></form>"
                + "<a href='/orders'>注文履歴</a><a href='/cart'>🛒 カート" + badge + "</a></header>"
                + "<main><h1>" + esc(title) + "</h1>" + body + "</main></body></html>";
    }
}
