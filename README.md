# Mini Shop

Java + PostgreSQL で作った、最小構成のECサイト（商品一覧・検索・詳細・カート・注文・注文履歴）です。
ローカル（localhost）で動かして試せます。

## クローン後にやること

### 1. 事前に必要なもの

| もの | 備考 |
|---|---|
| JDK 21 以上 | `javac -version` で確認。PATH に通しておく |
| PostgreSQL | インストールして起動しておく（Windowsのサービスとして動いていればOK） |

Maven は不要です。（使いたい場合は後述）

### 2. データベースを作る（初回だけ）

PowerShell またはコマンドプロンプトで、プロジェクトのフォルダに移動して実行します。

```
.\setup-db.bat
```

空の `shop` データベースが作られます。すでにあれば何もしません。
テーブルとサンプル商品は、アプリの初回起動時に自動で作られます。

> PostgreSQL のパスワードを `postgres` 以外にした場合は、先に「設定を変える」を読んでください。

### 3. 起動する

```
.\run.bat
```

「`http://localhost:8080/`」と表示されたら、ブラウザで開きます。止めるときは `Ctrl+C` です。

初回は PostgreSQL のドライバ（jar）を自動でダウンロードして `lib` フォルダに置くので、インターネット接続が必要です。

### VS Code から動かす場合

Java 拡張機能（Extension Pack for Java）を入れて、`F5`（実行構成「Mini Shop」）で起動できます。
実行前に `build.bat` が自動で走ります。

### うまくいかないとき

| 症状 | 対処 |
|---|---|
| `javac not found` | JDK 21 以上を入れて PATH に通す |
| `psql not found` | 下の「psql が見つからないとき」を参照 |
| `Cannot connect to PostgreSQL` | PostgreSQL が起動しているか確認。パスワードが違うなら「設定を変える」 |
| `Address already in use` | すでに起動中。前回のサーバーを `Ctrl+C` で止めるか、ポートを変える |
| `設定ファイルが見つかりません` | プロジェクトのルートフォルダで実行する |

### psql が見つからないとき

`setup-db.bat` は、次の順番で `psql.exe` を探します。

1. `config/app.properties` の `psql.path`
2. PATH
3. レジストリ（PostgreSQL のインストール情報。インストール先がどこでも見つかる）
4. `C:\Program Files\PostgreSQL\<バージョン>\bin`

それでも見つからないときは、次のどちらかです。

- **別の場所にある**: PostgreSQL の `bin` フォルダから `psql.exe` を探し、`config/app.properties` に書く。
  ```
  psql.path=D:\PostgreSQL\18\bin\psql.exe
  ```
- **psql.exe が無い**: インストール時に「Command Line Tools」を外している。インストーラをもう一度実行して追加する。

pgAdmin など別のツールで `shop` データベースを作っても構いません。その場合は `setup-db.bat` は不要で、`.\run.bat` から始められます。

## 設定を変える

設定は `config` フォルダのファイルで管理します。

| ファイル | 役割 | git |
|---|---|---|
| `config/app.default.properties` | 既定値 | 管理する |
| `config/app.properties` | 自分用の上書き | **管理しない**（`.gitignore` 済み） |

変えたいときは、`app.default.properties` を `app.properties` にコピーして、変更したい項目だけ残して編集します。
`app.properties` に書いた項目だけが既定値より優先されます。パスワードを書いても公開されません。

例: パスワードを変えて、ポートを 3000 にする（`config/app.properties`）

```
db.password=自分のパスワード
server.port=3000
```

設定できる項目:

| キー | 既定値 | 意味 |
|---|---|---|
| `db.host` | `localhost` | PostgreSQL のホスト |
| `db.port` | `5432` | PostgreSQL のポート |
| `db.name` | `shop` | データベース名 |
| `db.user` | `postgres` | ユーザー名 |
| `db.password` | `postgres` | パスワード |
| `server.port` | `8080` | このアプリのポート |
| `psql.path` | （未設定） | `setup-db.bat` 専用。`psql.exe` が自動で見つからないときだけ指定 |

`setup-db.bat` と `run.bat` は、どちらも同じ設定ファイルを読みます。

## 構成 (MVC)

```
shop/Main                 起動と配線のみ
shop/Config               設定ファイルの読み込み
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

## バッチファイルについて

`build.bat` / `run.bat` / `setup-db.bat` は、コメントとメッセージを英語（ASCII）で書いています。
Windows の cmd.exe は、UTF-8 の日本語を含む .bat を誤って解釈することがあるためです。

- `build.bat`: ドライバのダウンロード（初回のみ）→ `javac` でコンパイル → `out/classes` に出力
- `run.bat`: `build.bat` を呼んでから起動
- `setup-db.bat`: 設定ファイルを読んで `psql` で空のDBを作る

`lib/`（ドライバ）と `out/`（ビルド結果）は git 管理外です。

## Maven で動かす場合（任意）

Maven を入れている場合は、バッチを使わず次でも起動できます。

```
mvn compile exec:java
```
