package shop.view;

import static shop.view.Html.esc;

/** エラーページ（404・400・500など）のHTMLを作る。 */
public final class ErrorView {
    private ErrorView() {
    }

    /** Routerに渡すエラーページ生成関数。カート個数は取得しないので0（バッジなし）とする。 */
    public static String page(int status, String message) {
        return Layout.page("エラー " + status, "", 0,
                "<p>" + esc(message) + "</p><p><a href='/'>トップへ戻る</a></p>");
    }
}
