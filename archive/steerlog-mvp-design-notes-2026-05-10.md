もちろん。今日の学習ログ用に、そのまま貼れる形でまとめるね。

````md
# 学習引き継ぎメモ：2026/05/10（日）

## 実施ポモドーロ数
未確定  
※今日は主に SteerLog MVP の LearningSession / Level / 証跡設計を整理。

---

## 今日の学習テーマ

SteerLog MVP における **LearningSession / LearningSessionRecord / LevelHistory / Lv.1〜Lv.3到達ルール** の設計整理。

今日は、SteerLogを「AIチャットアプリ」や「クイズアプリ」に寄せすぎず、**学習証跡つき技術プロフィール** として成立させるための重要な仕様をかなり固めた。

---

## 今日決めたこと

### 1. LearningSessionの位置づけ

LearningSessionは、章ごとの通常メモ機能ではなく、**Resource全体に対して行う Lv.2 / Lv.3 用のAI Reflection / Recallイベント** とする。

章・セクション単位の通常記録は、以下が担当する。

- SectionStudyStatus
  - セクションを読んだか
  - セクション理解度
- StudyMemo
  - 気づき
  - 疑問
  - 弱点
  - 次にやること

LearningSessionは、毎章ごとにAI対話するものではなく、節目でResource全体について振り返り・想起を行う特別なイベントとして扱う。

---

### 2. LearningSession APIでは messages ではなく responses を使う

LearningSessionはチャットログ保存機能ではないため、API名は `messages` ではなく `responses` を使う方針にした。

```http
POST /learning-sessions/{learningSessionId}/responses
````

理由は、LearningSessionを「AIチャット」ではなく、AIからの問いに対する **Reflection / Recall の回答セッション** として扱うため。

---

### 3. 生の会話ログ・回答本文は正式な証跡として保存しない

LearningSession中の会話内容は、AIがその場で弱点判定や要約生成に使う。

ただし、正式な証跡として保存するのは、生ログではなく **LearningSessionRecord** のみ。

保存しないもの：

* ユーザー回答本文
* AIとの生会話ログ
* 質問全文の履歴
* 回答全文の履歴

保存するもの：

* summary
* conceptTags
* weakPointTags
* weakPointSummary
* nextAction
* aiAssessment
* generationBasis
* userComment

SteerLogは、AIチャット履歴アプリではなく、学習証跡アプリとして設計する。

---

### 4. LearningSessionのUI/API接続方針

LearningSession画面は、ページ遷移や画面再読み込みではなく、**1画面内で進むUI** にする。

途中回答時：

```text
ボタン：回答して次へ
API：POST /learning-sessions/{learningSessionId}/responses
```

最終回答時：

```text
IMMEDIATE_REFLECTION：
ボタン：振り返り結果を作成

DELAYED_RECALL：
ボタン：想起結果を作成

API：POST /learning-sessions/{learningSessionId}/complete
```

APIレスポンスDTOには、画面制御用に以下を含める。

```text
learningSessionId
status
sessionType
aiPrompt
step.currentStep
step.totalSteps
step.isFinalStep
nextAction.type
```

`nextAction.type` は以下。

```text
SUBMIT_RESPONSE
COMPLETE_SESSION
SAVE_OR_DISCARD_RECORD
```

ボタン文言そのものはAPIでは返さず、フロント側で `sessionType` と `nextAction.type` を見て決める。

---

### 5. Lv.1の到達条件

Lv.1は「初回学習済み / 一通り触れた状態」とする。

Lv.1到達条件は以下のどちらか。

```text
1. 全Sectionの studiedAt が入力済み
2. ユーザーが「このResourceを一通り学習済みにする」操作を実行済み
```

Resource単位の「一通り学習済み」操作は、セクションがあるResourceにもないResourceにも共通で用意する。

ただし、この操作を実行しても、SectionStudyStatusを自動で全部 `studiedAt` 済みにしない。

理由は、個別Sectionを確認した証跡と、Resource全体を一通り学習した自己申告を分けるため。

Progressには以下を追加する。

```text
initialStudiedAt
```

論理名：

```text
初回学習完了日時
```

API候補：

```http
POST /resources/{resourceId}/progress/complete-initial-study
```

実行時の処理：

```text
1. Progress.initialStudiedAt を現在日時で更新
2. Progress.lastStudiedAt を現在日時で更新
3. currentLevel が 0 なら 1 に更新
4. Lv.1の LevelHistory を作成
5. SectionStudyStatus は自動更新しない
```

---

### 6. Lv.2 / Lv.3の定義

Lv.2 / Lv.3は、理解度保証ではなく **学習証跡の状態** として扱う。

```text
Lv.2：
即時振り返り済み。
対象Resourceについて、学習後に自分の言葉で振り返った証跡がある状態。

Lv.3：
遅延想起済み。
時間を空けて、対象Resourceについて思い出し説明した証跡がある状態。
```

Lv.2到達条件：

```text
IMMEDIATE_REFLECTION の LearningSessionRecord が保存されている
かつ
aiAssessment != OFF_TOPIC
```

Lv.3到達条件：

```text
Lv.2到達済み
かつ
DELAYED_RECALL の LearningSessionRecord が保存されている
かつ
aiAssessment != OFF_TOPIC
```

重要な決定として、Lv.2未到達の場合、DELAYED_RECALL のLearningSession自体を開始できないようにする。

---

### 7. 不正対策より証跡の透明性を重視する

読んでいないResourceを登録し、AI対話でそれっぽくLv.3まで進めるような不正は、技術的には完全には防げない。

ただし、SteerLogは資格証明アプリではなく、**学習証跡を透明に見せるアプリ** とする。

そのため、不正を完全に締め出すより、真面目に使う人の証跡が自然に厚く見える設計を優先する。

プロフィール表示では、Level単体ではなく、以下を併記する方針。

* Section進捗
* StudyMemo件数
* Reflection記録
* Recall記録
* aiAssessment
* weakPoint
* nextAction

Lv.3は「完全理解の証明」ではなく、「遅延想起の証跡がある状態」として扱う。

---

### 8. aiAssessmentの判定基準

LearningSessionRecordの `aiAssessment` は以下の3種類。

```text
PASSED
NEEDS_REVIEW
OFF_TOPIC
```

#### PASSED

対象Resourceについて、自分の言葉で説明できており、具体例・比較・実務や自作アプリとの接続がある状態。

Level到達に有効。

#### NEEDS_REVIEW

対象Resourceには沿っているが、説明が浅い、曖昧、具体例不足、混同がある状態。

Level到達には有効だが、表示上は「要復習あり」としてPASSEDと区別する。

#### OFF_TOPIC

対象Resourceとの関係が薄い、回答が成立していない、別テーマに逸れている、読んでいない・分からないだけで終わっている状態。

Level到達には使わない。
LearningSessionRecordとしても保存不可とする。

---

### 9. LearningSessionRecord項目

LearningSessionRecordは、LearningSessionの結果としてユーザーが確認して保存した正式な学習証跡。

MVP項目は以下。

```text
learningSessionRecordId
userId
resourceId
learningSessionId
sessionType
summary
conceptTags
weakPointTags
weakPointSummary
nextAction
aiAssessment
generationBasis
userComment
createdAt
```

`confidenceLevel` はMVPでは採用しない。

理由は、以下の意味が混ざりやすく曖昧なため。

* ユーザーの自信度
* AIの確信度
* 理解度
* 証跡の強さ

`nextAction` は必須。
ただし、弱点補修だけではなく、次の学習・復習・実装・説明・応用アクションを表す。

`weakPointTags` と `weakPointSummary` は任意。
ただし、`aiAssessment = NEEDS_REVIEW` の場合は `weakPointSummary` を必須にする。

---

### 10. LevelHistoryの役割と項目

LevelHistoryは、ResourceがあるLevelに初めて到達したイベント履歴。

役割の違い：

```text
Progress.currentLevel
= 現在到達済みの最高Level

LevelHistory
= いつ・何を根拠に・どのLevelに到達したかの履歴
```

MVP項目：

```text
levelHistoryId
userId
resourceId
level
sourceType
sourceId
reasonCode
createdAt
```

`sourceType` は以下。

```text
INITIAL_STUDY_COMPLETION
SECTION_STUDY_STATUS
LEARNING_SESSION_RECORD
```

`reasonCode` は以下。

```text
INITIAL_STUDY_COMPLETED
ALL_SECTIONS_STUDIED
IMMEDIATE_REFLECTION_RECORD_SAVED
DELAYED_RECALL_RECORD_SAVED
```

`reasonCode` は画面表示だけでなく、LevelHistoryがなぜ作られたかを機械的に判別・集計できるようにするために持つ。

同じLevelの証跡が追加されても、LevelHistoryは増やさない。
LevelHistoryは証跡一覧ではなく、Level到達イベント履歴として扱う。

---

### 11. LearningSessionの状態遷移

LearningSession.status は以下。

```text
IN_PROGRESS
COMPLETED
RECORD_SAVED
DISCARDED
```

状態遷移：

```text
IN_PROGRESS
  ├─ responses → IN_PROGRESS
  ├─ complete  → COMPLETED
  └─ discard   → DISCARDED

COMPLETED
  ├─ record  → RECORD_SAVED
  └─ discard → DISCARDED

RECORD_SAVED
  └─ 終端状態

DISCARDED
  └─ 終端状態
```

操作可能な状態：

```text
responses：
IN_PROGRESS のみ可

complete：
IN_PROGRESS のみ可

record：
COMPLETED のみ可
ただし OFF_TOPIC は保存不可

discard：
IN_PROGRESS / COMPLETED のみ可
```

---

### 12. LearningSessionのエラー方針

MVPで使う主なエラーコード。

```text
RESOURCE_NOT_FOUND
PROGRESS_NOT_FOUND
INVALID_SESSION_TYPE
LEVEL_REQUIREMENT_NOT_MET
SESSION_ALREADY_IN_PROGRESS
INVALID_SESSION_STATUS
RECORD_NOT_ELIGIBLE
RECORD_ALREADY_SAVED
```

重要ルール：

```text
同じ userId + resourceId + sessionType で
IN_PROGRESS または COMPLETED のLearningSessionがある場合、
新しいLearningSessionは開始不可。
```

その場合は、

```text
SESSION_ALREADY_IN_PROGRESS
```

を返す。

DELAYED_RECALLは `Progress.currentLevel >= 2` の場合のみ開始可能。
満たさない場合は、

```text
LEVEL_REQUIREMENT_NOT_MET
```

を返す。

途中離脱は `IN_PROGRESS` のまま残す。
MVPでは再開機能を作り込まず、破棄してやり直す導線を用意する。

---

## 今日の重要な設計判断

今日一番大きかった判断は、SteerLogを以下のように整理したこと。

```text
SteerLogは「理解を完全に保証するアプリ」ではない。
SteerLogは「学習証跡を透明に見せるアプリ」である。
```

そのため、Levelは理解度スコアではなく、証跡状態として扱う。

```text
Lv.1 = 初回学習済み
Lv.2 = 即時振り返り証跡あり
Lv.3 = 遅延想起証跡あり
```

真面目に使う人にとって、学習過程・関心・弱点・次アクション・技術嗜好が自然に蓄積され、技術プロフィールとして見えることを重視する。

---

## 次回やること

次回は、MVP設計を実装に落とすために **DB設計初版** から進める。

対象テーブル：

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

決めること：

```text
物理名
論理名
型
nullable
主な制約
外部キー
論理削除の有無
```

その後、API一覧の最終整形、実装順序の確定に進む。

---

## 残り設計の目安

実装に入れるレベルのMVP設計完了まで、残りはおよそ **4〜8ポモドーロ**。

残り項目：

```text
1. DB設計初版
2. API一覧の最終整形
3. 実装順序の確定
4. MVP範囲 / MVP外の最終線引き
```

MVP設計の思想・中核仕様はほぼ固まったため、次回からは実装用に落とし込む段階に入る。

```
```
