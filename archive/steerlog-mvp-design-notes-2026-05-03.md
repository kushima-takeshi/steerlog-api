もちろん。
そのまま .md に貼れる形で出します。

# SteerLog MVP設計メモ 2026-05-03
## 1. 今日の結論
2026-05-03は、SteerLog MVPの設計方針を大きく整理した。
主な結論は以下。
- MVPは Lv.1〜Lv.3 までを対象にする
- Lv.4 Artifact、Lv.5 AI Defense、Summary API、Galaxy / Skill Map はMVP外とする
- ReviewRecord ではなく CheckRecord / 確認記録 を採用する
- Evidence / 証跡 を共通概念として置く
- currentLevel は Resource全体の代表到達レベルとして扱う
- LevelHistory は内部モデルとして持つが、API化はしない
- completedAt と archivedAt を分ける
- CheckRecord.result は CHECKED / PASSED / NOT_PASSED とする
- needsReview は result ではなくフラグとして扱う
- method はMVPでは採用しない
- Lv.2 / Lv.3 はAI選択式チェックを使って判定する
- Resource API と Progress API の方針を確定した
---
## 2. SteerLog MVPの目的
SteerLog MVPは、エンジニアが学習リソースごとに、
- 学習した
- 直後確認した
- 時間を置いて再確認した
という状態を管理し、AI選択式チェック・学習メモ・レベル履歴によって、自分の学習状態を説明できるようにするシステムである。
単なる読書ログや学習時間の記録ではなく、学習リソースごとに「どこまで理解・確認できているか」を可視化することを目的とする。
---
## 3. SteerLogの価値
### 個人利用での価値
ChatGPTやGeminiは、今この瞬間の学習支援には強い。
一方で、過去に何を学び、どこまで理解し、どこに不安が残っていたかを、後から信頼できる形で見返す用途には弱い。
SteerLogは、学習リソース・確認結果・復習ポイント・到達レベルを構造化して残すことで、未来の自分が「今どこまで身についているのか」をすぐに取り戻せるようにする。
キャッチコピー案：
> AIで学んだことを、流さずに積む。  
> SteerLogは、エンジニアの学習状態を証跡として残すアプリ。
### 第三者向けの価値
職務経歴書は、基本的に過去の実績を示すもの。
SteerLogの将来の公開ページでは、以下を見せられる。
- 今何を学んでいるか
- 何に興味を持っているか
- どの技術を継続的に追っているか
- どのResourceで学び、どこまで確認しているか
- どの領域に強みが積み上がっているか
つまり、職務経歴書では見えにくい「現在進行形の学習状態」と「強みの由来」を見せられる。
---
## 4. MVP範囲
### MVPに含めるもの
- Resource
- Progress
- StudyMemo
- CheckRecord
- CheckQuestion
- LevelHistory
- Lv.1〜Lv.3の到達管理
- AI選択式チェック
- Resource一覧・詳細
- Progress確認・更新
### MVPに含めないもの
- Artifact
- DefenseRecord
- Summary API
- Galaxy / Skill Map
- SectionProgress
- Lv.4
- Lv.5
ただし、MVP外の要素は将来拡張として設計思想に残す。
---
## 5. 違和感チェック結果
### 違和感1：CheckRecord は「Record」まで必要か
結論：
- CheckRecord / 確認記録 で確定
理由：
- Check だけだと一般的すぎる
- SteerLogで扱うのは「確認した事実の記録」であるため、CheckRecord が自然
確定名：
| 物理名 | 日本語の論理名 |
|---|---|
| CheckRecord | 確認記録 |
| checkId | 確認ID |
| CheckRecord API | 確認記録API |
---
### 違和感2：Progressに何でもぶら下げすぎていないか
結論：
- Evidence / 証跡 を共通概念として置く
構造：
```text
Progress
  ├─ Evidence：証跡
  │    ├─ CheckRecord：確認記録
  │    ├─ Artifact：成果物
  │    └─ DefenseRecord：Defense記録
  └─ LevelHistory：レベル履歴

MVPでは Evidence テーブルは作らず、共通概念として扱う。

⸻

違和感3：currentLevel は単一値でいいのか

結論：

* currentLevel は Resource全体の代表到達レベルとして扱う

物理名	日本語の論理名	意味
currentLevel	Resource全体の代表到達レベル	対象Resource全体として現在どのレベルか

SectionごとのLv.1〜Lv.5はMVPでは持たない。

一部Sectionのみ学習・確認した場合は、sectionId付きEvidenceとして残す。

SectionProgress はMVPでは作らず、将来候補とする。

⸻

違和感4：LevelHistory はMVPに入れるか

結論：

* LevelHistory は内部モデルとして持つ
* ただし LevelHistory API は作らない

物理名	日本語の論理名	意味
LevelHistory	レベル履歴	currentLevelの変更履歴
changedAt	変更日時	レベル到達日時
evidenceType	証跡種別	根拠になった証跡の種類
evidenceId	証跡ID	根拠になった証跡ID

ユーザーが直接作成・更新・削除するものではなく、currentLevel が変わったときに内部処理で自動作成する。

⸻

違和感5：completedAt と currentLevel の関係

結論：

* completedAt は Resource学習そのものの完了日
* 各レベルの到達日は LevelHistory.changedAt で管理する
* ARCHIVED を追加する

status：

物理名	日本語の論理名	意味
NOT_STARTED	未着手	登録したが、まだ学習していない
IN_PROGRESS	学習中	現在学習対象として進めている
PAUSED	中断中	一時停止中。再開可能性あり
COMPLETED	学習完了	ResourceTypeごとの完了条件を満たした
ARCHIVED	アーカイブ済み	今後の学習対象から外した

日時項目：

物理名	日本語の論理名	意味
completedAt	学習完了日時	完了条件を満たした日時
archivedAt	アーカイブ日時	学習対象から外した日時
lastStudiedAt	最終学習日時	最後に学習・確認した日時
startedAt	学習開始日時	学習を開始した日時

⸻

違和感6：CheckRecord.result の扱い

結論：

* result は CHECKED / PASSED / NOT_PASSED
* NEEDS_REVIEW は result ではなく needsReview フラグにする
* 復習詳細は StudyMemo に保存する

result：

値	日本語の論理名	意味
CHECKED	確認済み	合否判定なしで確認した
PASSED	通過	合格条件を満たした
NOT_PASSED	未通過	合格条件を満たさなかった

needsReview：

物理名	日本語の論理名	意味
needsReview	要復習フラグ	復習ポイントがあるか
studyMemoId	学習メモID	復習ポイントメモへの参照

復習ポイント本文は、StudyMemo.memoType = REVIEW_POINT として保存する。

⸻

違和感7：method = SELF_CHECK が曖昧

結論：

* method はMVPでは採用しない
* checkType / checkScope / checkMode / result で確認の意味を表す

代わりに使う項目：

物理名	日本語の論理名	値
checkType	確認種別	IMMEDIATE_CHECK / DELAYED_RECHECK
checkScope	確認範囲	SECTION / RESOURCE
checkMode	確認方式	AI_MULTIPLE_CHOICE
result	確認結果	CHECKED / PASSED / NOT_PASSED

Lv.2方針：

* SectionごとのAI選択式チェックは result = CHECKED として記録
* 全Section学習後のResource全体テストで PASSED になったら Lv.2 到達

⸻

違和感8：Artifact は本当にLv.4専用でいいのか

結論：

* Artifact は Lv.4到達の主根拠
* ただし、Lv.5 Defense時の説明材料にもなる
* MVPでは実装しない

方針：

* Lv.4は「作った・アウトプットした」ことを見る
* Lv.5で「作ったものや学習過程について説明・防衛できるか」を見る

Artifactは原則としてResource全体に対する成果物として扱う。

Section単位のArtifact管理はMVPではしない。

⸻

違和感9：DefenseRecord はAI前提に寄りすぎないか

結論：

* Lv.5はAI Defense前提でよい
* ただしMVPには含めない

Lv.5定義：

Lv.5 = AI Defenseを通過し、実務に近い説明・判断・防衛の証跡が残っている状態。

Defenseでは、Artifactだけでなく、以下も材料にする。

* CheckRecord
* CheckQuestion
* StudyMemo
* LevelHistory

ただし、Lv.5 AI DefenseはMVP後の拡張機能とする。

⸻

違和感10：Summary API はMVPに本当に必要か

結論：

* Summary API はMVPでは作らない
* Galaxy / Skill Map もMVPでは実装しない

MVPでは、Resource一覧・Resource詳細で学習状態が確認できれば十分。

Galaxy / Skill Map は、9月転職時点ではモックで将来像として見せる候補。

⸻

6. Lv.1〜Lv.3の定義

Lv.1：学習した

物理名	日本語の論理名	意味
Lv.1	学習した	Resource全体を一通り学習した状態

例：

* 本を一通り読んだ
* 記事を読み終えた
* 動画を視聴した
* 公式ドキュメントを一通り確認した

Lv.1到達時：

* Progress.currentLevel を 1 に更新
* LevelHistory を作成

⸻

Lv.2：直後確認済み

物理名	日本語の論理名	意味
Lv.2	直後確認済み	学習直後に確認し、Resource全体テストに通過した状態

BOOK / COURSE の場合の流れ：

1. Sectionを学習する
2. SectionごとにAI選択式チェックを実施する
3. Sectionチェックは合否ではなく CHECKED として記録する
4. 全Sectionの学習後、Resource全体テストを受ける
5. Resource全体テストで PASSED になったら Lv.2 到達

Sectionチェック：

項目	値
checkType	IMMEDIATE_CHECK
checkScope	SECTION
checkMode	AI_MULTIPLE_CHOICE
result	CHECKED

Resource全体テスト：

項目	値
checkType	IMMEDIATE_CHECK
checkScope	RESOURCE
checkMode	AI_MULTIPLE_CHOICE
result	PASSED / NOT_PASSED

PASSED時：

* Progress.currentLevel を Lv.2 に更新
* LevelHistory を作成

⸻

Lv.3：時間を置いた再確認済み

物理名	日本語の論理名	意味
Lv.3	時間を置いた再確認済み	一定期間後に再確認し、Resource全体テストに通過した状態

Lv.3定義：

Lv.3は、Lv.2到達から一定期間を置いた後、Resource全体の再確認テストに合格した状態。

MVPでは、Lv.2到達から3日後以降に再確認可能とする。

将来的には、ResourceType別・ユーザー設定で変更可能にする。

Lv.3テスト：

* Resource全体の再確認テスト
* ただし、問題は各Section由来で出題する
* SectionごとにLv.3を管理するわけではない

問題数：

* 各Section 3問
* 例：10Section × 3問 = 30問

問題の性質：

レベル	問題の性質
Lv.2	用語・基本理解・内容把握
Lv.3	比較・判断・使いどころ・なぜそうなるか

合格条件：

* 全体正答率80%以上でPASSED
* 例：30問中24問以上正解でPASSED

NOT_PASSED時：

* Progress.currentLevel は Lv.2 のまま
* CheckRecord.result = NOT_PASSED
* needsReview = true
* 間違えた問題・復習ポイントは StudyMemo に残す

再受験：

* MVPでは翌日以降に再受験可能
* 将来的にはユーザー設定やResourceType別に変更可能

PASSED時：

* Progress.currentLevel を Lv.3 に更新
* LevelHistory を作成
* BOOK / COURSE / IMPLEMENTATION は completedAt を設定

⸻

7. Resource API 確定内容

Resource API一覧

操作	HTTPメソッド / パス	役割
Resource登録	POST /resources	学習リソースを登録する
Resource一覧取得	GET /resources	学習リソース一覧を取得する
Resource詳細取得	GET /resources/{resourceId}	学習リソース詳細を取得する
Resource更新	PATCH /resources/{resourceId}	学習リソース情報を更新する
Resource削除	DELETE /resources/{resourceId}	学習リソースを削除扱いにする

⸻

POST /resources

Resource登録時に、Progressを自動作成する。

Progress初期値：

物理名	初期値
status	NOT_STARTED
currentLevel	0
currentSectionId	null
startedAt	null
completedAt	null
archivedAt	null
lastStudiedAt	null

登録項目：

物理名	日本語の論理名	必須/任意
title	タイトル・リソース名	必須
resourceType	リソース種別	必須
description	説明・概要	任意
sourceUrl	参照URL	任意
author	著者・作成者	任意
tags	タグ	任意
sections	目次・章・セクション	任意

sectionsは、MVPでは手入力またはコピー&ペースト入力を想定する。

将来的には、目次写真/OCR登録も候補。

⸻

GET /resources

Resource一覧を取得する。

これはProgress一覧APIではなく、Resource一覧にProgress概要がくっついてくるAPI。

返却イメージ：

[
  {
    "resourceId": "res-001",
    "title": "Webを支える技術",
    "resourceType": "BOOK",
    "tags": ["HTTP", "REST", "Web API"],
    "progress": {
      "status": "IN_PROGRESS",
      "currentLevel": 2,
      "currentSectionId": "sec-003",
      "lastStudiedAt": "2026-05-03T10:00:00"
    }
  }
]

MVP絞り込み候補：

クエリパラメータ	日本語の論理名
resourceType	リソース種別
tag	タグ
status	進捗ステータス
currentLevel	現在の到達レベル

将来候補：

クエリパラメータ	日本語の論理名
needsReview	要復習あり
keyword	キーワード検索

⸻

GET /resources/{resourceId}

特定Resourceの詳細を取得する。

返却内容：

物理名	日本語の論理名
resource	リソース基本情報
sections	セクション一覧
progress	進捗
recentStudyMemos	最近の学習メモ
recentCheckRecords	最近の確認記録

recentStudyMemos / recentCheckRecords は、Resource詳細画面で現在状態を把握しやすくするために返してよい。

⸻

PATCH /resources/{resourceId}

Resource情報を部分更新する。

更新対象：

物理名	日本語の論理名
title	タイトル・リソース名
resourceType	リソース種別
description	説明・概要
sourceUrl	参照URL
author	著者・作成者
tags	タグ
sections	目次・章・セクション

⸻

DELETE /resources/{resourceId}

Resourceを削除扱いにする。

物理削除ではなく、論理削除とする。

物理名	日本語の論理名
deletedAt	削除日時

通常のGET /resourcesでは、deletedAt が入っているResourceは返さない。

⸻

8. Progress API 確定内容

Progress API一覧

操作	HTTPメソッド / パス	役割
Progress取得	GET /resources/{resourceId}/progress	対象Resourceの進捗を取得する
Progress更新	PATCH /resources/{resourceId}/progress	進捗ステータスや現在Sectionなどを更新する

Progress作成APIは作らない。

POST /resources 時にProgressを自動作成する。

⸻

GET /resources/{resourceId}/progress

指定したResourceに対するProgressを取得する。

返却項目：

物理名	日本語の論理名
progressId	進捗ID
resourceId	リソースID
status	進捗ステータス
currentLevel	Resource全体の代表到達レベル
currentSectionId	現在のセクションID
startedAt	学習開始日時
completedAt	学習完了日時
archivedAt	アーカイブ日時
archiveReason	アーカイブ理由
lastStudiedAt	最終学習日時

⸻

PATCH /resources/{resourceId}/progress

対象ResourceのProgressを部分更新する。

ユーザーが直接更新できる項目：

物理名	日本語の論理名
status	進捗ステータス
currentSectionId	現在のセクションID
archiveReason	アーカイブ理由

自動更新される項目：

物理名	日本語の論理名	更新タイミング
startedAt	学習開始日時	初めて学習開始したとき
lastStudiedAt	最終学習日時	学習・メモ・確認などをしたとき
currentLevel	現在の到達レベル	Lv到達イベント時
completedAt	学習完了日時	完了条件を満たしたとき
archivedAt	アーカイブ日時	statusをARCHIVEDにしたとき

currentLevel は通常PATCHで直接更新させない。

currentLevelは以下のようなLv到達イベントで更新する。

* Lv.1到達操作
* Lv.2テストPASSED
* Lv.3再確認テストPASSED

⸻

9. 今日固まったAPI

今日確定したAPIカテゴリ：

1. Resource API
2. Progress API

⸻

10. 次回固めること

次回以降に固めるもの：

1. Level到達操作API
2. StudyMemo API
3. CheckRecord API
4. CheckQuestionの扱い
5. LevelHistory内部更新ルール
6. API一覧の最終整理

優先順としては、次回はまず Level到達操作API から始める。

⸻

11. 今日の学習ログ

日付

2026/05/03（日）

ポモドーロ数

10ポモドーロ

内容

SteerLog MVP設計の違和感チェックと、MVP要件定義・API設計を進めた。

前半では、ReviewRecordの名称、Progress配下の構造、currentLevel、LevelHistory、completedAt、CheckRecord.result、method、Artifact、DefenseRecord、Summary APIなど、設計上の違和感を10個洗い出して整理した。

中盤では、MVP範囲をLv.1〜Lv.3までに再定義した。

Lv.2は、SectionごとのAI選択式チェックをCHECKEDとして記録し、全Section学習後にResource全体テストでPASSEDになったら到達とした。

Lv.3は、Lv.2到達から一定期間後に、各Section由来の問題で構成されたResource全体再確認テストを受け、全体正答率80%以上でPASSEDとする方針にした。

後半では、Resource APIとProgress APIを確認し、方針を確定した。

学んだこと

SteerLogの価値は、単なる学習進捗管理ではなく、学習状態を説明可能な形で可視化することにある。

AIは今この瞬間の学習支援には強いが、過去の学習状態を構造化して信頼できる形で残す用途には弱い。

SteerLogは、AI時代の学習台帳として、いつ・何を学び、どこまで確認できたかを証跡として積み上げるアプリとして整理できる。

また、API設計では、データとして存在するものをすべてAPI化するのではなく、外部から独立した操作として扱いたいものだけをAPI化するという考え方を確認した。

課題

次回は、APIエンドポイント設計の続きを進める。

特に以下を固める。

* Level到達操作API
* StudyMemo API
* CheckRecord API
* CheckQuestionの扱い
* LevelHistory内部更新ルール

次やること

次回は、Level到達操作API から確認する。

候補：

* Lv.1到達登録API
* Lv.2到達時の自動更新
* Lv.3到達時の自動更新
* LevelHistoryの自動作成ルール

