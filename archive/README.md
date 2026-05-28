# docs/archive README

## 目的

このディレクトリは、SteerLogの過去の日付別設計メモを保存する場所である。

archive内のファイルは、最新版仕様ではなく、設計判断の履歴・方針転換の記録として扱う。

AIコード生成に直接読ませる標準入力は、`docs/00〜08` の最新版docsである。

---

# archiveの使い方

archiveは以下の用途で使う。

```text
過去の判断理由を確認する
古い方針との差分を確認する
READMEや面接説明の材料にする
設計変更の経緯を確認する
```

AIに読ませる場合は、必要な日付メモだけを指定して補足資料として扱う。

---

# 日付別メモ一覧

## 2026-05-02-design-notes.md

### 位置づけ

SteerLog MVPの初期設計メモ。

### 主な内容

```text
ResourceとProgressの基本分離
Resource登録時のProgress自動作成
Progress.noteを持たせない方針
StudyMemoとProgress.noteの分離
ReviewRecord初期案
Lv.2 / Lv.3初期案
```

### 注意

ReviewRecord中心設計は旧方針。  
最新版ではLearningSession / LearningSessionRecordに置き換わっている。

---

## 2026-05-03-design-notes.md

### 位置づけ

MVP範囲、Resource API、Progress API、LevelHistory初期方針を整理したメモ。

### 主な内容

```text
MVPはLv.1〜Lv.3
Lv.4 / Lv.5 / GalaxyはMVP外
currentLevelはResource全体の代表到達Level
LevelHistoryの初期整理
completedAtとarchivedAtの分離
Resource詳細にProgress概要を含める
```

### 注意

CheckRecord / AI選択式チェック中心の要素は旧方針。

---

## 2026-05-04-design-notes.md

### 位置づけ

SteerLogの学習思想とLevel設計の根本を整理したメモ。

### 主な内容

```text
初回学習は地図を作る段階
理解は振り返りで深まる
定着には時間を置いた想起が必要
エンジニアにはアウトプットが必要
Defenseの将来構想
```

### 注意

Check API / Answer API中心の実装案は旧方針。

---

## 2026-05-05-design-notes.md

### 位置づけ

NotebookLMとの差別化、AIクイズ中心からReflection / Recall中心への方針転換メモ。

### 主な内容

```text
NotebookLM代替にしない
本文QAアプリにしない
AIクイズ中心から離れる
Reflection / Recall中心へ移行
著作権リスク
Galaxy API構想
```

### 注意

AIクイズ中心、正答率でLv.3判定、本文大量取り込みは旧方針。

---

## 2026-05-06-design-notes.md

### 位置づけ

Progress API、SectionStudyStatus、StudyMemo、LearningSession方針を具体化したメモ。

### 主な内容

```text
Progress.status
currentSectionId
SectionStudyStatus
StudyMemo
LearningSession / LearningSessionRecord
外部LLMログ全文インポートを避ける方針
```

### 注意

StudyMemoをLevel必須条件にしない。  
Pomodoro連携やtotalStudyTimeはMVP外。

---

## 2026-05-10-design-notes.md

### 位置づけ

LearningSession、LearningSessionRecord、aiAssessment、状態遷移を固めた重要メモ。

### 主な内容

```text
LearningSessionはResource単位
messagesではなくresponses
raw回答ログを正式保存しない
LearningSessionRecordが正式証跡
IN_PROGRESS / COMPLETED / RECORD_SAVED / DISCARDED
PASSED / NEEDS_REVIEW / OFF_TOPIC
NEEDS_REVIEWでもLevel到達候補
OFF_TOPICは保存不可
```

---

## 2026-05-16-design-notes.md

### 位置づけ

DB実装、認可、削除、datetime方針を固めたメモ。

### 主な内容

```text
PostgreSQL採用
BIGINT ID方針
user_id認可
deleted_at方針
timestamptz
正式証跡・履歴はMVPでは削除しない
```

---

## 2026-05-17-design-notes.md

### 位置づけ

DB設計、Level、Galaxy、Tag、再学習軸を整理したメモ。

### 主な内容

```text
主要8テーブルの整理
Resource / Progress / StudyMemo / LearningSession / LevelHistoryの位置づけ
Tag / Galaxyの将来構想
再学習軸
Artifact / Defenseの将来構想
```

### 注意

Galaxy、Tag正規化、再学習軸、Artifact、DefenseはMVP外。

---

## 2026-05-26-design-notes.md

### 位置づけ

現時点の実装前仕様に最も近い最新整理。

### 主な内容

```text
主要8テーブル
Progress ARCHIVEDの意味
LearningSession resultDraft保存前確認
主要API一覧
実装順序
Flyway
Cursor / Claude Code / ChatGPTの使い分け
Markdown設計書の再編方針
```

### 注意

最新版docsの主材料。  
ただし日付別判断履歴としてarchiveにも保存する。

---

# archive利用時の注意

archiveには旧方針も含まれる。

以下は最新版仕様として採用しない。

```text
AIクイズ中心設計
CheckRecord
CheckQuestion
ReviewRecord
Answer API
正答率でLv.3
NotebookLM代替
本文丸ごと取り込み
StudyMemoをLevel必須条件にする
Progress.note
StudyMemo importantフラグ
Progress.totalStudyTime
Pomodoro連携
LearningSessionをSection単位にする
LearningSessionをチャットログ保存にする
```

最新版仕様は `docs/00〜08` を参照すること。
