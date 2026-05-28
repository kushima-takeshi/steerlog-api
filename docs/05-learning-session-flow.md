# 05-learning-session-flow.md

# SteerLog LearningSession Flow

## 目的

このドキュメントは、SteerLog MVPにおけるLearningSession / LearningSessionRecordの状態遷移、APIフロー、保存前確認、rawログ非保存方針を定義する。

LearningSession実装、Service、Controller、DTO、テスト作成時は、この内容を基準にする。

---

# 1. LearningSessionの役割

LearningSessionは、AI Reflection / Recallの実行単位である。

LearningSessionは正式証跡ではない。  
正式証跡は、ユーザーが保存した `LearningSessionRecord` である。

```text
learning_sessions
= セッションの進行状態

learning_session_records
= 保存された正式な学習証跡
```

---

# 2. MVPでの対象

MVPではLearningSessionはResource単位で実行する。

```text
対象 = Resource
```

Section単位のLearningSessionはMVPでは実装しない。

理由：

```text
MVPではSection学習状態はSectionStudyStatusとStudyMemoで扱う
LearningSessionはLv.2 / Lv.3のResource-wide証跡にする
```

---

# 3. sessionType

MVPでは2種類。

```text
IMMEDIATE_REFLECTION
DELAYED_RECALL
```

## 3.1 IMMEDIATE_REFLECTION

学習直後の振り返り。

```text
Lv.2に関係する
学習内容を自分の言葉で説明する
AIが要約・概念・弱点・次アクションを整理する
```

## 3.2 DELAYED_RECALL

時間を置いた想起。

```text
Lv.3に関係する
時間が経っても思い出せるかを確認する
Lv.2到達後でなければ開始できない
```

---

# 4. status

LearningSession.statusは以下。

```text
IN_PROGRESS
COMPLETED
RECORD_SAVED
DISCARDED
```

## 4.1 IN_PROGRESS

回答中。

許可操作：

```text
responses
complete
discard
```

## 4.2 COMPLETED

AI結果案 `resultDraft` 作成済み。  
ユーザー確認待ち。

許可操作：

```text
record
discard
```

## 4.3 RECORD_SAVED

ユーザーが確認して保存済み。  
LearningSessionRecord作成済み。

終端状態。

## 4.4 DISCARDED

ユーザーが保存しなかった、または破棄した状態。

終端状態。  
Level更新なし。  
LearningSessionRecordなし。

---

# 5. 状態遷移

```text
IN_PROGRESS
  ├─ responses → IN_PROGRESS
  ├─ complete → COMPLETED
  └─ discard → DISCARDED

COMPLETED
  ├─ record → RECORD_SAVED
  └─ discard → DISCARDED

RECORD_SAVED
  └─ terminal

DISCARDED
  └─ terminal
```

---

# 6. rawログ非保存方針

SteerLogはチャットログ保存アプリではない。

MVPでは以下を正式保存しない。

```text
ユーザーの生回答全文
AI質問履歴全文
AIとの会話ログ全文
回答履歴全文
外部LLMログ全文
```

responses APIやcomplete APIでは、ユーザー回答をリクエストで受け取ってAI生成に使う。  
ただし、DBに正式保存しない。

保存するのは、AIが整理した `resultDraft` と、ユーザーが確認して保存した `LearningSessionRecord`。

---

# 7. resultDraft

## 7.1 役割

`resultDraft` は、complete後にAIが生成する保存候補である。

`learning_sessions.result_draft JSONB` に一時保存する。

## 7.2 含める内容

```json
{
  "summary": "...",
  "conceptTags": ["REST", "HTTP"],
  "weakPointTags": ["PATCH"],
  "weakPointSummary": "...",
  "nextAction": "...",
  "aiAssessment": "NEEDS_REVIEW",
  "generationBasis": "..."
}
```

## 7.3 なぜDBに持つか

```text
record保存時にサーバー側のresultDraftを使うため
AI生成部分をユーザーに改ざんさせないため
保存前確認画面に表示するため
```

---

# 8. APIフロー

## 8.1 セッション開始

```http
POST /resources/{resourceId}/learning-sessions
```

Request：

```json
{
  "sessionType": "IMMEDIATE_REFLECTION"
}
```

処理：

```text
1. Resource所有者チェック
2. sessionType確認
3. DELAYED_RECALLならProgress.currentLevel >= 2を確認
4. 同一userId + resourceId + sessionTypeでIN_PROGRESS/COMPLETEDがないか確認
5. LearningSession作成
6. 最初のaiPromptを返す
```

---

## 8.2 responses

```http
POST /learning-sessions/{learningSessionId}/responses
```

役割：

```text
ユーザー回答をもとに次のAI質問を生成する。
```

注意：

```text
回答を正式保存するAPIではない。
```

Request：

```json
{
  "responses": [
    {
      "step": 1,
      "prompt": "このResourceで学んだ内容を説明してください。",
      "response": "REST APIでは..."
    }
  ]
}
```

処理：

```text
1. LearningSession.status = IN_PROGRESS を確認
2. responsesをもとに次のaiPromptを生成
3. currentStepを進める
4. aiPromptを更新
5. 次の質問を返す
```

---

## 8.3 complete

```http
POST /learning-sessions/{learningSessionId}/complete
```

役割：

```text
全回答をもとにAIがresultDraftを生成する。
```

Request：

```json
{
  "responses": [
    {
      "step": 1,
      "prompt": "このResourceで学んだ内容を説明してください。",
      "response": "REST APIでは..."
    },
    {
      "step": 2,
      "prompt": "自分の実装にどう活かせますか？",
      "response": "Progress更新APIで..."
    },
    {
      "step": 3,
      "prompt": "まだ曖昧な点は何ですか？",
      "response": "PATCHの冪等性が曖昧です。"
    }
  ]
}
```

処理：

```text
1. LearningSession.status = IN_PROGRESS を確認
2. responsesをもとにAIがresultDraftを生成
3. result_draftに保存
4. status = COMPLETED
5. completed_at = now
6. resultDraftを返す
```

---

## 8.4 保存前確認

complete後、画面にresultDraftを表示する。

表示対象：

```text
summary
conceptTags
weakPointTags
weakPointSummary
nextAction
aiAssessment
generationBasis
```

画面文言例：

```text
この内容を学習証跡として保存しますか？
AI生成内容は保存後に編集できません。
納得できない場合は保存せず、もう一度セッションを実行してください。
```

ユーザーができること：

```text
保存する
保存しない
userCommentを追加する
```

ユーザーができないこと：

```text
summaryの編集
conceptTagsの編集
weakPointTagsの編集
weakPointSummaryの編集
nextActionの編集
aiAssessmentの編集
generationBasisの編集
```

---

## 8.5 record

```http
POST /learning-sessions/{learningSessionId}/record
```

役割：

```text
ユーザー確認済みのresultDraftをLearningSessionRecordとして正式保存する。
```

Request：

```json
{
  "userComment": "PUT/PATCHの違いはまだ弱いので、次に自分のProgress更新APIで整理する。"
}
```

処理：

```text
1. LearningSession.status = COMPLETED を確認
2. resultDraftが存在することを確認
3. resultDraft.aiAssessment != OFF_TOPIC を確認
4. LearningSessionRecord作成
5. LearningSession.status = RECORD_SAVED
6. recordSavedAt = now
7. Progress.lastStudiedAt = now
8. sessionTypeに応じてLv.2 / Lv.3到達判定
9. LevelHistory作成
```

注意：

```text
AI生成部分はRequestで受け取らない。
サーバー側のresultDraftを使用する。
```

---

## 8.6 discard

```http
POST /learning-sessions/{learningSessionId}/discard
```

処理：

```text
status = DISCARDED
discarded_at = now
```

効果：

```text
LearningSessionRecordは作成しない
Progress.currentLevelは更新しない
LevelHistoryは作成しない
```

---

# 9. aiAssessment

## 9.1 値

```text
PASSED
NEEDS_REVIEW
OFF_TOPIC
```

## 9.2 PASSED

対象Resourceについて、自分の言葉である程度説明できている。

Level到達候補。

## 9.3 NEEDS_REVIEW

対象Resourceに関係する回答だが、浅い・曖昧・混乱がある。

Level到達候補。  
ただしUIでは「要復習あり」として表示する。

## 9.4 OFF_TOPIC

対象Resourceの証跡として無効。

LearningSessionRecord保存不可。  
Level到達不可。

---

# 10. 同時セッション制御

同一userId + resourceId + sessionTypeについて、以下のstatusのLearningSessionがある場合、新規開始不可。

```text
IN_PROGRESS
COMPLETED
```

エラー：

```text
SESSION_ALREADY_IN_PROGRESS
```

PostgreSQLでは部分ユニークインデックスで制御できる。

```sql
CREATE UNIQUE INDEX uq_learning_sessions_active
ON learning_sessions (user_id, resource_id, session_type)
WHERE status IN ('IN_PROGRESS', 'COMPLETED');
```

---

# 11. DELAYED_RECALL開始条件

DELAYED_RECALLは、対象ResourceがLv.2以上でなければ開始できない。

```text
Progress.currentLevel >= 2
```

満たさない場合：

```text
LEVEL_REQUIREMENT_NOT_MET
```

---

# 12. エラーコード候補

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

---

# 13. MVPでやらないこと

```text
LearningSessionをSection単位にする
会話ログ全文を保存する
ユーザー回答全文を正式保存する
AI質問履歴を保存する
AI生成部分をユーザー編集可能にする
OFF_TOPICを保存可能にする
保存前確認なしで自動保存する
```

---

# 14. まとめ

LearningSessionの基本フローは以下。

```text
start
→ IN_PROGRESS
→ responses
→ complete
→ COMPLETED / resultDraft確認
→ record or discard
→ RECORD_SAVED or DISCARDED
```

正式証跡になるのは、ユーザーが保存したLearningSessionRecordのみ。  
保存しない場合、Levelは上がらない。  
raw回答ログは正式保存しない。
