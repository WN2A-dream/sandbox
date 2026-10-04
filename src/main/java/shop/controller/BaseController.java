package shop.controller;

import shop.repository.CartRepository;
import shop.view.Layout;
import shop.web.Request;
import shop.web.Response;

import java.sql.SQLException;

/** 各コントローラの共通部分。「共通の枠つきでページを返す」処理を持つ。 */
abstract class BaseController {
    private final CartRepository cart;

    BaseController(CartRepository cart) {
        this.cart = cart;
    }

    /**
     * 共通レイアウトに本文を埋め込んで、200 OK のレスポンスにする。
     * ヘッダーのカート個数バッジのため、ここでカート件数を取得する。
     */
    Response page(Request request, String title, String body) throws SQLException {
        int cartCount = cart.count(request.sessionId());
        return Response.html(200, Layout.page(title, request.param("q", ""), cartCount, body));
    }
}
