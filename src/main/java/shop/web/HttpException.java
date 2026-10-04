package shop.web;

/**
 * 「このHTTPステータスで応答したい」ことをコントローラから伝えるための例外。
 * 例: 存在しない商品なら new HttpException(404, "商品が見つかりません")。
 * Routerが捕まえてエラーページに変換する。
 */
public final class HttpException extends RuntimeException {
    private final int status;

    public HttpException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
