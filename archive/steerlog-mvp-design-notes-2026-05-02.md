もちろん。今日の設計まとめとして、別チャットや docs/2026-05-02-design-summary.md に貼れる形で整理します。

# 2026/05/02（土） SteerLog 設計まとめ
## 1. 今日のテーマ
SteerLog Status API のMVP設計。
今日は、SteerLogを「ただの学習記録アプリ」ではなく、学習リソース・進捗・学習メモ・成果物・Defense記録をつなげて、学習状態を説明しやすくするアプリとして整理した。
特に以下を中心に設計した。
- MVPの範囲
- APIとは何か
- Status API全体像
- Resource API
- Progress API
- Lv.1〜Lv.5の扱い
- 完了条件
- 将来のAI連携方針
---
## 2. MVP方針
### MVPの考え方
MVPは、単に「動く最小機能」ではなく、アプリの核となる価値を感じられる最小単位とする。
SteerLogの場合、核となる価値は以下。
> 学習したことを、進捗・メモ・成果物・Defense記録と紐づけて、説明可能な状態として管理できること。
そのため、単なる読書ログや学習メモだけではMVPとして弱い。
MVPでは、以下を最小単位として扱う。
- 学習リソースを登録できる
- 進捗を管理できる
- 学習メモを残せる
- Lv.2 / Lv.3 の確認履歴を残せる
- Lv.4 の成果物を登録できる
- Lv.5 のDefense記録を残せる
- 現在の学習状態を一覧・詳細で確認できる
---
## 3. LLM連携のMVP方針
MVPではLLM連携は実装しない。
理由は、MVPでまず検証したい価値がLLMではなく、以下だから。
> 学習リソース、進捗、学習メモ、成果物、Defense記録をつなげることで、学習状態を説明しやすくなるか。
LLMは将来的には有効だが、MVP段階では以下を実装しない。
- LLMによるLv.4判定
- LLMによるLv.5判定
- AI質問生成
- AI回答レビュー
- GitHubやブログの自動解析
- 成果物との関連度自動判定
ただし、将来AI補助を入れやすいように、ArtifactやDefenseRecord、StudyMemoのデータ構造は意識する。
将来的な方針は以下。
- LLMは判定者ではなく確認補助者として扱う
- Artifact登録時はLv.4候補にする
- DefenseRecord登録時はLv.5候補にする
- AI確認後にProgress.currentLevelを更新する
- AIの判断は絶対評価ではなく補助評価にする
---
## 4. Status API全体像
Status APIは、学習状態管理という大きな機能領域として捉える。
その中に以下のAPIが含まれる。
- Resource API
- Progress API
- StudyMemo API
- ReviewRecord API
- Artifact API
- DefenseRecord API
- Summary API
つまり、Status APIを1つの巨大なAPIとして作るのではなく、責務ごとに分ける。
```text
Status API
├─ Resource API
├─ Progress API
├─ StudyMemo API
├─ ReviewRecord API
├─ Artifact API
├─ DefenseRecord API
└─ Summary API

APIの理解

APIとは、画面・アプリ・他システムが、サーバー側の機能やデータを利用するための決まった呼び出し口である。

APIを用意することで、以下が可能になる。

* フロントとバックエンドを分離できる
* Vue / TypeScript と Java / Spring Boot を分けて作れる
* Web画面、スマホアプリ、外部システムから同じバックエンド機能を使える
* DBを直接触らせず、入力チェックや保存ルールをサーバー側に集約できる
* Resource / Progress / Artifact などの責務を整理しやすい

SteerLogでは以下の構成を想定する。

フロントエンド：Vue 3 / TypeScript
↓
REST API
↓
バックエンド：Java 21 / Spring Boot 3
↓
DB：MySQL or PostgreSQL

⸻

5. Resource API

Resourceとは

Resourceは、ユーザーが学習対象として登録するもの。

例：

* BOOK
* ARTICLE
* VIDEO
* COURSE
* PROBLEM
* IMPLEMENTATION
* DOCUMENTATION

MVPではユーザー体験としてBookを中心に考える。
ただし、内部設計ではBook専用にせず、Resourceという共通概念で扱う。

これにより、将来的に記事、動画、講座、問題、実装課題などへ拡張しやすくする。

⸻

Resource登録項目

必須項目

* title：タイトル・リソース名
* resourceType：リソース種別

任意項目

* description：説明・概要
* sourceUrl：参照URL
* author：著者・作成者
* tags：タグ
* sections：目次・章・チャプター

⸻

Resource項目の設計判断

tags

tagsはMVP全体では重要。

理由は、将来的に以下へ使えるため。

* 技術領域ごとの整理
* Resource一覧の絞り込み
* スキル状態の可視化
* Summary APIでの集計

ただし、Resource登録時に必須にすると分類で迷いやすいため、任意項目とする。

sourceUrl

sourceUrlはResource全体では任意。

理由は、BookやImplementationTaskなど、必ずしもURLを持たないResourceがあるため。

ただし、ARTICLE / VIDEO / DOCUMENTATION ではURL登録を推奨する。

sections

sectionsは目次・章・チャプターを表す。

MVPでは任意項目とするが、将来的に以下へ使えるため重要。

* Lv.2確認
* Lv.3再確認
* StudyMemoとの紐づけ
* Defense質問生成
* LLM連携時の安全な入力情報

本文そのものではなく、目次・章構成を扱うことで、著作権リスクを下げながらAI連携にもつなげやすい。

⸻

Resource API エンドポイント

POST /resources

学習リソースを登録する。

登録項目：

* title
* resourceType
* description
* sourceUrl
* author
* tags
* sections

Resource登録時に、Progressも自動作成する。

⸻

GET /resources

学習リソース一覧を取得する。

MVPでは以下で絞り込み可能にする。

* resourceType
* tag
* status
* currentLevel

返却内容には、Resource基本情報に加えてProgress概要も含める。

通常は deletedAt が null のResourceのみ返す。

⸻

GET /resources/{resourceId}

学習リソース詳細を取得する。

返却内容：

* Resource基本情報
* tags
* sections
* progress

Resource詳細画面では、学習対象そのものの情報だけでなく、現在の学習状態も同時に必要になるため、Progressも含める。

⸻

PATCH /resources/{resourceId}

学習リソース情報を更新する。

更新対象：

* title
* resourceType
* description
* sourceUrl
* author
* tags
* sections

⸻

DELETE /resources/{resourceId}

学習リソースを削除扱いにする。

MVPでは物理削除ではなく論理削除とする。

内部的には deletedAt を設定し、通常一覧・詳細取得では表示しない。

理由：

* ResourceはProgress、StudyMemo、Artifact、DefenseRecordの起点になる
* 物理削除すると学習証跡とのつながりが壊れる
* 誤削除から復元できる余地を残したい

⸻

6. Progress API

Progressとは

Progressは、ユーザーが対象Resourceを現在どこまで学習しているかを表す。

Resource = 学習対象そのもの
Progress = そのResourceに対するユーザー個人の学習状態

例：

Resource:
Webを支える技術
Progress:
status = IN_PROGRESS
currentLevel = 2
currentSectionId = 第3章
lastStudiedAt = 2026-05-02

currentLevelやstatusはResourceではなくProgressに持たせる。
理由は、到達レベルや学習状態はResourceそのものの性質ではなく、ユーザーとResourceの関係だから。

⸻

Progress項目

* resourceId
* status
* currentLevel
* currentSectionId
* startedAt
* completedAt
* lastStudiedAt

status：

* NOT_STARTED：未着手
* IN_PROGRESS：学習中
* COMPLETED：完了
* PAUSED：中断

currentLevel：

* Lv.0：未学習・未到達
* Lv.1：学習した
* Lv.2：直後確認済み
* Lv.3：時間を置いた再確認済み
* Lv.4：成果物あり
* Lv.5：Defense済み

⸻

Progress作成方針

Resource登録時にProgressを自動作成する。

初期値：

* status = NOT_STARTED
* currentLevel = 0
* currentSectionId = null
* startedAt = null
* completedAt = null
* lastStudiedAt = null

そのため、MVPではProgressの明示的な作成APIは用意しない。

⸻

Progress API エンドポイント

GET /resources/{resourceId}/progress

対象Resourceに紐づくProgressを取得する。

返却内容：

* resourceId
* status
* currentLevel
* currentSectionId
* startedAt
* completedAt
* lastStudiedAt

⸻

PATCH /resources/{resourceId}/progress

対象Resourceに紐づくProgressを部分更新する。

更新対象：

* status
* currentLevel
* currentSectionId
* startedAt
* completedAt
* lastStudiedAt

Progress更新は既存Progressの一部更新であるため、POSTではなくPATCHを採用する。

REST APIでは、URLは対象リソースを表し、HTTPメソッドはその対象に対する操作を表す。

PATCH /resources/{resourceId}/progress
= 特定Resourceに紐づくProgressの一部を更新する

⸻

Progress一覧API

MVPではProgress単体の一覧APIは用意しない。

理由は、ユーザーが一覧画面で見たいのはProgress単体ではなく、ResourceとProgress概要を組み合わせた情報だから。

そのため、GET /resources のレスポンスにProgress概要を含める。

例：

GET /resources?status=IN_PROGRESS

で、学習中のResource一覧を取得する。

⸻

7. currentLevel更新方針

Lv.1〜Lv.3

Lv.1〜Lv.3は、Progress更新やReviewRecordに応じて管理する。

* Lv.1：学習した
* Lv.2：直後確認した
* Lv.3：時間を置いて再確認した

Lv.2 / Lv.3 の根拠はReviewRecordで管理する。

⸻

Lv.4

Lv.4はArtifact登録時にProgress.currentLevelを自動更新する。

MVPでは、Artifact登録は証跡つき自己申告として扱う。

将来AI確認を導入する場合は、即Lv.4に更新せず、Lv.4候補として扱い、AI確認後に更新する。

⸻

Lv.5

Lv.5はDefenseRecord登録時にProgress.currentLevelを自動更新する。

MVPでは、DefenseRecord登録はDefenseログつき自己申告として扱う。

将来AI確認を導入する場合は、即Lv.5に更新せず、Lv.5候補として扱い、AI確認後に更新する。

⸻

8. completedAt と完了条件

completedAt

completedAtは、そのResourceを完了扱いにした日。

Lv.3到達日やLv.4到達日そのものではない。

完了時点の強さは currentLevel で表す。

例：

status = COMPLETED
currentLevel = 2
completedAt = 2026-05-02

これは、Lv.2相当で完了したことを表す。

status = COMPLETED
currentLevel = 5
completedAt = 2026-05-20

これは、Defense済みで完了したことを表す。

⸻

完了の度合い

完了しているかどうかは status で表す。

完了の強さ・信頼度は currentLevel で表す。

完了しているか = status
どのレベルで完了したか = currentLevel

⸻

ResourceTypeごとの完了可能条件

ResourceTypeごとに、完了可能な最低レベルを変える。

軽いResource：

* ARTICLE：Lv.2以上で完了可能
* VIDEO：Lv.2以上で完了可能
* DOCUMENTATION：Lv.2以上で完了可能
* PROBLEM：Lv.2以上で完了可能

重いResource：

* BOOK：Lv.3以上で完了可能
* COURSE：Lv.3以上で完了可能
* IMPLEMENTATION：Lv.3以上で完了可能

理由：

* 短い記事や動画までLv.3必須にすると運用が重くなる
* BookやCourseは学習負荷が大きいため、時間を置いた再確認まで求める方が自然
* 完了を主観だけにせず、到達レベルとセットで扱うことで証跡性を保てる

⸻

9. 再学習時の扱い

MVPでは、再学習用に別Resourceを作らない。

同じResourceに対するProgressを再開する。

再学習時：

* status を IN_PROGRESS に戻す
* completedAt は前回完了日として残す
* lastStudiedAt を更新する
* currentSectionId を更新する

将来的には、1周目・2周目・面接前復習などを管理するために、LearningCycleのような概念を追加する可能性がある。

MVPでは対象外。

⸻

10. StudyMemo / ReviewRecord / Progress.note の整理

StudyMemo

StudyMemoは、学習内容そのものを残す正式なメモ。

残す内容：

* 学習内容
* 気づき
* 疑問
* 弱点
* 再学習ポイント

将来的にはAI質問生成やDefense補助にも利用できる。

⸻

Progress.note

MVPでは持たせない。

理由：

* Progressは現在状態の管理に集中させる
* StudyMemoと役割が重なる
* ユーザーに複数のメモ欄を見せると混乱する

⸻

ReviewRecord.memo

MVPでは持たせない。

理由：

* ReviewRecordは確認イベントの記録に集中させる
* 確認後の気づきや弱点はStudyMemoに書けばよい
* StudyMemoとReviewRecord.memoが重なると、どこに書くべきか分かりにくくなる

⸻

11. ReviewRecord方針

ReviewRecordは、Lv.2 / Lv.3 の理解確認・再確認の履歴を表す。

Progress = 今の状態
ReviewRecord = いつ、どの範囲を、どう確認して、結果がどうだったか

MVPの項目案：

* resourceId
* sectionId
* reviewType
* reviewedAt
* method
* result

reviewType：

* IMMEDIATE_CHECK：Lv.2用の直後確認
* DELAYED_RECHECK：Lv.3用の時間を置いた再確認

method：
s
* SELF_CHECK
* SUMMARY
* QUIZ
* EXTERNAL_TEST

result：

* PASSED
* FAILED
* NEEDS_REVIEW

MVPではmemoは持たせない。
学習内容・気づき・弱点はStudyMemoに集約する。

ReviewRecord APIは次回以降に詳細設計する。

⸻

12. 今日決まった重要な設計判断

1. ResourceとProgressを分ける

Resourceは学習対象そのもの。
ProgressはユーザーとResourceの関係・進捗。

同じResourceでも、ユーザーごとにProgressは異なるため分離する。

⸻

2. currentLevelはProgressに持たせる

currentLevelはResourceそのものではなく、ユーザーがそのResourceに対してどこまで到達しているかを表す。

そのためProgressに持たせる。

⸻

3. Resource登録時にProgressを自動作成する

Resource登録後すぐに学習状態を管理できるようにするため、Progressは自動作成する。

ただし、Resource登録時点では学習開始扱いにはしない。

初期状態は NOT_STARTED / Lv.0。

⸻

4. Resource詳細にはProgressも含める

Resource詳細画面では、Resource情報だけでなく現在の学習状態も必要になる。

そのため、GET /resources/{resourceId} ではProgressも返す。

⸻

5. Progress一覧APIはMVPでは作らない

Progress単体の一覧より、Resource一覧にProgress概要を含める方がユーザー体験に合う。

⸻

6. Lv.4 / Lv.5 は証跡登録時に自動更新する

MVPでは、Artifact登録時にLv.4、DefenseRecord登録時にLv.5へ自動更新する。

将来AI導入後は、即時更新ではなく候補化してAI確認後に更新する。

⸻

7. 完了条件はResourceTypeごとに変える

軽いResourceはLv.2以上で完了可能。
重いResourceはLv.3以上で完了可能。

完了の度合いは status と currentLevel の組み合わせで表す。

⸻

8. メモはStudyMemoに集約する

Progress.note と ReviewRecord.memo はMVPでは持たせない。

ユーザーが迷わないよう、学習内容・気づき・弱点はStudyMemoに集約する。

⸻

13. 次回やること

次回は ReviewRecord API を設計する。

考えること：

* ReviewRecordのエンドポイント
* sectionIdを必須にするか任意にするか
* Lv.2 / Lv.3 登録時にcurrentLevelをどう更新するか
* Lv.3に必要な「時間を置いた再確認」の扱い
* ReviewRecordの一覧取得が必要か
* ReviewRecordとStudyMemoの関係
* 将来AI連携時の拡張余地

候補エンドポイント：

POST /resources/{resourceId}/reviews
GET /resources/{resourceId}/reviews
GET /resources/{resourceId}/reviews/{reviewId}
DELETE /resources/{resourceId}/reviews/{reviewId}

次回は、ReviewRecordがLv.2 / Lv.3の根拠になるため、Status API全体の中でもかなり重要な設計になる。

今日の進捗としてはかなり大きいです。  
**Resource API と Progress API はかなり仮確定。次は ReviewRecord API に入れる状態** まで来ています。