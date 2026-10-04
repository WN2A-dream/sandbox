package shop.repository;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * PostgreSQLへの接続を管理するクラス。
 *
 * 最小構成にするためコネクションプールは使わず、呼ばれるたびに新しい接続を開く。
 * 利用側（各Repository）は try-with-resources で必ず閉じること。
 */
public final class Db {
    private final String url;
    private final String user;
    private final String password;

    public Db(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** 新しいDB接続を開いて返す。呼び出し側が close() する。 */
    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * 起動時にテーブルとサンプル商品を作成する。
     * schema.sql は何度実行しても安全な書き方（IF NOT EXISTS 等）にしてある。
     */
    public void initSchema() throws Exception {
        String sql;
        try (var in = Db.class.getResourceAsStream("/schema.sql")) {
            sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (Connection c = open(); Statement st = c.createStatement()) {
            // PostgreSQLのJDBCドライバは、セミコロン区切りの複数文を1回で実行できる
            st.execute(sql);
        }
    }
}
