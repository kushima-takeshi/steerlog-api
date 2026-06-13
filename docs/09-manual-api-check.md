# 09-manual-api-check.md

# SteerLog API 手動動作確認（curl）

## 目的

実装済み API を `localhost:8080` 上で順番に確認するための curl 集。  
認証は未実装のため、ヘッダーは不要。

## 使い方

1. アプリを起動する（`mvn spring-boot:run` 等）
2. 下の curl を上から順に実行する
3. 各ステップのレスポンスから ID を控え、次の curl のプレースホルダに置き換える

```text
{resourceId}                  … Step 1 の resourceId
{sectionId}                   … Step 5 の resourceSectionId
{memoId}                      … Step 11 の studyMemoId
{learningSessionId}           … Step 16 start（IMMEDIATE_REFLECTION）。Step 17〜21 で共通
{learningSessionIdRecall}     … Step 22 start（DELAYED_RECALL）。Step 22 の responses / complete / record で使用
{learningSessionIdDiscard}    … Step 23 start（discard 用）。Step 23 の discard で使用
```

## HTTPステータスを確認したい場合

このドキュメントではレスポンス本文を見やすくするため、多くの例で `-s` を使っています。  
HTTPステータスも確認したい場合は、以下のように `-i` を付けるか、`-w "%{http_code}\n"` を使ってください。

例1：ヘッダー込みで確認する

```bash
curl -i http://localhost:8080/resources/{resourceId}
```

例2：HTTPステータスだけ確認する

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/resources/{resourceId}
```

---

## 1. Resource 作成

```bash
curl -s -X POST http://localhost:8080/resources \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "BOOK",
    "title": "Webを支える技術",
    "author": "山本陽平",
    "description": "手動確認用Resource"
  }'
```

**確認ポイント**
- HTTP 201 Created
- `resourceId` が返る（以降 `{resourceId}` に使う）
- `progress.status` が `NOT_STARTED`、`currentLevel` が `0`

---

## 2. Resource 一覧取得

```bash
curl -s http://localhost:8080/resources
```

**確認ポイント**
- HTTP 200 OK
- 配列に Step 1 で作成した Resource が含まれる
- 各要素に `progress` が含まれる

---

## 3. Resource 詳細取得

```bash
curl -s http://localhost:8080/resources/{resourceId}
```

**確認ポイント**
- HTTP 200 OK
- `resourceId` / `title` / `progress` が返る
- 現状は Resource + Progress のみ（Sections / Memos は含まない）
- 統合詳細は Step 15 の `GET /resources/{resourceId}/details` を使う（本 API は変更しない）

---

## 4. Resource 更新

```bash
curl -s -X PATCH http://localhost:8080/resources/{resourceId} \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Webを支える技術（更新版）",
    "description": "PATCH確認済み"
  }'
```

**確認ポイント**
- HTTP 200 OK
- `title` / `description` が更新されている
- `updatedAt` が変わっている

---

## 5. Section 作成

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/sections \
  -H "Content-Type: application/json" \
  -d '{
    "title": "第1章 Webとは何か",
    "sectionOrder": 1
  }'
```

**確認ポイント**
- HTTP 201 Created
- `resourceSectionId` が返る（以降 `{sectionId}` に使う）
- Section 作成と同時に SectionStudyStatus が作られる（この API レスポンスには含まれない）

---

## 6. Section 一覧取得

```bash
curl -s http://localhost:8080/resources/{resourceId}/sections
```

**確認ポイント**
- HTTP 200 OK
- Step 5 の Section が `sectionOrder` 昇順で返る
- `resourceSectionId` / `title` / `sectionOrder` を確認

---

## 7. SectionStudyStatus 更新

```bash
curl -s -X PATCH http://localhost:8080/resources/{resourceId}/sections/{sectionId}/study-status \
  -H "Content-Type: application/json" \
  -d '{
    "studiedAt": "2026-06-07T10:00:00Z"
  }'
```

**確認ポイント**
- HTTP 200 OK
- `studiedAt` が設定されている
- Section が 1 件だけの Resource なら、この時点で Lv.1 自動到達の条件を満たす

---

## 8. Progress 取得

```bash
curl -s http://localhost:8080/resources/{resourceId}/progress
```

**確認ポイント**
- HTTP 200 OK
- `status` が `IN_PROGRESS` になっている
- `lastStudiedAt` が更新されている
- Section 全件学習済みなら `currentLevel` が `1`、`initialStudiedAt` がセットされている

---

## 9. LevelHistory 取得

```bash
curl -s http://localhost:8080/resources/{resourceId}/level-histories
```

**確認ポイント**
- HTTP 200 OK
- Lv.1 の履歴が 1 件ある（Section 自動到達経路）
- `sourceType` が `SECTION_STUDY_STATUS`、`reasonCode` が `ALL_SECTIONS_STUDIED`

---

## 10. complete-initial-study（Lv.1 明示到達）

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/progress/complete-initial-study
```

**確認ポイント**
- HTTP 200 OK
- すでに Lv.1 でも `currentLevel` は下がらない
- LevelHistory Lv.1 は重複作成されない（件数は Step 9 と同じ）
- 別経路として `sourceType=INITIAL_STUDY_COMPLETION` の履歴は、Step 9 時点ではまだ無い Resource で確認するのが分かりやすい

---

## 11. StudyMemo 作成

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/memos \
  -H "Content-Type: application/json" \
  -d '{
    "resourceSectionId": {sectionId},
    "memoType": "LEARNED",
    "content": "HTTPの基本を理解した"
  }'
```

**確認ポイント**
- HTTP 201 Created
- `studyMemoId` が返る（以降 `{memoId}` に使う）
- `memoType` / `content` / `resourceSectionId` が正しい
- Progress の `lastStudiedAt` は更新されるが Level は上がらない

---

## 12. StudyMemo 一覧取得

```bash
curl -s http://localhost:8080/resources/{resourceId}/memos
```

**確認ポイント**
- HTTP 200 OK
- Step 11 のメモが `createdAt` 降順で返る
- `content` 全文が含まれる

---

## 13. StudyMemo 更新

```bash
curl -s -X PATCH http://localhost:8080/resources/{resourceId}/memos/{memoId} \
  -H "Content-Type: application/json" \
  -d '{
    "content": "HTTPの基本を理解した（更新版）",
    "memoType": "QUESTION"
  }'
```

**確認ポイント**
- HTTP 200 OK
- `content` / `memoType` が更新されている
- `updatedAt` が変わっている

---

## 14. StudyMemo 削除

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  -X DELETE http://localhost:8080/resources/{resourceId}/memos/{memoId}
```

**確認ポイント**
- HTTP 204 No Content
- 再度 Step 12 を実行すると、削除したメモは一覧に出ない

---

## 15. Resource Detail 取得（統合詳細）

Phase 8 で実装済み。Resource に紐づく進捗・Section・Memo・LevelHistory・LearningSessionRecord を 1 リクエストでまとめて取得する。

```bash
curl -i http://localhost:8080/resources/{resourceId}/details
```

レスポンス本文だけ見る場合:

```bash
curl -s http://localhost:8080/resources/{resourceId}/details
```

**確認対象**

```http
GET /resources/{resourceId}/details
```

**目的**

- Resource / Progress / Sections + StudyStatus / StudyMemo / LevelHistory / LearningSessionRecord をまとめて取得できること
- 子データが 0 件でも空配列で返ること
- 既存 `GET /resources/{resourceId}`（Step 3）は変更しないこと

**確認ポイント**

- HTTP 200 OK
- レスポンスに以下のトップレベルキーが含まれる
  - `resource`
  - `progress`
  - `sections`
  - `memos`
  - `levelHistories`
  - `learningSessionRecords`
- `sections` は `sectionOrder` 昇順
- `sections[].studyStatus` が含まれる（未学習 Section は `studiedAt: null` になりうる）
- `memos` は `createdAt` 降順
- `levelHistories` は `createdAt` 昇順
- `learningSessionRecords` は `createdAt` 降順
- `learningSessionRecords[].conceptTags` は配列で返る（DB 上はカンマ区切り文字列）
- raw 回答ログは返らない
- `resultDraft` は返らない

**推奨タイミング**

- Step 5〜12 実行後 … Sections / Memos / LevelHistory 等が入った状態で確認しやすい
- `memos` を含む状態で確認したい場合は、Step 14（StudyMemo 削除）の**前**に Step 15 を実行するか、削除後に再度 StudyMemo を作成（Step 11）してから確認する
- Step 1 直後 … 子データ 0 件の空配列確認にも使える

**子データ 0 件の確認**

Sections / Memos / LevelHistories / LearningSessionRecords が 0 件でも、404 ではなく空配列で返る。

```json
{
  "sections": [],
  "memos": [],
  "levelHistories": [],
  "learningSessionRecords": []
}
```

**注意（本 API の性質）**

- 参照専用（DB 更新しない）
- Level 更新しない
- LearningSession 作成しない
- 既存 `GET /resources/{resourceId}`（Step 3）とは別 API

---

## 16. LearningSession — IMMEDIATE_REFLECTION（start）

AI 連携は未実装。`aiPrompt` / `resultDraft` は固定文言。  
前提: Step 7 以降で Lv.1 到達済みの Resource を使うと分かりやすい。

正常フロー全体:

```text
start → responses → responses → complete → record
→ Progress.currentLevel が Lv.2 以上
→ LevelHistory に Lv.2 が作成される
```

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/learning-sessions \
  -H "Content-Type: application/json" \
  -d '{
    "sessionType": "IMMEDIATE_REFLECTION"
  }'
```

**確認ポイント**
- HTTP 201 Created
- `learningSessionId` が返る（以降 `{learningSessionId}` に使う）
- `sessionType` が `IMMEDIATE_REFLECTION`
- `status` が `IN_PROGRESS`
- `step.currentStep` が `1`
- `step.totalSteps` が `3`
- `nextAction.type` が `SUBMIT_RESPONSE`

---

## 17. LearningSession — IMMEDIATE_REFLECTION（responses 1回目）

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/learning-sessions/{learningSessionId}/responses \
  -H "Content-Type: application/json" \
  -d '{
    "responseText": "REST APIではリソースをURIで表現し、HTTPメソッドで操作を表します。"
  }'
```

**確認ポイント**
- HTTP 200 OK
- `step.currentStep` が `2`
- `status` は `IN_PROGRESS` のまま
- raw 回答本文は DB 保存されない
- `nextAction.type` が `SUBMIT_RESPONSE`

---

## 18. LearningSession — IMMEDIATE_REFLECTION（responses 2回目）

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/learning-sessions/{learningSessionId}/responses \
  -H "Content-Type: application/json" \
  -d '{
    "responseText": "PUTとPATCHの違いを、Progress更新APIの設計に活かせます。"
  }'
```

**確認ポイント**
- HTTP 200 OK
- `step.currentStep` が `3`
- `step.totalSteps` が `3`
- `nextAction.type` が `COMPLETE_SESSION`

---

## 19. LearningSession — IMMEDIATE_REFLECTION（complete）

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/learning-sessions/{learningSessionId}/complete
```

**確認ポイント**
- HTTP 200 OK
- `status` が `COMPLETED`
- `completedAt` が入る
- `resultDraft` が返る
- `nextAction.type` が `SAVE_RECORD`
- `resultDraft` は DB 保存されない

---

## 20. LearningSession — IMMEDIATE_REFLECTION（record）

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/learning-sessions/{learningSessionId}/record \
  -H "Content-Type: application/json" \
  -d '{
    "summary": "REST APIの基本とHTTPメソッドの使い分けを振り返った",
    "conceptTags": ["REST", "HTTP", "PATCH"],
    "weakPointSummary": "PUTとPATCHの使い分けにはまだ復習余地がある",
    "nextAction": "Progress更新APIのPATCH設計を再確認する",
    "aiAssessment": "NEEDS_REVIEW"
  }'
```

**確認ポイント**
- HTTP 201 Created
- LearningSessionRecord が作成される
- `conceptTags` が配列で返る
- LearningSession の `status` が `RECORD_SAVED` になる
- `aiAssessment = NEEDS_REVIEW` でも Lv 到達対象になる

---

## 21. LearningSession — IMMEDIATE_REFLECTION 後の Progress / LevelHistory 確認

```bash
curl -s http://localhost:8080/resources/{resourceId}/progress
```

**確認ポイント**
- `currentLevel` が `2` 以上になる
- 既に Lv.3 なら下がらない

```bash
curl -s http://localhost:8080/resources/{resourceId}/level-histories
```

**確認ポイント**
- Lv.2 の履歴がある
- `sourceType` が `LEARNING_SESSION_RECORD`
- `reasonCode` が `IMMEDIATE_REFLECTION_RECORDED`
- `sourceId` が `learningSessionRecordId`

---

## 22. LearningSession — DELAYED_RECALL 正常フロー

前提: Step 16〜21 で Lv.2 到達済み（`currentLevel >= 2`）。

正常フロー全体:

```text
start DELAYED_RECALL → responses → responses → complete → record
→ Progress.currentLevel が Lv.3 以上
→ LevelHistory に Lv.3 が作成される
```

### start

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/learning-sessions \
  -H "Content-Type: application/json" \
  -d '{
    "sessionType": "DELAYED_RECALL"
  }'
```

**確認ポイント**
- Lv.2 未満では開始できない（エラーは Step 25-1 参照）
- Lv.2 以上なら HTTP 201 Created
- `sessionType` が `DELAYED_RECALL`
- `status` が `IN_PROGRESS`
- 返った `learningSessionIdRecall` を以降の curl に使う（Step 17〜19 と同じパス・本文で可）

### responses / complete

Step 17〜19 と同じ curl を、`{learningSessionIdRecall}` に差し替えて 2 回実行する（responses）→ complete。

### record

Step 20 と同様の record curl を、`{learningSessionIdRecall}` で実行する。

**確認ポイント（record 後）**
- `reasonCode` が `DELAYED_RECALL_RECORDED`
- `sourceType` が `LEARNING_SESSION_RECORD`
- Progress の `currentLevel` が `3` 以上になる
- LevelHistory に Lv.3 が 1 件作成される（重複しない）

---

## 23. LearningSession — discard 確認

Step 16〜21 で `RECORD_SAVED` になった Session は破棄不可のため、**新しい Session** を作って確認する。  
同じ Resource でよい（`RECORD_SAVED` は active 制約の対象外）。

### start（破棄用の新規 Session）

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/learning-sessions \
  -H "Content-Type: application/json" \
  -d '{
    "sessionType": "IMMEDIATE_REFLECTION"
  }'
```

返った `learningSessionIdDiscard` を破棄用に使う。

### discard

```bash
curl -s -X POST http://localhost:8080/resources/{resourceId}/learning-sessions/{learningSessionIdDiscard}/discard
```

**確認ポイント**
- HTTP 200 OK
- `status` が `DISCARDED`
- LearningSessionRecord は作成されない
- Progress の `currentLevel` は更新されない
- LevelHistory は作成されない
- `IN_PROGRESS` / `COMPLETED` から破棄可能
- `RECORD_SAVED` / `DISCARDED` からは破棄不可（Step 25-6 参照）

---

## 24. LearningSessionRecord 保存後の Resource Detail 確認

```bash
curl -s http://localhost:8080/resources/{resourceId}/details
```

**確認ポイント**
- `learningSessionRecords` に保存した Record が含まれる
- `conceptTags` が配列で返る
- `levelHistories` に Lv.2 / Lv.3 の履歴が含まれる
- raw 回答ログは返らない
- `resultDraft` は返らない

---

## 25. LearningSession — エラー系

### 25-1. DELAYED_RECALL を Lv.2 未満で開始

新規 Resource 作成直後など、`currentLevel` が 2 未満の状態で確認する（Step 1 のみ実行した Resource 等）。

```bash
curl -i -X POST http://localhost:8080/resources/{resourceId}/learning-sessions \
  -H "Content-Type: application/json" \
  -d '{
    "sessionType": "DELAYED_RECALL"
  }'
```

**確認ポイント**
- HTTP 400 Bad Request
- JSON に `"code": "LEVEL_REQUIREMENT_NOT_MET"`

### 25-2. active session 重複

同じ `userId + resourceId + sessionType` で `IN_PROGRESS` または `COMPLETED` の Session がある状態で、再度 start する。

```bash
curl -i -X POST http://localhost:8080/resources/{resourceId}/learning-sessions \
  -H "Content-Type: application/json" \
  -d '{
    "sessionType": "IMMEDIATE_REFLECTION"
  }'
```

**確認ポイント**
- HTTP 409 Conflict
- JSON に `"code": "SESSION_ALREADY_IN_PROGRESS"`

### 25-3. responses 不可状態

`COMPLETED` / `RECORD_SAVED` / `DISCARDED` の Session に responses を送る。

```bash
curl -i -X POST http://localhost:8080/resources/{resourceId}/learning-sessions/{learningSessionId}/responses \
  -H "Content-Type: application/json" \
  -d '{
    "responseText": "回答テキスト"
  }'
```

**確認ポイント**
- HTTP 400 Bad Request
- JSON に `"code": "LEARNING_SESSION_CANNOT_ACCEPT_RESPONSE"`

### 25-4. complete 不可状態

`currentStep < totalSteps` の状態（responses 未完了）で complete する。

```bash
curl -i -X POST http://localhost:8080/resources/{resourceId}/learning-sessions/{learningSessionId}/complete
```

**確認ポイント**
- HTTP 400 Bad Request
- JSON に `"code": "LEARNING_SESSION_CANNOT_BE_COMPLETED"`

### 25-5. record 保存不可

以下のいずれかで確認する。

- `status != COMPLETED`（`IN_PROGRESS` のまま record）
- `aiAssessment = OFF_TOPIC`
- 同一 LearningSession から Record を 2 回保存

```bash
curl -i -X POST http://localhost:8080/resources/{resourceId}/learning-sessions/{learningSessionId}/record \
  -H "Content-Type: application/json" \
  -d '{
    "summary": "テスト",
    "aiAssessment": "OFF_TOPIC"
  }'
```

**確認ポイント**
- HTTP 400 Bad Request
- JSON に `"code": "LEARNING_SESSION_RECORD_CANNOT_BE_SAVED"`

### 25-6. discard 不可

`RECORD_SAVED` または `DISCARDED` の Session を discard する（Step 20 完了後の Session 等）。

```bash
curl -i -X POST http://localhost:8080/resources/{resourceId}/learning-sessions/{learningSessionId}/discard
```

**確認ポイント**
- HTTP 400 Bad Request
- JSON に `"code": "LEARNING_SESSION_CANNOT_BE_DISCARDED"`

---

## 26. 存在しない Resource で 404 確認

```bash
curl -s http://localhost:8080/resources/999999
```

**確認ポイント**
- HTTP 404 Not Found
- JSON に `"code": "RESOURCE_NOT_FOUND"` が含まれる

---

## 27. Resource Detail — 存在しない Resource で 404 確認

```bash
curl -i http://localhost:8080/resources/999999/details
```

**確認ポイント**
- HTTP 404 Not Found
- JSON に `"code": "RESOURCE_NOT_FOUND"` が含まれる

---

## 28. Resource Detail — Progress 不存在で 404 確認（補足）

通常は Resource 作成時に Progress も自動作成されるため、本番相当の操作だけでは再現しにくい。  
DB を直接操作して `progresses` 行を削除した場合の確認用。

**確認ポイント**
- HTTP 404 Not Found
- JSON に `"code": "PROGRESS_NOT_FOUND"` が含まれる

---

## 未実装 API（参考）

以下は現時点では未実装。

```text
PATCH  /resources/{resourceId}/progress
PATCH  /resources/{resourceId}/sections/{sectionId}
DELETE /resources/{resourceId}/sections/{sectionId}
GET    /resources/{resourceId}/sections/{sectionId}/study-status
GET    /resources/{resourceId}/memos/{memoId}
```

※ `GET /resources/{resourceId}/details`（Resource 統合詳細）は Step 15 参照。  
※ LearningSession 系 API は Step 16〜25 参照。

---

## 手動確認履歴

| 項目 | 内容 |
|------|------|
| 実施日（Step 1〜14） | 2026-06-07 |
| 実施日（Step 15〜25、27〜28） | 2026-06-13 |
| 環境 | local / localhost:8080 / PostgreSQL Docker Compose |
| 前提 | DB を初期化した状態から確認 |

### 確認済み（2026-06-07）

```text
Resource 作成
Progress 自動作成
Section 作成
SectionStudyStatus 自動作成
SectionStudyStatus 更新
全 Section 学習済みによる Lv.1 自動到達
Progress 取得
LevelHistory 取得
StudyMemo 作成
StudyMemo 一覧取得
StudyMemo 更新
StudyMemo 論理削除
存在しない Resource で 404 確認
```

**結果:** 期待通り動作

### 確認済み（2026-06-13）

```text
Resource Detail 取得（統合詳細）
LearningSession IMMEDIATE_REFLECTION 正常フロー
LearningSession DELAYED_RECALL 正常フロー
LearningSession discard
LearningSession エラー系
Resource Detail — 存在しない Resource で 404 確認
Resource Detail — Progress 不存在で 404 確認
```

**結果:** 期待通り動作
