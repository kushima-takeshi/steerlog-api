もちろん。
そのまま `2026-05-06.md` に貼れる形でまとめます。

````md
# 学習ログ：2026/05/06（水）

## 実施ポモドーロ数

8ポモドーロ

## 今日の学習テーマ

SteerLog MVP API設計の続きとして、以下を中心に整理した。

- Progress APIの残り論点
- SectionStudyStatus API
- StudyMemo API
- LearningSession / LearningSessionRecord の入口設計
- 将来構想としてのLLM連携 / MCP連携

---

## 今日の到達点

当初の予定では、Progress APIを確定し、SectionStudyStatus APIまで整理できれば十分だった。

実際にはそれを超えて、StudyMemo APIまで主要方針を確定し、LearningSession / LearningSessionRecord の大枠まで整理できた。

今日の時点で、SteerLog MVPの主要API設計は以下の状態になった。

- Resource API：既に大枠整理済み
- Progress API：主要論点確定
- SectionStudyStatus API：主要方針確定
- StudyMemo API：主要方針確定
- LearningSession API：責務と大枠方針まで整理
- LearningSessionRecord API：責務と保存方針まで整理

---

## 1. Progress APIの整理

### statusの手動更新

Progress.status はユーザーが手動更新できるようにする。

ただし、COMPLETED は無条件の手動更新にはしない。

NOT_STARTED / IN_PROGRESS / PAUSED / ARCHIVED は手動更新可能。  
COMPLETED は、ResourceTypeごとの最低到達レベルを満たす場合のみ許可する方針。

### currentSectionId

Progress.currentSectionId は、Resource全体における現在の学習位置を表す。

これは SectionStudyStatus 側ではなく、Progress 側に持たせる。

理由は、SectionStudyStatus に `isCurrent` のような状態を持たせると、Sectionが多いResourceで管理が複雑になるため。

Progress.currentSectionId は、

- 最後に更新されたSection
- 最後にメモを書いたSection

ではなく、

- ユーザーが現在メインで学習しているSection

を表す。

### currentSectionId変更時の確認

currentSectionIdを変更する場合、すでに別のSectionが学習中であれば、確認ダイアログを出す。

例：

- 「このSectionを学習中にしますか？」
- 「現在学習中のSectionを『第2章』から『第5章』に変更しますか？」
- 「『第3章』『第4章』は未学習のままです。このSectionを学習中にしますか？」

この制御は、学習順序を強制するためではなく、ユーザーが自分の学習状態を誤って上書きしないようにするため。

### lastStudiedAt

Progress.lastStudiedAt は持つ。

ただし、totalStudyTime は持たない。

SteerLogは学習時間管理・ポモドーロ伴走アプリではなく、学習証跡・技術プロフィールの可視化アプリであるため。

学習時間やポモドーロ連携はMVP外とする。

どの領域にどれだけ向き合ったかは、実時間ではなく、Resource、Tag、StudyMemo、LearningSessionRecord、LevelHistoryなどの証跡量・密度・到達度から表現する。

### IMPLEMENTATIONの扱い

ResourceType = IMPLEMENTATION は、Lv.4 Output / 制作物に近い位置づけとする。

MVPは Lv.1〜Lv.3 を中心にするため、IMPLEMENTATION の自動COMPLETED判定は行わない。

MVPでは、IMPLEMENTATION をResourceとして手動登録できる程度に留める。

将来的には、Artifact、GitHub URL、README、テスト、設計書、Defense記録などと紐づけて、Lv.4 / Lv.5 の証跡として扱う。

---

## 2. SectionStudyStatus APIの整理

### 責務

SectionStudyStatus は、Resource内の各Sectionごとの学習状態を管理する。

Progress が Resource全体の進捗を表すのに対して、SectionStudyStatus は Section単位の状態を表す。

StudyMemo が学習内容・気づき・弱点などの文章記録を扱うのに対して、SectionStudyStatus は studiedAt や understandingLevel などの軽い状態だけを扱う。

長文メモは持たせない。

### 主な項目

| 物理名 | 論理名 | 説明 |
|---|---|---|
| sectionStudyStatusId | セクション学習状態ID | SectionStudyStatusを一意に識別するID |
| userId | ユーザーID | 所有ユーザーを識別するID |
| resourceId | 学習リソースID | 対象ResourceのID |
| sectionId | セクションID | 対象SectionのID |
| studiedAt | 学習済み日時 | そのSectionを学習済みにした日時。nullの場合は未学習扱い |
| understandingLevel | 理解度 | Section単位の自己理解度 |
| createdAt | 作成日時 | レコード作成日時 |
| updatedAt | 更新日時 | レコード更新日時 |

### understandingLevel

understandingLevel は、Section単位の自己理解度を表す。

例：

| 値 | 意味 |
|---|---|
| 1 | ほぼ分からない |
| 2 | なんとなく分かる |
| 3 | 普通に理解した |
| 4 | 人に説明できそう |
| 5 | 実装・応用できそう |

Progress.currentLevel とは別物として扱う。

- Progress.currentLevel：Resource全体のSteerLog到達レベル
- SectionStudyStatus.understandingLevel：Section単位の自己理解度

### エンドポイント

```http
GET /resources/{resourceId}/sections/{sectionId}/study-status
PATCH /resources/{resourceId}/sections/{sectionId}/study-status
````

Resource詳細画面では、SectionStudyStatusを1件ずつ取得しない。

`GET /resources/{resourceId}` のレスポンスに、sections と各Sectionの studyStatus を含める。

これにより、画面側は追加リクエストなしで、各Sectionの学習状態を一覧表示できる。

### PATCH時の方針

`PATCH /resources/{resourceId}/sections/{sectionId}/study-status` では、studiedAt / understandingLevel を部分更新する。

studiedAt は null に戻せる。

これは、誤操作で学習済みにしてしまったSectionを未学習状態へ戻せるようにするため。

ただし、studiedAt を null に戻しても、一度到達した Progress.currentLevel や LevelHistory は下げない。

### SectionStudyStatus更新時のProgress反映

SectionStudyStatus更新時には、Progressへ一部だけ自動反映する。

自動反映するもの：

* Progress.status が NOT_STARTED の場合、IN_PROGRESS に更新する
* Progress.lastStudiedAt をサーバー側の現在日時で更新する

自動反映しないもの：

* Progress.currentSectionId
* Progress.status = COMPLETED
* Progress.currentLevel

currentSectionId はユーザーの明示的な操作でのみ更新する。

全Sectionが studiedAt ありになった場合は、Lv.1到達判定の候補とするが、このAPI単体で無条件に currentLevel を更新するかは、別途 LevelHistory ルールで定義する。

---

## 3. StudyMemo APIの整理

### 責務

StudyMemo は、ユーザーが学習中に自分の言葉で残す短い生メモを管理する。

Progress や SectionStudyStatus が状態を扱うのに対して、StudyMemo は、その時点の気づき・疑問・弱点・次アクションなどの文章記録を扱う。

LearningSessionRecord が AI対話後に整理された学習証跡であるのに対して、StudyMemo はユーザーがその場で残す未整理の生メモである。

### StudyMemoの位置づけ

StudyMemo は任意機能とする。

StudyMemoを書かなくても、

* Progress更新
* SectionStudyStatus更新
* LearningSession実行
* Level到達

は可能とする。

StudyMemoの有無だけで、ユーザーの理解度・到達レベル・学習価値を判定しない。

理由は、StudyMemoを必須化すると、SteerLogが学習伴走・日々の学習管理アプリに寄ってしまうため。

### 主な項目

| 物理名         | 論理名      | 説明                                 |
| ----------- | -------- | ---------------------------------- |
| studyMemoId | 学習メモID   | StudyMemoを一意に識別するID                |
| userId      | ユーザーID   | 所有ユーザーを識別するID                      |
| resourceId  | 学習リソースID | 対象ResourceのID。必須                   |
| sectionId   | セクションID  | 対象SectionのID。Resource全体メモの場合はnull  |
| memoType    | メモ種別     | メモの種類。未指定の場合はGENERAL               |
| content     | メモ本文     | ユーザーが自分の言葉で残すメモ本文                  |
| tags        | タグ       | メモで扱っている概念・技術テーマ                   |
| deletedAt   | 削除日時     | 論理削除日時。通常一覧ではdeletedAtがnullのものだけ表示 |
| createdAt   | 作成日時     | レコード作成日時                           |
| updatedAt   | 更新日時     | レコード更新日時                           |

### 作成単位

StudyMemo は、ResourceまたはSectionに対して複数作成できる。

1つのStudyMemoは、ある時点でのユーザーの学び・気づき・疑問・弱点・次アクションを短く残す記録とする。

同じResource / Sectionに対して、日中の学習時、夜の復習時、数日後の再確認時など、複数のStudyMemoを時系列で蓄積できる。

StudyMemoは長文ノートではなく、軽い学習証跡として扱う。

### memoType

memoType はDB・API項目として用意する。

ただし、MVP画面で必須入力にするかは保留する。

未指定の場合は GENERAL として扱う。

候補値：

* GENERAL / 一般メモ
* LEARNED / 学んだこと
* QUESTION / 疑問
* WEAKNESS / 弱点
* TODO / 次やること
* IDEA / アイデア
* SUMMARY / 要約

### content制限

StudyMemo.content は 1〜500文字とする。

StudyMemoは、書籍・記事・教材本文を保存するためのものではなく、ユーザーが自分の言葉で学び・気づき・疑問・弱点・次アクションを残すための軽いメモである。

画面上では、以下の注意文を表示する。

> 本文の丸写しではなく、自分の言葉で学び・気づき・疑問を残しましょう。

### エンドポイント

```http
POST   /resources/{resourceId}/memos
GET    /resources/{resourceId}/memos
GET    /resources/{resourceId}/memos/{memoId}
PATCH  /resources/{resourceId}/memos/{memoId}
DELETE /resources/{resourceId}/memos/{memoId}
```

### 一覧取得

`GET /resources/{resourceId}/memos` では、content全文ではなく preview を返す。

content全文は、`GET /resources/{resourceId}/memos/{memoId}` で取得する。

一覧はページングする。

方針：

* createdAt desc で新しい順
* デフォルト20件
* 最大50件
* 通常一覧では deletedAt が null のメモのみ返す

絞り込み条件：

* sectionId
* memoType
* tag

keyword検索はMVPでは後回しでもよい。

### 削除

DELETE は物理削除ではなく論理削除とする。

deletedAt を設定し、通常一覧から非表示にする。

自動削除は行わない。

### importantフラグ

StudyMemoには important フラグを持たせない。

ユーザーに「このメモが重要かどうか」を選択させない。

理由は、StudyMemoは分類・評価作業ではなく、学習中にその場で残す軽い生メモだからである。

将来的に重要そうなメモを扱う場合も、ユーザーの明示的な重要判定ではなく、LearningSessionやLearningSessionRecordで参照・関連づいた実績から見える化する。

### LearningSessionとの関係

StudyMemoはLearningSessionの参考材料にはなり得るが、必須条件にはしない。

LearningSessionでどのStudyMemoを優先して参照するかは、LearningSession API設計時に別途決める。

MVPでは、StudyMemo側に以下の情報を持たせておくことで、後から参照しやすい構造にする。

* resourceId
* sectionId
* memoType
* tags
* createdAt

### StudyMemo作成時のProgress反映

StudyMemoを作成した場合、Progressへ一部だけ自動反映する。

自動反映するもの：

* Progress.status が NOT_STARTED の場合、IN_PROGRESS に更新する
* Progress.lastStudiedAt をサーバー側の現在日時で更新する

自動反映しないもの：

* Progress.currentSectionId
* Progress.currentLevel
* Progress.status = COMPLETED

---

## 4. LearningSession / LearningSessionRecord の入口整理

### 基本分離

LearningSession / LearningMessage / LearningSessionRecord は分けて管理する。

* LearningSession：AIとのReflection / Recall対話の実行単位
* LearningMessage：LearningSession中の1メッセージ
* LearningSessionRecord：LearningSession完了後に保存される正式な学習証跡

### sessionType

MVPでは以下の2種類を扱う。

* IMMEDIATE_REFLECTION
* DELAYED_RECALL

IMMEDIATE_REFLECTION は Lv.2 到達に関係する。

DELAYED_RECALL は Lv.3 到達に関係する。

### SectionStudyStatusとの違い

SectionStudyStatus は、Section単位の軽い学習状態を管理する。

各Sectionを学習済みにしていき、Sectionを持つResourceで全Sectionが studiedAt ありになった場合、Lv.1到達候補とする。

Lv.1到達後、Resource全体または対象範囲に対して IMMEDIATE_REFLECTION の LearningSession を実行する。

IMMEDIATE_REFLECTION は Lv.2 到達に関係する。

つまり、

* SectionStudyStatus：Lv.1到達候補に関係
* LearningSession：Lv.2 / Lv.3到達に関係

と整理する。

### AI対話の流れ

LearningSessionは、長時間の学習伴走ではなく、Lv.2 / Lv.3到達のための短いReflection / Recallセッションとする。

想定フロー：

1. ユーザーがLearningSessionを開始する
2. AIが対象Resource / Sectionについて質問する
3. ユーザーが自分の言葉で回答する
4. AIが追加で1〜3問程度質問する
5. ユーザーが回答する
6. AIがsummary / conceptTags / weakPointTags / nextAction / aiAssessment などを整理する
7. ユーザーがAI整理結果を確認する
8. 保存する / 保存しない / もう一度実行する を選択する
9. 保存した場合のみLearningSessionRecordとして正式な学習証跡になる

### LearningSessionRecord候補に含める項目

* summary
* conceptTags
* weakPointTags
* nextAction
* confidenceLevel
* aiAssessment
* generationBasis

### AI生成部分の編集方針

LearningSessionRecordのAI生成部分は、ユーザーが直接編集できない。

対象：

* summary
* conceptTags
* weakPointTags
* nextAction
* confidenceLevel
* aiAssessment
* generationBasis

理由は、LearningSessionRecordがAIとの対話をもとにした第三者的な整理・評価の証跡であり、ユーザーが自由に書き換えられると信頼性が下がるため。

ユーザーは、AI生成内容を確認したうえで以下を選択できる。

* 保存する
* 保存しない
* もう一度実行する

また、必要に応じて userComment として補足コメントを追加できる。

userCommentはAI評価を書き換えるものではなく、ユーザー自身の補足・自己認識・異議を残すための項目とする。

### 保存しない場合

ユーザーが保存しないを選んだ場合、LearningSessionRecordは作成しない。

そのLearningSessionはLv.2 / Lv.3到達判定には使わない。

Progress.currentLevelは更新しない。

LevelHistoryも作成しない。

画面上は、該当sessionTypeの確認は未完了として扱う。

LearningSession自体は、必要に応じて DISCARDED として残す。

ただし、学習証跡としては扱わない。

### Level連動

LearningSessionRecordが保存され、aiAssessment が OFF_TOPIC でない場合、Lv.2 / Lv.3到達候補とする。

* IMMEDIATE_REFLECTION の有効なLearningSessionRecord → Lv.2候補
* DELAYED_RECALL の有効なLearningSessionRecord → Lv.3候補

aiAssessment の候補：

* PASSED：概ね説明できている
* NEEDS_REVIEW：学習証跡としては有効だが、弱点が残っている
* OFF_TOPIC：対象テーマから外れていて、レベル到達には使わない

NEEDS_REVIEWでも、学習証跡として有効ならLv.2 / Lv.3到達候補にしてよい。

SteerLogは試験アプリではなく、学習証跡アプリだからである。

---

## 5. LLM連携 / MCP連携の将来方針

今日の途中で、SteerLogと外部LLM連携・MCP連携について深掘りした。

### 外部LLMログ取り込みについて

MVPでは、ChatGPT / Gemini / Claude などの外部LLMログ全文を取り込む機能は提供しない。

理由：

* 外部LLMログには、書籍本文、記事本文、教材本文、業務情報、個人情報などが混在する可能性がある
* 解析目的であっても、アプリ側に一度取り込む時点で著作権・プライバシー・情報管理上のリスクが高い
* SteerLogがログ保管アプリや学習伴走アプリに寄る可能性がある

将来的に対応する場合も、外部ログ本文をそのまま保存するのではなく、ユーザーがLLMにSteerLog向けの構造化データを出力させ、それをユーザー確認のうえで取り込む方向がよい。

### MCP連携について

SteerLogが将来MCP / Connector的に使えるようになる構想はあり。

ただし、MVPでは実装しない。

将来的に実装する場合も、まずはRead Only MCPから始めるのがよい。

例：

* get_learning_overview
* search_resources
* get_resource_progress
* get_weak_points
* get_next_actions

書き込み対応する場合は、LLMから直接保存せず、draftとして作成し、ユーザー確認後に保存する。

### MCP連携の基本方針

* 外部LLMログ全文は取得・保存しない
* raw data ではなく learning state を返す
* StudyMemo全文を外部に無制限に返さない
* Read Onlyから始める
* 書き込みはdraft化する
* 監査ログを残す
* スコープを分ける
* 接続解除・トークン失効を可能にする

### スキル観点

MCPや外部LLM連携は、単なるAI実装ではなく、AI時代の外部連携・認証認可・セキュリティ・監査ログ・運用設計が絡む領域。

今の段階では本格実装を目指すより、将来構想として設計に織り込む程度でよい。

まずは、通常のプロダクト系バックエンドAPIとして安全に作れることを優先する。

---

## 6. 今日の学び

今日の大きな学びは、SteerLogの設計において、機能を増やすことよりも、アプリの重心を守ることが重要だということ。

特に以下の判断が大きかった。

* 学習時間・ポモドーロ連携は入れない
* StudyMemoは任意の短い生メモに留める
* StudyMemoをProgressやLevel到達の必須条件にしない
* LLMログ全文取り込みはしない
* LearningSessionRecordのAI評価はユーザーが編集できないようにする
* ユーザーは補足コメントだけ追加できる
* 保存されたLearningSessionRecordだけを正式な学習証跡にする

これにより、SteerLogが「勉強伴走アプリ」や「ログ保管アプリ」に寄らず、学習証跡・技術プロフィール可視化アプリとしての方向性を保てた。

---

## 7. 次回やること

次回は、LearningSession APIのエンドポイント設計から入る。

主な論点：

* LearningSession開始API
* LearningMessage送信API
* LearningSession完了API
* LearningSessionRecord保存API
* LearningSessionRecord一覧・詳細取得API
* LearningSession.status の状態遷移
* LearningSessionRecord保存時のProgress / LevelHistory更新
* Lv.2 / Lv.3到達ルール
* OFF_TOPIC / NEEDS_REVIEW / PASSED の扱い

次回ここを整理すれば、SteerLog MVPの主要API設計はほぼ一周する。

```

今日は当初予定をかなり超えて進んでます。  
このMDを残しておけば、次回は **LearningSession APIのエンドポイント設計** からスムーズに再開できます。
```
