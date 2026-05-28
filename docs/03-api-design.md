# 03-api-design.md

# SteerLog MVP API Design

## 目的

このドキュメントは、SteerLog MVPで実装するAPI仕様を定義する。  
Controller、Request DTO、Response DTO、Serviceの作成時は、この内容を基準にする。

MVPでは、Resource登録からLv.1〜Lv.3の学習証跡作成までをAPIで実現する。

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

```http
GET /resources
```

### Query

MVPでは以下をサポートする。

```text
status
resourceType
page
size
```

将来的に追加候補：

```text
currentLevel
keyword
tag
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

```http
GET /resources/{resourceId}
```

### Responseに含めるもの

MVP初期：

```text
Resource本体
Progress
Sections + StudyStatus
```

後続で追加：

```text
RecentMemos
LatestLearningSessionRecords
LevelHistories
```

### 注意

SectionStudyStatusはResource詳細でSectionと一緒に返すとよい。  
N+1取得に注意する。

---

## 2.4 Resource更新

```http
PATCH /resources/{resourceId}
```

### 更新可能

```text
resourceType
title
author
sourceUrl
description
```

---

## 2.5 Resource削除

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

```http
POST /resources/{resourceId}/sections
```

### Request

```json
{
  "parentSectionId": null,
  "title": "第1章 Webとは何か",
  "displayOrder": 1,
  "level": 1
}
```

### 処理

```text
1. Resourceが自分のものか確認
2. ResourceSectionを作成
3. SectionStudyStatusを作成
   studiedAt = null
   understandingLevel = null
4. Section + StudyStatusを返す
```

### 注意

Section作成とSectionStudyStatus作成は同一トランザクションで行う。

---

## 3.2 Section一覧

```http
GET /resources/{resourceId}/sections
```

### Response

```text
sections
studyStatus
```

Resource詳細APIに含めるなら、MVP初期では単独APIは省略可能。

---

## 3.3 Section更新

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

```http
GET /resources/{resourceId}/progress
```

Resource詳細APIに含めるなら、MVP初期では単独APIは省略可能。

---

## 4.2 Progress更新

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
sourceId = progressId
reasonCode = INITIAL_STUDY_COMPLETED
```

---

# 5. SectionStudyStatus API

## 5.1 Section学習状態取得

```http
GET /resources/{resourceId}/sections/{sectionId}/study-status
```

Resource詳細で一緒に返す場合、MVP初期では省略可能。

---

## 5.2 Section学習状態更新

```http
PATCH /resources/{resourceId}/sections/{sectionId}/study-status
```

### Request

```json
{
  "studiedAt": "2026-05-26T21:00:00+09:00",
  "understandingLevel": 3
}
```

### 処理

```text
1. ResourceとSectionの所有者・親子関係を確認
2. SectionStudyStatus更新
3. Progress.status が NOT_STARTED なら IN_PROGRESS
4. Progress.lastStudiedAt = now
5. 全Section studiedAt ありならLv.1到達判定
```

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

```http
POST /resources/{resourceId}/memos
```

### Request

```json
{
  "resourceSectionId": 10,
  "memoType": "QUESTION",
  "content": "PUTとPATCHの使い分けがまだ曖昧",
  "tags": ["REST", "HTTP", "API設計"]
}
```

### 処理

```text
1. Resource所有者チェック
2. sectionIdが指定された場合、Resource配下のSectionか確認
3. StudyMemo作成
4. Progress.status が NOT_STARTED なら IN_PROGRESS
5. Progress.lastStudiedAt = now
```

### 注意

StudyMemo作成ではLevelを上げない。

---

## 6.2 メモ一覧

```http
GET /resources/{resourceId}/memos
```

### Query

```text
sectionId
memoType
page
size
```

MVP初期では `keyword` / `tag` 検索は省略可能。

### Response

一覧では全文ではなくpreviewを返す。

```json
{
  "studyMemoId": 1,
  "memoType": "QUESTION",
  "preview": "PUTとPATCHの使い分けがまだ曖昧",
  "tags": ["REST", "HTTP"],
  "createdAt": "2026-05-26T21:00:00+09:00"
}
```

---

## 6.3 メモ詳細

```http
GET /resources/{resourceId}/memos/{memoId}
```

詳細ではcontent全文を返す。

---

## 6.4 メモ更新

```http
PATCH /resources/{resourceId}/memos/{memoId}
```

### 更新可能

```text
resourceSectionId
memoType
content
tags
```

---

## 6.5 メモ削除

```http
DELETE /resources/{resourceId}/memos/{memoId}
```

### 処理

論理削除。

```text
deleted_at = now
```

---

# 7. LearningSession API

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

```http
GET /resources/{resourceId}/level-histories
```

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

MVP APIは、Resource登録からLv.1〜Lv.3の証跡作成までを実現する。

重要な原則：

```text
ProgressはResource作成時に自動作成
SectionStudyStatusはSection作成時に自動作成
StudyMemoではLevelを上げない
LearningSessionRecordは保存前確認後に作る
LevelHistoryはLevel到達処理で作る
currentLevelは直接更新しない
raw回答ログは正式保存しない
```
