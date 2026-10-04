package shop.model;

/**
 * 商品（モデル）。productsテーブルの1行に対応する。
 *
 * @param id          商品ID
 * @param name        商品名
 * @param description 商品説明
 * @param price       価格（円・税込）
 * @param emoji       商品画像の代わりに表示する絵文字
 */
public record Product(int id, String name, String description, int price, String emoji) {
}
