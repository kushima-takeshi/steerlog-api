# 06-implementation-rules.md

# SteerLog MVP Implementation Rules

## 目的

このドキュメントは、SteerLog MVPを実装する際の共通ルールを定義する。  
AIコード生成、Service実装、Repository実装、テスト作成、レビュー時は、この内容を必ず確認する。

---

# 1. 最重要ルール

```text
user_idで所有者チェックする
deleted_at対象ではdeleted_at is null を見る
currentLevelを外部から直接更新しない
LearningSessionRecordを直接CRUDしない
LevelHistoryを直接CRUDしない
raw回答ログをDBに正式保存しない
MVP外のテーブルやAPIを勝手に作らない
```

---

# 2. 認可ルール

## 2.1 user_idチェック必須

すべての内部APIでは、対象データが現在ユーザーのものであることを確認する。

例：

```sql
SELECT *
FROM resources
WHERE resource_id = :resourceId
  AND user_id = :userId
  AND deleted_at IS NULL;
```

## 2.2 UUIDは認可の代替ではない

将来的にUUIDやpublicIdを使っても、認可チェックは必須。

```text
IDが推測しづらいこと
= アクセスしてよいこと
```

ではない。

---

# 3. deleted_atルール

## 3.1 論理削除対象

```text
resources
resource_sections
study_memos
```

これらを取得する通常APIでは、必ず `deleted_at IS NULL` を条件に含める。

## 3.2 論理削除しない対象

```text
progresses
section_study_statuses
learning_sessions
learning_session_records
level_histories
```

理由：

```text
progresses / section_study_statuses は親Resource/Sectionの表示状態に従う
learning_sessions は status=DISCARDED で破棄を表す
learning_session_records は正式証跡
level_histories は初到達履歴
```

---

# 4. トランザクションルール

以下は必ずトランザクションで処理する。

## 4.1 Resource作成

```text
Resource作成
Progress作成
```

片方だけ作成される状態を避ける。

---

## 4.2 Section作成

```text
ResourceSection作成
SectionStudyStatus作成
```

片方だけ作成される状態を避ける。

---

## 4.3 SectionStudyStatus更新

```text
SectionStudyStatus更新
Progress更新
Lv.1判定
LevelHistory作成
```

---

## 4.4 complete-initial-study

```text
Progress.initialStudiedAt更新
Progress.currentLevel更新
LevelHistory Lv.1作成
```

---

## 4.5 StudyMemo作成

```text
StudyMemo作成
Progress.status/lastStudiedAt更新
```

---

## 4.6 LearningSessionRecord保存

```text
LearningSessionRecord作成
LearningSession.status更新
Progress.lastStudiedAt更新
Progress.currentLevel更新
LevelHistory作成
```

---

# 5. Progress更新ルール

## 5.1 currentLevel直接更新禁止

`currentLevel` は外部APIから直接更新させない。

禁止：

```json
{
  "currentLevel": 2
}
```

許可される更新経路：

```text
complete-initial-study
全Section学習済み判定
LearningSessionRecord保存
```

---

## 5.2 StudyMemo作成時

StudyMemo作成時に更新するもの：

```text
Progress.status が NOT_STARTED なら IN_PROGRESS
Progress.lastStudiedAt = now
```

更新しないもの：

```text
currentLevel
currentSectionId
completedAt
```

---

## 5.3 SectionStudyStatus更新時

更新するもの：

```text
Progress.status が NOT_STARTED なら IN_PROGRESS
Progress.lastStudiedAt = now
全Section学習済みならLv.1判定
```

更新しないもの：

```text
currentSectionId
completedAt
StudyMemo
```

---

## 5.4 currentSectionId

`currentSectionId` は、現在メインで学習しているSectionを表す。  
最後にメモを書いたSectionや、最後に更新したSectionではない。

そのため、StudyMemo作成やSectionStudyStatus更新で自動変更しない。

---

## 5.5 ARCHIVED

`ARCHIVED` は学習対象から外した状態。

ARCHIVEDにする場合：

```text
status = ARCHIVED
archivedAt = now
archiveReason = 入力値。任意
```

ARCHIVEDから戻しても、`archivedAt` と `archiveReason` は履歴として残す。

---

# 6. LevelHistoryルール

## 6.1 初到達履歴

LevelHistoryは、各Levelへの初到達履歴である。

```text
userId + resourceId + level
```

で一意。

## 6.2 重複作成禁止

同じLevelのLevelHistoryが既にある場合、新しく作らない。

## 6.3 直接CRUD禁止

LevelHistoryは以下のAPIで直接作らない。

```text
POST /level-histories
PATCH /level-histories/{id}
DELETE /level-histories/{id}
```

Level到達処理の結果として作る。

---

# 7. LearningSessionルール

## 7.1 rawログ正式保存禁止

以下をDBに正式保存しない。

```text
ユーザーの生回答全文
AI質問履歴全文
会話ログ全文
回答履歴全文
```

## 7.2 resultDraft

complete時にAI生成結果を `learning_sessions.result_draft` に保存する。

record保存時は、RequestのAI生成内容ではなく、DB上の `result_draft` を使う。

## 7.3 保存前確認必須

complete後、resultDraftを画面表示し、ユーザー確認後にrecord保存する。

保存しない場合：

```text
LearningSession.status = DISCARDED
LearningSessionRecordなし
Progress.currentLevel更新なし
LevelHistoryなし
```

## 7.4 AI生成部分編集不可

ユーザーが編集できるのは `userComment` のみ。

編集不可：

```text
summary
conceptTags
weakPointTags
weakPointSummary
nextAction
aiAssessment
generationBasis
```

## 7.5 OFF_TOPIC

`aiAssessment = OFF_TOPIC` の場合、LearningSessionRecord保存不可。  
Level更新不可。

---

# 8. StudyMemoルール

## 8.1 Level条件ではない

StudyMemoは任意の短いメモ。  
StudyMemo作成でLevelを上げない。

## 8.2 長文ノート化しない

StudyMemoは1〜500文字程度に制限する。  
本文丸写し・長文ノート化は避ける。

## 8.3 importantフラグなし

MVPでは `important` フラグを持たせない。

---

# 9. Resource / Section整合性

Section操作では、必ず対象Sectionが対象Resource配下であることを確認する。

例：

```sql
SELECT *
FROM resource_sections
WHERE resource_section_id = :sectionId
  AND resource_id = :resourceId
  AND user_id = :userId
  AND deleted_at IS NULL;
```

StudyMemoで `resourceSectionId` を指定する場合も同様。

SectionStudyStatusの `resource_id` と `resource_section_id` の整合性はアプリケーション側で確認する。

---

# 10. DTO / APIレスポンスルール

## 10.1 Listではpreview

StudyMemo一覧では全文ではなくpreviewを返す。

## 10.2 Detailでは必要情報をまとめる

Resource詳細では、MVP後半で以下をまとめて返す。

```text
Resource本体
Progress
Sections + StudyStatus
RecentMemos
LatestLearningSessionRecords
LevelHistories
```

MVP初期ではResource + Progress + Sections + StudyStatusでよい。

---

# 11. AIコード生成時に作らせないもの

```text
check_records
check_questions
review_records
answers
learning_session_messages
learning_session_raw_answers
progress.note
progress.total_study_time
study_memos.important
artifact tables
defense tables
galaxy tables
tag normalization tables
learning_cycles
mcp tables
external_llm_logs
```

---

# 12. テスト観点

## 12.1 Resource作成

```text
Resourceが作成される
Progressが同時にNOT_STARTEDで作成される
userIdが保存される
```

## 12.2 Section作成

```text
Sectionが作成される
SectionStudyStatusが同時に作成される
```

## 12.3 SectionStudyStatus更新

```text
studiedAt更新
understandingLevel更新
Progress.lastStudiedAt更新
NOT_STARTEDならIN_PROGRESS
全Section学習済みでLv.1
```

## 12.4 StudyMemo作成

```text
StudyMemo作成
Progress.lastStudiedAt更新
Levelは上がらない
```

## 12.5 LearningSessionRecord保存

```text
status=COMPLETEDのみ保存可能
resultDraft必須
OFF_TOPICは保存不可
LearningSessionRecord作成
LearningSession.status=RECORD_SAVED
Progress.currentLevel更新
LevelHistory作成
```

---

# 13. まとめ

実装では、便利さよりも以下を優先する。

```text
証跡の整合性
所有者チェック
Level更新の一貫性
正式証跡と一時データの分離
MVP境界の維持
```

AI生成コードは必ずこのルールに照らしてレビューする。
