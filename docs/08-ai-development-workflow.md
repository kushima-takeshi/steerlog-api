# 08-ai-development-workflow.md

# SteerLog AI Development Workflow

## 目的

このドキュメントは、SteerLog MVPをAIコード生成ツールと併用して開発するための運用ルールを定義する。

Cursor、Claude Code、ChatGPT、Markdown設計書をどのように使い分けるかを明確にする。

---

# 1. 基本方針

SteerLog開発では、全部を手書きする必要はない。  
一方で、全部をAI任せにしてはいけない。

基本方針：

```text
設計・判断・レビューは自分で行う
実装のひな形・定型コード・テストの叩き台はAIに生成させる
生成されたコードは必ず自分で説明できる状態まで読む・直す
```

---

# 2. 役割分担

## 2.1 自分が主導するもの

```text
プロダクト思想
MVP境界
DB分割
API責務
Level更新ルール
LearningSession状態遷移
認可・削除・トランザクション方針
レビュー観点
面接で説明する内容
```

## 2.2 AIに任せやすいもの

```text
Flyway SQLの叩き台
Entity
Repository
DTO
Controller雛形
Service雛形
Mapper
バリデーション
単体テストの叩き台
エラーレスポンス共通化
READMEやコメントの叩き台
```

## 2.3 自分が特にレビューするもの

```text
Service層の業務ロジック
Level更新処理
LearningSession状態遷移
認可チェック
deleted_at条件
トランザクション境界
例外設計
テストケース
```

---

# 3. 推奨ツール構成

## 3.1 ChatGPT

主な役割：

```text
設計・方針整理
DB/API/Level/Sessionの判断
実装順序の整理
面接説明の整理
Markdown仕様書作成
```

ChatGPTでは、プロダクト設計・実装方針・レビュー観点を整理する。

---

## 3.2 Cursor

主な役割：

```text
メイン開発環境
ファイル構成を見ながら実装
AIに小さな単位でコード生成させる
生成コードを見ながら修正する
Spring Bootプロジェクト全体を把握しながら進める
```

SteerLogではCursorをメインのAIエディタとして使うのがよい。

---

## 3.3 Claude Code

主な役割：

```text
ターミナル中心の作業
複数ファイルの一括修正
テスト実行
エラー修正
リファクタ
既存コードの構成確認
```

Cursorで見ながら進めるのが基本。  
大きめの変更やテスト実行込みの作業ではClaude Codeも併用する。

---

# 4. Markdown設計書の使い方

AIコード生成には、日付別メモをそのまま読ませない。

AIに読ませるのは最新版docs。

```text
docs/
  00-product-principles.md
  01-mvp-scope.md
  02-db-design.md
  03-api-design.md
  04-level-rules.md
  05-learning-session-flow.md
  06-implementation-rules.md
  07-implementation-order.md
  08-ai-development-workflow.md
```

日付別メモは以下に置く。

```text
docs/archive/
```

archiveは判断履歴であり、AIコード生成の標準入力にはしない。

---

# 5. AIに読ませる順番

実装AIに最初に読ませる基本セット：

```text
00-product-principles.md
01-mvp-scope.md
06-implementation-rules.md
07-implementation-order.md
```

DB実装時：

```text
02-db-design.md
06-implementation-rules.md
```

API実装時：

```text
03-api-design.md
06-implementation-rules.md
```

Level実装時：

```text
04-level-rules.md
06-implementation-rules.md
```

LearningSession実装時：

```text
05-learning-session-flow.md
04-level-rules.md
06-implementation-rules.md
```

---

# 6. AIへの依頼ルール

## 6.1 いきなり実装させない

悪い例：

```text
SteerLog MVPを全部実装して
```

良い例：

```text
まず docs/00-product-principles.md, docs/01-mvp-scope.md, docs/02-db-design.md を読んでください。
まだコードは変更しないでください。
resources/progresses実装に必要なファイル一覧と実装方針だけ提案してください。
```

---

## 6.2 まず計画を出させる

最初にやること：

```text
対象docsを読ませる
変更対象ファイル一覧を出させる
実装方針を出させる
まだコードは変更させない
```

---

## 6.3 小さい単位で実装させる

例：

```text
まずFlyway migrationだけ作成してください。
EntityやControllerはまだ作らないでください。
```

次：

```text
Resource Entity と Progress Entityだけ作成してください。
ServiceやControllerはまだ作らないでください。
```

次：

```text
POST /resources のServiceだけ作成してください。
Resource作成とProgress作成を同一トランザクションで行ってください。
```

---

## 6.4 テストも作らせる

例：

```text
POST /resources のServiceテストを作成してください。
確認したいこと:
- Resourceが作成される
- ProgressがNOT_STARTEDで同時作成される
- user_idが保存される
```

---

# 7. AI生成コードのレビュー観点

AIが生成したコードは、必ず以下を確認する。

```text
user_idで所有者チェックしているか
deleted_at is null を見ているか
Resource作成とProgress作成が同一トランザクションか
Section作成とSectionStudyStatus作成が同一トランザクションか
currentLevelを外部から直接更新していないか
StudyMemo作成でLevelを上げていないか
LevelHistoryを重複作成していないか
LearningSessionRecord保存前確認を壊していないか
OFF_TOPICを保存可能にしていないか
raw回答ログをDBに正式保存していないか
MVP外のテーブル/APIを勝手に作っていないか
```

---

# 8. コミット単位

コミットは小さく分ける。

例：

```text
docs: add product principles and MVP scope
db: add resources and progresses migrations
feat: implement resource creation with progress
test: add resource creation service tests
db: add sections and section study statuses
feat: implement section creation
feat: implement section study status update
feat: implement initial study completion
```

---

# 9. 開発ログの残し方

機能単位で、以下をMarkdownに残すとよい。

```text
今回作ったAPI
更新するテーブル
トランザクション境界
認可チェック
失敗時のエラー
AI生成コードの修正点
設計判断
```

これは転職時の説明材料にもなる。

---

# 10. AIに渡すプロンプト例

## 10.1 DB migration

```text
docs/00-product-principles.md, docs/01-mvp-scope.md, docs/02-db-design.md, docs/06-implementation-rules.md を読んでください。

まず resources と progresses のFlyway migrationだけ作成してください。
Entity, Repository, Service, Controllerはまだ作らないでください。

要件:
- PostgreSQL
- BIGSERIAL主キー
- user_id必須
- resourcesはdeleted_atあり
- progressesはresource_idにFK
- user_id + resource_idをUNIQUE
- current_levelは0〜5
- statusはNOT_STARTED / IN_PROGRESS / PAUSED / ARCHIVED / COMPLETEDのみ
```

## 10.2 Service実装

```text
POST /resources のServiceを実装してください。

要件:
- Resource作成とProgress作成を同一トランザクションで行う
- Progress初期値はstatus=NOT_STARTED, currentLevel=0
- userIdは引数で受け取る
- currentLevelは外部入力から受け取らない
- まだControllerは変更しないでください
```

## 10.3 Review依頼

```text
生成したコードをレビューしてください。
特に以下を確認してください。

- user_idで所有者チェックしているか
- deleted_at対象の取得でdeleted_at is nullを見ているか
- currentLevelを外部から更新していないか
- トランザクション境界は正しいか
- docs/06-implementation-rules.mdに反していないか
```

---

# 11. 手書きとAI生成のバランス

目安：

```text
設計: 自分 70% / AI 30%
実装雛形: 自分 20% / AI 80%
業務ロジック: 自分 60% / AI 40%
テスト: 自分 50% / AI 50%
レビュー: 自分 80% / AI 20%
ドキュメント: 自分 50% / AI 50%
```

---

# 12. 面接での説明方針

AIを使ったことは隠さなくてよい。

説明例：

```text
実装ではAIコーディングツールも使いましたが、設計判断、DB分割、状態遷移、Level更新ルール、トランザクション境界、テスト観点は自分で定義しました。

AIにはEntityやDTO、Controller雛形、テストの叩き台などを生成させ、生成物は認可・削除条件・Level更新の整合性を中心にレビューしました。
```

---

# 13. まとめ

SteerLog開発では、AIを単なる自動生成ツールではなく、実装速度を上げる補助として使う。

ただし、以下は自分が握る。

```text
何を作るか
なぜその設計にするか
どこまでをMVPにするか
どの状態遷移を許可するか
どのデータを正式証跡にするか
どのコードを採用するか
```

AIを使っても、設計と品質を崩さず、自分で説明できる状態にすることを重視する。
