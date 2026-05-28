# SteerLog Docs README

## 目的

この `docs/` ディレクトリは、SteerLog MVPを実装するための最新版仕様書を管理する場所である。

Cursor、Claude Code、ChatGPTなどのAI支援ツールにコード生成を依頼する場合は、原則としてこの `docs/` 配下の最新版ファイルを読ませる。

日付別の設計メモは、判断履歴として `docs/archive/` に保存する。  
AIコード生成に直接読ませる標準入力にはしない。

---

# docs構成

```text
docs/
  README.md
  00-product-principles.md
  01-mvp-scope.md
  02-db-design.md
  03-api-design.md
  04-level-rules.md
  05-learning-session-flow.md
  06-implementation-rules.md
  07-implementation-order.md
  08-ai-development-workflow.md

docs/archive/
  README.md
  2026-05-02-design-notes.md
  2026-05-03-design-notes.md
  2026-05-04-design-notes.md
  2026-05-05-design-notes.md
  2026-05-06-design-notes.md
  2026-05-10-design-notes.md
  2026-05-16-design-notes.md
  2026-05-17-design-notes.md
  2026-05-26-design-notes.md
```

---

# 各ファイルの役割

## 00-product-principles.md

SteerLogのプロダクト思想を定義する。

主な内容：

```text
SteerLogは学習証跡アプリ
学習時間管理アプリではない
NotebookLM代替ではない
AIクイズアプリではない
Levelは理解保証ではなく証跡段階
AI生成証跡は保存前にユーザー確認する
raw回答ログを正式保存しない
```

AIに最初に読ませるべきファイル。

---

## 01-mvp-scope.md

MVPで作るもの・作らないものを定義する。

主な内容：

```text
MVP対象
MVP外
旧方針として採用しないもの
最初のMVP到達ライン
```

AIがMVP外の機能を勝手に作らないようにするためのファイル。

---

## 02-db-design.md

MVPのDB設計を定義する。

主な内容：

```text
PostgreSQL方針
ID方針
datetime方針
削除方針
主要8テーブル
制約
Index
Flyway作成順
```

Flyway migration、Entity、Repository作成時に読ませる。

---

## 03-api-design.md

MVPのAPI仕様を定義する。

主な内容：

```text
Resource API
ResourceSection API
Progress API
SectionStudyStatus API
StudyMemo API
LearningSession API
LevelHistory API
作らないAPI
```

Controller、Request DTO、Response DTO、Service作成時に読ませる。

---

## 04-level-rules.md

Lv.1〜Lv.3の意味と到達条件を定義する。

主な内容：

```text
Lv.1 = 初回学習済み
Lv.2 = Immediate Reflection証跡あり
Lv.3 = Delayed Recall証跡あり
NEEDS_REVIEWの扱い
OFF_TOPICの扱い
LevelHistory作成条件
currentLevel直接更新禁止
```

Level更新処理やテスト作成時に読ませる。

---

## 05-learning-session-flow.md

LearningSession / LearningSessionRecordのフローを定義する。

主な内容：

```text
IN_PROGRESS
COMPLETED
RECORD_SAVED
DISCARDED
responses API
complete API
resultDraft
保存前確認
record保存
discard
rawログ非保存
```

LearningSession実装時に読ませる。

---

## 06-implementation-rules.md

実装時の共通ルールを定義する。

主な内容：

```text
user_id認可
deleted_at条件
トランザクション境界
currentLevel更新禁止
LearningSessionRecord直接CRUD禁止
LevelHistory直接CRUD禁止
AI生成コードのレビュー観点
```

AIコード生成時には必ず読ませる。

---

## 07-implementation-order.md

MVPの実装順序を定義する。

主な内容：

```text
Phase 1: プロジェクト土台
Phase 2: Resource + Progress
Phase 3: ResourceSection + SectionStudyStatus
Phase 4: Lv.1 + LevelHistory
Phase 5: StudyMemo
Phase 6: LearningSession
Phase 7: LearningSessionRecord + Lv.2/Lv.3
Phase 8: 一覧・詳細整理
```

タスク分割やスプリント計画に使う。

---

## 08-ai-development-workflow.md

AIコード生成ツールを使った開発運用を定義する。

主な内容：

```text
ChatGPT / Cursor / Claude Codeの使い分け
AIに読ませるdocs
AIへの依頼ルール
小さい単位で実装させる
AI生成コードのレビュー観点
コミット単位
面接での説明方針
```

CursorやClaude Codeに作業を依頼する前に読む。

---

# AIに読ませる基本セット

## 最初に必ず読ませる

```text
docs/00-product-principles.md
docs/01-mvp-scope.md
docs/06-implementation-rules.md
docs/07-implementation-order.md
```

---

## DB実装時

```text
docs/00-product-principles.md
docs/01-mvp-scope.md
docs/02-db-design.md
docs/06-implementation-rules.md
```

---

## API実装時

```text
docs/00-product-principles.md
docs/01-mvp-scope.md
docs/03-api-design.md
docs/06-implementation-rules.md
```

---

## Level実装時

```text
docs/04-level-rules.md
docs/06-implementation-rules.md
```

---

## LearningSession実装時

```text
docs/04-level-rules.md
docs/05-learning-session-flow.md
docs/06-implementation-rules.md
```

---

# AIコード生成時の基本プロンプト

```text
まず docs/00-product-principles.md, docs/01-mvp-scope.md, docs/06-implementation-rules.md, docs/07-implementation-order.md を読んでください。

まだコードは変更しないでください。
対象機能の実装に必要なファイル一覧と実装方針だけ提案してください。
```

実装に進む場合：

```text
方針OKです。
まずFlyway migrationだけ作成してください。
Entity, Repository, Service, Controllerはまだ作らないでください。
```

---

# archiveの扱い

`docs/archive/` には、日付別の設計メモを保存する。

archiveは以下の用途で使う。

```text
判断履歴の確認
古い方針との差分確認
READMEや面接説明の材料
なぜその設計になったかの根拠確認
```

原則として、AIコード生成にはarchiveを直接読ませない。  
必要な場合のみ、特定の日付メモを補足資料として読ませる。

---

# MVP実装時の最初のゴール

最初の縦切りは以下。

```text
POST /resources
GET /resources/{resourceId}
POST /resources/{resourceId}/sections
PATCH /resources/{resourceId}/sections/{sectionId}/study-status
POST /resources/{resourceId}/progress/complete-initial-study
GET /resources/{resourceId}/level-histories
```

この縦切りで、以下が実現できる。

```text
Resourceを登録する
Progressが自動作成される
Sectionを追加する
Section学習状態を更新する
ResourceをLv.1にする
LevelHistoryを確認する
```

---

# 注意

AIコード生成では、以下を作らせない。

```text
CheckRecord
ReviewRecord
Answer API
AIクイズ中心設計
Lv.4
Lv.5
Galaxy API
MCP
外部LLMログ全文保存
学習時間/Pomodoro連携
タグ正規化テーブル本格実装
LearningCycle / 再学習軸
```

これらはMVP外または旧方針である。
