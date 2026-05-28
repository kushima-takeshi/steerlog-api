# 01-mvp-scope.md

# SteerLog MVP Scope

## 目的

このドキュメントは、SteerLog MVPで実装する範囲、MVP外にする範囲、旧方針として採用しない範囲を定義する。

AIコード生成、設計判断、実装レビューでは、このファイルを参照し、MVP外の機能を勝手に実装しないこと。

---

# 1. MVPの基本方針

SteerLog MVPでは、以下を最優先にする。

```text
Resourceを登録できる
ResourceのSectionを管理できる
Resource単位のProgressを管理できる
Section単位の学習状態を残せる
短いStudyMemoを残せる
Lv.1〜Lv.3の学習証跡を作れる
LearningSessionでReflection / Recallを実行できる
LearningSessionRecordとして正式証跡を保存できる
LevelHistoryでLevel初到達履歴を残せる
```

MVPの目的は、SteerLogの最小価値である、

```text
学習対象
学習状態
学習メモ
振り返り証跡
想起証跡
Level初到達履歴
```

を一通り動かすことである。

---

# 2. MVPで実装するLevel

MVPでは、Lv.1〜Lv.3までを実装対象にする。

```text
Lv.1 = 初回学習済み / 一通り触れた状態
Lv.2 = Immediate Reflectionの正式証跡がある状態
Lv.3 = Delayed Recallの正式証跡がある状態
```

Lv.4 / Lv.5 はMVPでは実装しない。

```text
Lv.4 = Artifact / 成果物による証跡
Lv.5 = Defense / 説明・防衛できる状態
```

ただし、DB上の `current_level` や `level_histories.level` は、将来拡張を見据えて1〜5を許容してよい。

---

# 3. MVPで実装する主要テーブル

MVPでは、以下の8テーブルを中心にする。

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

---

# 4. MVPで実装する機能

## 4.1 Resource管理

### 実装する

```text
Resource登録
Resource一覧取得
Resource詳細取得
Resource更新
Resource論理削除
Resource登録時のProgress自動作成
```

### 対象となるResourceType

MVPでは以下を扱えるようにする。

```text
BOOK
ARTICLE
VIDEO
COURSE
PROBLEM
IMPLEMENTATION
DOCUMENTATION
OTHER
```

### 判断

Resourceは学習対象そのものを表す。  
ProgressはResourceに対するユーザーの学習状態を表す。  
この2つは分ける。

---

## 4.2 ResourceSection管理

### 実装する

```text
Section手動追加
Section一覧取得
Section更新
Section論理削除
Section追加時のSectionStudyStatus自動作成
```

### 判断

MVPでは、Sectionはまず手動追加でよい。  
AIによる目次・Section自動生成はMVP外とする。

---

## 4.3 Progress管理

### 実装する

```text
Progress取得
Progress更新
status更新
currentSectionId更新
archiveReason更新
complete-initial-study
```

### status

```text
NOT_STARTED
IN_PROGRESS
PAUSED
ARCHIVED
COMPLETED
```

### 判断

`current_level` はユーザーが直接更新しない。  
Level到達条件を満たしたときに、システム側で更新する。

`ARCHIVED` は削除ではなく、学習対象から一旦外した状態。  
`archived_at` と `archive_reason` は保持する。

---

## 4.4 SectionStudyStatus管理

### 実装する

```text
Section学習状態取得
Section学習状態更新
studiedAt更新
understandingLevel更新
全Section学習済みによるLv.1到達判定
```

### 判断

SectionStudyStatusは、Section単位の軽い学習状態を表す。

```text
studiedAt
understandingLevel
```

を持つ。

長文メモは持たせない。  
長文ではなく、Section単位の状態管理に限定する。

---

## 4.5 StudyMemo管理

### 実装する

```text
StudyMemo作成
StudyMemo一覧取得
StudyMemo詳細取得
StudyMemo更新
StudyMemo論理削除
```

### memoType

```text
GENERAL
LEARNED
QUESTION
WEAKNESS
TODO
IDEA
SUMMARY
```

### 判断

StudyMemoは、ユーザーが学習中に残す短い生メモである。

StudyMemoはLevel到達条件ではない。  
StudyMemo作成時にProgressの `last_studied_at` は更新してよいが、`current_level` は上げない。

MVPでは `tags TEXT[]` を使ってよい。  
タグ正規化テーブルはMVP外。

---

## 4.6 LearningSession

### 実装する

```text
LearningSession開始
responses API
complete API
discard API
```

### sessionType

```text
IMMEDIATE_REFLECTION
DELAYED_RECALL
```

### status

```text
IN_PROGRESS
COMPLETED
RECORD_SAVED
DISCARDED
```

### 判断

LearningSessionは、AI Reflection / Recallの実行単位である。  
MVPではResource単位とし、Section単位にはしない。

LearningSessionはチャットログ保存ではない。  
raw回答ログ、AI質問履歴、会話ログ全文は正式保存しない。

---

## 4.7 LearningSessionRecord

### 実装する

```text
LearningSession complete後のresultDraft表示
ユーザー確認後のrecord保存
Lv.2 / Lv.3到達処理
Progress更新
LevelHistory作成
```

### 判断

LearningSessionRecordは正式な学習証跡である。

AI生成部分はユーザーが編集できない。  
ユーザーが編集・追加できるのは `userComment` のみ。

保存前にresultDraftを画面表示し、ユーザーが納得した場合のみ保存する。  
保存しない場合はLevel更新しない。

---

## 4.8 LevelHistory

### 実装する

```text
Lv.1初到達履歴
Lv.2初到達履歴
Lv.3初到達履歴
LevelHistory一覧取得
```

### 判断

LevelHistoryは、Levelの初到達履歴である。  
証跡本文ではない。

同じResource・同じLevelのLevelHistoryは1回だけ作る。

LevelHistoryは直接CRUDしない。  
Level到達処理の結果として作成する。

---

# 5. MVPで実装するAPI

## 5.1 Resource API

```text
POST /resources
GET /resources
GET /resources/{resourceId}
PATCH /resources/{resourceId}
DELETE /resources/{resourceId}
```

## 5.2 ResourceSection API

```text
POST /resources/{resourceId}/sections
GET /resources/{resourceId}/sections
PATCH /resources/{resourceId}/sections/{sectionId}
DELETE /resources/{resourceId}/sections/{sectionId}
```

## 5.3 Progress API

```text
GET /resources/{resourceId}/progress
PATCH /resources/{resourceId}/progress
POST /resources/{resourceId}/progress/complete-initial-study
```

## 5.4 SectionStudyStatus API

```text
GET /resources/{resourceId}/sections/{sectionId}/study-status
PATCH /resources/{resourceId}/sections/{sectionId}/study-status
```

Resource詳細APIでSection + StudyStatusを返す場合、単独GETはMVP初期では省略可能。

## 5.5 StudyMemo API

```text
POST /resources/{resourceId}/memos
GET /resources/{resourceId}/memos
GET /resources/{resourceId}/memos/{memoId}
PATCH /resources/{resourceId}/memos/{memoId}
DELETE /resources/{resourceId}/memos/{memoId}
```

## 5.6 LearningSession API

```text
POST /resources/{resourceId}/learning-sessions
POST /learning-sessions/{learningSessionId}/responses
POST /learning-sessions/{learningSessionId}/complete
POST /learning-sessions/{learningSessionId}/record
POST /learning-sessions/{learningSessionId}/discard
```

## 5.7 LevelHistory API

```text
GET /resources/{resourceId}/level-histories
```

---

# 6. MVP外にするもの

以下はMVPでは実装しない。

```text
Lv.4 Artifact
Lv.5 Defense
Artifact登録
Artifact Tags
Defense Record
Galaxy API
Skill Map
プロフィール公開
Summary API
AI目次自動生成
目次写真/OCR登録
MCP連携
外部LLMログ全文インポート
学習時間管理
Pomodoro連携
LearningCycle / 再学習軸
タグ正規化テーブル本格実装
Resource推薦
学習計画自動生成
企業向け採用機能
```

---

# 7. MVP後に検討するもの

MVP外だが、将来的に検討するもの。

## 7.1 AI目次・Section自動生成

将来的には、以下の流れを検討する。

```text
Resource登録
AIがSection案を生成
ユーザーが確認
採用したSectionだけ保存
```

ただし、MVPでは手動追加を優先する。

また、著作物本文を大量に入力・保存しない。  
目次・章タイトル程度にとどめる。

---

## 7.2 Tag / Galaxy

将来的には、Resource、Section、LearningSessionRecord、Artifactなどから得られるタグを標準化し、Galaxy APIやプロフィール表示に使う。

ただし、MVPでは本格的なタグ正規化テーブルは作らない。

MVPでは以下のような軽量保持にとどめる。

```text
study_memos.tags TEXT[]
learning_session_records.concept_tags TEXT[]
learning_session_records.weak_point_tags TEXT[]
```

---

## 7.3 Lv.4 Artifact

将来的には、実装・設計・README・成果物などをArtifactとして登録し、Lv.4の証跡にする。

MVPでは実装しない。

---

## 7.4 Lv.5 Defense

将来的には、ユーザーが学習内容や設計判断を説明・防衛できるかを記録するDefense機能を検討する。

MVPでは実装しない。

---

## 7.5 再学習軸

将来的には、初回学習由来の証跡と、再学習後に育った理解を分けて扱う。

MVPでは `learning_cycle` や再学習専用テーブルは作らない。

---

## 7.6 MCP / 外部LLM連携

将来的には、外部LLMやMCPとの連携を検討する。

ただし、MVPでは外部LLMログ全文を取り込まない。  
将来的に実装する場合も、構造化された学習状態・証跡だけを扱う方針にする。

---

# 8. 旧方針として採用しないもの

以下は、過去の設計メモには出てきたが、現在のMVPでは採用しない。

```text
AIクイズ中心設計
CheckRecord
CheckQuestion
ReviewRecord
Answer API
正答率でLv.3
NotebookLM代替
本文丸ごと取り込み
Resource本文を大量投入して問題生成
StudyMemoをLevel必須条件にする
StudyMemo importantフラグ
Progress.note
ReviewRecord.memo
Progress.totalStudyTime
Pomodoro連携
外部LLMログ全文インポート
LearningSessionをSection単位にする
LearningSessionをチャットログ保存として扱う
LearningSessionRecordのAI生成部分をユーザー編集可能にする
LevelHistoryを一般CRUD対象にする
```

---

# 9. MVP境界の判断基準

機能を追加するか迷った場合は、以下の基準で判断する。

## MVPに入れるべきもの

```text
Lv.1〜Lv.3の証跡作成に必要
Resource詳細画面の基本体験に必要
Progress / Section / Memo / LearningSessionの整合性に必要
後から変えるとDBや設計が大きく壊れる
```

## MVP外にすべきもの

```text
プロフィール公開に関係する
Galaxy / 可視化に関係する
Lv.4 / Lv.5に関係する
AI自動化の高度化
外部連携
学習時間管理
タグの高度集計
最初の縦切りに不要
```

---

# 10. 最初のMVP到達ライン

最初のMVP開発では、いきなり全機能を作らない。

最初の到達ラインは以下。

```text
Resourceを登録する
Progressが自動作成される
Resource詳細でProgressが見える
Sectionを追加できる
SectionStudyStatusを更新できる
Resource全体を初回学習済みにできる
Lv.1のLevelHistoryが作成される
```

その後、以下へ進む。

```text
StudyMemo
LearningSession
LearningSessionRecord
Lv.2 / Lv.3
```

---

# 11. AIコード生成時の注意

AIに実装させる場合、このファイルの内容を前提にする。

特に以下を守る。

```text
MVP外の機能を勝手に実装しない
CheckRecord / ReviewRecordを作らない
Lv.4 / Lv.5を実装しない
Galaxy APIを実装しない
AI目次自動生成をMVPに入れない
タグ正規化テーブルを勝手に作らない
StudyMemoでLevelを上げない
currentLevelを外部入力で直接更新させない
LearningSessionRecordを直接CRUDにしない
```

---

# 12. まとめ

SteerLog MVPは、以下を作る。

```text
Resource
ResourceSection
Progress
SectionStudyStatus
StudyMemo
LearningSession
LearningSessionRecord
LevelHistory
Lv.1〜Lv.3
```

一方で、以下はMVP外とする。

```text
Lv.4
Lv.5
Galaxy
AI目次自動生成
MCP
外部LLMログ全文連携
学習時間管理
タグ本格実装
再学習軸
```

MVPでは、SteerLogの核である **Resource単位の学習証跡とLv.1〜Lv.3の到達体験** を最優先にする。
