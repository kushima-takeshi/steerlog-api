もちろん。
design-notes-2026-05-04.md に貼る用として、今日の設計決定をまとめます。

# SteerLog 設計ノート：2026-05-04（月）
## 今日のテーマ
SteerLog MVP における Level / Check / StudyMemo / API 設計の違和感つぶし。
特に以下を中心に整理した。
- Lv.1〜Lv.3 の意味
- Section単位の学習済み状態
- Check開始API / Answer API
- CheckRecordの責務
- weakPointTags / weakPointSummary の扱い
- StudyMemoとCheckRecordの関係
- Evidence APIのMVP扱い
- MVP API一覧の再整理
---
## 1. SteerLogの学習思想
SteerLogのLevel設計は、単なる進捗管理ではなく、効率的な学習プロセスを表す。
基本思想は以下。
1. 1周目では深く理解しきれない  
   - まだ「何が分からないか分からない」状態では、細部を深く理解しづらい。
   - 1周目は、まず全体像・地図を作る段階。
2. 2周目以降で理解が深まる  
   - 一度全体像をつかむと、次に読むときに概念同士のつながりが見えやすくなる。
3. 定着には「思い出す」ことが必要  
   - 読むだけではなく、時間を置いて思い出すことで記憶が定着しやすくなる。
4. エンジニアにはアウトプットが必要  
   - 読んだ・理解しただけでなく、実装・設計・成果物として表現できることが重要。
5. 最終的には説明・防衛できることが重要  
   - なぜそう考えたか、なぜその設計にしたかを説明できることが、エンジニアとして重要。
この思想から、将来的なLv.1〜Lv.5は以下のように位置づける。
- Lv.1：一通り触れた / 地図を作った
- Lv.2：直後確認を通過した
- Lv.3：時間を置いて思い出せた
- Lv.4：アウトプット・成果物で示せた
- Lv.5：説明・Defenseできた
ただし、MVPでは Lv.1〜Lv.3 のみ扱う。
---
## 2. MVPのLevel定義
MVPでは Lv.1〜Lv.3 までを扱う。
### Lv.1：全Section学習済み
全Sectionの `SectionStudyStatus.studiedAt` が埋まった状態。
意味：
- Resource全体に一通り触れた
- 学習の地図を作った段階
- 本であれば「全章を読了した」に相当する
到達条件：
- 全Sectionに `studiedAt` が入っていること
### Lv.2：Resource全体の直後確認合格
Resource全体の `IMMEDIATE_CHECK` に合格した状態。
到達条件：
- `checkScope = RESOURCE`
- `checkType = IMMEDIATE_CHECK`
- `result = PASSED`
意味：
- 一通り学習した直後に、Resource全体の理解確認を通過した段階
### Lv.3：Resource全体の遅延再確認合格
Lv.2到達から3日後以降に、Resource全体の `DELAYED_RECHECK` に合格した状態。
到達条件：
- `checkScope = RESOURCE`
- `checkType = DELAYED_RECHECK`
- `result = PASSED`
- Lv.2到達から3日以上経過
意味：
- 時間を置いても思い出せるかを確認した段階
### Lv.4 / Lv.5
MVPでは実装しない。
ただし思想としては残す。
- Lv.4：アウトプット・成果物で示す段階
- Lv.5：説明・Defenseできる段階
---
## 3. ResourceTypeごとの推奨到達レベル
MVPでは Lv.1〜Lv.3 までを扱うため、ResourceTypeごとの推奨到達レベルも Lv.2〜Lv.3 の範囲で考える。
仮方針：
- ARTICLE：Lv.2推奨
- VIDEO：Lv.2推奨
- DOCUMENTATION：Lv.2推奨
- PROBLEM：Lv.2推奨
- BOOK：Lv.3推奨
- COURSE：Lv.3推奨
- IMPLEMENTATION：MVPでは記録対象に留める
`status = COMPLETED` / `completedAt` は、ResourceTypeごとの推奨到達レベルに到達した時点で設定する。
例：
- ARTICLE が Lv.2 到達 → COMPLETED
- BOOK が Lv.3 到達 → COMPLETED
---
## 4. SectionStudyStatus
Section単位の学習済み状態は、Resource全体の `Progress` とは分ける。
### 採用名
`SectionStudyStatus`
### 理由
- Resource全体の `Progress` と、Section単位の状態管理を混同しないため
- Section側で扱うのは主に「学習済みかどうか」であり、Levelやstatus全体ではないため
- `read` は本寄りなので、汎用的に `studiedAt` を使うため
### API
```http
PATCH /resources/{resourceId}/sections/{sectionId}/study-status

リクエスト例

{
  "studied": true
}

内部項目

* sectionStudyStatusId
* resourceId
* sectionId
* studiedAt
* createdAt
* updatedAt

方針

* studiedAt が入っていれば、そのSectionは学習済み
* 学習済みSectionはSection確認テストを開始できる
* 全Sectionの studiedAt が埋まった場合、LevelEvaluationService がLv.1到達を判定する
* SectionReadRecord は作らない

画面表示

ResourceTypeごとに表示文言を変える。

* BOOK / ARTICLE：読了
* VIDEO：視聴済み
* COURSE：受講済み
* DOCUMENTATION：確認済み
* PROBLEM：実施済み
* IMPLEMENTATION：作業済み

⸻

5. Check開始API

確認テスト開始APIは統一する。

POST /resources/{resourceId}/checks

画面上の入口は異なるが、APIは同じにする。

Section確認

Section横の確認テストボタンから開始する。

{
  "checkScope": "SECTION",
  "sectionId": "section_001",
  "checkType": "IMMEDIATE_CHECK"
}

条件：

* sectionId 必須
* 対象SectionがそのResourceに属していること
* 対象Sectionの SectionStudyStatus.studiedAt が入っていること

結果：

* Levelは更新しない
* CheckRecordとして保存する

Resource全体の直後確認

Resource全体確認ボタンから開始する。

{
  "checkScope": "RESOURCE",
  "checkType": "IMMEDIATE_CHECK"
}

条件：

* sectionId は指定しない
* 全Sectionが学習済みであること

合格時：

* Lv.2到達

Resource全体の遅延再確認

Lv.3再確認ボタンから開始する。

{
  "checkScope": "RESOURCE",
  "checkType": "DELAYED_RECHECK"
}

条件：

* sectionId は指定しない
* currentLevel >= 2
* Lv.2到達から3日以上経過していること

合格時：

* Lv.3到達

⸻

6. Answer API

回答送信APIは共通。

POST /checks/{checkId}/answers

役割：

* 回答受付
* 採点
* weakPointTags 生成
* weakPointSummary 生成
* CheckRecord作成
* 必要に応じてLevelEvaluationService呼び出し
* 結果レスポンス返却

ただし、AnswerServiceにすべて詰め込まない。

Service分割

* AnswerService
    * 回答受付
    * 採点
* WeakPointService
    * weakPointTags 生成
    * weakPointSummary 生成
* CheckRecordService
    * CheckRecord作成
* LevelEvaluationService
    * Lv.1 / Lv.2 / Lv.3 の更新要否判定
* ProgressService
    * Progress.currentLevel
    * status
    * completedAt
    * lastStudiedAt
    * などの更新
* LevelHistoryService
    * LevelHistory作成

⸻

7. CheckRecord

CheckRecordは、確認テストの結果サマリーとして扱う。

CheckRecordを直接POSTするAPIは作らない。
CheckRecordはAnswer APIの結果として作成する。

参照API

GET /resources/{resourceId}/check-records
GET /check-records/{checkRecordId}

保存するもの

* checkRecordId
* resourceId
* sectionId
* checkType
* checkScope
* checkMode
* result
* score
* passedThreshold
* weakPointTags
* weakPointSummary
* checkedAt
* createdAt

保存しないもの

* 問題文
* 選択肢
* ユーザー回答
* 正解
* 問題ごとの詳細な正誤
* incorrectAreas
* needsReview
* reviewPriority
* reviewStatus

⸻

8. Section確認の扱い

Section確認は補助確認。

checkScope = SECTION
checkType = IMMEDIATE_CHECK

Levelは更新しない。

目的：

* 章ごとに理解の揺らぎや復習ポイントを見つける
* 弱点タグや復習ポイント要約を残す
* Resource全体確認前の補助として使う

Section確認の result は基本的に CHECKED として扱う方針。

ただし、以下は保存してよい。

* score
* passedThreshold
* weakPointTags
* weakPointSummary

Lv.2 / Lv.3 の判定には、Resource全体確認のみを使う。

⸻

9. weakPointTags

weakPointTags は、不合格時だけでなく、誤答があれば合格時でも生成する。

生成ルール

* score = 100
    * 生成しない
* score < 100
    * 生成する

保存形式

MVPでは文字列配列として保存する。

{
  "weakPointTags": ["HTTP_METHOD", "IDEMPOTENCY"]
}

タグ生成ルール

* 英大文字スネークケース
* 最大5個
* 概念単位
* 既存タグ候補を優先
* 該当がない場合だけ新規タグを提案

将来構想

将来的にはタグ状態を持たせる。

* SUGGESTED
* APPROVED
* MERGED
* REJECTED

Lv.5 Defense / Galaxy集計では、承認済みタグを主に使う想定。

⸻

10. weakPointSummary

物理名は以下で採用。

weakPointSummary

画面表示名は以下にする。

復習ポイント要約

生成ルール

* 人格評価・能力評価をしない
* 「今回の確認では〜」のように確認結果ベースで書く
* 「理解していない」「能力が低い」などの断定表現を避ける
* 問題文や選択肢をそのまま引用しない
* 80〜150文字程度にする

実装時の管理

設計ドキュメントと、実行時プロンプトテンプレートを分ける。

docs/ai-guidelines/weak-point-summary.md
src/main/resources/prompts/weak-point-summary-prompt.md

⸻

11. needsReview / reviewPriority

MVPでは採用しない。

採用しないもの

* needsReview
* reviewPriority
* reviewStatus

理由

* result / score / passedThreshold から復習推奨か判断できる
* weakPointTags / weakPointSummary で復習ポイントを表現できる
* 判定ルールが増えてCheckRecordが重くなるため

画面表示

DBには保存せず、画面表示時に算出する。

* result = NOT_PASSED または score < passedThreshold
    * 復習推奨
* result = PASSED かつ score < 100
    * 確認ポイントあり
* score = 100
    * 弱点なし

AI推薦や復習キューはMVP外とする。

⸻

12. StudyMemo

StudyMemoは、Resource / Section / CheckRecord に紐づく任意メモ。

CheckRecordには studyMemoId を持たせない。
StudyMemo側に nullable な checkRecordId を持たせる。

API

POST   /resources/{resourceId}/study-memos
GET    /resources/{resourceId}/study-memos
GET    /study-memos/{studyMemoId}
PATCH  /study-memos/{studyMemoId}
DELETE /study-memos/{studyMemoId}

項目

* studyMemoId
* resourceId
* sectionId
* checkRecordId
* memoType
* content
* createdAt
* updatedAt

使い方

学習中メモ：

* checkRecordId = null

テスト後メモ：

* checkRecordId = 対象CheckRecord
* memoType = REVIEW_POINT

UI方針

MVPでは学習伴走機能として常時メモを出すのではなく、以下の入口から任意で書けるようにする。

* Resource詳細
* Section詳細
* テスト結果画面

⸻

13. Evidence

MVPではEvidence API / Evidenceテーブルは作らない。

MVP外

* Evidence API
* Evidenceテーブル

ただし、Evidenceは概念として残す。

MVPで証跡に相当するもの：

* SectionStudyStatus.studiedAt
* CheckRecord
* StudyMemo
* LevelHistory

将来的に証跡を横断表示したくなった段階で、読み取り専用APIを検討する。

GET /resources/{resourceId}/evidence-timeline

Evidence APIは、登録APIではなく、まずは既存データを横断表示するTimeline APIとして考える。

⸻

14. 不採用になったAPI・機能

今日の議論で不採用になったもの。

* POST /resources/{resourceId}/level-achievements
* POST /resources/{resourceId}/study-finish-records
* POST /resources/{resourceId}/study-passes
* POST /resources/{resourceId}/check-records
* SectionReadRecord
* Evidence API
* Evidenceテーブル
* needsReview
* reviewPriority
* AI推薦
* StudySession
* PomodoroLog
* Lv.4 Artifact
* Lv.5 Defense
* Galaxy / Skill Map
* Summary API

⸻

15. MVP API一覧 最新版

Resource

POST   /resources
GET    /resources
GET    /resources/{resourceId}
PATCH  /resources/{resourceId}
DELETE /resources/{resourceId}

Progress

GET    /resources/{resourceId}/progress
PATCH  /resources/{resourceId}/progress

SectionStudyStatus

PATCH  /resources/{resourceId}/sections/{sectionId}/study-status

Check

POST   /resources/{resourceId}/checks
POST   /checks/{checkId}/answers

CheckRecord

GET    /resources/{resourceId}/check-records
GET    /check-records/{checkRecordId}

StudyMemo

POST   /resources/{resourceId}/study-memos
GET    /resources/{resourceId}/study-memos
GET    /study-memos/{studyMemoId}
PATCH  /study-memos/{studyMemoId}
DELETE /study-memos/{studyMemoId}

⸻

16. 今日の結論

今日の設計で、SteerLog MVPは以下の方向に整理された。

* Levelを直接操作するAPIは作らない
* Lv.1は全Section学習済みで到達する
* Lv.2 / Lv.3はResource全体確認の結果で到達する
* Section確認はLevelを上げない補助確認にする
* CheckRecordは回答結果のサマリーとして薄く保つ
* 回答詳細や問題文は保存しない
* weakPointTags / weakPointSummary に復習ポイントを圧縮して保存する
* needsReview / reviewPriority はMVPでは持たない
* StudyMemoは任意メモとして独立させる
* Evidenceは概念として残し、API化はMVP外にする
* Section単位の状態は SectionStudyStatus として、Resource全体の Progress と分離する

次回は、この決定内容をもとに以下を進める。

* API定義の正式化
* DBテーブル設計
* Entity / DTO / Service / Repository の整理
* 実装順序の決定

これをそのまま `design-notes-2026-05-04.md` に貼って大丈夫。