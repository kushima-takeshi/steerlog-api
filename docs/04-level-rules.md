# 04-level-rules.md

# SteerLog Level Rules

## 目的

このドキュメントは、SteerLog MVPにおけるLv.1〜Lv.3の意味、到達条件、Progress更新、LevelHistory作成ルールを定義する。

AIコード生成、Service実装、テスト作成では、この内容を基準にする。

---

# 0. 実装状況（2026-06）

| 項目 | 状態 |
|------|------|
| Lv.1 明示到達（complete-initial-study） | ✅ 実装済み |
| Lv.1 自動到達（全Section studiedAt） | ✅ 実装済み |
| LevelHistory GET | ✅ 実装済み |
| LevelHistory 重複防止 | ✅ 実装済み |
| currentLevel 下げない | ✅ 実装済み |
| StudyMemo 作成時の Progress 更新 | ✅ 実装済み |
| StudyMemo 作成で Level 上げない | ✅ 実装済み |
| Lv.2 / Lv.3（LearningSessionRecord） | ✅ 実装済み |

---

# 1. Levelの基本思想

SteerLogのLevelは、理解保証ではなく **証跡段階** である。

```text
Level = そのResourceに対して、どの種類の学習証跡が存在するか
```

であり、

```text
Level = その技術を完全に理解した証明
```

ではない。

そのため、`NEEDS_REVIEW` のLearningSessionRecordでも、対象Resourceに関係する有効な証跡であれば、Lv.2 / Lv.3の到達候補になる。

ただし、UIでは `PASSED` と `NEEDS_REVIEW` を区別して表示する。

---

# 2. Level一覧

MVPで実装するのはLv.1〜Lv.3。

| Level | 意味 | MVP |
|---:|---|---|
| Lv.1 | 初回学習済み / 一通り触れた状態 | ○ |
| Lv.2 | Immediate Reflectionの正式証跡あり | ○ |
| Lv.3 | Delayed Recallの正式証跡あり | ○ |
| Lv.4 | Artifact / 成果物による証跡 | MVP外 |
| Lv.5 | Defense / 説明・防衛できる状態 | MVP外 |

DB上の `current_level` と `level_histories.level` は、将来拡張を見据えて0〜5または1〜5を許容する。

---

# 3. Lv.1

## 3.1 意味

```text
初回学習済み / 一通り触れた状態
```

Lv.1は完全理解ではない。  
初回学習によってResourceの全体像に触れた状態を表す。

---

## 3.2 到達ルート

Lv.1到達ルートは2つ。

```text
1. Resource全体を一通り学習済みにする
2. 全SectionのstudiedAtが埋まる
```

---

## 3.3 Resource全体を一通り学習済みにする

**状態: ✅ 実装済み**

API：

```http
POST /resources/{resourceId}/progress/complete-initial-study
```

処理：

```text
1. Progressを取得
2. Progress.initialStudiedAt = now
3. Progress.lastStudiedAt = now
4. Progress.status が NOT_STARTED なら IN_PROGRESS
5. Progress.currentLevel が1未満なら1に更新
6. LevelHistory Lv.1 がなければ作成
```

LevelHistory：

```text
level = 1
sourceType = INITIAL_STUDY_COMPLETION
sourceId = null
reasonCode = INITIAL_STUDY_COMPLETED
```

`currentLevel` は 0 のときのみ 1 に更新。既に 1 以上なら下げない。

注意：

```text
この操作では、各SectionStudyStatus.studiedAtを自動で埋めない。
```

理由：

```text
Resource全体を一通り触った自己申告
Sectionごとの個別学習済み証跡
```

は分けるため。

---

## 3.4 全Section学習済みによるLv.1

**状態: ✅ 実装済み**

SectionStudyStatus更新後、**未削除Section** がすべて `studiedAt IS NOT NULL` になった場合、Lv.1到達候補にする。Section が 0 件の Resource では自動到達しない。

処理：

```text
1. SectionStudyStatus更新
2. Progress.status が NOT_STARTED なら IN_PROGRESS
3. Progress.lastStudiedAt / updatedAt = now
4. 未削除SectionのstudiedAtを確認
5. 全Section学習済みなら currentLevel を 0 のときのみ 1
6. initialStudiedAt が null ならセット
7. LevelHistory Lv.1 がなければ作成
```

LevelHistory：

```text
level = 1
sourceType = SECTION_STUDY_STATUS
sourceId = null
reasonCode = ALL_SECTIONS_STUDIED
```

---

# 4. Lv.2

**状態: ✅ 実装済み**

## 4.1 意味

```text
Immediate Reflectionの正式証跡がある状態
```

学習直後に、そのResourceについて自分の言葉で振り返った証跡がある状態。

Lv.2は完全理解を意味しない。  
Immediate ReflectionのLearningSessionRecordが保存された状態を意味する。

---

## 4.2 到達条件

以下を満たす場合、Lv.2到達候補にする。

```text
sessionType = IMMEDIATE_REFLECTION
LearningSessionRecordが保存される
aiAssessment != OFF_TOPIC
```

---

## 4.3 処理

LearningSessionRecord保存時に以下を行う。

```text
1. LearningSessionRecord作成
2. LearningSession.status = RECORD_SAVED
3. LearningSession.recordSavedAt = now
4. Progress.lastStudiedAt = now
5. Progress.currentLevel が2未満なら2に更新
6. LevelHistory Lv.2 がなければ作成
```

LevelHistory：

```text
level = 2
sourceType = LEARNING_SESSION_RECORD
sourceId = learningSessionRecordId
reasonCode = IMMEDIATE_REFLECTION_RECORD_SAVED
```

---

# 5. Lv.3

**状態: ✅ 実装済み**

## 5.1 意味

```text
Delayed Recallの正式証跡がある状態
```

時間を置いて、そのResourceについて思い出す・説明する証跡がある状態。

Lv.3も完全理解を意味しない。  
Delayed RecallのLearningSessionRecordが保存された状態を意味する。

---

## 5.2 開始条件

DELAYED_RECALLのLearningSessionは、Lv.2到達済みでなければ開始できない。

```text
Progress.currentLevel >= 2
```

満たさない場合：

```text
LEVEL_REQUIREMENT_NOT_MET
```

---

## 5.3 到達条件

以下を満たす場合、Lv.3到達候補にする。

```text
sessionType = DELAYED_RECALL
LearningSessionRecordが保存される
aiAssessment != OFF_TOPIC
Progress.currentLevel >= 2
```

---

## 5.4 処理

LearningSessionRecord保存時に以下を行う。

```text
1. LearningSessionRecord作成
2. LearningSession.status = RECORD_SAVED
3. LearningSession.recordSavedAt = now
4. Progress.lastStudiedAt = now
5. Progress.currentLevel が3未満なら3に更新
6. LevelHistory Lv.3 がなければ作成
```

LevelHistory：

```text
level = 3
sourceType = LEARNING_SESSION_RECORD
sourceId = learningSessionRecordId
reasonCode = DELAYED_RECALL_RECORD_SAVED
```

---

# 6. aiAssessmentの扱い

## 6.1 値

```text
PASSED
NEEDS_REVIEW
OFF_TOPIC
```

---

## 6.2 PASSED

対象Resourceについて、自分の言葉である程度説明できている状態。

例：

```text
具体例を使って説明できている
自分の実装・設計に接続できている
概念の関係を説明できている
```

Level到達候補になる。

---

## 6.3 NEEDS_REVIEW

対象Resourceに関係する回答ではあるが、浅い・曖昧・混乱がある状態。

例：

```text
説明が抽象的
具体例が弱い
用語の使い分けが曖昧
一部誤解がある
```

Level到達候補になる。

理由：

```text
SteerLogのLevelは理解保証ではなく証跡段階だから。
```

ただし、UIでは「要復習あり」として表示する。

---

## 6.4 OFF_TOPIC

対象Resourceの証跡として無効な状態。

例：

```text
回答がResourceと関係ない
短すぎて判断できない
読んでいない/分からないだけ
質問に答えていない
```

Level到達候補にしない。  
MVPではLearningSessionRecord保存も不可。

---

# 7. LevelHistory

## 7.1 役割

LevelHistoryは、各Levelに初めて到達した履歴である。

```text
LevelHistory = 初到達イベント
LearningSessionRecord = 証跡本文
```

---

## 7.2 一意制約

同じResource・同じLevelのLevelHistoryは1件だけ。

```text
userId + resourceId + level
```

で一意。

---

## 7.3 追加証跡の扱い

すでにLv.2到達済みのResourceに対して、追加でIMMEDIATE_REFLECTIONを保存した場合：

```text
LearningSessionRecordは作成する
LevelHistory Lv.2は追加しない
Progress.currentLevelは変わらない
```

同じく、すでにLv.3到達済みのResourceに対して、追加でDELAYED_RECALLを保存した場合も、LevelHistoryは増やさない。

---

# 8. Levelを下げない方針

MVPでは、一度到達したLevelは通常操作では下げない。

例：

```text
SectionStudyStatus.studiedAtをnullに戻す
StudyMemoを削除する
Progress.statusをPAUSEDにする
```

こうした操作をしても、すでに作成されたLevelHistoryは削除しない。  
Progress.currentLevelも原則下げない。

理由：

```text
LevelHistoryは過去の初到達履歴だから。
```

---

# 9. currentLevel直接更新禁止

`Progress.currentLevel` は、外部APIから直接更新させない。

禁止：

```text
PATCH /resources/{resourceId}/progress
{
  "currentLevel": 2
}
```

currentLevelは以下の処理だけで更新する。

```text
complete-initial-study
全Section学習済み判定
LearningSessionRecord保存
```

---

# 10. トランザクション

以下は必ずトランザクションで処理する。

```text
complete-initial-study + Progress更新 + LevelHistory作成
SectionStudyStatus更新 + Lv.1判定 + Progress更新 + LevelHistory作成
LearningSessionRecord保存 + LearningSession更新 + Progress更新 + LevelHistory作成
```

---

# 11. 旧方針として採用しないもの

MVPでは以下を採用しない。

```text
正答率でLv.3判定
AIクイズのスコアでLevelを上げる
CheckRecordでLevelを判定する
ReviewRecordでLevelを判定する
StudyMemo作成でLevelを上げる
ユーザーがcurrentLevelを直接更新する
```

---

# 12. StudyMemoとLevel（実装済み）

StudyMemo 作成時：

```text
Progress.status: NOT_STARTED → IN_PROGRESS
Progress.lastStudiedAt / updatedAt = now
currentLevel / initialStudiedAt / LevelHistory は更新しない
SectionStudyStatus は更新しない
```

StudyMemo 更新・削除時は Progress / LevelHistory を更新しない。

---

# 13. まとめ

現時点（Phase 5 完了）で動いている Level 設計：

```text
Lv.1 = 初回学習済み / 一通り触れた（2経路: 明示API / 全Section自動）
Lv.2 = Immediate Reflectionの正式証跡あり（未実装）
Lv.3 = Delayed Recallの正式証跡あり（未実装）
```

Levelは理解保証ではなく証跡段階。  
LevelHistoryは初到達履歴。  
LearningSessionRecordは正式証跡本文（未実装）。  
currentLevelはLevel到達処理でのみ更新する。
