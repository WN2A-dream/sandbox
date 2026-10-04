package shop.web;

/**
 * コントローラが返すHTTPレスポンス。
 * HTMLを返す場合は body、リダイレクトする場合は location を持つ。
 */
public record Response(int status, String body, String location) {

    /** HTMLページを返す。 */
    public static Response html(int status, String body) {
        return new Response(status, body, null);
    }

    /**
     * 別のURLへ移動させる（303 See Other）。
     * POST処理のあとに使うと、ブラウザのリロードでPOSTが再送されるのを防げる。
     */
    public static Response redirect(String location) {
        return new Response(303, null, location);
    }
}
