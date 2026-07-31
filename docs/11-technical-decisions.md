# 11-technical-decisions.md

# SteerLog Technical Decisions / Known Improvements

## 目的

このドキュメントは、SteerLog MVP における **技術的な決定** と **既知の制約・改善点** をまとめる正本である。

```text
仕様の詳細 → 01〜10 の各設計 docs
作業チケット → GitHub Issue（着手時に切る）
決定・制約・将来改善 → このファイル
```

AI 実装時も、既存仕様に加えてこのファイルを参照する。  
Issue にだけ書かず、ここに残す。

---

# 書き方

各項目は次の4項目だけ書く。

```text
Context（背景）
Decision（決定）
Consequences（既知の制約）
Future（改善点・将来やること）
```

完璧な ADR 番号管理は不要。日付と短いタイトルで足していく。

---

# 決定一覧

## 1. タグ保存形式: API は List、DB は TEXT + カンマ（2026-07）

### Context

学習証跡の軽いラベル付けが必要。  
`LearningSessionRecord.conceptTags` は既に API `List<String>` / DB `TEXT`（カンマ区切り）で実装済み。  
StudyMemo の `tags` も同様の軽量が求められ、タグ正規化テーブルは MVP 外（`01-mvp-scope` / `02-db-design`）。

### Decision

- Request / Response: `List<String>`
- DB: `TEXT`（例: `"HTTP,REST"`）。空は `null`
- StudyMemo `tags` も `conceptTags` と同じ format / parse 方針に揃える
- タグマスタ・中間テーブルは作らない

### Consequences

- タグ名にカンマを含められない
- タグ単位の検索・集計は弱い（`LIKE` / アプリ側パース前提）
- DB 上は配列型ではない

### Future

- タグ正規化テーブル（例: `tags` / `study_memo_tags`）への移行を Galaxy 前などに再検討
- タグでの一覧フィルタ・検索 API
- 着手時に GitHub Issue を切る

関連: `docs/01-mvp-scope.md` §4.5、`docs/02-db-design.md`、`LearningSessionService` の format/parse

---

## 2. 認証未実装: TEMP_USER_ID 固定（2026-07）

### Context

MVP ではまず学習証跡の縦切りを優先した。認可・ログインより Resource / Progress / Session の流れを先に固める判断。

### Decision

- 認証は未実装
- Controller では `TEMP_USER_ID = 1L` 固定で user を扱う
- テーブルには `user_id` を持ち、将来の認可に備える

### Consequences

- マルチユーザーでは使えない
- 認可バグが本番相当では致命傷になる（現状は単一ユーザー前提）

### Future

- 認証・認可の導入（MVP Next）
- 導入後も「自分の Resource 以外は触れない」チェックは必須（`06-implementation-rules`）

関連: ルート `README.md`、`docs/06-implementation-rules.md`

---

## 3. StudyMemo に important フラグを持たせない（2026-07）

### Context

メモの重要度フラグ需要はあるが、MVP では短い生メモに留め、重要度軸を増やさない方針（`06-implementation-rules` §8.3）。

### Decision

- `study_memos.important` は持たせない
- API にも `important` を追加しない

### Consequences

- 「重要メモだけ一覧」などの UX は後回し
- README 等で `important` を未実装と書かない（**MVP 外**として扱う）

### Future

- プロダクトとして必要になったら改めて設計（タグやピン留め等の代替案も含む）

関連: `docs/06-implementation-rules.md` §8.3、`docs/01-mvp-scope.md`

---

## 4. MVP 外として先送りしている改善候補（2026-07）

### Context

プロダクト思想と MVP 境界を守るため、拡張は意図的に後回しにしている。

### Decision

以下は **今作らない**（詳細は各設計 docs）。

```text
タグ正規化テーブル本格実装
タグ検索 API
Lv.4 / Lv.5
Galaxy / MCP
AI 連携（動的 aiPrompt / resultDraft）
学習時間管理 / Pomodoro
LearningCycle / 再学習軸
```

### Consequences

- 実装 AI が勝手に上記を足さないよう、`01` / `06` / このファイルを参照する

### Future

- 必要になった項目だけ Design / ADR 節を追加し、Issue を切って着手する

関連: `docs/01-mvp-scope.md`、`docs/README.md` の注意欄

---

# 運用

- 新しいトレードオフや暫定実装が出たら、このファイルに節を足す
- 仕様の正は引き続き `01`〜`10`。ここは「なぜそうなっているか / 何が弱いか」のメモ
- 改善に着手するタイミングで Issue を作り、該当節へリンクする
