package shop.view;

import shop.model.CartLine;

import java.util.List;

import static shop.view.Html.esc;
import static shop.view.Html.yen;

/** カート画面の本文HTMLを作る。 */
public final class CartView {
    private CartView() {
    }

    /** カートの明細表・合計金額・注文ボタン。カートが空ならメッセージだけ。 */
    public static String show(List<CartLine> lines) {
        if (lines.isEmpty()) {
            return "<p>カートは空です。</p>";
        }
        StringBuilder sb = new StringBuilder(
                "<table><tr><th>商品</th><th>単価</th><th>数量</th><th>小計</th><th></th></tr>");
        int total = 0;
        for (CartLine line : lines) {
            total += line.subtotal();
            sb.append("<tr><td>").append(esc(line.product().name())).append("</td><td>")
              .append(yen(line.product().price())).append("</td><td>").append(line.quantity())
              .append("</td><td>").append(yen(line.subtotal())).append("</td><td>")
              .append("<form method='post' action='/cart/remove'><input type='hidden' name='product_id' value='")
              .append(line.product().id()).append("'><button class='sub'>削除</button></form></td></tr>");
        }
        return sb.append("</table>")
                .append("<p class='price'>合計: ").append(yen(total)).append("</p>")
                .append("<form method='post' action='/checkout'><button>注文を確定する</button></form>")
                .toString();
    }
}
