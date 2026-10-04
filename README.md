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
