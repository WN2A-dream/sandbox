package shop.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 「HTTPメソッド + パス」を、対応するコントローラのメソッドに振り分ける。
 * JDK標準のHttpServerには "/" 配下をすべて受け取るハンドラとして登録する。
 */
public final class Router implements HttpHandler {

    /** コントローラのメソッドの型。Requestを受け取り、Responseを返す。 */
    @FunctionalInterface
    public interface Handler {
        Response handle(Request request) throws Exception;
    }

    private final Map<String, Handler> routes = new HashMap<>();
    /** エラー時のHTML生成を担当する関数（ステータス, メッセージ → HTML）。Viewに依存しないよう外から渡す。 */
    private final BiFunction<Integer, String, String> errorPage;

    public Router(BiFunction<Integer, String, String> errorPage) {
        this.errorPage = errorPage;
    }

    public void get(String path, Handler handler) {
        routes.put("GET " + path, handler);
    }

    public void post(String path, Handler handler) {
        routes.put("POST " + path, handler);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            Request request = new Request(ex);
            Response response;
            try {
                Handler handler = routes.get(request.method() + " " + request.path());
                if (handler == null) {
                    throw new HttpException(404, "ページが見つかりません。");
                }
                response = handler.handle(request);
            } catch (HttpException e) {
                // コントローラが意図的に投げたエラー（404や400など）
                response = Response.html(e.status(), errorPage.apply(e.status(), e.getMessage()));
            } catch (Exception e) {
                // 想定外のエラー。詳細はサーバーのログにだけ出し、画面には内部情報を出さない
                e.printStackTrace();
                response = Response.html(500, errorPage.apply(500, "サーバーでエラーが発生しました。"));
            }
            if (request.isNewSession()) {
                ex.getResponseHeaders().add("Set-Cookie",
                        "sid=" + request.sessionId() + "; Path=/; HttpOnly; SameSite=Lax");
            }
            write(ex, response);
        } finally {
            ex.close();
        }
    }

    /** Responseの内容をHTTPとして書き出す。 */
    private static void write(HttpExchange ex, Response response) throws IOException {
        if (response.location() != null) {
            ex.getResponseHeaders().set("Location", response.location());
            ex.sendResponseHeaders(response.status(), -1); // ボディなし
            return;
        }
        byte[] bytes = response.body().getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(response.status(), bytes.length);
        ex.getResponseBody().write(bytes);
    }
}
