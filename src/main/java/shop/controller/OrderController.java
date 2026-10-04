package shop.controller;

import shop.repository.CartRepository;
import shop.repository.OrderRepository;
import shop.view.OrderView;
import shop.web.Request;
import shop.web.Response;

import java.util.OptionalInt;

/** 注文の確定と注文履歴を扱うコントローラ。 */
public final class OrderController extends BaseController {
    private final OrderRepository orders;

    public OrderController(OrderRepository orders, CartRepository cart) {
        super(cart);
        this.orders = orders;
    }

    /**
     * POST /checkout : カートの中身を注文として確定する。
     * 完了画面を直接返さず、注文履歴へリダイレクトする（リロードによる二重注文の防止）。
     * カートが空ならカート画面へ戻す。
     */
    public Response checkout(Request request) throws Exception {
        OptionalInt orderId = orders.place(request.sessionId());
        if (orderId.isEmpty()) {
            return Response.redirect("/cart");
        }
        return Response.redirect("/orders?placed=" + orderId.getAsInt());
    }

    /** GET /orders : 注文履歴。?placed=N があれば注文完了メッセージも表示する。 */
    public Response list(Request request) throws Exception {
        // placedは表示用の番号に過ぎず、改ざんされても履歴自体は自分のセッション分しか出ない
        OptionalInt placed = request.param("placed", "").matches("\\d{1,9}")
                ? OptionalInt.of(request.intParam("placed")) : OptionalInt.empty();
        return page(request, "注文履歴", OrderView.list(orders.list(request.sessionId()), placed));
    }
}
