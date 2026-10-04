package shop.view;

import shop.model.Product;

import java.util.List;

import static shop.view.Html.esc;
import static shop.view.Html.yen;

/** 商品一覧・商品詳細の本文HTMLを作る。 */
public final class ProductView {
    private ProductView() {
    }

    /** 商品一覧（カードを並べたグリッド）。 */
    public static String list(List<Product> products) {
        if (products.isEmpty()) {
            return "<p>該当する商品がありません。</p>";
        }
        StringBuilder sb = new StringBuilder("<div class='grid'>");
        for (Product p : products) {
            sb.append("<a class='card' href='/product?id=").append(p.id()).append("'>")
              .append("<div class='emoji'>").append(esc(p.emoji())).append("</div>")
              .append("<div>").append(esc(p.name())).append("</div>")
              .append("<div class='price'>").append(yen(p.price())).append("</div></a>");
        }
        return sb.append("</div>").toString();
    }

    /** 商品詳細（説明と「カートに入れる」ボタン）。 */
    public static String detail(Product p) {
        return "<div class='detail'><div class='emoji big'>" + esc(p.emoji()) + "</div><div>"
                + "<p>" + esc(p.description()) + "</p>"
                + "<p class='price'>" + yen(p.price()) + "</p>"
                + "<form method='post' action='/cart/add'>"
                + "<input type='hidden' name='product_id' value='" + p.id() + "'>"
                + "<button>カートに入れる</button></form></div></div>";
    }
}
