もちろん。
昨日は途中でかなり大きく方針転換したから、**「なぜ変更したのか」→「何に変えたのか」→「今後どう進めるか」** のストーリーで残すのが良いです。

````md
# SteerLog 設計ログ：2026/05/05（火）

## 今日のテーマ

SteerLog MVP のAPI設計を進める予定だったが、途中で以下の論点が出たため、設計思想を大きく見直した。

- NotebookLMとの差別化
- AIクイズ機能の価値
- 著作権リスク
- Lv.2 / Lv.3 の意味
- Galaxy APIとの関係
- ユーザー入力負荷と離脱率
- LearningSession中心の設計への転換

結果として、SteerLog MVPは **Check / Quiz中心の設計** から、**LearningSession / Reflection / Recall中心の設計** に方針転換した。

---

## 1. 最初の整理：SteerLogの価値はNotebookLMの代替ではない

当初、SteerLogには「Resourceに対してAIクイズを出し、Lv.2 / Lv.3を判定する」という構想があった。

しかし、NotebookLMのようにResource本文を読み込んで、精度の高いクイズ・要約・音声解説を出すツールと比較すると、SteerLogが同じ領域で勝つのは難しい。

そこで、SteerLogの価値は以下のように再定義した。

```text
NotebookLM / ChatGPT / Gemini
= 学習する場所・理解を深める場所

SteerLog
= 学んだ結果を、学習証跡・技術プロフィールとして残す場所
````

SteerLogは、AI教材アプリではなく、エンジニアが何を学び、何に興味を持ち、どこに弱点があり、どのように理解を積み上げてきたかを残すアプリとして考える。

---

## 2. Galaxy APIの位置づけ

SteerLogの本筋が見えるのは、将来的にGalaxy APIが入った段階だと整理した。

```text
MVP
= 学習証跡を蓄積する土台

Galaxy API
= 蓄積した学習証跡をもとに、技術的な興味・強み・弱点・スキルの由来を可視化する機能
```

MVPではGalaxy APIは作らない。

ただし、将来Galaxy APIで使えるように、以下のデータは構造化して残す。

* ResourceTag
* StudyMemo
* LearningSessionRecord.summary
* LearningSessionRecord.conceptTags
* LearningSessionRecord.weakPointTags
* LevelHistory

MVPはResource中心の入力・蓄積体験、GalaxyはTag / Concept中心の可視化体験として位置づける。

---

## 3. AIクイズ機能の見直し

当初のAIクイズ案には以下のメリットがあった。

* ユーザー入力が少なくても動く
* 選択式なのでUXが軽い
* 名著や公開記事なら、それに沿った問題を作れる可能性がある
* Lv.2 / Lv.3の判定が分かりやすい

しかし、次の問題が見えてきた。

### 問題1：NotebookLMの劣化版になりやすい

Resource本文を使ったクイズ生成はNotebookLMの得意領域であり、SteerLogがそこで勝つのは難しい。

### 問題2：著作権リスクがある

書籍本文・記事本文に沿った問題文を生成・保存・公開する場合、著作権リスクが上がる。

特に、AIが有名書籍や記事の内容を知っている場合、URL・タイトル・章名だけでも元Resourceの表現に寄った出力が生成される可能性がある。

### 問題3：タグ・概念クイズに寄せるとResourceごとの差が薄くなる

本文を使わず、タグや目次から概念クイズを作ると、同じ技術領域のResourceでは問題が似やすい。

例：

```text
ネットワークはなぜつながるのか
TCP/IP入門
TCP/IP応用
```

これらはすべてTCP/IPやDNS、HTTP、ルーティングなど似た問題になりやすい。

そのため、ResourceごとのLv.2 / Lv.3の意味がぼやける。

---

## 4. 方針転換：Check / Quiz中心からLearningSession中心へ

上記の問題を踏まえ、SteerLogはAIクイズ中心ではなく、AIとの対話形式に寄せる方針に変更した。

旧設計：

```text
Lv.2 = AI選択式クイズに合格
Lv.3 = 3日後以降のAI再確認クイズに合格
```

新設計：

```text
Lv.2 = 学習直後に、自分の言葉で要点・疑問・理解を整理した
Lv.3 = 一定期間後に、過去の自分の記録をもとに思い出して再説明した
Lv.5 = 質問・反論・応用問いに対して説明・防衛した
```

つまり、Lv.2 / Lv.3 / Lv.5は、AIとの対話形式の違いとして整理する。

```text
Lv.2：Immediate Reflection
Lv.3：Delayed Recall
Lv.5：Defense
```

---

## 5. 新しいLevel定義

### Lv.1：初回学習済み

Resource全体に一通り触れた状態。

到達条件：

```text
Resource内の全Sectionの SectionStudyStatus.studiedAt が埋まっている
```

意味：

```text
初回学習が完了した
学習の地図を作った
深い理解を保証するものではない
```

---

### Lv.2：Immediate Reflection済み

学習直後に、自分の言葉で要点・疑問・理解を整理した状態。

到達条件：

```text
Lv.1に到達している
sessionType = IMMEDIATE_REFLECTION の LearningSession が completed
LearningSessionRecord が作成されている
aiAssessment != OFF_TOPIC
```

意味：

```text
学習直後に、AIとの対話または自己記述を通じて、自分の理解を言語化した
正解率ではなく、Reflectionの実施と記録を重視する
```

---

### Lv.3：Delayed Recall済み

一定期間後に、過去の自分の記録をもとに思い出し、再説明した状態。

到達条件：

```text
Lv.2に到達している
Lv.2到達から3日以上経過している
sessionType = DELAYED_RECALL の LearningSession が completed
LearningSessionRecord が作成されている
aiAssessment != OFF_TOPIC
```

意味：

```text
時間を置いて、何を覚えているか・何が曖昧かを再整理した
記憶の定着を目的としたRecallの証跡を残す
```

---

### Lv.4：Output済み

MVP外。

学習した概念を、記事・設計メモ・実装・READMEなどの成果物に反映した状態。

---

### Lv.5：Defense済み

MVP外。

学習内容やOutputに対して、質問・反論・応用問いに答え、自分の理解や設計判断を説明・防衛できた状態。

将来的には、LearningSession の `sessionType = DEFENSE` として扱う。

---

## 6. MVPに残すAPI / 残さないAPI

### MVPに残すAPI

```text
Resource API
Progress API
SectionStudyStatus API
StudyMemo API
LearningSession API
LearningSessionRecord API
```

### MVPから外すAPI

```text
Check API
CheckQuestion
CheckAnswer
CheckRecord
Galaxy API
Recommendation API
Artifact API
Defense専用API
Public Profile API
Team機能
```

旧Check系は廃止し、LearningSession系に置き換える。

Defenseは専用APIを作らず、将来的に LearningSession の `sessionType = DEFENSE` として拡張する。

---

## 7. 最新ドメインモデル

### Resource

学習対象そのもの。

例：

```text
技術書
記事
動画
講座
公式Docs
実装課題
```

---

### ResourceSection

Resource内の章・セクション。

---

### ResourceTag

Resourceに紐づく技術タグ・分類タグ。

将来Galaxy APIの材料になる。

---

### Progress

Resource全体に対するユーザーの進捗状態。

持つもの：

```text
status
currentLevel
currentSectionId
startedAt
completedAt
lastStudiedAt
```

Resource登録時に自動作成する。

---

### SectionStudyStatus

Sectionごとの学習状態。

持つもの：

```text
studiedAt
understandingLevel
```

`studied` boolean は持たない。

```text
studiedAt != null
= 学習済み

studiedAt == null
= 学習済み扱いではない
```

---

### StudyMemo

ユーザーが任意で残す生メモ。

```text
StudyMemo
= ユーザーが書いた生の記録
```

Section完了時の一言メモもStudyMemoとして扱う。

---

### LearningSession

AIとの学習対話イベントの実行単位。

```text
LearningSession
= AI対話イベントの部屋・実行単位
```

sessionType：

```text
IMMEDIATE_REFLECTION
DELAYED_RECALL
DEFENSE 将来
```

---

### LearningMessage

LearningSession内の対話ログ。

```text
AIの問いかけ
ユーザーの回答
AIの追加質問
```

LearningSession APIの中で扱う。

---

### LearningSessionRecord

LearningSession完了後に作成される整理済みの学習証跡。

```text
LearningSessionRecord
= AI対話後に整理された学習証跡
```

StudyMemoとの違い：

```text
StudyMemo
= ユーザーが書いた生の記録

LearningSessionRecord
= AI対話後に整理された証跡
```

---

### LevelHistory

Lv.1〜Lv.3の到達履歴。

ユーザーが直接作るものではなく、Level更新時に自動作成する。

---

## 8. LearningSessionRecordの項目

LearningSessionRecordには以下を持たせる方針。

```text
learningSessionRecordId
learningSessionId
resourceId
sectionId
sessionType
summary
conceptTags
weakPointTags
nextAction
confidenceLevel
aiAssessment
generationBasis
completedAt
createdAt
```

### summary

人間向けの文章要約。

例：

```text
ユーザーはHTTPメソッドの使い分けについて説明できていたが、PUTとPATCHの違い、冪等性との関係にはまだ曖昧さが残っていた。
```

---

### conceptTags

扱った概念タグ。

Galaxyや検索に使う。

例：

```text
HTTP_METHOD
IDEMPOTENCY
RESOURCE_DESIGN
```

---

### weakPointTags

曖昧だった概念・補強ポイント。

例：

```text
PUT_VS_PATCH
IDEMPOTENCY
```

---

### nextAction

次にやるとよいこと。

LearningSession完了時にAIが対話内容から生成する。

例：

```text
PUTとPATCHの違いを、冪等性と絡めて自分のAPI設計メモに整理する。
```

---

### confidenceLevel

ユーザー自身の理解の自信度。

AIが評価するのではなく、ユーザーがセッション完了時に選ぶ。

例：

```text
HIGH
MEDIUM
LOW
```

---

### aiAssessment

AIの内部判定。

ユーザー画面には原則表示しない。

目的：

```text
LearningSessionが学習対話として成立しているかを内部的に判定する
次回のRecall / Defenseの深掘り材料にする
OFF_TOPICを検出する
```

値の候補：

```text
CLEAR
PARTIAL
NEEDS_REVIEW
OFF_TOPIC
```

MVPでは、OFF_TOPIC の場合だけLv到達NGとする。

---

### generationBasis

AIが何を材料にしたかを表す内部メタ情報。

例：

```text
RESOURCE_TAGS
SECTION_TITLES
SECTION_STUDY_STATUS
STUDY_MEMO
PREVIOUS_LEARNING_SESSION
PREVIOUS_LEARNING_SESSION_RECORD
USER_MESSAGE
```

本文・PDF・URL本文を使わない方針のため、以下はMVPでは使わない。

```text
RESOURCE_BODY
EXTERNAL_PAGE_CONTENT
PDF_CONTENT
```

---

## 9. Progress APIの整理

### Progressの役割

```text
Progress
= Resource全体に対するユーザーの進捗状態
```

### API

```text
GET /resources/{resourceId}/progress
PATCH /resources/{resourceId}/progress
```

### Progress.status

```text
NOT_STARTED
IN_PROGRESS
COMPLETED
ARCHIVED
```

### 学習開始ボタン

画面上では「学習開始」ボタンを用意する。

ユーザーが押すと、

```text
PATCH /resources/{resourceId}/progress
```

で、

```json
{
  "status": "IN_PROGRESS"
}
```

を送る。

このとき、

```text
startedAt が null なら現在日時を設定
lastStudiedAt も更新
```

### 自動IN_PROGRESS遷移

学習開始ボタンを押していなくても、以下の学習行動があれば自動で `IN_PROGRESS` にする。

```text
SectionStudyStatus更新
StudyMemo作成
LearningSession開始または完了
```

これにより、「学習開始ボタンを押し忘れた」場合でも進捗が自然に更新される。

---

## 10. Resource APIの整理

Resource APIは以下で確定。

```text
POST   /resources
GET    /resources
GET    /resources/{resourceId}
PATCH  /resources/{resourceId}
DELETE /resources/{resourceId}
```

方針：

```text
Resource登録時にProgressを自動作成する
Resource一覧にはProgress概要を含める
Resource詳細にはsections / tags / Progress / SectionStudyStatusを含める
currentLevelやstatusはResourceではなくProgressに持たせる
DELETEは論理削除を基本にする
```

---

## 11. ユーザー入力負荷とUX方針

AIクイズをやめたことで、ユーザー自身の入力が価値の源泉になった。

そのため、ユーザーに長文メモを強制しない方針にする。

### Section完了時の軽量入力

Sectionを学習済みにするときに、軽い入力を行う。

```text
理解度を選ぶ
一言メモを書く
```

例：

```text
理解度：PARTIAL
一言メモ：PUTとPATCHの違いがまだ曖昧
```

保存先：

```text
studiedAt / understandingLevel
→ SectionStudyStatus

一言メモ
→ StudyMemo
```

画面上は一緒に入力するが、DB上は責務ごとに分ける。

---

## 12. StudyMemoとSectionStudyStatusの責務分離

```text
SectionStudyStatus
= セクションの学習状態

StudyMemo
= ユーザーが残す生メモ
```

SectionStudyStatusには以下を持たせる。

```text
studiedAt
understandingLevel
```

StudyMemoには以下を持たせる。

```text
content
memoType
resourceId
sectionId
learningSessionId
```

SectionごとのweakPointTagsはSectionStudyStatusには持たせない。

ユーザーが「ここが曖昧」と書いた内容はStudyMemo.contentに残る。

AI対話後に整理された弱点タグはLearningSessionRecord.weakPointTagsに残る。

---

## 13. 著作権リスク対策方針

SteerLogはResource本文を取り込んでクイズ化するアプリではない。

MVPでは以下を行わない。

```text
本文PDFアップロード
書籍本文の保存
記事本文のスクレイピング
URL先本文をAIに読ませる
問題文・選択肢・正解の第三者公開
対話ログ全文の公開プロフィール表示
```

AIはResource本文ではなく、以下を材料にする。

```text
ResourceTag
SectionTitle
SectionStudyStatus
StudyMemo
PreviousLearningSessionRecord
UserMessage
```

StudyMemo入力欄には以下の注意文を出す方針。

```text
書籍・記事・教材などの本文をそのまま貼り付けないでください。
自分の言葉で要点・疑問・理解を書いてください。
```

---

## 14. 最新のSteerLog MVP v3の一文

```text
SteerLog MVPは、AIクイズアプリではない。

Resourceを学習し、Sectionごとに状態と軽いメモを残し、AIとのReflection / Recall対話を通じて、自分の言葉で理解を積み上げる学習証跡アプリである。
```

---

## 15. 次回やること

次回は、Progress APIの残り論点から再開する。

### Progress APIで決めるべき論点

```text
1. statusをユーザーが手動更新できるようにするか
2. currentSectionIdをProgressに持たせるか
3. LearningMessage送信ごとにlastStudiedAtを更新するか
4. IMPLEMENTATIONのCOMPLETED条件をどうするか
```

現時点の仮方針：

```text
1. statusは手動更新可。ただしCOMPLETEDは原則自動
2. currentSectionIdは持たせる
3. lastStudiedAtはSession完了時に更新。Messageごとは不要
4. IMPLEMENTATIONはMVPではCOMPLETED自動判定なし
```

その後、SectionStudyStatus APIへ進む。

```

昨日分のMDに残すなら、これでかなり十分です。  
特に大事なのは、**Check / Quiz中心からLearningSession中心に変わった理由**を残しておくことです。
```
