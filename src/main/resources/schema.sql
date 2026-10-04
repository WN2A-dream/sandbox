CREATE TABLE IF NOT EXISTS products (
  id          SERIAL PRIMARY KEY,
  name        TEXT    NOT NULL,
  description TEXT    NOT NULL,
  price       INTEGER NOT NULL,
  emoji       TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS cart_items (
  session_id TEXT    NOT NULL,
  product_id INTEGER NOT NULL REFERENCES products(id),
  quantity   INTEGER NOT NULL CHECK (quantity > 0),
  PRIMARY KEY (session_id, product_id)
);

CREATE TABLE IF NOT EXISTS orders (
  id         SERIAL PRIMARY KEY,
  session_id TEXT        NOT NULL,
  total      INTEGER     NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS order_items (
  order_id   INTEGER NOT NULL REFERENCES orders(id),
  product_id INTEGER NOT NULL REFERENCES products(id),
  quantity   INTEGER NOT NULL,
  price      INTEGER NOT NULL,
  PRIMARY KEY (order_id, product_id)
);

INSERT INTO products (name, description, price, emoji)
SELECT * FROM (VALUES
  ('ワイヤレスイヤホン', 'ノイズキャンセリング対応。最大24時間再生。', 8980, '🎧'),
  ('機械式キーボード', '青軸・日本語配列。打鍵感のよいキーボード。', 12800, '⌨️'),
  ('モバイルバッテリー', '10000mAh。USB-C急速充電対応。', 3480, '🔋'),
  ('デスクライト', '調光・調色できるLEDライト。', 4580, '💡'),
  ('ステンレスボトル', '保冷保温。500ml。', 2480, '🥤'),
  ('リュックサック', '15.6インチPC収納可。撥水生地。', 6980, '🎒'),
  ('Javaの本', 'はじめてのJavaプログラミング。', 2970, '📘'),
  ('コーヒー豆 200g', '深煎り。挽き立ての香り。', 1280, '☕')
) AS v(name, description, price, emoji)
WHERE NOT EXISTS (SELECT 1 FROM products);
