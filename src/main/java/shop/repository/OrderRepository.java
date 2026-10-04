package shop.repository;

import shop.model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/** 注文（orders / order_itemsテーブル）の登録と参照を担当する。 */
public final class OrderRepository {
    private final Db db;

    public OrderRepository(Db db) {
        this.db = db;
    }

    /**
     * カートの中身を注文に変換する。全体を1つのトランザクションで行い、
     * 途中で失敗した場合は注文もカートも元の状態に戻る。
     *
     * 合計金額は、保存した注文明細から集計して決める。
     * こうすることで、明細と合計が食い違うことがない。
     *
     * @return 作成した注文番号。カートが空で注文できなかった場合は空
     */
    public OptionalInt place(String sessionId) throws SQLException {
        try (Connection c = db.open()) {
            c.setAutoCommit(false); // ここからトランザクション開始
            try {
                // 1. 注文の「頭」を作る（合計は後で確定させるので仮に0）
                int orderId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO orders (session_id, total) VALUES (?, 0) RETURNING id")) {
                    ps.setString(1, sessionId);
                    ResultSet rs = ps.executeQuery();
                    rs.next();
                    orderId = rs.getInt(1);
                }

                // 2. カートの中身を注文明細へコピーする。価格は「注文時点」の商品価格を保存する
                int lines;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO order_items (order_id, product_id, quantity, price) "
                                + "SELECT ?, p.id, ci.quantity, p.price FROM cart_items ci "
                                + "JOIN products p ON p.id = ci.product_id WHERE ci.session_id = ?")) {
                    ps.setInt(1, orderId);
                    ps.setString(2, sessionId);
                    lines = ps.executeUpdate();
                }
                if (lines == 0) {
                    // カートが空だった。作りかけの注文を取り消す
                    c.rollback();
                    return OptionalInt.empty();
                }

                // 3. 保存した明細から合計金額を集計して確定する
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE orders SET total = (SELECT SUM(price * quantity) FROM order_items WHERE order_id = ?) "
                                + "WHERE id = ?")) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, orderId);
                    ps.executeUpdate();
                }

                // 4. 注文済みなのでカートを空にする
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM cart_items WHERE session_id = ?")) {
                    ps.setString(1, sessionId);
                    ps.executeUpdate();
                }

                c.commit();
                return OptionalInt.of(orderId);
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }

    /** そのセッションの注文履歴を、新しい順に返す。 */
    public List<Order> list(String sessionId) throws SQLException {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, total, created_at FROM orders WHERE session_id = ? ORDER BY id DESC")) {
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();
            List<Order> list = new ArrayList<>();
            while (rs.next()) {
                list.add(new Order(rs.getInt("id"), rs.getInt("total"),
                        rs.getObject("created_at", OffsetDateTime.class)));
            }
            return list;
        }
    }
}
