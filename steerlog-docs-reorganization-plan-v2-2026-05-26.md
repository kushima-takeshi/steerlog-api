# SteerLog docs再編方針 V2：全アップロードファイル精査版（2026-05-26）

## 0. このファイルの目的

このファイルは、2026-05-02〜2026-05-26に作成・アップロードされたSteerLog設計メモを精査し、AIコード生成・実装作業に使う `docs/` 構成へ再編するための方針をまとめたもの。

前回作成した `steerlog-docs-reorganization-plan-2026-05-26.md` は一次整理版だった。  
このV2では、日付別メモを以下の観点で再整理する。

```text
1. 最新docsに採用する内容
2. archiveに判断履歴として残す内容
3. 旧方針として採用しない内容
4. MVP外として後回しにする内容
5. AIコード生成時に必ず読ませるべき内容
```

---

# 1. 精査対象ファイル

今回の再編対象は以下。

```text
steerlog-mvp-design-notes-2026-05-02.md
steerlog-mvp-design-notes-2026-05-03.md
steerlog-mvp-design-notes-2026-05-04.md
steerlog-mvp-design-notes-2026-05-05.md
steerlog-mvp-design-notes-2026-05-06.md
steerlog-mvp-design-notes-2026-05-10.md
steerlog-mvp-design-notes-2026-05-16.md
steerlog-mvp-design-notes-2026-05-17.md
steerlog-mvp-design-notes-2026-05-26.md
```

---

# 2. 最終的なdocs構成

AIコード生成・実装作業で読む最新版仕様は、以下に分割する。

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
  08-ai-development-workflow.md

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

## 前回案からの変更点

前回案では `00〜07` だったが、V2では以下を追加する。

```text
08-ai-development-workflow.md
```

理由：

2026-05-26の議論で、Cursor / Claude Code / ChatGPT / Markdown設計書を使ったAIコード生成方針がかなり具体化したため。  
これは単なる実装順序ではなく、開発プロセス・AIへの指示方法・レビュー観点に関わるため、独立ファイルにした方がよい。

---

# 3. 日付別メモの精査結果

## 3.1 2026-05-02

### この日の位置づけ

SteerLog MVPの初期設計メモ。  
Resource / Progress / StudyMemo / ReviewRecord などの初期構造を整理した段階。

### 最新docsに採用する内容

```text
ResourceとProgressを分ける
Resourceは学習対象そのもの
ProgressはユーザーとResourceの関係・進捗
Resource登録時にProgressを自動作成する
Resource削除は物理削除ではなく論理削除
Progress.noteは持たせない
StudyMemoとProgress.noteを混ぜない
再学習用にMVPでは別Resourceを作らない
将来的にLearningCycleのような概念を検討する
```

### 最新docsへの反映先

```text
00-product-principles.md
01-mvp-scope.md
02-db-design.md
03-api-design.md
06-implementation-rules.md
```

### archiveに残す内容

```text
ReviewRecord初期案
Lv.2 / Lv.3を確認・再確認で扱う初期案
再学習時の初期整理
Progress / Resource / StudyMemoの責務整理過程
```

### 採用しない旧方針

```text
ReviewRecord中心設計
ReviewRecordでLv.2 / Lv.3を管理する方針
reviewType = IMMEDIATE_CHECK / DELAYED_RECHECK
method = SELF_CHECK / SUMMARY / QUIZ / EXTERNAL_TEST
result = PASSED / FAILED / NEEDS_REVIEW
```

これらは後続でLearningSession / LearningSessionRecord方針に置き換わった。

---

## 3.2 2026-05-03

### この日の位置づけ

MVP範囲、Resource API、Progress API、LevelHistoryの初期方針を整理した日。  
この時点ではCheckRecord / AI選択式チェック中心の設計がまだ残っていた。

### 最新docsに採用する内容

```text
MVPはLv.1〜Lv.3まで
Lv.4 Artifact、Lv.5 Defense、Galaxy / Skill MapはMVP外
Evidence / 証跡を共通概念として扱う
currentLevelはResource全体の代表到達レベル
LevelHistoryは内部モデルとして持つ
completedAtとarchivedAtを分ける
Resource一覧にはProgress概要を含める
GET /resources/{resourceId} はResource基本情報・sections・progressを返す
PATCH /resources/{resourceId}/progress でユーザーが直接更新できるのはstatus/currentSectionId/archiveReason
currentLevelは通常PATCHで直接更新させない
```

### 最新docsへの反映先

```text
00-product-principles.md
01-mvp-scope.md
02-db-design.md
03-api-design.md
04-level-rules.md
06-implementation-rules.md
```

### archiveに残す内容

```text
CheckRecord採用前提の初期API設計
AI選択式チェックでLv.2/Lv.3を判定する方針
LevelHistoryをAPI化しない初期方針
needsReviewをresultではなくフラグにする案
```

### 採用しない旧方針

```text
Lv.2 / Lv.3をAI選択式チェックで判定する
CheckRecord.result = CHECKED / PASSED / NOT_PASSED
recentCheckRecords をResource詳細の中心にする
```

最新版では、CheckRecordではなくLearningSessionRecordを正式証跡にする。

---

## 3.3 2026-05-04

### この日の位置づけ

SteerLogの学習思想とLevel設計の根本を整理した日。  
ただし、具体実装はまだCheck / Answer API中心だった。

### 最新docsに採用する内容

```text
1周目は深く理解しきる段階ではなく、まず全体像・地図を作る段階
2周目以降で理解が深まる
定着には読むだけでなく思い出すことが必要
エンジニアにはアウトプットが必要
最終的には説明・Defenseできることが重要
Lv.1 = 一通り触れた / 地図を作った
Lv.4 = アウトプット・成果物で示せた
Lv.5 = 説明・Defenseできた
MVPではLv.1〜Lv.3のみ扱う
Section確認は補助確認であり、Resource全体Levelを直接上げないという考え方
```

### 最新docsへの反映先

```text
00-product-principles.md
01-mvp-scope.md
04-level-rules.md
```

### archiveに残す内容

```text
Check開始API
Answer API
CheckRecord作成
WeakPointService
LevelEvaluationService
Section確認/Resource全体確認の初期案
```

### 採用しない旧方針

```text
Check API / Answer API中心のLv.2/Lv.3判定
CheckRecordを正式証跡にする
問題文・選択肢・回答・採点に基づくAIチェック中心設計
```

最新版では、LearningSession / responses / complete / record に置き換える。

---

## 3.4 2026-05-05

### この日の位置づけ

SteerLogの方針転換において非常に重要。  
AIクイズ・NotebookLM的な本文QAアプリ化から距離を取り、LearningSession / Reflection / Recall中心へ移行するきっかけになった。

### 最新docsに採用する内容

```text
SteerLogはNotebookLMの代替ではない
著作物本文を大量に取り込むアプリにしない
AIクイズ中心ではなくReflection / Recall中心にする
AIは先生ではなく、学習後の振り返り・想起の聞き手
Lv.2 / Lv.3は理解保証ではなく、証跡段階として扱う
Galaxy APIは魅力的だがMVP外
ユーザーの技術的関心・好きな技術・読んだResourceの由来を将来的に見せる
```

### 最新docsへの反映先

```text
00-product-principles.md
01-mvp-scope.md
04-level-rules.md
05-learning-session-flow.md
```

### archiveに残す内容

```text
AIクイズ方針の見直し過程
NotebookLMとの差別化議論
著作権リスク整理
Galaxy API構想の初期議論
```

### 採用しない旧方針

```text
AIクイズ中心
NotebookLM代替
本文QAアプリ化
Resource本文を大量投入してAIに問題生成させる方向
正答率でLv.3判定
```

---

## 3.5 2026-05-06

### この日の位置づけ

Progress API、SectionStudyStatus API、StudyMemo API、LearningSession / LearningSessionRecordの入口方針を具体化した日。  
最新版docsへの反映量が多い。

### 最新docsに採用する内容

```text
Progress.statusは手動更新可能
COMPLETEDは無条件手動更新不可
NOT_STARTED / IN_PROGRESS / PAUSED / ARCHIVED は手動更新可能
currentSectionIdはProgress側に持つ
currentSectionIdは現在メインで学習しているSection
currentSectionId変更時は誤操作防止の確認ダイアログを出す
lastStudiedAtは持つ
totalStudyTime / Pomodoro連携はMVP外
IMPLEMENTATIONはResourceTypeとして扱えるが、自動COMPLETED判定はMVPではしない
SectionStudyStatusはSection単位の軽い状態のみを持つ
SectionStudyStatusには長文メモを持たせない
studiedAtはnullに戻せる
studiedAtをnullに戻してもProgress.currentLevelやLevelHistoryは下げない
StudyMemoは任意の短い生メモ
StudyMemoはLevel条件ではない
StudyMemo作成時はProgress.statusとlastStudiedAtだけ反映する
contentは1〜500文字
本文丸写し防止の注意文を表示する
StudyMemo一覧はpreviewを返し、全文は詳細で返す
StudyMemoにimportantフラグは持たせない
外部LLMログ全文インポートはMVP外
```

### 最新docsへの反映先

```text
02-db-design.md
03-api-design.md
04-level-rules.md
05-learning-session-flow.md
06-implementation-rules.md
```

### archiveに残す内容

```text
API設計の詳細化過程
LearningSession / LearningSessionRecordの入口整理
MCP / 外部LLM連携の将来案
```

### 採用しない/注意する旧方針

```text
StudyMemoをLevel必須条件にする
StudyMemoにimportantフラグを持たせる
ProgressにtotalStudyTimeを持たせる
Pomodoro連携をMVPに入れる
外部LLMログをそのまま取り込む
```

---

## 3.6 2026-05-10

### この日の位置づけ

LearningSession、LearningSessionRecord、Level到達条件、aiAssessment、状態遷移を強く固めた日。  
最新版のLearningSession仕様の中核。

### 最新docsに採用する内容

```text
LearningSessionはAI Reflection / Recallの実行単位
LearningSessionはResource単位、MVPではSection単位にしない
LearningSessionはチャットログ保管ではない
messagesではなくresponsesという用語を使う
raw会話ログ・ユーザー回答全文・AI質問全文は正式証跡として保存しない
LearningSessionRecordが正式証跡
LearningSession.status = IN_PROGRESS / COMPLETED / RECORD_SAVED / DISCARDED
IN_PROGRESS または COMPLETED の同一 user_id + resource_id + session_type セッションがあれば新規開始不可
DELAYED_RECALLはProgress.current_level >= 2でなければ開始不可
aiAssessment = PASSED / NEEDS_REVIEW / OFF_TOPIC
NEEDS_REVIEWでもLevel到達候補
OFF_TOPICは保存不可・Level対象外
confidenceLevelはMVPでは持たない
nextActionは必須
weakPointSummaryはNEEDS_REVIEW時必須
LearningSessionRecordのAI生成部分は編集不可
userCommentのみユーザー補足として許可
LevelHistoryは初到達履歴であり、証跡本文ではない
```

### 最新docsへの反映先

```text
04-level-rules.md
05-learning-session-flow.md
06-implementation-rules.md
02-db-design.md
03-api-design.md
```

### archiveに残す内容

```text
LearningSession API状態遷移の議論
Lv.2 / Lv.3証跡の意味整理
AI評価値の定義
LearningSessionRecordフィールド決定過程
```

---

## 3.7 2026-05-16

### この日の位置づけ

DB実装・認可・削除・日時方針を固めた日。  
実装時のルールとして非常に重要。

### 最新docsに採用する内容

```text
DBはPostgreSQL
内部学習管理テーブルはBIGINT
外部公開用は将来UUID / public_id / handle / slug
UUIDは認可の代わりではない
内部APIでは必ずuser_idで所有者チェックする
deleted_at対象のテーブルではdeleted_at is nullを見る
日時はtimestamptz
全主要テーブルにcreated_at / updated_at
論理削除対象にdeleted_at
resources / resource_sections / study_memos は論理削除
progresses / section_study_statuses は親Resource/Sectionの可視性に従う
learning_sessionsはdeleted_atではなくstatus=DISCARDEDで破棄を表現
learning_session_recordsとlevel_historiesは正式証跡・履歴としてMVPではユーザー削除なし
物理削除は通常MVP運用では使わない
```

### 最新docsへの反映先

```text
02-db-design.md
06-implementation-rules.md
07-implementation-order.md
```

### archiveに残す内容

```text
DB選定理由
ID設計の検討過程
削除ポリシーの検討過程
datetime方針の検討過程
```

---

## 3.8 2026-05-17

### この日の位置づけ

DB設計、Level、Galaxy、Tag、再学習軸を整理した日。  
MVPと将来構想の境界を明確化する材料として重要。

### 最新docsに採用する内容

```text
SteerLogは学習後の理解・印象・弱点・次アクションを証跡化するアプリ
MVPではGalaxy API / Lv.4 / Lv.5は作らない
MVPではResource / Section / StudyMemo / Lv.1〜Lv.3のReflection / Recallで証跡を残す
DBはPostgreSQL
Resourceは学習対象そのもの
Resource重複はDB制約で禁止しない
ResourceSectionはSection単位の状態やMemo、将来のSection Tagsのために分ける
ProgressはResource-wideな状態
SectionStudyStatusはSectionごとの軽い状態
StudyMemoは短い生メモ
LearningSessionはAI Reflection / Recallの実行単位
LearningSessionRecordは正式証跡
LevelHistoryは初到達履歴
Tag/Galaxyは重要だがMVPでは軽量に留める
LearningSessionRecordのタグはMVPではTEXT[]でもよい
再学習軸はMVP外
Lv.4 / Lv.5用テーブルはMVP外
```

### 最新docsへの反映先

```text
00-product-principles.md
01-mvp-scope.md
02-db-design.md
04-level-rules.md
05-learning-session-flow.md
06-implementation-rules.md
```

### archiveに残す内容

```text
Galaxy API構想
タグの光り方
初回由来・再学習由来・成果物由来
Evidence Origin
Artifact/Defense将来DB案
```

### MVP外として明示する内容

```text
tags / tag関連テーブルの本格実装
Galaxy API
Lv.4 Artifact
Lv.5 Defense
learning_cycle / 再学習軸
evidence_origin_type
artifact_tags
defense_record_tags
```

---

## 3.9 2026-05-26

### この日の位置づけ

現時点の実装前仕様に最も近い最新整理。  
DB、API、実装順序、AI開発フロー、docs再編方針の中心材料。

### 最新docsに採用する内容

```text
MVP主要8テーブル
resources
resource_sections
progresses
section_study_statuses
study_memos
learning_sessions
learning_session_records
level_histories
ProgressのARCHIVED意味
archived_at / archive_reasonを残す
LearningSessionにresult_draft JSONBを持つ
complete後にresultDraftを保存前確認する
AI生成部分は編集不可
保存しない場合はLevel更新しない
raw回答ログをDBに正式保存しない
主要API一覧
Spring Boot実装順序
Flyway
Cursor / Claude Code / ChatGPTの使い分け
Markdown設計書をAIコード生成用docsへ再編する
```

### 最新docsへの反映先

```text
00-product-principles.md
01-mvp-scope.md
02-db-design.md
03-api-design.md
04-level-rules.md
05-learning-session-flow.md
06-implementation-rules.md
07-implementation-order.md
08-ai-development-workflow.md
```

### archiveに残す内容

2026-05-26メモ自体も日付別判断履歴としてarchiveに保存する。  
ただし、最新版docs作成の主材料としても使う。

---

# 4. 最新docsごとの詳細構成

## 4.1 00-product-principles.md

### 目的

SteerLogのプロダクト思想をAIコード生成に固定する。  
AIに「普通のCRUDアプリ」「読書ログ」「クイズアプリ」「NotebookLM代替」と誤解させないための最上位方針。

### 反映元

```text
2026-05-03
2026-05-04
2026-05-05
2026-05-17
2026-05-26
```

### 入れる内容

```text
SteerLogは学習証跡アプリ
学習時間管理アプリではない
本文QAアプリ/NotebookLM代替ではない
AIが学習そのものを教えるアプリではない
AIの役割はReflection / Recallの聞き手
学習後の理解・印象・弱点・次アクションを証跡化する
Levelは理解保証ではなく証跡段階
初回学習は地図を作る段階
定着には時間を置いた想起が必要
エンジニアにはアウトプット・説明・Defenseが重要
将来的には技術的興味・知識の源泉・理解の濃淡を可視化する
```

---

## 4.2 01-mvp-scope.md

### 目的

MVPに含めるもの、MVP外にするもの、旧方針として採用しないものを明確化する。

### 反映元

```text
2026-05-03
2026-05-05
2026-05-06
2026-05-17
2026-05-26
```

### MVPに含めるもの

```text
Resource登録
ResourceSection管理
Progress
SectionStudyStatus
StudyMemo
Lv.1 初回学習済み
Lv.2 Immediate Reflection
Lv.3 Delayed Recall
LearningSession
LearningSessionRecord
LevelHistory
将来タグ証跡へ接続できる軽量データ
```

### MVP外

```text
Lv.4 Artifact
Lv.5 Defense
Galaxy API
Skill Map公開ページ
Summary API
AI目次自動生成
目次写真/OCR登録
MCP連携
外部LLMログ全文インポート
学習時間管理
Pomodoro連携
LearningCycle / 再学習軸
タグ正規化テーブル本格実装
Artifact/Defense関連テーブル
```

### 採用しない旧方針

```text
AIクイズ中心
CheckRecord中心
ReviewRecord中心
Answer API中心
正答率でLv.3判定
NotebookLM代替
本文丸ごと取り込み
StudyMemoをLevel必須条件にする
Progress.note
StudyMemo importantフラグ
```

---

## 4.3 02-db-design.md

### 目的

AIコード生成に使うDB設計の最新版。  
Flyway migration / Entity / Repository作成時に読ませる。

### 反映元

```text
2026-05-02
2026-05-03
2026-05-06
2026-05-10
2026-05-16
2026-05-17
2026-05-26
```

### 入れる内容

```text
DBはPostgreSQL
内部IDはBIGINT
外部公開IDは将来UUID/public_id/handle/slug
timestamptz
created_at / updated_at
deleted_at対象テーブル
resources
resource_sections
progresses
section_study_statuses
study_memos
learning_sessions
learning_session_records
level_histories
主キー
外部キー
一意制約
CHECK制約
論理削除方針
result_draft JSONB
TEXT[]タグ方針
```

### 注意

旧CheckRecord / ReviewRecord / CheckQuestion / Answer系テーブルは入れない。

---

## 4.4 03-api-design.md

### 目的

AIコード生成に使うAPI仕様の最新版。  
Controller / Request / Response / Service作成時に読ませる。

### 反映元

```text
2026-05-02
2026-05-03
2026-05-06
2026-05-10
2026-05-26
```

### 入れるAPI

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

### 明示する禁止

```text
Progress作成APIは作らない
POST /resources時にProgressを自動作成する
currentLevelをPATCHで直接更新しない
LearningSessionRecordを直接CRUDしない
LevelHistoryを直接作成・更新・削除しない
Check API / Answer APIはMVPに作らない
```

---

## 4.5 04-level-rules.md

### 目的

Lv.1〜Lv.3の意味、到達条件、Progress / LevelHistory更新ルールを固定する。

### 反映元

```text
2026-05-04
2026-05-06
2026-05-10
2026-05-17
2026-05-26
```

### 入れる内容

```text
Lv.1 = 初回学習済み / 一通り触れた状態
Lv.2 = Immediate Reflectionの正式証跡あり
Lv.3 = Delayed Recallの正式証跡あり
Lv.4 = Artifact / MVP外
Lv.5 = Defense / MVP外
Lv.1到達ルート
Lv.2到達条件
Lv.3到達条件
DELAYED_RECALL開始条件
NEEDS_REVIEWでもLevel到達候補
OFF_TOPICは保存不可・Level対象外
LevelHistoryは初到達履歴
同じLevelのLevelHistoryは1回だけ
studiedAtをnullに戻してもLevelHistoryは消さない
```

---

## 4.6 05-learning-session-flow.md

### 目的

LearningSession / LearningSessionRecordの状態遷移、保存前確認、rawログ非保存方針を固定する。

### 反映元

```text
2026-05-05
2026-05-10
2026-05-17
2026-05-26
```

### 入れる内容

```text
LearningSessionはAI Reflection / Recallの実行単位
Resource単位
MVPではSection単位にしない
messagesではなくresponses
raw回答ログ・AI質問全文・会話ログ全文は正式保存しない
IN_PROGRESS / COMPLETED / RECORD_SAVED / DISCARDED
result_draft JSONB
complete後にresultDraftを画面表示
保存前確認
AI生成部分は編集不可
userCommentのみ追加可
保存しない場合はDISCARDED
保存しない場合はLearningSessionRecordなし、Level更新なし
同時セッション制約
DELAYED_RECALL開始条件
```

---

## 4.7 06-implementation-rules.md

### 目的

AIコード生成時の事故を防ぐための実装ルール。  
Repository / Service / Transaction / Authorization / Delete policyの注意点をまとめる。

### 反映元

```text
2026-05-06
2026-05-10
2026-05-16
2026-05-17
2026-05-26
```

### 入れる内容

```text
必ずuser_idで所有者チェックする
deleted_at対象ではdeleted_at is null
UUIDは認可の代替ではない
Resource/Section/Memoは論理削除
LearningSessionはDISCARDEDで破棄
LearningSessionRecordとLevelHistoryはMVPでは削除しない
current_levelを外部入力で直接更新しない
Progress更新とLevelHistory作成はトランザクション
Resource作成とProgress作成はトランザクション
Section作成とSectionStudyStatus作成はトランザクション
LearningSessionRecord保存とProgress/LevelHistory更新はトランザクション
LevelHistory重複作成禁止
StudyMemo作成ではLevelを上げない
SectionStudyStatus更新ではcurrentSectionIdを自動更新しない
raw回答ログをDBに正式保存しない
```

---

## 4.8 07-implementation-order.md

### 目的

実装順序をAIと人間が共有する。  
全部を一気に作らず、縦切りで動くMVPを作るための順序。

### 反映元

```text
2026-05-26
```

### 入れる内容

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

### 最初の縦切り

```text
POST /resources
GET /resources/{resourceId}
POST /resources/{resourceId}/sections
PATCH /resources/{resourceId}/sections/{sectionId}/study-status
POST /resources/{resourceId}/progress/complete-initial-study
```

---

## 4.9 08-ai-development-workflow.md

### 目的

Cursor / Claude Code / ChatGPT / Markdown設計書を使った実装運用ルールをまとめる。

### 反映元

```text
2026-05-26
```

### 入れる内容

```text
設計・判断・レビューは自分で行う
実装雛形・定型コード・テスト叩き台はAIに生成させる
生成コードは自分で説明できる状態まで読む
Cursorをメイン開発環境にする
Claude Codeは重めの修正・テスト実行・リファクタに使う
ChatGPTは設計・方針整理に使う
AIに渡す単位は小さくする
まずファイル構成・変更計画を出させる
いきなり全部実装させない
AI生成コードのレビュー観点
docs/00〜08をAIに読ませる
archiveの日付別メモは原則読ませない
```

---

# 5. 旧方針・MVP外として明示する一覧

AIコード生成時に誤って採用しないよう、`01-mvp-scope.md` または `06-implementation-rules.md` に明示する。

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
StudyMemoをLevel条件にする
StudyMemo importantフラグ
Progress.note
ReviewRecord.memo
Progress.totalStudyTime
Pomodoro連携
外部LLMログ全文インポート
MCP連携
Galaxy API
Skill Map
Summary API
Lv.4 Artifact
Lv.5 Defense
Artifact / Defense DB
LearningCycle / 再学習軸
タグ正規化テーブルのMVP実装
AI目次自動生成
目次写真/OCR登録
```

---

# 6. docs作成順序

次は以下の順で実際にファイル化する。

```text
1. 00-product-principles.md
2. 01-mvp-scope.md
3. 02-db-design.md
4. 04-level-rules.md
5. 05-learning-session-flow.md
6. 03-api-design.md
7. 06-implementation-rules.md
8. 07-implementation-order.md
9. 08-ai-development-workflow.md
```

## なぜこの順番か

まずプロダクト思想とMVP境界を固めることで、AIが旧方針やMVP外の機能を勝手に実装しにくくなる。

次にDBとLevel/Sessionを固める。  
その後、API・実装ルール・実装順序へ進む。

---

# 7. 最終判断

全日付メモを踏まえたV2方針は以下。

```text
日付別メモはすべてarchiveへ
AIコード生成には最新版docs/00〜08を読ませる
最新版docsは2026-05-26を軸に、5/2〜5/17の採用方針を取り込む
CheckRecord / ReviewRecord / AIクイズ中心方針は旧方針として明示的に除外する
DB実装方針は5/16・5/17・5/26を中心に整理する
LearningSession方針は5/10・5/17・5/26を中心に整理する
プロダクト思想は5/3・5/4・5/5・5/17を中心に整理する
AI開発運用は5/26の内容を独立docsにする
```

このV2をもとに、次は実際の `00-product-principles.md` から作成する。
