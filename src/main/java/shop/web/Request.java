package shop.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * HTTPリクエストをコントローラ向けに扱いやすくしたもの。
 * クエリ文字列・フォームの値と、Cookieによるセッションを提供する。
 */
public final class Request {
    /** POSTボディの最大サイズ。巨大な入力でメモリを使い切られないための上限。 */
    private static final int MAX_BODY_BYTES = 64 * 1024;
    private static final String SESSION_COOKIE = "sid";

    private final String method;
    private final String path;
    private final Map<String, String> params = new HashMap<>();
    private final String sessionId;
    private final boolean newSession;

    Request(HttpExchange ex) throws IOException {
        this.method = ex.getRequestMethod();
        this.path = ex.getRequestURI().getPath();

        // ?q=...&id=... のようなクエリ文字列
        parseForm(ex.getRequestURI().getRawQuery());
        // POSTのフォーム送信（application/x-www-form-urlencoded）。サイズ上限つきで読む
        if (method.equals("POST")) {
            parseForm(new String(ex.getRequestBody().readNBytes(MAX_BODY_BYTES), StandardCharsets.UTF_8));
        }

        // Cookieからセッションを復元する。無い・形式が不正なら新しく発行する
        String found = null;
        String cookies = ex.getRequestHeaders().getFirst("Cookie");
        if (cookies != null) {
            for (String part : cookies.split(";\\s*")) {
                if (part.startsWith(SESSION_COOKIE + "=")) found = part.substring(SESSION_COOKIE.length() + 1);
            }
        }
        if (found != null && found.matches("[0-9a-f-]{36}")) {
            this.sessionId = found;
            this.newSession = false;
        } else {
            this.sessionId = UUID.randomUUID().toString();
            this.newSession = true;
        }
    }

    /** "a=1&b=2" 形式の文字列を解析して params に入れる。 */
    private void parseForm(String s) {
        if (s == null || s.isEmpty()) return;
        for (String pair : s.split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    /** このブラウザを識別するセッションID。カートや注文履歴の持ち主の判定に使う。 */
    public String sessionId() {
        return sessionId;
    }

    /** セッションIDをこのリクエストで新規発行したか（trueならCookieを送る必要がある）。 */
    boolean isNewSession() {
        return newSession;
    }

    /** 文字列パラメータを取得する。無ければ既定値を返す。 */
    public String param(String name, String defaultValue) {
        return params.getOrDefault(name, defaultValue);
    }

    /** 整数パラメータを取得する。無い・数値でない場合は 400 Bad Request にする。 */
    public int intParam(String name) {
        try {
            return Integer.parseInt(params.getOrDefault(name, ""));
        } catch (NumberFormatException e) {
            throw new HttpException(400, "パラメータ「" + name + "」が正しくありません。");
        }
    }
}
