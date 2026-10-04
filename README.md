# Mini Shop

Java + PostgreSQL の最小ECサイト (商品一覧 / 詳細 / 検索 / カート / 注文)。

## 起動

1. PostgreSQL を起動し、DB を作成: `CREATE DATABASE shop;`
2. `mvn compile exec:java`
3. http://localhost:8080/ を開く

テーブルとサンプル商品は起動時に自動作成されます。

## 設定 (環境変数)

| 変数 | 既定値 |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/shop` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `PORT` | `8080` |

## 構成 (MVC)

```
shop/Main                 起動と配線のみ
shop/web/*                Router, Request, Response, HttpException (HTTPの入出力)
shop/controller/*         リクエストを受けてRepositoryとViewを呼ぶ
shop/model/*              Product, CartLine, Order (データの型)
shop/repository/*         SQLはここだけ
shop/view/*               HTMLはここだけ
```

| URL | 内容 |
|---|---|
| `GET /` , `GET /?q=` | 商品一覧・検索 |
| `GET /product?id=` | 商品詳細 |
| `GET /cart` | カート |
| `POST /cart/add` , `POST /cart/remove` | カートの追加・削除 |
| `POST /checkout` | 注文確定 |
| `GET /orders` | 注文履歴 |
