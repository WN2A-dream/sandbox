package shop;

import com.sun.net.httpserver.HttpServer;
import shop.controller.CartController;
import shop.controller.OrderController;
import shop.controller.ShopController;
import shop.repository.CartRepository;
import shop.repository.Db;
import shop.repository.OrderRepository;
import shop.repository.ProductRepository;
import shop.view.ErrorView;
import shop.web.Router;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * アプリの起動点。部品を作ってつなぐ（配線する）ことだけを行う。
 *
 * 全体の構成:
 *   リクエスト → Router → Controller → Repository(DB) → Model
 *                              └────→ View(HTML) ──→ レスポンス
 */
public class Main {
    public static void main(String[] args) throws Exception {
        // --- 設定（環境変数で上書き可能） ---
        Db db = new Db(
                env("DB_URL", "jdbc:postgresql://localhost:5432/shop"),
                env("DB_USER", "postgres"),
                env("DB_PASSWORD", "postgres"));
        int port = Integer.parseInt(env("PORT", "8080"));

        db.initSchema();

        // --- Repository（データアクセス） ---
        ProductRepository products = new ProductRepository(db);
        CartRepository cart = new CartRepository(db);
        OrderRepository orders = new OrderRepository(db);

        // --- Controller ---
        ShopController shop = new ShopController(products, cart);
        CartController cartController = new CartController(cart, products);
        OrderController orderController = new OrderController(orders, cart);

        // --- URLとControllerの対応表 ---
        Router router = new Router(ErrorView::page);
        router.get("/", shop::home);
        router.get("/product", shop::product);
        router.get("/cart", cartController::show);
        router.post("/cart/add", cartController::add);
        router.post("/cart/remove", cartController::remove);
        router.post("/checkout", orderController::checkout);
        router.get("/orders", orderController::list);

        // --- サーバー起動。リクエストごとに軽量スレッド（仮想スレッド）で並列処理する ---
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", router);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("http://localhost:" + port + "/");
    }

    /** 環境変数を読む。未設定または空なら既定値を返す。 */
    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return v == null || v.isEmpty() ? defaultValue : v;
    }
}
