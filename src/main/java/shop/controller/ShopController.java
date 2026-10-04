package shop.controller;

import shop.model.Product;
import shop.repository.CartRepository;
import shop.repository.ProductRepository;
import shop.view.ProductView;
import shop.web.HttpException;
import shop.web.Request;
import shop.web.Response;

/** 商品の一覧・検索・詳細を扱うコントローラ。 */
public final class ShopController extends BaseController {
    private final ProductRepository products;

    public ShopController(ProductRepository products, CartRepository cart) {
        super(cart);
        this.products = products;
    }

    /** GET / : 商品一覧。?q=キーワード で絞り込める。 */
    public Response home(Request request) throws Exception {
        String keyword = request.param("q", "");
        return page(request, "商品一覧", ProductView.list(products.search(keyword)));
    }

    /** GET /product?id=N : 商品詳細。存在しないIDなら404。 */
    public Response product(Request request) throws Exception {
        Product product = products.find(request.intParam("id"))
                .orElseThrow(() -> new HttpException(404, "商品が見つかりません。"));
        return page(request, product.name(), ProductView.detail(product));
    }
}
