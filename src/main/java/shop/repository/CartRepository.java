package shop.repository;

import shop.model.CartLine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * カート（cart_itemsテーブル）の読み書きを担当する。
 * ログイン機能がないため、カートはCookieのセッションIDごとに分けて保存する。
 */
public final class CartRepository {
    private final Db db;

    public CartRepository(Db db) {
        this.db = db;
    }

    /** 商品を1個カートに入れる。すでに入っていれば数量を+1する。 */
    public void add(String sessionId, int productId) throws SQLException {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO cart_items (session_id, product_id, quantity) VALUES (?, ?, 1) "
                             + "ON CONFLICT (session_id, product_id) DO UPDATE SET quantity = cart_items.quantity + 1")) {
            ps.setString(1, sessionId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    /** 商品をカートから取り除く（数量に関わらず1行まるごと削除）。 */
    public void remove(String sessionId, int productId) throws SQLException {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM cart_items WHERE session_id = ? AND product_id = ?")) {
            ps.setString(1, sessionId);
            ps.setInt(2, productId);
            ps.executeUpdate();
        }
    }

    /** カートの中身を商品情報つきで、商品ID順に返す。 */
    public List<CartLine> lines(String sessionId) throws SQLException {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT p.id, p.name, p.description, p.price, p.emoji, ci.quantity FROM cart_items ci "
                             + "JOIN products p ON p.id = ci.product_id WHERE ci.session_id = ? ORDER BY p.id")) {
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();
            List<CartLine> list = new ArrayList<>();
            while (rs.next()) list.add(new CartLine(ProductRepository.map(rs), rs.getInt("quantity")));
            return list;
        }
    }

    /** カート内の商品の合計個数（ヘッダーのバッジ表示用）。空なら0。 */
    public int count(String sessionId) throws SQLException {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COALESCE(SUM(quantity), 0) FROM cart_items WHERE session_id = ?")) {
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }
}
