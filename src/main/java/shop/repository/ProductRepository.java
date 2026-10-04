package shop.repository;

import shop.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** 商品（productsテーブル）の検索を担当する。SQLはこのクラスに閉じ込める。 */
public final class ProductRepository {
    private static final String COLUMNS = "id, name, description, price, emoji";

    private final Db db;

    public ProductRepository(Db db) {
        this.db = db;
    }

    /**
     * 商品名にキーワードを含む商品をID順に返す（大文字小文字は区別しない）。
     * キーワードが空文字なら全商品が返る。
     */
    public List<Product> search(String keyword) throws SQLException {
        // LIKEの特殊文字（\ % _）をエスケープし、入力をそのままの文字として検索する
        String pattern = "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT " + COLUMNS + " FROM products WHERE name ILIKE ? ORDER BY id")) {
            ps.setString(1, pattern);
            ResultSet rs = ps.executeQuery();
            List<Product> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        }
    }

    /** IDで商品を1件取得する。存在しなければ空のOptionalを返す。 */
    public Optional<Product> find(int id) throws SQLException {
        try (Connection c = db.open();
             PreparedStatement ps = c.prepareStatement("SELECT " + COLUMNS + " FROM products WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? Optional.of(map(rs)) : Optional.empty();
        }
    }

    /** 結果セットの現在行をProductに変換する（CartRepositoryからも使う）。 */
    static Product map(ResultSet rs) throws SQLException {
        return new Product(rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                rs.getInt("price"), rs.getString("emoji"));
    }
}
