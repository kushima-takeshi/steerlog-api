# 12-frontend-plan.md

# SteerLog Frontend Plan

## 目的

このドキュメントは、SteerLog のフロントエンド実装計画の正本である。  
チャットや一時メモに頼らず、ここを見て次の作業を決める。

```text
仕様の詳細（API / DB / Level） → 01〜11
フロントの進め方・現在地       → このファイル
作業チケット                   → GitHub Issue
技術決定・既知制約             → 11-technical-decisions.md
```

フロント作業を始めるときは、まずこのファイルを読む。

---

# 1. 前提

- バックエンド API（JWT 認証込み）は実装済み
- 画面イメージはルート `README.md` の UI モックを参照
- フロント実装経験が浅い前提で、小さく進める
- 最初から完成 UI を目指さない

想定スタック:

```text
React + TypeScript
Vite
React Router
fetch（最初はこれで十分）
JWT は Authorization: Bearer
開発 URL: http://localhost:5173（API CORS 許可済み）
```

リポジトリ:

```text
フロント: https://github.com/kushima-takeshi/steerlog-web（ローカル: Desktop/steerlog-web）
API:      https://github.com/kushima-takeshi/steerlog-api

※ Phase 0 の Vite 雛形は入れず、自分で作成する前提
```

---

# 2. 基本方針

```text
きれいな UI を最初から作らない
API を呼べる最小画面を作る
認証をつなぐ
一覧 → 詳細 → 学習フローの順に足す
```

やらないこと（最初の段階）:

```text
洗練されたデザインシステム
リフレッシュトークン UI
OAuth / ソーシャルログイン
オフライン対応
状態管理ライブラリの本格導入（Redux 等）
完璧なエラーハンドリング網羅
```

---

# 3. 現在地（2026-08-09）

```text
[x] バックエンド JWT 認証
[x] 手動 API 確認（認証付き）
[x] フロント用 GitHub リポジトリ作成（steerlog-web・空 + README）
[ ] Phase 0: Vite + React + TS（自分で作成）
[ ] API 疎通（トークン手貼りでも可）
[ ] 登録 / ログイン画面
[ ] リソース一覧
[ ] リソース作成
[ ] リソース詳細
[ ] 学習フロー（振り返り）
```

---

# 4. フェーズ計画

## Phase 0: プロジェクト箱

目標:

```text
Vite + React + TS でアプリが localhost:5173 で開く
```

完了条件:

- `npm create vite@latest`（または同等）で作成
- 画面に「SteerLog」と表示される

## Phase 1: API 疎通

目標:

```text
フロントから SteerLog API を呼べることを確認する
```

完了条件:

- 環境変数または定数で API Base URL（例: `http://localhost:8080`）を持つ
- ボタン1つで API を叩ける
- 最初は Postman / curl で取得した JWT を手で貼ってもよい
- レスポンス JSON を画面に表示できる

## Phase 2: 認証

目標:

```text
登録・ログインし、以降の API に Bearer JWT を付けられる
```

使う API:

```text
POST /auth/register
POST /auth/login
GET  /auth/me
```

完了条件:

- 登録画面がある
- ログイン画面がある
- JWT を保存する（学習用は `localStorage` で可）
- 認証付きリクエストヘルパーがある（`Authorization: Bearer ...`）
- 401 のときログイン画面へ戻せる
- `/auth/me` でログイン中ユーザーを表示できる

## Phase 3: 最初の縦切り（最重要マイルストーン）

目標:

```text
登録 → ログイン → 教材一覧 → 教材作成
```

使う API:

```text
GET  /resources
POST /resources
```

完了条件:

- ログイン後に教材一覧が見られる
- 教材を1件作成できる
- 作成後に一覧へ反映される（再取得で可）

**ここまでできたらフロントの土台は完了。**

## Phase 4: リソース詳細

目標:

```text
一覧から詳細へ遷移し、統合詳細を表示する
```

使う API:

```text
GET /resources/{resourceId}/details
```

完了条件:

- 一覧から詳細画面へ遷移できる
- Progress / Sections / Memos / LevelHistories など主要ブロックが表示される
- 未実装フィールドを無理に埋めない

## Phase 5: 学習フロー（後続）

モック画面順:

```text
振り返り開始
回答
確認
完了
```

使う API（概要）:

```text
POST .../learning-sessions
POST .../learning-sessions/{id}/responses
POST .../learning-sessions/{id}/complete
POST .../learning-sessions/{id}/record
```

Phase 3〜4 が安定してから着手する。

---

# 5. 今やること / 次やること

## Now（次に着手）

```text
1. Phase 0 を自分で進める（steerlog-web の README 参照）
2. Vite + React + TS で localhost:5173 を確認する
```

## Next

```text
Phase 1: API 疎通
Phase 2: 認証
Phase 3: 一覧 + 作成
```

## Later

```text
Phase 4 詳細
Phase 5 学習フロー
UI の見た目改善
Issue #56（不正 body が 401）がフロント開発中に邪魔なら API 側で修正
```

---

# 6. 作業の進め方（AI / 自分）

```text
このファイルを正として次の1ステップだけ進める
1ステップごとに動作確認する
完了したら「現在地」のチェックを更新する
大きな作業は GitHub Issue を切る
```

Cursor への依頼例:

```text
docs/12-frontend-plan.md を読んで、Phase 0 だけ進めて。
スコープを広げないで。
```

---

# 7. 関連ドキュメント

- ルート [`README.md`](../README.md) … UI モック・実装済み API
- [`03-api-design.md`](./03-api-design.md) … API 仕様
- [`09-manual-api-check.md`](./09-manual-api-check.md) … curl での動作確認
- [`11-technical-decisions.md`](./11-technical-decisions.md) … JWT 決定・既知課題
- Issue #56 … 不正リクエストが 401 になる問題

---

# 運用

- フェーズ完了時に「現在地」を更新する
- 方針を変えたらこのファイルを先に直してから実装する
- チャットで決めた次の一手は、忘れないうちに Now / Next へ書く
