package shop.controller;

import shop.repository.CartRepository;
import shop.repository.ProductRepository;
import shop.view.CartView;
import shop.web.HttpException;
import shop.web.Request;
import shop.web.Response;

/** カートの表示・追加・削除を扱うコントローラ。 */
public final class CartController extends BaseController {
    private final CartRepository cart;
    private final ProductRepository products;

    public CartController(CartRepository cart, ProductRepository products) {
        super(cart);
        this.cart = cart;
        this.products = products;
    }

    /** GET /cart : カートの中身を表示する。 */
    public Response show(Request request) throws Exception {
        return page(request, "カート", CartView.show(cart.lines(request.sessionId())));
    }

    /** POST /cart/add : 商品を1個追加してカート画面へ。存在しない商品なら404。 */
    public Response add(Request request) throws Exception {
        int productId = request.intParam("product_id");
        // 存在しない商品をカートに入れようとするとDB制約違反（500）になるので、先に確認して404にする
        products.find(productId).orElseThrow(() -> new HttpException(404, "商品が見つかりません。"));
        cart.add(request.sessionId(), productId);
        return Response.redirect("/cart");
    }

    /** POST /cart/remove : 商品をカートから削除してカート画面へ。 */
    public Response remove(Request request) throws Exception {
        cart.remove(request.sessionId(), request.intParam("product_id"));
        return Response.redirect("/cart");
    }
}
