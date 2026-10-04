package shop;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Main {
    static final String DB_URL = env("DB_URL", "jdbc:postgresql://localhost:5432/shop");
    static final String DB_USER = env("DB_USER", "postgres");
    static final String DB_PASSWORD = env("DB_PASSWORD", "postgres");

    public static void main(String[] args) throws Exception {
        initSchema();

        int port = Integer.parseInt(env("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        route(server, "/", Main::home);
        route(server, "/product", Main::product);
        route(server, "/cart", Main::cart);
        route(server, "/cart/add", Main::cartAdd);
        route(server, "/cart/remove", Main::cartRemove);
        route(server, "/checkout", Main::checkout);
        route(server, "/orders", Main::orders);
        server.start();
        System.out.println("http://localhost:" + port + "/");
    }

    // ---------- pages ----------

    static void home(Ctx c) throws Exception {
        if (!c.path().equals("/")) {
            c.html(404, page(c, "Not Found", "<p>ページが見つかりません。</p>"));
            return;
        }
        String q = c.param("q", "");
        StringBuilder sb = new StringBuilder("<div class='grid'>");
        try (Connection db = db();
             PreparedStatement ps = db.prepareStatement(
                     "SELECT id, name, price, emoji FROM products WHERE name ILIKE ? ORDER BY id")) {
            ps.setString(1, "%" + q + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sb.append("<a class='card' href='/product?id=").append(rs.getInt("id")).append("'>")
                  .append("<div class='emoji'>").append(esc(rs.getString("emoji"))).append("</div>")
                  .append("<div>").append(esc(rs.getString("name"))).append("</div>")
                  .append("<div class='price'>").append(yen(rs.getInt("price"))).append("</div></a>");
            }
        }
        sb.append("</div>");
        c.html(200, page(c, "商品一覧", sb.toString()));
    }

    static void product(Ctx c) throws Exception {
        try (Connection db = db();
             PreparedStatement ps = db.prepareStatement(
                     "SELECT id, name, description, price, emoji FROM products WHERE id = ?")) {
            ps.setInt(1, Integer.parseInt(c.param("id", "0")));
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                c.html(404, page(c, "Not Found", "<p>商品が見つかりません。</p>"));
                return;
            }
            String body = "<div class='detail'><div class='emoji big'>" + esc(rs.getString("emoji")) + "</div><div>"
                    + "<h1>" + esc(rs.getString("name")) + "</h1>"
                    + "<p>" + esc(rs.getString("description")) + "</p>"
                    + "<p class='price'>" + yen(rs.getInt("price")) + "</p>"
                    + "<form method='post' action='/cart/add'>"
                    + "<input type='hidden' name='product_id' value='" + rs.getInt("id") + "'>"
                    + "<button>カートに入れる</button></form></div></div>";
            c.html(200, page(c, rs.getString("name"), body));
        }
    }

    static void cartAdd(Ctx c) throws Exception {
        try (Connection db = db();
             PreparedStatement ps = db.prepareStatement(
                     "INSERT INTO cart_items (session_id, product_id, quantity) VALUES (?, ?, 1) "
                             + "ON CONFLICT (session_id, product_id) DO UPDATE SET quantity = cart_items.quantity + 1")) {
            ps.setString(1, c.sid);
            ps.setInt(2, Integer.parseInt(c.param("product_id", "0")));
            ps.executeUpdate();
        }
        c.redirect("/cart");
    }

    static void cartRemove(Ctx c) throws Exception {
        try (Connection db = db();
             PreparedStatement ps = db.prepareStatement(
                     "DELETE FROM cart_items WHERE session_id = ? AND product_id = ?")) {
            ps.setString(1, c.sid);
            ps.setInt(2, Integer.parseInt(c.param("product_id", "0")));
            ps.executeUpdate();
        }
        c.redirect("/cart");
    }

    static void cart(Ctx c) throws Exception {
        StringBuilder sb = new StringBuilder("<table><tr><th>商品</th><th>単価</th><th>数量</th><th>小計</th><th></th></tr>");
        int total = 0;
        try (Connection db = db();
             PreparedStatement ps = db.prepareStatement(
                     "SELECT p.id, p.name, p.price, ci.quantity FROM cart_items ci "
                             + "JOIN products p ON p.id = ci.product_id WHERE ci.session_id = ? ORDER BY p.id")) {
            ps.setString(1, c.sid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int sub = rs.getInt("price") * rs.getInt("quantity");
                total += sub;
                sb.append("<tr><td>").append(esc(rs.getString("name"))).append("</td><td>")
                  .append(yen(rs.getInt("price"))).append("</td><td>").append(rs.getInt("quantity"))
                  .append("</td><td>").append(yen(sub)).append("</td><td>")
                  .append("<form method='post' action='/cart/remove'><input type='hidden' name='product_id' value='")
                  .append(rs.getInt("id")).append("'><button class='sub'>削除</button></form></td></tr>");
            }
        }
        sb.append("</table>");
        if (total == 0) {
            c.html(200, page(c, "カート", "<p>カートは空です。</p>"));
            return;
        }
        sb.append("<p class='price'>合計: ").append(yen(total)).append("</p>")
          .append("<form method='post' action='/checkout'><button>注文を確定する</button></form>");
        c.html(200, page(c, "カート", sb.toString()));
    }

    static void checkout(Ctx c) throws Exception {
        int orderId;
        try (Connection db = db()) {
            db.setAutoCommit(false);
            try {
                int total;
                try (PreparedStatement ps = db.prepareStatement(
                        "SELECT COALESCE(SUM(p.price * ci.quantity), 0) FROM cart_items ci "
                                + "JOIN products p ON p.id = ci.product_id WHERE ci.session_id = ?")) {
                    ps.setString(1, c.sid);
                    ResultSet rs = ps.executeQuery();
                    rs.next();
                    total = rs.getInt(1);
                }
                if (total == 0) {
                    c.redirect("/cart");
                    return;
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO orders (session_id, total) VALUES (?, ?) RETURNING id")) {
                    ps.setString(1, c.sid);
                    ps.setInt(2, total);
                    ResultSet rs = ps.executeQuery();
                    rs.next();
                    orderId = rs.getInt(1);
                }
                try (PreparedStatement ps = db.prepareStatement(
                        "INSERT INTO order_items (order_id, product_id, quantity, price) "
                                + "SELECT ?, p.id, ci.quantity, p.price FROM cart_items ci "
                                + "JOIN products p ON p.id = ci.product_id WHERE ci.session_id = ?")) {
                    ps.setInt(1, orderId);
                    ps.setString(2, c.sid);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = db.prepareStatement("DELETE FROM cart_items WHERE session_id = ?")) {
                    ps.setString(1, c.sid);
                    ps.executeUpdate();
                }
                db.commit();
            } catch (Exception e) {
                db.rollback();
                throw e;
            }
        }
        c.html(200, page(c, "注文完了", "<p>ご注文ありがとうございました。注文番号: " + orderId
                + "</p><p><a href='/orders'>注文履歴を見る</a></p>"));
    }

    static void orders(Ctx c) throws Exception {
        StringBuilder sb = new StringBuilder("<table><tr><th>注文番号</th><th>日時</th><th>合計</th></tr>");
        try (Connection db = db();
             PreparedStatement ps = db.prepareStatement(
                     "SELECT id, total, created_at FROM orders WHERE session_id = ? ORDER BY id DESC")) {
            ps.setString(1, c.sid);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sb.append("<tr><td>").append(rs.getInt("id")).append("</td><td>")
                  .append(rs.getTimestamp("created_at")).append("</td><td>")
                  .append(yen(rs.getInt("total"))).append("</td></tr>");
            }
        }
        sb.append("</table>");
        c.html(200, page(c, "注文履歴", sb.toString()));
    }

    // ---------- layout ----------

    static String page(Ctx c, String title, String body) throws SQLException {
        int count = 0;
        try (Connection db = db();
             PreparedStatement ps = db.prepareStatement(
                     "SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE session_id = ?")) {
            ps.setString(1, c.sid);
            ResultSet rs = ps.executeQuery();
            rs.next();
            count = rs.getInt(1);
        }
        return "<!doctype html><html lang='ja'><head><meta charset='utf-8'>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                + "<title>" + esc(title) + " - Mini Shop</title><style>"
                + "body{margin:0;font-family:sans-serif;background:#f3f3f3;color:#111}"
                + "header{background:#232f3e;color:#fff;display:flex;gap:16px;align-items:center;padding:10px 20px}"
                + "header a{color:#fff;text-decoration:none}.logo{font-size:20px;font-weight:bold}"
                + "header form{flex:1;display:flex}header input{flex:1;padding:8px;border:0;border-radius:4px 0 0 4px}"
                + "header button{border-radius:0 4px 4px 0}"
                + "main{max-width:960px;margin:20px auto;padding:0 16px}"
                + ".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(200px,1fr));gap:16px}"
                + ".card{background:#fff;padding:16px;text-align:center;color:inherit;text-decoration:none;border-radius:4px}"
                + ".card:hover{box-shadow:0 2px 8px #0003}.emoji{font-size:56px}.big{font-size:120px}"
                + ".price{color:#b12704;font-weight:bold}.detail{display:flex;gap:32px;background:#fff;padding:24px;border-radius:4px}"
                + "table{width:100%;background:#fff;border-collapse:collapse}th,td{padding:10px;border-bottom:1px solid #ddd;text-align:left}"
                + "button{background:#ffd814;border:1px solid #fcd200;border-radius:4px;padding:8px 14px;cursor:pointer}"
                + "button.sub{background:#eee;border-color:#ccc}"
                + "</style></head><body><header><a class='logo' href='/'>Mini Shop</a>"
                + "<form action='/'><input name='q' placeholder='商品を検索' value='" + esc(c.param("q", "")) + "'><button>検索</button></form>"
                + "<a href='/orders'>注文履歴</a><a href='/cart'>🛒 カート (" + count + ")</a></header>"
                + "<main><h1>" + esc(title) + "</h1>" + body + "</main></body></html>";
    }

    // ---------- infrastructure ----------

    static Connection db() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    static void initSchema() throws Exception {
        String sql;
        try (var in = Main.class.getResourceAsStream("/schema.sql")) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection db = db(); Statement st = db.createStatement()) {
            st.execute(sql);
        }
    }

    interface Handler { void handle(Ctx c) throws Exception; }

    static void route(HttpServer server, String path, Handler h) {
        server.createContext(path, ex -> {
            try {
                h.handle(new Ctx(ex));
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    send(ex, 500, "text/plain; charset=utf-8", "Internal error: " + e.getMessage());
                } catch (IOException ignored) { }
            } finally {
                ex.close();
            }
        });
    }

    static void send(HttpExchange ex, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
    }

    /** Request context: query/form params and the cookie-based session id. */
    static class Ctx {
        final HttpExchange ex;
        final Map<String, String> params = new HashMap<>();
        final String sid;

        Ctx(HttpExchange ex) throws IOException {
            this.ex = ex;
            parse(ex.getRequestURI().getRawQuery());
            parse(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            String found = null;
            String cookies = ex.getRequestHeaders().getFirst("Cookie");
            if (cookies != null) {
                for (String part : cookies.split(";\\s*")) {
                    if (part.startsWith("sid=")) found = part.substring(4);
                }
            }
            if (found == null || !found.matches("[0-9a-f-]{36}")) {
                found = UUID.randomUUID().toString();
                ex.getResponseHeaders().add("Set-Cookie", "sid=" + found + "; Path=/; HttpOnly; SameSite=Lax");
            }
            this.sid = found;
        }

        private void parse(String s) {
            if (s == null || s.isEmpty()) return;
            for (String pair : s.split("&")) {
                String[] kv = pair.split("=", 2);
                params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
            }
        }

        String path() { return ex.getRequestURI().getPath(); }

        String param(String name, String def) { return params.getOrDefault(name, def); }

        void html(int status, String body) throws IOException { send(ex, status, "text/html; charset=utf-8", body); }

        void redirect(String to) throws IOException {
            ex.getResponseHeaders().set("Location", to);
            ex.sendResponseHeaders(303, -1);
        }
    }

    static String env(String key, String def) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? def : v;
    }

    static String yen(int n) { return String.format("¥%,d", n); }

    static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
