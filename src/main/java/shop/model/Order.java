package shop.model;

import java.time.OffsetDateTime;

/**
 * 注文（モデル）。ordersテーブルの1行に対応する。
 *
 * @param id        注文番号
 * @param total     合計金額（円）。注文時点の価格で確定した値
 * @param createdAt 注文日時
 */
public record Order(int id, int total, OffsetDateTime createdAt) {
}
