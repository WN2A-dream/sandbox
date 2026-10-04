package shop;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * アプリの設定。config フォルダのファイルから読み込む。
 *
 * 読み込み順（後のものが優先）:
 *   1. config/app.default.properties … 既定値。git管理。必須
 *   2. config/app.properties         … 個人用の上書き。git管理外。無くてもよい
 *
 * ファイルは実行時のカレントディレクトリからの相対パスで探すので、
 * プロジェクトのルートで実行すること。
 */
public record Config(String dbUrl, String dbUser, String dbPassword, int serverPort) {
    private static final Path DEFAULT_FILE = Path.of("config", "app.default.properties");
    private static final Path LOCAL_FILE = Path.of("config", "app.properties");

    public static Config load() throws IOException {
        Properties p = new Properties();
        read(p, DEFAULT_FILE, true);
        read(p, LOCAL_FILE, false);

        String url = "jdbc:postgresql://%s:%s/%s".formatted(
                p.getProperty("db.host"), p.getProperty("db.port"), p.getProperty("db.name"));
        return new Config(url, p.getProperty("db.user"), p.getProperty("db.password"),
                Integer.parseInt(p.getProperty("server.port")));
    }

    /** ファイルを読んで props に上書きで取り込む。required が true で無ければエラー。 */
    private static void read(Properties props, Path file, boolean required) throws IOException {
        if (!Files.exists(file)) {
            if (required) {
                throw new IOException("設定ファイルが見つかりません: " + file.toAbsolutePath()
                        + "（プロジェクトのルートフォルダで実行してください）");
            }
            return;
        }
        // 日本語のコメントを書けるよう、UTF-8として読む
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(reader);
        }
    }
}
