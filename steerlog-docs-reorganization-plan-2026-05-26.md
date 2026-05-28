# SteerLog docs再編方針：2026-05-26

## 目的

このファイルは、これまで日付ごとに作成してきたSteerLog設計メモを、AIコード生成・実装作業で使いやすい `docs/` 構成へ再編するための仕分け方針をまとめる。

対象ファイル：

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

# 1. 基本方針

## 1.1 日付別メモの扱い

日付別メモは、基本的に **議論ログ・判断履歴** として扱う。

```text
docs/archive/
```

に配置する。

理由：

- 古い方針と新しい方針が混ざっている
- AIコード生成にそのまま読ませると、旧方針を採用してしまう危険がある
- ただし、設計判断の経緯としては価値がある

---

## 1.2 AIコード生成に読ませるファイル

AIコード生成に読ませるのは、日付別メモではなく、最新版として整理した以下のファイル群にする。

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
```

---

## 1.3 最新方針の中心

最新版docsの中心にするのは、2026-05-26メモ。

理由：

- 主要8テーブルが整理されている
- API一覧が整理されている
- 実装順序が整理されている
- LearningSessionRecord保存前確認フローが反映されている
- Cursor / Claude Code / AIコード生成方針が整理されている

---

# 2. archiveに入れる日付別メモ

以下は原則として `docs/archive/` に保存する。

```text
docs/archive/2026-05-02-design-notes.md
docs/archive/2026-05-03-design-notes.md
docs/archive/2026-05-04-design-notes.md
docs/archive/2026-05-05-design-notes.md
docs/archive/2026-05-06-design-notes.md
docs/archive/2026-05-10-design-notes.md
docs/archive/2026-05-16-design-notes.md
docs/archive/2026-05-17-design-notes.md
docs/archive/2026-05-26-design-notes.md
```

## archiveの用途

```text
なぜその判断になったか確認する
古い案との差分を見る
設計変更の経緯をREADMEや面接説明に使う
AIに判断経緯を補足させたいときだけ参照する
```

---

# 3. 各日付メモの仕分け

## 2026-05-02

### 主な内容

- SteerLog初期MVP方針
- Status API全体像
- Resource / Progress / Section / Memo / Check系の初期整理
- LLM連携のMVP方針

### 扱い

archive行き。

### 最新docsへ反映する要素

```text
SteerLogは学習状態を可視化するアプリ
Resource / Progress / Section の基本分離
学習証跡を中心にする方向性
```

### 注意

初期案として、後続の設計で置き換わった内容が含まれる可能性がある。  
AIコード生成には直接読ませない。

---

## 2026-05-03

### 主な内容

- SteerLog MVPの目的
- 個人利用価値・第三者向け価値
- MVP範囲
- Level/証跡の方向性

### 扱い

archive行き。

### 最新docsへ反映する要素

```text
SteerLogは単なる学習ログではなく、学習証跡・技術的関心を見せるアプリ
個人利用と将来のプロフィール/第三者向け価値を両方持つ
```

---

## 2026-05-04

### 主な内容

- SteerLogの学習思想
- Lv.1〜Lv.5の初期定義
- 初回学習、確認、遅延再確認、Artifact、Defenseの整理

### 扱い

archive行き。

### 最新docsへ反映する要素

```text
初回学習は理解保証ではなく、まず地図を作る段階
学習は、初回接触 → 振り返り → 時間を置いた想起 → 出力 → 説明/防衛へ進む
```

### 注意

Lv.2/Lv.3の古い「確認/再確認」表現は、後続のLearningSession方針で更新されている。  
最新版では以下に置き換える。

```text
Lv.2 = Immediate Reflectionの正式証跡あり
Lv.3 = Delayed Recallの正式証跡あり
```

---

## 2026-05-05

### 主な内容

- NotebookLMとの差別化
- AIクイズ機能の見直し
- 著作権リスク
- Check/Quiz中心からLearningSession/Reflection/Recall中心への方針転換
- Galaxy APIの位置づけ

### 扱い

archive行きだが、重要な方針転換メモ。

### 最新docsへ反映する要素

```text
SteerLogはNotebookLMの代替ではない
本文・著作物を大量に取り込むアプリにしない
AIクイズ中心ではなく、Reflection / Recall中心にする
Galaxy APIはMVP外
```

### 破棄/非採用として明示すべき要素

```text
AIクイズ中心
CheckRecord中心
正答率でLv.3判定
NotebookLM的な本文QAアプリ化
```

---

## 2026-05-06

### 主な内容

- Progress APIの整理
- status手動更新
- currentSectionId
- SectionStudyStatus
- StudyMemo
- LearningSession / LearningSessionRecord方針
- MCP / 外部LLMログ連携方針

### 扱い

archive行きだが、最新版docsへの反映対象が多い。

### 最新docsへ反映する要素

```text
Progress.statusはNOT_STARTED / IN_PROGRESS / PAUSED / ARCHIVEDなどを持つ
currentSectionIdは現在メインで学習しているSection
SectionStudyStatusはSection単位の軽い学習状態
StudyMemoは短い任意メモでLevel条件ではない
LearningSessionRecordはAI整理済み正式証跡
外部LLMログの全文インポートはMVP外
```

---

## 2026-05-10

### 主な内容

- LearningSessionの位置づけ
- messagesではなくresponsesという用語
- 生の会話ログ・回答本文を正式証跡として保存しない方針
- LearningSession UI/API接続
- Lv.1 / Lv.2 / Lv.3の整理
- aiAssessment

### 扱い

archive行きだが、LearningSession関連の重要ソース。

### 最新docsへ反映する要素

```text
LearningSessionはチャットログ保存ではなく短いReflection/Recallセッション
responsesという用語を使う
raw回答ログは正式証跡として保存しない
LearningSessionRecordが正式証跡
aiAssessment = PASSED / NEEDS_REVIEW / OFF_TOPIC
NEEDS_REVIEWでもLevel到達候補
OFF_TOPICは保存不可/Level対象外
```

---

## 2026-05-16

### 主な内容

- DB選定
- PostgreSQL採用
- ID方針
- 認可方針
- 削除方針
- datetime方針
- enum管理
- user_id / authorization

### 扱い

archive行きだが、DB実装ルールの重要ソース。

### 最新docsへ反映する要素

```text
DBはPostgreSQL
内部学習管理テーブルはBIGINT
外部公開IDは将来UUID/public_id/handle等
認可はuser_idで必ず行う
UUIDは認可の代替ではない
日時はtimestamptz
主要テーブルにcreated_at / updated_at
論理削除対象はdeleted_at
resources / resource_sections / study_memos は論理削除
learning_session_records / level_histories は正式証跡・履歴としてMVPでは削除しない
```

---

## 2026-05-17

### 主な内容

- DB設計・Level/Galaxy整理
- PostgreSQL再確認
- Resource / ResourceSection / Progress / StudyMemo / LearningSession / LevelHistory の整理
- Tags/Galaxy方針
- star brightness / evidence origin
- 再学習軸

### 扱い

archive行きだが、DB・Tag・Galaxyの重要ソース。

### 最新docsへ反映する要素

```text
主要8テーブルの前提
Resourceは学習対象そのもの
ProgressはResource-wideな学習状態
LevelHistoryは初到達履歴
Tags/Galaxyは重要だがMVPでは後回し
タグは将来標準化・証跡由来を重視する
再学習軸はMVP外
```

---

## 2026-05-26

### 主な内容

- MVP主要8テーブルの確定寄り整理
- Progress ARCHIVEDの意味
- LearningSession resultDraft保存前確認
- API一覧
- 実装順序
- Flyway
- Cursor / Claude Code / ChatGPTの使い分け
- Markdown設計書の再編方針

### 扱い

archiveにも保存するが、最新版docs作成の主材料にする。

### 最新docsへ反映する要素

ほぼ全体。

```text
00-product-principles.md
01-mvp-scope.md
02-db-design.md
03-api-design.md
04-level-rules.md
05-learning-session-flow.md
06-implementation-rules.md
07-implementation-order.md
```

---

# 4. 最新docsへの割り当て

## 00-product-principles.md

主に反映元：

```text
2026-05-03
2026-05-04
2026-05-05
2026-05-26
```

入れる内容：

```text
SteerLogは学習証跡アプリ
学習時間管理アプリではない
本文QAアプリ/NotebookLM代替ではない
学習は初回接触・振り返り・遅延想起・出力・Defenseで深まる
Levelは理解保証ではなく証跡段階
AI生成証跡はユーザー確認後に保存
```

---

## 01-mvp-scope.md

主に反映元：

```text
2026-05-03
2026-05-05
2026-05-17
2026-05-26
```

入れる内容：

```text
MVPに含めるもの
MVP外にするもの
後で検討するもの
古い方針から破棄したもの
```

---

## 02-db-design.md

主に反映元：

```text
2026-05-16
2026-05-17
2026-05-26
```

入れる内容：

```text
resources
resource_sections
progresses
section_study_statuses
study_memos
learning_sessions
learning_session_records
level_histories
DB共通方針
ID方針
日時方針
削除方針
```

---

## 03-api-design.md

主に反映元：

```text
2026-05-06
2026-05-10
2026-05-26
```

入れる内容：

```text
Resource API
ResourceSection API
Progress API
SectionStudyStatus API
StudyMemo API
LearningSession API
LevelHistory API
```

---

## 04-level-rules.md

主に反映元：

```text
2026-05-04
2026-05-10
2026-05-26
```

入れる内容：

```text
Lv.1
Lv.2
Lv.3
NEEDS_REVIEWの扱い
OFF_TOPICの扱い
LevelHistory作成条件
```

---

## 05-learning-session-flow.md

主に反映元：

```text
2026-05-05
2026-05-10
2026-05-26
```

入れる内容：

```text
LearningSessionの状態遷移
IN_PROGRESS / COMPLETED / RECORD_SAVED / DISCARDED
responses API
complete API
resultDraft
保存前確認
record保存
discard
raw回答ログを保存しない方針
```

---

## 06-implementation-rules.md

主に反映元：

```text
2026-05-16
2026-05-26
```

入れる内容：

```text
user_idで認可チェック
deleted_at is null
current_level直接更新禁止
LearningSessionRecord直接CRUD禁止
LevelHistory重複作成禁止
トランザクション境界
AI生成コードレビュー観点
```

---

## 07-implementation-order.md

主に反映元：

```text
2026-05-26
```

入れる内容：

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

---

# 5. AIに読ませない/注意して扱う旧方針

以下は旧方針またはMVP外として扱う。  
AIコード生成時に採用しないよう明示する。

```text
AIクイズ中心設計
CheckRecord
CheckQuestion
正答率でLv.3
ReviewRecord中心設計
Lv.4 ArtifactをMVPに含める
Lv.5 DefenseをMVPに含める
Galaxy APIをMVPに含める
外部LLMログ全文インポート
MCP連携
学習時間/Pomodoro統合
NotebookLM代替
本文丸ごと取り込み
タグ正規化テーブルのMVP実装
再学習サイクルのMVP実装
AI目次自動生成のMVP実装
```

---

# 6. 次の作業順

次は以下の順で進める。

```text
1. 00-product-principles.md を作成
2. 01-mvp-scope.md を作成
3. 02-db-design.md を作成
4. 03-api-design.md を作成
5. 04-level-rules.md を作成
6. 05-learning-session-flow.md を作成
7. 06-implementation-rules.md を作成
8. 07-implementation-order.md を作成
9. 日付別メモを docs/archive/ に配置する
```

最初に作るべきは `00-product-principles.md` と `01-mvp-scope.md`。  
理由は、AIがSteerLogを普通のCRUDアプリやクイズアプリとして実装しないよう、最初にプロダクト思想とMVP境界を固定するため。

---

# 7. 最終判断

2026-05-26時点では、以下の運用で進める。

```text
日付別メモ
= docs/archive/ に保存する判断履歴

docs/00〜07
= AIコード生成に読ませる最新版仕様

2026-05-26メモ
= 最新版docs作成の中心材料

過去メモ
= 方針の背景・判断理由の補足材料
```
