package shop.view;

import shop.model.Order;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.OptionalInt;

import static shop.view.Html.yen;

/** 注文履歴画面の本文HTMLを作る。 */
public final class OrderView {
    private OrderView() {
    }

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 注文履歴の表。
     *
     * @param orders 表示する注文（新しい順）
     * @param placed 今まさに確定した注文番号。あれば先頭に「注文完了」メッセージを出す
     */
    public static String list(List<Order> orders, OptionalInt placed) {
        StringBuilder sb = new StringBuilder();
        if (placed.isPresent()) {
            sb.append("<div class='notice'>ご注文ありがとうございました。注文番号: ")
              .append(placed.getAsInt()).append("</div>");
        }
        if (orders.isEmpty()) {
            return sb.append("<p>注文履歴はありません。</p>").toString();
        }
        sb.append("<table><tr><th>注文番号</th><th>日時</th><th>合計</th></tr>");
        for (Order o : orders) {
            sb.append("<tr><td>").append(o.id()).append("</td><td>")
              .append(DATE_TIME.format(o.createdAt().atZoneSameInstant(ZoneId.systemDefault())))
              .append("</td><td>").append(yen(o.total())).append("</td></tr>");
        }
        return sb.append("</table>").toString();
    }
}
