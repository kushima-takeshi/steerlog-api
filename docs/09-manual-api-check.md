# 09-manual-api-check.md

# SteerLog API 手動動作確認（curl）

## 目的

実装済み API を `localhost:8080` 上で順番に確認するための curl 集。  
認証は未実装のため、ヘッダーは不要。

## 使い方

1. アプリを起動する（`mvn spring-boot:run` 等）
2. 下の curl を上から順に実行する
3. 各ステップのレスポンスから ID を控え、次の curl の `{resourceId}` / `{sectionId}` / `{memoId}` に置き換える

```text
{resourceId}  … Step 1 の resourceId
{sectionId}   … Step 5 の resourceSectionId
{memoId}      … Step 11 の studyMemoId
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

## 15. 存在しない Resource で 404 確認

```bash
curl -s http://localhost:8080/resources/999999
```

**確認ポイント**
- HTTP 404 Not Found
- JSON に `"code": "RESOURCE_NOT_FOUND"` が含まれる

---

## 未実装 API（参考）

以下は現時点では未実装。

```text
PATCH  /resources/{resourceId}/progress
PATCH  /resources/{resourceId}/sections/{sectionId}
DELETE /resources/{resourceId}/sections/{sectionId}
GET    /resources/{resourceId}/sections/{sectionId}/study-status
GET    /resources/{resourceId}/memos/{memoId}
LearningSession 関連 API 一式
LearningSessionRecord 保存 API
Resource 詳細への Sections / Memos / Records 統合表示
```
