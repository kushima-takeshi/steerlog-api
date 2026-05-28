# SteerLog MVP設計メモ：2026-05-26

## 位置づけ

このメモは、2026-05-26に行ったSteerLog MVP設計・実装方針の整理内容をまとめたもの。  
これまで日付ごとに管理してきた設計メモと同じく、今回の議論を一度単一ファイルに集約する。

今後、このメモをもとに、AIコード生成用の `docs/` 構成へ再編する。

---

## 今日の主なテーマ

- MVP DB設計初版の整理
- 主要8テーブルの責務整理
- ProgressのARCHIVED扱いの再確認
- LearningSession / LearningSessionRecord の保存前確認フロー
- MVP API一覧
- Spring Bootでの実装順序
- AIコード生成を前提にした開発スタイル
- Cursor / Claude Code / Markdown設計書の運用方針
- 既存MarkdownをAIコード生成用docsへ再編する方針

---

# 1. MVP DB設計の対象

MVPでは以下の8テーブルを中心にする。

```text
resources
resource_sections
progresses
section_study_statuses
study_memos
learning_sessions
learning_session_records
level_histories
```

この8テーブルで、SteerLogのMVPに必要な以下の機能を表現する。

```text
Resource登録
Section管理
Resource単位のProgress
Section単位のStudyStatus
短いStudyMemo
LearningSessionによるReflection / Recall
LearningSessionRecordによる正式証跡
LevelHistoryによるLevel初到達履歴
```

MVPではLv.1〜Lv.3までを対象にする。  
Lv.4 Artifact、Lv.5 Defense、Galaxy API、再学習サイクル、MCP、外部LLMログ連携はMVP外とする。

---

# 2. resources

## 役割

`resources` は、ユーザーが登録する学習対象を表す。

対象例：

```text
本
記事
動画
講座
問題
実装課題
ドキュメント
その他
```

## カラム案

```text
resource_id
user_id
resource_type
title
author
source_url
description
deleted_at
created_at
updated_at
```

## 判断

- `resources` は学習対象そのものを表す。
- `Progress` とは分ける。
- 同じタイトル・著者のResource重複はDB制約では禁止しない。
- 重複検知は将来的にUI警告で対応する。
- `deleted_at` による論理削除を採用する。

---

# 3. resource_sections

## 役割

`resource_sections` は、Resource内の章・節・チャプター・レッスンを表す。

## カラム案

```text
resource_section_id
user_id
resource_id
parent_section_id
title
display_order
level
deleted_at
created_at
updated_at
```

## 判断

- MVPではまず手動でSection追加できるようにする。
- `parent_section_id` は持たせるが、MVP画面では深い階層UIを無理に作らなくてよい。
- Section追加時に対応する `section_study_statuses` も作成する。
- AIによる目次・Section自動生成はMVP後回し。
- 将来的には「AIがSection案を作る → ユーザーが確認 → 採用したものだけ保存」の流れにする。

---

# 4. progresses

## 役割

`progresses` は、Resource単位のユーザー進捗・状態を表す。

## カラム案

```text
progress_id
user_id
resource_id
status
current_level
current_section_id
started_at
completed_at
archived_at
archive_reason
initial_studied_at
last_studied_at
created_at
updated_at
```

## status

```text
NOT_STARTED
IN_PROGRESS
PAUSED
ARCHIVED
COMPLETED
```

## 判断

- `current_level` は外部から直接更新させない。
- `current_level` はLevel到達処理によって更新する。
- `COMPLETED` は無条件の手動更新ではなく、ResourceTypeごとの完了条件を満たした場合のみ許可する方向。
- `current_section_id` は「最後にメモを書いたSection」ではなく「現在メインで学習しているSection」。
- StudyMemo作成時やSectionStudyStatus更新時に自動で `current_section_id` は変えない。

## ARCHIVEDの意味

`ARCHIVED` は削除ではない。  
「学習対象から一旦外した状態」を表す。

例：

```text
途中まで読んだが、今は優先度を下げた
仕事の内容が変わったため、今は対象から外した
別Resourceで学び直すことにした
古くなったので今は追わない
```

## archived_at / archive_reason

`archived_at` と `archive_reason` は残す。

理由：

```text
updated_at は最後に何か更新された日時
archived_at は学習対象から外した日時
```

`archive_reason` は、なぜそのResourceを学習対象から外したかという学習判断の履歴になる。

ARCHIVEDからIN_PROGRESSなどに戻しても、`archived_at` と `archive_reason` は履歴として残す方針。

---

# 5. section_study_statuses

## 役割

`section_study_statuses` は、Section単位の学習状態を表す。

```text
この章・節を学習したか
どれくらい理解した感覚があるか
```

## カラム案

```text
section_study_status_id
user_id
resource_id
resource_section_id
studied_at
understanding_level
created_at
updated_at
```

## understanding_level

SteerLogの `current_level` とは別物。  
Section単位の自己理解度。

```text
1: ほぼ分からない
2: なんとなく分かる
3: 普通に理解した
4: 人に説明できそう
5: 実装・応用できそう
```

## 更新時のProgress反映

SectionStudyStatus更新時に自動反映するもの：

```text
Progress.status が NOT_STARTED なら IN_PROGRESS
Progress.last_studied_at = now
```

自動更新しないもの：

```text
Progress.current_section_id
Progress.current_level
Progress.completed_at
StudyMemo
```

## Lv.1との関係

Lv.1到達ルートは2つ。

```text
1. Resource全体を一通り学習済みにする
2. 全Sectionの studied_at が埋まる
```

全Section学習済みの場合：

```text
Progress.current_level = 1
LevelHistory Lv.1作成
source_type = SECTION_STUDY_STATUS
reason_code = ALL_SECTIONS_STUDIED
```

---

# 6. study_memos

## 役割

`study_memos` は、ユーザーが学習中に残す短い生メモ。

例：

```text
RESTの冪等性がまだ曖昧
PUTとPATCHの使い分けをあとで確認
この章はDB設計に関係ありそう
```

## カラム案

```text
study_memo_id
user_id
resource_id
resource_section_id
memo_type
content
tags
deleted_at
created_at
updated_at
```

## memo_type

```text
GENERAL
LEARNED
QUESTION
WEAKNESS
TODO
IDEA
SUMMARY
```

## 判断

- StudyMemoはLearningSessionRecordとは別物。
- StudyMemoはユーザーが自由に書く短い任意メモ。
- Level到達の必須条件にはしない。
- StudyMemo作成時にLevelは上げない。
- 内容は1〜500文字程度。
- 本文丸写しや長文ノート化は避ける。
- MVPでは `tags TEXT[]` でよい。
- 将来的にはタグテーブルに正規化する余地を残す。
- `deleted_at` による論理削除を採用する。

## 作成時のProgress反映

```text
Progress.status が NOT_STARTED なら IN_PROGRESS
Progress.last_studied_at = now
```

自動更新しないもの：

```text
SectionStudyStatus.studied_at
Progress.current_section_id
Progress.current_level
```

---

# 7. learning_sessions

## 役割

`learning_sessions` は、AI Reflection / Recall セッションの実行状態を管理する。

正式証跡ではなく、セッションの進行管理を行う。

## カラム案

```text
learning_session_id
user_id
resource_id
session_type
status
current_step
total_steps
ai_prompt
result_draft
completed_at
discarded_at
record_saved_at
created_at
updated_at
```

## session_type

```text
IMMEDIATE_REFLECTION
DELAYED_RECALL
```

## status

```text
IN_PROGRESS
COMPLETED
RECORD_SAVED
DISCARDED
```

## 状態の意味

```text
IN_PROGRESS
= 回答中

COMPLETED
= AI結果案 resultDraft 作成済み。ユーザー確認待ち

RECORD_SAVED
= ユーザーが確認して保存済み。LearningSessionRecord作成済み

DISCARDED
= ユーザーが保存しなかった/破棄した
```

## result_draft

`complete` 後、AIが生成した保存候補の結果を `result_draft JSONB` として一時保存する。

理由：

```text
record保存時にサーバー側のresultDraftを使う
AI生成部分をユーザーが改ざんできないようにする
保存前確認画面に表示できる
```

## raw回答ログについて

MVPでは以下を正式保存しない。

```text
ユーザーの生回答全文
AIとの全会話ログ
質問履歴全文
回答履歴全文
```

`responses` APIや `complete` APIでは、回答内容をリクエストで受け取ってAI生成に使うが、DBには正式保存しない。

---

# 8. learning_session_records

## 役割

`learning_session_records` は、LearningSession完了後にAIが整理した結果を、ユーザーが保存した正式な学習証跡。

Lv.2 / Lv.3到達の根拠になる。

## カラム案

```text
learning_session_record_id
user_id
resource_id
learning_session_id
session_type
summary
concept_tags
weak_point_tags
weak_point_summary
next_action
ai_assessment
generation_basis
user_comment
created_at
updated_at
```

## ai_assessment

```text
PASSED
NEEDS_REVIEW
OFF_TOPIC
```

## 判断

- AI生成部分はユーザーが編集できない。
- 編集できるのは `user_comment` のみ。
- `summary`, `concept_tags`, `weak_point_tags`, `weak_point_summary`, `next_action`, `ai_assessment`, `generation_basis` は編集不可。
- `NEEDS_REVIEW` でもLv.2/Lv.3到達候補にする。
- `OFF_TOPIC` は保存不可。
- `OFF_TOPIC` はLevel到達に使わない。
- `weak_point_summary` は `NEEDS_REVIEW` の場合必須。
- `next_action` は必須。
- `confidence_level` はMVPに含めない。

## 保存前確認フロー

LearningSession complete後、すぐRecord保存しない。

```text
complete
  → AIがresultDraftを生成
  → resultDraftを画面に表示
  → ユーザーが確認
      ├─ 保存する
      │   → LearningSessionRecord作成
      │   → Progress.current_level更新
      │   → LevelHistory作成
      └─ 保存しない
          → LearningSessionをDISCARDED
          → Recordなし
          → Level更新なし
```

保存前確認画面では、保存予定の内容を表示する。

```text
summary
conceptTags
weakPointTags
weakPointSummary
nextAction
aiAssessment
generationBasis
```

ユーザーができること：

```text
保存する
保存しない
userCommentを追加する
```

ユーザーができないこと：

```text
summaryの編集
aiAssessmentの編集
weakPointSummaryの編集
nextActionの編集
generationBasisの編集
```

## 保存前に見せる理由

- 保存されるものがユーザー自身の学習証跡だから。
- AIがズレた要約・タグ・弱点・nextActionを作る可能性があるから。
- SteerLogは認定試験アプリではなく、納得できる学習証跡を残すアプリだから。
- AI生成部分の編集不可と、保存する/しないの選択を両立できるから。

---

# 9. level_histories

## 役割

`level_histories` は、ResourceがあるLevelに初めて到達した履歴を保存する。

証跡本文ではなく、初到達イベントの記録。

```text
learning_session_records
= Lv.2 / Lv.3 の詳しい証跡本文

level_histories
= いつ、どの理由で、どの証跡によって、そのLevelに初到達したか
```

## カラム案

```text
level_history_id
user_id
resource_id
level
source_type
source_id
reason_code
created_at
updated_at
```

## level

DB上は1〜5を許容。  
MVPでは1〜3を使用する。

```text
1: 初回学習済み
2: Immediate Reflection証跡あり
3: Delayed Recall証跡あり
4: Artifact証跡あり（MVP外）
5: Defense証跡あり（MVP外）
```

## source_type

```text
INITIAL_STUDY_COMPLETION
SECTION_STUDY_STATUS
LEARNING_SESSION_RECORD
```

## reason_code

```text
INITIAL_STUDY_COMPLETED
ALL_SECTIONS_STUDIED
IMMEDIATE_REFLECTION_RECORD_SAVED
DELAYED_RECALL_RECORD_SAVED
```

## 判断

- `user_id + resource_id + level` は一意。
- 同じLevelのLevelHistoryは初回のみ作る。
- 追加のReflection/RecallはLearningSessionRecordには残るが、LevelHistoryは増やさない。
- `source_type + source_id` はポリモーフィック参照。
- DBのFKは張らず、アプリ側で存在チェックする。
- `deleted_at` は持たせない。

---

# 10. Lv.1〜Lv.3到達ルール

## Lv.1

意味：

```text
初回学習済み / 一通り触れた状態
```

到達ルート：

```text
1. POST /resources/{resourceId}/progress/complete-initial-study
2. 全Sectionの studied_at が埋まる
```

Resource全体完了の場合：

```text
Progress.initial_studied_at = now
Progress.last_studied_at = now
Progress.current_level = max(current_level, 1)
LevelHistory Lv.1作成
source_type = INITIAL_STUDY_COMPLETION
reason_code = INITIAL_STUDY_COMPLETED
```

全Section学習済みの場合：

```text
Progress.current_level = max(current_level, 1)
LevelHistory Lv.1作成
source_type = SECTION_STUDY_STATUS
reason_code = ALL_SECTIONS_STUDIED
```

## Lv.2

意味：

```text
Immediate Reflectionの正式証跡がある
```

条件：

```text
IMMEDIATE_REFLECTIONのLearningSessionRecordが保存される
ai_assessment != OFF_TOPIC
```

処理：

```text
Progress.current_level = max(current_level, 2)
LevelHistory Lv.2作成
source_type = LEARNING_SESSION_RECORD
reason_code = IMMEDIATE_REFLECTION_RECORD_SAVED
```

## Lv.3

意味：

```text
Delayed Recallの正式証跡がある
```

条件：

```text
Progress.current_level >= 2
DELAYED_RECALLのLearningSessionRecordが保存される
ai_assessment != OFF_TOPIC
```

処理：

```text
Progress.current_level = max(current_level, 3)
LevelHistory Lv.3作成
source_type = LEARNING_SESSION_RECORD
reason_code = DELAYED_RECALL_RECORD_SAVED
```

DELAYED_RECALLセッションは、Lv.2到達済みでなければ開始不可。

---

# 11. MVP主要API

## Resource API

```text
POST /resources
GET /resources
GET /resources/{resourceId}
PATCH /resources/{resourceId}
DELETE /resources/{resourceId}
```

`POST /resources` では、Resource作成と同時にProgressを `NOT_STARTED` で作成する。

## ResourceSection API

```text
POST /resources/{resourceId}/sections
GET /resources/{resourceId}/sections
PATCH /resources/{resourceId}/sections/{sectionId}
DELETE /resources/{resourceId}/sections/{sectionId}
```

`POST /sections` では、Section作成と同時にSectionStudyStatusを作成する。

## Progress API

```text
GET /resources/{resourceId}/progress
PATCH /resources/{resourceId}/progress
POST /resources/{resourceId}/progress/complete-initial-study
```

`current_level` はPATCHで直接更新させない。

## SectionStudyStatus API

```text
GET /resources/{resourceId}/sections/{sectionId}/study-status
PATCH /resources/{resourceId}/sections/{sectionId}/study-status
```

Resource詳細APIでSection + StudyStatusを一緒に返すなら、単独GETはMVPでは必須ではない。

## StudyMemo API

```text
POST /resources/{resourceId}/memos
GET /resources/{resourceId}/memos
GET /resources/{resourceId}/memos/{memoId}
PATCH /resources/{resourceId}/memos/{memoId}
DELETE /resources/{resourceId}/memos/{memoId}
```

## LearningSession API

```text
POST /resources/{resourceId}/learning-sessions
POST /learning-sessions/{learningSessionId}/responses
POST /learning-sessions/{learningSessionId}/complete
POST /learning-sessions/{learningSessionId}/record
POST /learning-sessions/{learningSessionId}/discard
```

## LevelHistory API

```text
GET /resources/{resourceId}/level-histories
```

LevelHistoryは直接作成・更新・削除しない。

---

# 12. 実装順序

## Phase 1：プロジェクト土台

```text
Spring Bootプロジェクト
PostgreSQL接続
Flyway
共通Entity
例外ハンドリング
仮userId運用
```

初期は認証を作り込まず、仮に `userId = 1` 固定でもよい。  
ただし、Repository/Serviceの検索条件には必ず `user_id` を入れる。

## Phase 2：Resource + Progress

```text
resources
progresses
POST /resources
GET /resources/{resourceId}
```

Resource作成時にProgressも作る。

## Phase 3：ResourceSection + SectionStudyStatus

```text
resource_sections
section_study_statuses
POST /resources/{resourceId}/sections
PATCH /resources/{resourceId}/sections/{sectionId}/study-status
```

Section作成時にStudyStatusも作る。

## Phase 4：Lv.1 + LevelHistory

```text
level_histories
POST /resources/{resourceId}/progress/complete-initial-study
全Section学習済みによるLv.1到達
```

## Phase 5：StudyMemo

```text
study_memos
POST /resources/{resourceId}/memos
GET /resources/{resourceId}/memos
```

## Phase 6：LearningSession

```text
learning_sessions
POST /resources/{resourceId}/learning-sessions
POST /learning-sessions/{learningSessionId}/responses
POST /learning-sessions/{learningSessionId}/complete
POST /learning-sessions/{learningSessionId}/discard
```

最初はAI連携をモックにしてよい。

## Phase 7：LearningSessionRecord + Lv.2/Lv.3

```text
learning_session_records
POST /learning-sessions/{learningSessionId}/record
GET /resources/{resourceId}/level-histories
```

## Phase 8：一覧・詳細の見せ方整理

Resource詳細に以下を含める方向。

```text
Resource本体
Progress
Sections + StudyStatus
RecentMemos
LatestLearningSessionRecords
LevelHistories
```

---

# 13. Flywayについて

Flywayは、DBスキーマ変更をバージョン管理するためのDBマイグレーションツール。

Spring Bootそのものの機能ではないが、Spring Bootと一緒によく使われる。

例：

```text
src/main/resources/db/migration/V1__create_resources.sql
src/main/resources/db/migration/V2__create_progresses.sql
src/main/resources/db/migration/V3__create_resource_sections.sql
```

Flywayを使うと、DBテーブル定義や変更履歴をGitで管理できる。

SteerLogでは、最初は以下のように管理する案。

```text
V1__create_resources.sql
V2__create_progresses.sql
V3__create_resource_sections.sql
V4__create_section_study_statuses.sql
V5__create_level_histories.sql
V6__create_study_memos.sql
V7__create_learning_sessions.sql
V8__create_learning_session_records.sql
```

実装順に合わせて、まずV1〜V5まで作り、Lv.1まで動かしてからV6〜V8に進んでもよい。

---

# 14. AIコード生成を使った開発方針

## 基本方針

全部を手書きするでも、全部をAI任せにするでもない。

```text
設計・判断・レビューは自分でやる
実装のひな形・定型コード・テストの叩き台はAIに生成させる
生成されたコードは必ず自分で説明できる状態まで読む・直す
```

## 役割分担

```text
設計: 自分 70% / AI 30%
実装雛形: 自分 20% / AI 80%
業務ロジック: 自分 60% / AI 40%
テスト: 自分 50% / AI 50%
レビュー: 自分 80% / AI 20%
ドキュメント: 自分 50% / AI 50%
```

## AIに任せやすいもの

```text
Entity
Repository
DTO
Controller雛形
Service雛形
Flyway SQL
Mapper
単体テストの叩き台
バリデーション
エラーレスポンス共通化
```

## 自分で特に見るべきもの

```text
Service層の業務ロジック
Level更新処理
LearningSessionの状態遷移
認可チェック
トランザクション処理
例外設計
テストケース
```

## AI生成コードのレビュー観点

```text
user_idで所有者チェックしているか
deleted_at is null を見ているか
トランザクション境界は正しいか
current_levelを外部から直接更新していないか
LevelHistoryを重複作成しないか
LearningSessionRecord保存前確認の思想を壊していないか
raw回答ログを保存していないか
```

---

# 15. Cursor / Claude Code / ChatGPTの使い分け

## Cursor

SteerLog開発のメイン開発環境として有力。

向いていること：

```text
普段の開発
ファイル構成を見ながら実装
AIにコードを生成・修正させる
Spring Bootプロジェクト全体を見ながら学習
```

## Claude Code

ターミナル中心の重めの作業に向いている。

向いていること：

```text
複数ファイルの一括修正
テスト実行
エラー修正
リファクタ
既存コードの構成確認
```

## ChatGPT

設計・方針整理に使う。

向いていること：

```text
DB設計
API設計
状態遷移
実装順序
設計判断の言語化
面接での説明整理
```

## 推奨構成

```text
設計・方針整理:
  ChatGPT

実装メイン:
  Cursor

大きめの修正・テスト実行・リファクタ:
  Claude Code または Cursor Agent

補完:
  Cursor内補完 / 必要ならCopilot
```

---

# 16. Markdown設計書の運用方針

## 今までのMarkdown

今までの日付別Markdownは、議論ログ・判断履歴として残す。

用途：

```text
なぜその判断になったか確認する
古い案との差分を見る
設計変更の経緯をREADMEに書く
```

## AIコード生成用docs

今後、実装AIに読ませるための最新版仕様書として、以下のように再編する。

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

docs/archive/
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

AIに主に読ませるのは `docs/00〜07`。  
過去ログは `docs/archive/` に置く。

## なぜ仕分けるか

今までのMarkdownには、古い方針も含まれている。

例：

```text
AIクイズ中心
CheckRecord
正答率でLv.3
ReviewRecord
Lv.4/Lv.5もMVPに含める初期案
```

これらをそのままAIに読ませると、最新方針と混ざる可能性がある。  
そのため、AIコード生成には「最新版docs」だけを基本的に読ませる。

---

# 17. 次にやること

この2026-05-26メモをもとに、次はAIコード生成用docs構成を作る。

順番としては以下。

```text
1. docs構成案を確定する
2. 00-product-principles.md を作る
3. 01-mvp-scope.md を作る
4. 02-db-design.md を作る
5. 03-api-design.md を作る
6. 04-level-rules.md を作る
7. 05-learning-session-flow.md を作る
8. 06-implementation-rules.md を作る
9. 07-implementation-order.md を作る
10. 旧日付メモを docs/archive/ に移す
```

まずは `00-product-principles.md` と `01-mvp-scope.md` から作るのがよい。

---

# 18. 今日の最終判断

2026-05-26時点の方針は以下。

```text
MVPは主要8テーブルで進める
Lv.1〜Lv.3を対象にする
Lv.4/Lv.5/Galaxy/API自動目次生成はMVP外
Progressのarchived_at/archive_reasonは残す
LearningSession complete後はresultDraftを保存前確認する
LearningSessionRecordのAI生成部分は編集不可
保存しない場合はLevel更新しない
raw回答ログはDBに正式保存しない
実装はResource〜Lv.1まで縦に通してからStudyMemo/LearningSessionへ進む
開発はCursor中心、Claude Code併用、設計整理はChatGPTで行う
Markdownは日付別ログからAIコード生成用docsへ再編する
```
