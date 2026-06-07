# 03-api-design.md

# SteerLog MVP API Design

## 目的

このドキュメントは、SteerLog MVPで実装するAPI仕様を定義する。  
Controller、Request DTO、Response DTO、Serviceの作成時は、この内容を基準にする。

MVPでは、Resource登録からLv.1〜Lv.3の学習証跡作成までをAPIで実現する。

---

# 0. 実装状況サマリ（2026-06）

| 分類 | API | 状態 |
|------|-----|------|
| Resource | POST/GET/GET詳細/PATCH/DELETE `/resources` | ✅ 実装済み |
| Progress | GET `/resources/{resourceId}/progress` | ✅ 実装済み |
| Progress | POST `.../progress/complete-initial-study` | ✅ 実装済み |
| Progress | PATCH `/resources/{resourceId}/progress` | ⬜ 未実装 |
| ResourceSection | POST/GET `/resources/{resourceId}/sections` | ✅ 実装済み |
| ResourceSection | PATCH/DELETE `.../sections/{sectionId}` | ⬜ 未実装 |
| SectionStudyStatus | PATCH `.../sections/{sectionId}/study-status` | ✅ 実装済み |
| SectionStudyStatus | GET `.../sections/{sectionId}/study-status` | ⬜ 未実装 |
| StudyMemo | POST/GET/PATCH/DELETE `/resources/{resourceId}/memos` | ✅ 実装済み |
| StudyMemo | GET `.../memos/{memoId}` | ⬜ 未実装 |
| LevelHistory | GET `/resources/{resourceId}/level-histories` | ✅ 実装済み |
| LearningSession | 全API | ⬜ 未実装 |
| LearningSessionRecord | record保存 | ⬜ 未実装 |

認証は未実装。Controller では `TEMP_USER_ID = 1L` 固定。

Resource詳細（`GET /resources/{resourceId}`）は現状 **Resource + Progress のみ**。Sections / Memos / LevelHistories の統合表示は Phase 8 予定。

---

# 1. API全体方針

## 1.1 API分類

MVPのAPIは、以下に分ける。

```text
Resource API
ResourceSection API
Progress API
SectionStudyStatus API
StudyMemo API
LearningSession API
LearningSessionRecord保存API
LevelHistory参照API
```

---

## 1.2 直接CRUDしないもの

以下は通常CRUD対象にしない。

```text
Progress作成
LearningSessionRecord直接CRUD
LevelHistory直接CRUD
```

理由：

```text
ProgressはResource作成時に自動作成する
LearningSessionRecordはLearningSessionのrecord処理で作成する
LevelHistoryはLevel到達処理の結果として作成する
```

---

## 1.3 認可方針

すべてのAPIで、対象データが現在ユーザーのものであることを確認する。

実装初期は仮userIdでもよいが、Service/Repositoryでは必ず `user_id` を検索条件に含める。

```sql
WHERE resource_id = :resourceId
  AND user_id = :userId
```

論理削除対象では以下も見る。

```sql
AND deleted_at IS NULL
```

---

# 2. Resource API

## 2.1 Resource登録

**状態: ✅ 実装済み**

```http
POST /resources
```

### 役割

学習対象を登録する。  
同時にProgressを `NOT_STARTED` で作成する。

### Request

```json
{
  "resourceType": "BOOK",
  "title": "Webを支える技術",
  "author": "山本陽平",
  "sourceUrl": null,
  "description": "REST/API設計の基礎を学ぶための本"
}
```

### 処理

```text
1. Resourceを作成
2. Progressを作成
   status = NOT_STARTED
   currentLevel = 0
3. Resource詳細を返す
```

### 注意

Progress作成APIは別で作らない。  
Resource作成とProgress作成は同一トランザクションで行う。

---

## 2.2 Resource一覧

**状態: ✅ 実装済み**（Query パラメータは未対応）

```http
GET /resources
```

### Query

⬜ 未実装。将来候補：

```text
status
resourceType
page
size
keyword
tag
currentLevel
```

### Responseに含める候補

```text
resourceId
resourceType
title
author
sourceUrl
progress.status
progress.currentLevel
progress.lastStudiedAt
createdAt
updatedAt
```

---

## 2.3 Resource詳細

**状態: ✅ 実装済み**（Resource + Progress のみ）

```http
GET /resources/{resourceId}
```

### Responseに含めるもの

現状：

```text
Resource本体
Progress
```

Phase 8 で追加予定：

```text
Sections + StudyStatus
RecentMemos
LatestLearningSessionRecords
LevelHistories
```

### 注意

SectionStudyStatusはResource詳細でSectionと一緒に返すとよい。  
N+1取得に注意する。

---

## 2.4 Resource更新

**状態: ✅ 実装済み**

```http
PATCH /resources/{resourceId}
```

### 更新可能

```text
title
author
sourceUrl
description
```

`resourceType` は現状更新不可。

---

## 2.5 Resource削除

**状態: ✅ 実装済み**

```http
DELETE /resources/{resourceId}
```

### 処理

物理削除ではなく、論理削除。

```text
resources.deleted_at = now
```

関連データは消さない。  
通常画面ではResourceごと非表示にする。

---

# 3. ResourceSection API

## 3.1 Section追加

**状態: ✅ 実装済み**

```http
POST /resources/{resourceId}/sections
```

### Request

```json
{
  "title": "第1章 Webとは何か",
  "sectionOrder": 1
}
```

### 処理

```text
1. Resourceが自分のものか確認
2. ResourceSectionを作成
3. SectionStudyStatusを作成
   studiedAt = null
4. Sectionを返す
```

### 注意

Section作成とSectionStudyStatus作成は同一トランザクションで行う。

---

## 3.2 Section一覧

**状態: ✅ 実装済み**

```http
GET /resources/{resourceId}/sections
```

### Response

Section の一覧（`sectionOrder` 昇順）。StudyStatus は含めない。

---

## 3.3 Section更新

**状態: ⬜ 未実装**

```http
PATCH /resources/{resourceId}/sections/{sectionId}
```

### 更新可能

```text
parentSectionId
title
displayOrder
level
```

---

## 3.4 Section削除

**状態: ⬜ 未実装**

```http
DELETE /resources/{resourceId}/sections/{sectionId}
```

### 処理

物理削除ではなく、論理削除。

```text
resource_sections.deleted_at = now
```

SectionStudyStatusは消さない。  
削除済みSectionは通常表示しない。

---

# 4. Progress API

## 4.1 Progress取得

**状態: ✅ 実装済み**

```http
GET /resources/{resourceId}/progress
```

---

## 4.2 Progress更新

**状態: ⬜ 未実装**

```http
PATCH /resources/{resourceId}/progress
```

### 更新可能

```text
status
currentSectionId
archiveReason
```

### 更新不可

```text
currentLevel
initialStudiedAt
lastStudiedAt
completedAt
```

`currentLevel` はLevel到達処理で更新する。

---

## 4.3 status更新ルール

### NOT_STARTED → IN_PROGRESS

```text
status = IN_PROGRESS
started_at が null なら now
last_studied_at = now
```

### IN_PROGRESS → PAUSED

```text
status = PAUSED
updated_at = now
```

### PAUSED → IN_PROGRESS

```text
status = IN_PROGRESS
last_studied_at = now
```

### 任意状態 → ARCHIVED

```text
status = ARCHIVED
archived_at = now
archive_reason = 入力値。任意
```

### ARCHIVED → IN_PROGRESS / PAUSED / NOT_STARTED

```text
statusを変更
archived_atは残す
archive_reasonも残す
```

---

## 4.4 初回学習完了

**状態: ✅ 実装済み**

```http
POST /resources/{resourceId}/progress/complete-initial-study
```

### 役割

Resource全体を一通り学習済みにする。  
Lv.1到達候補にする。

### 処理

```text
1. Progress取得
2. initialStudiedAt = now
3. lastStudiedAt = now
4. status が NOT_STARTED なら IN_PROGRESS
5. currentLevel が1未満なら1に更新
6. LevelHistory Lv.1 がなければ作成
```

### LevelHistory

```text
level = 1
sourceType = INITIAL_STUDY_COMPLETION
sourceId = null
reasonCode = INITIAL_STUDY_COMPLETED
```

`currentLevel` は下げない。LevelHistory は重複作成しない。

---

# 5. SectionStudyStatus API

## 5.1 Section学習状態取得

**状態: ⬜ 未実装**

```http
GET /resources/{resourceId}/sections/{sectionId}/study-status
```

---

## 5.2 Section学習状態更新

**状態: ✅ 実装済み**

```http
PATCH /resources/{resourceId}/sections/{sectionId}/study-status
```

### Request

```json
{
  "studiedAt": "2026-05-26T21:00:00+09:00"
}
```

`studiedAt` が null の場合は更新しない（部分更新）。  
`understandingLevel` は ⬜ 未実装。

### 処理

```text
1. ResourceとSectionの所有者・親子関係を確認
2. SectionStudyStatus更新（studiedAt が指定された場合のみ）
3. Progress.status が NOT_STARTED なら IN_PROGRESS
4. Progress.lastStudiedAt / updatedAt = now
5. 未削除Sectionがすべて studiedAt ありなら Lv.1 自動到達
   currentLevel: 0 のときのみ 1
   initialStudiedAt: null のときのみセット
   LevelHistory 未存在時のみ作成
   sourceType = SECTION_STUDY_STATUS
   reasonCode = ALL_SECTIONS_STUDIED
```

Section が 0 件の Resource では Lv.1 自動到達しない。

### 自動更新しないもの

```text
Progress.currentSectionId
Progress.currentLevelの直接更新
Progress.completedAt
StudyMemo
```

---

# 6. StudyMemo API

## 6.1 メモ作成

**状態: ✅ 実装済み**

```http
POST /resources/{resourceId}/memos
```

### Request

```json
{
  "resourceSectionId": 10,
  "memoType": "QUESTION",
  "content": "PUTとPATCHの使い分けがまだ曖昧"
}
```

`memoType` が null の場合は `GENERAL`。`resourceSectionId` は任意。  
`tags` は ⬜ 未実装。

### 処理

```text
1. Resource所有者チェック
2. resourceSectionId 指定時、Section存在確認
3. StudyMemo作成
4. Progress.status が NOT_STARTED なら IN_PROGRESS
5. Progress.lastStudiedAt / updatedAt = now
```

StudyMemo作成では Level を上げない。LevelHistory / SectionStudyStatus は更新しない。

---

## 6.2 メモ一覧

**状態: ✅ 実装済み**

```http
GET /resources/{resourceId}/memos
```

### Query

⬜ 未実装（`sectionId` / `memoType` / ページング等）。

### Response

`createdAt` 降順。0 件は空配列。content 全文を返す（preview 形式は未採用）。

---

## 6.3 メモ詳細

**状態: ⬜ 未実装**

```http
GET /resources/{resourceId}/memos/{memoId}
```

---

## 6.4 メモ更新

**状態: ✅ 実装済み**

```http
PATCH /resources/{resourceId}/memos/{memoId}
```

### 更新可能

```text
memoType（null なら更新しない）
content（null なら更新しない、指定時 1〜500文字）
```

`resourceSectionId` / `tags` の更新は ⬜ 未実装。  
`updatedAt` は常に更新。Progress / LevelHistory は更新しない。

---

## 6.5 メモ削除

**状態: ✅ 実装済み**

```http
DELETE /resources/{resourceId}/memos/{memoId}
```

### 処理

論理削除。204 No Content。Progress / LevelHistory は更新しない。

---

# 7. LearningSession API

**状態: ⬜ 未実装（Phase 6）**

## 7.1 セッション開始

```http
POST /resources/{resourceId}/learning-sessions
```

### Request

```json
{
  "sessionType": "IMMEDIATE_REFLECTION"
}
```

または、

```json
{
  "sessionType": "DELAYED_RECALL"
}
```

### 開始条件

```text
IMMEDIATE_REFLECTION:
  Resourceが存在すれば開始可能

DELAYED_RECALL:
  Progress.currentLevel >= 2 の場合のみ開始可能
```

### 失敗例

```text
LEVEL_REQUIREMENT_NOT_MET
SESSION_ALREADY_IN_PROGRESS
```

### 処理

```text
1. Resource所有者チェック
2. DELAYED_RECALLならcurrentLevel >= 2を確認
3. 同一userId + resourceId + sessionTypeでIN_PROGRESS/COMPLETEDがないか確認
4. LearningSession作成
   status = IN_PROGRESS
   currentStep = 1
   totalSteps = 3
   aiPrompt = 最初の質問
   resultDraft = null
```

---

## 7.2 回答して次の質問を取得

```http
POST /learning-sessions/{learningSessionId}/responses
```

### 意味

このAPIは、回答を正式保存するAPIではない。  
次のAI質問を生成するためのAPIである。

### Request

```json
{
  "responses": [
    {
      "step": 1,
      "prompt": "このResourceで学んだ内容を説明してください。",
      "response": "REST APIではリソースをURIで表現し..."
    }
  ]
}
```

### 処理

```text
1. status = IN_PROGRESS を確認
2. 現在までのresponsesをもとに次のaiPromptを生成
3. currentStepを進める
4. aiPromptを更新
5. 次の質問を返す
```

### 注意

raw回答全文はDBに正式保存しない。

---

## 7.3 結果作成

```http
POST /learning-sessions/{learningSessionId}/complete
```

### Request

```json
{
  "responses": [
    {
      "step": 1,
      "prompt": "このResourceで学んだ内容を説明してください。",
      "response": "REST APIでは..."
    },
    {
      "step": 2,
      "prompt": "SteerLogのAPI設計にどう活かせますか？",
      "response": "Progress更新APIでPUT/PATCHを..."
    },
    {
      "step": 3,
      "prompt": "まだ曖昧な点は何ですか？",
      "response": "PATCHの冪等性が曖昧です。"
    }
  ]
}
```

### 処理

```text
1. status = IN_PROGRESS を確認
2. responsesをもとにAIがresultDraftを生成
3. learning_sessions.result_draft に保存
4. status = COMPLETED
5. completed_at = now
6. resultDraftを返す
```

### Response

```json
{
  "learningSessionId": 1,
  "status": "COMPLETED",
  "sessionType": "IMMEDIATE_REFLECTION",
  "resultDraft": {
    "summary": "...",
    "conceptTags": ["REST", "HTTP"],
    "weakPointTags": ["PATCH"],
    "weakPointSummary": "...",
    "nextAction": "...",
    "aiAssessment": "NEEDS_REVIEW",
    "generationBasis": "..."
  },
  "nextAction": {
    "type": "SAVE_OR_DISCARD_RECORD"
  }
}
```

---

## 7.4 Record保存

```http
POST /learning-sessions/{learningSessionId}/record
```

### Request

```json
{
  "userComment": "PUT/PATCHの違いはまだ弱いので、次に自分のProgress更新APIで整理する。"
}
```

### 処理

```text
1. LearningSession.status = COMPLETED を確認
2. resultDraftが存在することを確認
3. resultDraft.aiAssessment != OFF_TOPIC を確認
4. LearningSessionRecord作成
5. LearningSession.status = RECORD_SAVED
6. recordSavedAt = now
7. Progress.lastStudiedAt = now
8. Lv.2 / Lv.3到達判定
9. LevelHistory作成
```

### 注意

AI生成部分はリクエストで受け取らない。  
サーバー側の `result_draft` を使って保存する。

---

## 7.5 破棄

```http
POST /learning-sessions/{learningSessionId}/discard
```

### 処理

```text
status = DISCARDED
discarded_at = now
```

### 効果

```text
LearningSessionRecordは作らない
Progress.currentLevelは上げない
LevelHistoryは作らない
```

---

# 8. LevelHistory API

## 8.1 LevelHistory一覧

**状態: ✅ 実装済み**

```http
GET /resources/{resourceId}/level-histories
```

`createdAt` 昇順。0 件は空配列。

### Response

```text
level
sourceType
sourceId
reasonCode
createdAt
```

LevelHistoryは直接作成・更新・削除しない。

---

# 9. 作らないAPI

MVPでは以下を作らない。

```text
POST /progresses
POST /learning-session-records
PATCH /learning-session-records/{id}
DELETE /learning-session-records/{id}
POST /level-histories
PATCH /level-histories/{id}
DELETE /level-histories/{id}
POST /check-records
POST /answers
POST /review-records
```

---

# 10. まとめ

現時点（Phase 5 完了）で実装済みの主な流れ：

```text
Resource登録 → Progress自動作成
Section追加 → SectionStudyStatus自動作成
Section学習済み更新 → Progress更新 → 全Section完了でLv.1
complete-initial-study → Lv.1（別経路）
StudyMemo CRUD → Progress.lastStudiedAt更新（Levelは上がらない）
LevelHistory参照
```

MVP API全体の重要な原則：

```text
ProgressはResource作成時に自動作成
SectionStudyStatusはSection作成時に自動作成
StudyMemoではLevelを上げない
LearningSessionRecordは保存前確認後に作る
LevelHistoryはLevel到達処理で作る
currentLevelは直接更新しない
raw回答ログは正式保存しない
```
