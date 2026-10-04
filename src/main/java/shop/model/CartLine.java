package shop.model;

/**
 * カートの1行分（モデル）。「どの商品を何個入れたか」を表す。
 *
 * @param product  カートに入れた商品
 * @param quantity 数量（1以上）
 */
public record CartLine(Product product, int quantity) {

    /** この行の小計（単価 × 数量）。 */
    public int subtotal() {
        return product.price() * quantity;
    }
}
