# 07-implementation-order.md

# SteerLog MVP Implementation Order

## 目的

このドキュメントは、SteerLog MVPを実装する順序を定義する。  
AIコード生成、タスク分割、実装計画、スプリント計画では、この順序を基準にする。

MVPでは、全部を一気に作らず、まず縦に動く機能を作る。

---

# 1.1 現在地（2026-06）

```text
Phase 1: 土台                         ✅ 完了
Phase 2: Resource + Progress          ✅ 完了
Phase 3: Section + StudyStatus        ✅ 完了
Phase 4: Lv.1 + LevelHistory          ✅ 完了
Phase 5: StudyMemo                    ✅ 完了
Phase 6: LearningSession              ✅ 完了
Phase 7: Record + Lv.2/Lv.3           ✅ 完了
Phase 8: Resource Detail 統合詳細     ✅ 完了
```

最初の縦切り（§11）は完了済み。MVP コア縦切り（StudyMemo → LearningSession → Lv.2/Lv.3 → Resource Detail）まで到達済み。

手動確認（`docs/09-manual-api-check.md`）：

```text
Step 1〜14（Resource〜StudyMemo）              2026-06-07 確認済み
Step 15〜25、27〜28（Detail / LearningSession） 2026-06-13 確認済み
```

Flyway 実装番号（docs 当初案と異なる）：

```text
V1 resources
V2 progresses
V3 level_histories
V4 resource_sections
V5 section_study_statuses
V6 study_memos
V7 learning_sessions
V8 learning_session_records
V9 level_histories reasonCode（*_RECORDED 追加）
```

---

# 1. 実装方針

最初からすべてのテーブル・APIを作らない。

まずは以下の縦切りを作る。

```text
Resourceを登録する
Progressが自動作成される
Resource詳細でProgressが見える
Sectionを追加できる
SectionStudyStatusを更新できる
ResourceをLv.1にできる
LevelHistoryが作成される
```

この縦切りができてから、StudyMemo、LearningSession、Lv.2/Lv.3へ進む。

**現在地（2026-06）**: 上記に加え、StudyMemo CRUD、LearningSession、LearningSessionRecord、Lv.2/Lv.3 到達、Resource Detail 統合詳細まで実装済み。MVP コア（Phase 1〜8）は完了。

---

# 2. Phase一覧

```text
Phase 1: プロジェクト土台
Phase 2: Resource + Progress
Phase 3: ResourceSection + SectionStudyStatus
Phase 4: Lv.1 + LevelHistory
Phase 5: StudyMemo
Phase 6: LearningSession
Phase 7: LearningSessionRecord + Lv.2/Lv.3
Phase 8: Resource Detail 統合詳細
```

---

# 3. Phase 1：プロジェクト土台

**状態: ✅ 完了**（認証は未実装）

## 3.1 目的

Spring Bootアプリとして実装を進めるための最低限の土台を作る。

## 3.2 作るもの

```text
Spring Bootプロジェクト
PostgreSQL接続
Flyway
共通Entity
例外ハンドリング
バリデーション
仮userId取得
```

## 3.3 認証について

MVP初期では、認証を作り込まない。  
仮に `userId = 1` 固定でもよい。

ただし、Service/Repositoryでは必ず `user_id` を検索条件に含める。

---

# 4. Phase 2：Resource + Progress

**状態: ✅ 完了**（PATCH progress は未実装）

## 4.1 目的

Resource登録時にProgressも作られるところまで作る。

## 4.2 対象テーブル

```text
resources
progresses
```

## 4.3 作るAPI

```text
POST /resources                          ✅
GET /resources/{resourceId}              ✅
GET /resources                           ✅
PATCH /resources/{resourceId}            ✅
DELETE /resources/{resourceId}           ✅
GET /resources/{resourceId}/progress     ✅
PATCH /resources/{resourceId}/progress   ⬜
```

## 4.4 実装タスク

```text
V1__create_resources.sql                 ✅
V2__create_progresses.sql                ✅
Resource Entity                          ✅
Progress Entity                          ✅
ResourceRepository                       ✅
ProgressRepository                       ✅
ResourceService                          ✅
ResourceController                       ✅
Request/Response DTO                     ✅
POST /resources Service test             ✅
GET /resources/{resourceId} Service test ✅
```

## 4.5 完了条件

```text
Resourceを登録できる                              ✅
ProgressがNOT_STARTEDで自動作成される              ✅
Resource詳細でProgressが見える                    ✅
user_idで所有者チェックしている                   ✅
```

---

# 5. Phase 3：ResourceSection + SectionStudyStatus

**状態: ✅ 完了**（Section PATCH/DELETE、study-status GET は未実装）

## 5.1 目的

ResourceにSectionを追加し、Sectionごとの学習状態を管理できるようにする。

## 5.2 対象テーブル

```text
resource_sections
section_study_statuses
```

## 5.3 作るAPI

```text
POST /resources/{resourceId}/sections                              ✅
GET /resources/{resourceId}/sections                             ✅
PATCH /resources/{resourceId}/sections/{sectionId}/study-status  ✅
PATCH /resources/{resourceId}/sections/{sectionId}               ⬜
DELETE /resources/{resourceId}/sections/{sectionId}              ⬜
GET /resources/{resourceId}/sections/{sectionId}/study-status      ⬜
```

## 5.4 実装タスク

```text
V4__create_resource_sections.sql       ✅
V5__create_section_study_statuses.sql  ✅
ResourceSection / SectionStudyStatus Entity / Repository / Service / Controller  ✅
Service tests / Controller tests  ✅
```

※ 当初 docs では V3=sections だったが、実装では V3=level_histories。

## 5.5 完了条件

```text
Sectionを追加できる                                    ✅
Section追加時にSectionStudyStatusが作られる            ✅
SectionStudyStatusを更新できる                          ✅
Progress.status / lastStudiedAtが更新される             ✅
全Section学習済みでLv.1自動到達                          ✅
currentSectionIdは自動更新されない                      ✅
understandingLevelは未実装                              ⬜
```

---

# 6. Phase 4：Lv.1 + LevelHistory

**状態: ✅ 完了**

## 6.1 目的

SteerLogのLevel管理を最初に1本通す。  
まずLv.1だけ実装する。

## 6.2 対象テーブル

```text
level_histories
progresses
section_study_statuses
```

## 6.3 作るAPI

```text
POST /resources/{resourceId}/progress/complete-initial-study  ✅
GET /resources/{resourceId}/level-histories                   ✅
```

## 6.4 実装タスク

```text
V3__create_level_histories.sql   ✅
LevelHistory Entity / Repository / Service / Controller  ✅
completeInitialStudy             ✅
allSectionsStudied判定           ✅
LevelHistory重複防止             ✅
Service tests / Controller tests ✅
```

## 6.5 完了条件

```text
complete-initial-studyでLv.1になる              ✅
LevelHistory Lv.1が作成される                   ✅
全Section studiedAtありでもLv.1になる            ✅
同じLv.1のLevelHistoryが重複しない              ✅
studiedAtをnullに戻してもLevelHistoryは消えない  ✅（Level下げない方針）
```

---

# 7. Phase 5：StudyMemo

**状態: ✅ 完了**（GET detail / tags / important は未実装）

## 7.1 目的

学習中の短いメモを残せるようにする。

## 7.2 対象テーブル

```text
study_memos
```

## 7.3 作るAPI

```text
POST /resources/{resourceId}/memos                    ✅
GET /resources/{resourceId}/memos                     ✅
PATCH /resources/{resourceId}/memos/{memoId}          ✅
DELETE /resources/{resourceId}/memos/{memoId}         ✅
GET /resources/{resourceId}/memos/{memoId}            ⬜
```

## 7.4 実装タスク

```text
V6__create_study_memos.sql              ✅
StudyMemo Entity / StudyMemoType enum   ✅
StudyMemoRepository                     ✅
StudyMemoService / Controller           ✅
Request/Response DTO                    ✅
Service tests / Controller tests        ✅
```

## 7.5 完了条件

```text
メモを作成できる                              ✅
メモ一覧を取得できる                          ✅
メモ更新・論理削除できる                      ✅
Progress.lastStudiedAtが更新される（作成時）  ✅
StudyMemo作成でLevelは上がらない              ✅
tags / important flag は未実装                ⬜
```

---

# 8. Phase 6：LearningSession

**状態: ✅ 完了**

## 8.1 目的

AI Reflection / Recallのセッションを開始し、resultDraftを作れるようにする。

## 8.2 対象テーブル

```text
learning_sessions
```

## 8.3 作るAPI

```text
POST /resources/{resourceId}/learning-sessions                                              ✅
POST /resources/{resourceId}/learning-sessions/{learningSessionId}/responses                ✅
POST /resources/{resourceId}/learning-sessions/{learningSessionId}/complete                   ✅
POST /resources/{resourceId}/learning-sessions/{learningSessionId}/discard                    ✅
```

## 8.4 実装タスク

```text
V7__create_learning_sessions.sql          ✅
LearningSession Entity                    ✅
LearningSessionRepository                 ✅
LearningSessionService                    ✅
LearningSessionController                 ✅
AI mock service（固定 aiPrompt / resultDraft） ✅
Session state validation                  ✅
Service tests / Controller tests          ✅
```

## 8.5 AI連携

最初は本物のAI連携を入れなくてよい。  
固定のresultDraftを返すモックでよい。

目的はまず以下の状態遷移を動かすこと。

```text
IN_PROGRESS → COMPLETED
IN_PROGRESS → DISCARDED
COMPLETED → RECORD_SAVED
COMPLETED → DISCARDED
```

補足：

```text
COMPLETED 後は、正式証跡として保存する RECORD_SAVED ルートと、保存せず破棄する DISCARDED ルートがある
MVPでは長期resumeよりも、discardしてやり直せる流れを優先する
```

## 8.6 完了条件

```text
IMMEDIATE_REFLECTIONを開始できる                                    ✅
DELAYED_RECALLはLv.2未満で開始できない                              ✅
responsesで次のpromptを返せる                                       ✅
completeでresultDraftが作られる（レスポンスのみ、DB非保存）          ✅
discardできる                                                       ✅
```

---

# 9. Phase 7：LearningSessionRecord + Lv.2/Lv.3

**状態: ✅ 完了**

## 9.1 目的

resultDraftをユーザー確認後に正式証跡として保存し、Lv.2 / Lv.3を更新する。

## 9.2 対象テーブル

```text
learning_session_records
learning_sessions
progresses
level_histories
```

## 9.3 作るAPI

```text
POST /resources/{resourceId}/learning-sessions/{learningSessionId}/record  ✅
GET /resources/{resourceId}/level-histories                                ✅（Phase 4 で実装済み）
```

## 9.4 実装タスク

```text
V8__create_learning_session_records.sql     ✅
V9__add_level_history_reason_codes_for_record.sql  ✅
LearningSessionRecord Entity                ✅
LearningSessionRecordRepository             ✅
Record save service                         ✅
Lv.2 update                                 ✅
Lv.3 update                                 ✅
OFF_TOPIC save rejection                    ✅
Service tests / Controller tests            ✅
```

## 9.5 完了条件

```text
COMPLETEDのLearningSessionだけrecord保存できる     ✅
OFF_TOPICは保存できない                            ✅
LearningSessionRecordが作成される                  ✅
LearningSession.statusがRECORD_SAVEDになる         ✅
IMMEDIATE_REFLECTION保存でLv.2になる                 ✅
DELAYED_RECALL保存でLv.3になる                     ✅
LevelHistoryが作成される（reasonCode: *_RECORDED） ✅
同じLevelHistoryが重複しない                       ✅
```

---

# 10. Phase 8：Resource Detail 統合詳細

**状態: ✅ 完了**

## 10.1 目的

Resource詳細画面に必要な情報を整理して返せるようにする。

## 10.2 作るAPI

```text
GET /resources/{resourceId}/details  ✅
```

既存の `GET /resources/{resourceId}`（簡易詳細: Resource + Progress）は維持。

## 10.3 Resource詳細に含めるもの

```text
Resource本体
Progress
Sections + SectionStudyStatus
StudyMemos
LearningSessionRecords
LevelHistories
```

## 10.4 実装タスク

```text
ResourceDetailService / ResourceDetailController  ✅
Summary 系 DTO（ResourceDetailsResponse ほか）   ✅
SectionStudyStatus 一括取得 + Map（N+1 回避）      ✅
Service tests / Controller tests                  ✅
```

詳細設計は `docs/10-resource-detail-design.md` を参照。

## 10.5 完了条件

```text
Resource詳細だけで学習状態の概要が分かる        ✅
Sectionごとの状態が分かる                        ✅
StudyMemo 一覧が分かる                           ✅
LearningSessionRecord 一覧が分かる               ✅
Lv.1〜Lv.3の到達履歴が分かる                     ✅
```

---

# 11. 最初の縦切りタスク

**状態: ✅ 完了**

最初に作るべきAPI：

```text
POST /resources                                              ✅
GET /resources/{resourceId}                                ✅
POST /resources/{resourceId}/sections                        ✅
PATCH /resources/{resourceId}/sections/{sectionId}/study-status ✅
POST /resources/{resourceId}/progress/complete-initial-study ✅
GET /resources/{resourceId}/level-histories                  ✅
```

これで、最低限以下が実現できる。

```text
Resourceを登録する
Sectionを追加する
Sectionを学習済みにする
Resource全体を一通り学習済みにする
Lv.1に到達する
Lv.1の履歴を見る
```

---

# 12. AIコード生成の進め方

AIに依頼する場合は、以下のように小さく分ける。

悪い例：

```text
SteerLog MVPを全部実装して
```

良い例：

```text
resources と progresses のFlyway migrationだけ作成してください。
EntityやControllerはまだ作らないでください。
```

次に：

```text
Resource Entity と Progress Entityだけ作成してください。
ServiceやControllerはまだ作らないでください。
```

さらに：

```text
POST /resources のServiceだけ作成してください。
Resource作成とProgress作成を同一トランザクションで行ってください。
```

---

# 13. まとめ

## 13.1 完了済み（Phase 1〜8）

```text
1. プロジェクト土台                    ✅
2. Resource + Progress                 ✅
3. ResourceSection + SectionStudyStatus ✅
4. Lv.1 + LevelHistory                 ✅
5. StudyMemo                           ✅
6. LearningSession                     ✅
7. LearningSessionRecord + Lv.2/Lv.3   ✅
8. Resource Detail 統合詳細            ✅
```

MVP コア（Lv.1〜Lv.3 + LearningSession + Resource Detail 統合詳細）は実装完了。

## 13.2 未実装 / Next（MVP 内）

README.md の「未実装 / Next（MVP 内）」と整合：

```text
認証
PATCH /resources/{resourceId}/progress
GET /resources/{resourceId}/memos/{memoId}
GET /resources/{resourceId}/sections/{sectionId}/study-status
PATCH /resources/{resourceId}/sections/{sectionId}
DELETE /resources/{resourceId}/sections/{sectionId}
StudyMemo tags / important
```

## 13.3 MVP 外（まだ作らない）

```text
AI 連携（動的 aiPrompt / resultDraft）
Lv.4 / Lv.5
Galaxy / MCP / 学習時間管理
resultDraft / responseText の DB 保存
discardedAt / recordSavedAt 専用カラム
```
