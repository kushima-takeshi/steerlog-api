# SteerLog 設計メモ：2026/05/17 DB設計・Level/Galaxy整理まとめ

## 目的

このメモは、2026/05/17 の議論内容を整理したもの。

前半では、SteerLog MVP の主要DBテーブルについて、Resource / Section / Progress / StudyMemo / LearningSession / LevelHistory / Tags まわりの設計方針を確認した。

後半では、Lv.1〜Lv.5、再学習、Galaxy API、タグの光り方、初回由来・再学習由来・成果物由来の扱いを整理した。

---

# 1. この日の大きな結論

SteerLog は、単なる学習管理アプリではなく、最終的には以下を目指す。

> エンジニアが、どのResourceから何に触れ、何が印象に残り、何を説明でき、何を成果物に反映したかを証跡化し、その人の技術的な興味・知識の源泉・理解の濃淡を可視化するアプリ。

ただし、MVPでは Galaxy API / Lv.4 / Lv.5 は作らない。

MVPでは、Resource / Section / StudyMemo / Lv.1〜Lv.3 の Reflection / Recall を通じて、将来 Galaxy API に使える証跡データを残すところまでを作る。

---

# 2. MVPの位置づけ

MVPは、学習そのものをAIが教えるアプリではない。

AIの役割は先生ではなく、Reflection / Recall の聞き手。

MVPの価値は以下。

> 読んだ・見た・解いたResourceを、読んだだけで終わらせず、AIとのReflection / Recallを通じて、自分の理解・曖昧さ・次アクションを学習証跡として残すこと。

MVPで作る主なもの。

- Resource登録
- ResourceSection管理
- SectionStudyStatus
- StudyMemo
- Progress
- Lv.1 初回接触
- Lv.2 Immediate Reflection
- Lv.3 Delayed Recall
- LearningSessionRecord
- LevelHistory
- 将来のタグ証跡に接続できるデータ構造

---

# 3. DB全体の基本方針

## 3.1 DBはPostgreSQL

SteerLog MVPでは PostgreSQL を使う。

理由：

- Resource / Section / Progress / Memo / LearningSession / LevelHistory などの関係が多い
- FK / UNIQUE制約 / CHECK制約 / トランザクションを使いやすい
- 将来のタグ検索・弱点分析・証跡集計・Galaxy APIに拡張しやすい
- ポートフォリオとしてDB設計の説明がしやすい

## 3.2 ID方針

内部学習管理テーブルは BIGINT を使う。

対象：

- resources
- resource_sections
- progresses
- section_study_statuses
- study_memos
- learning_sessions
- learning_session_records
- level_histories
- tags / tag関連テーブル

外部公開用や共有用は、将来的に UUID / public_id / handle / slug を使う。

内部APIでは BIGINT を使ってもよいが、必ず user_id による認可チェックを行う。

## 3.3 認可方針

BIGINTでもUUIDでも、認可は必須。

内部APIでは基本的に以下を条件にする。

```sql
WHERE id = :id
  AND user_id = :currentUserId
  AND deleted_at IS NULL
```

UUIDは認可の代わりではない。

## 3.4 日時方針

PostgreSQLでは `timestamptz` を使う。

全主要テーブルに以下を持たせる。

- created_at
- updated_at

論理削除対象には、

- deleted_at

を持たせる。

ビジネス上の日時は別カラムで持つ。

例：

- progresses.started_at
- progresses.completed_at
- progresses.last_studied_at
- progresses.initial_studied_at
- section_study_statuses.studied_at
- learning_sessions.completed_at

---

# 4. resources テーブル方針

## 4.1 目的

Resourceは、ユーザーが学習対象として登録する本・記事・動画・講座・問題・実装課題などを表す。

SteerLogの中心になるテーブル。

## 4.2 Resourceは学習対象そのもの

Resourceは「その人の学習行為」ではなく、学習対象そのもの。

例：

- Webを支える技術
- ネットワークはなぜつながるのか
- API設計記事
- AtCoder問題
- 自作API実装課題

## 4.3 重複ResourceはDB制約では禁止しない

同じタイトル・同じ著者のResourceをDB制約で一意にしない。

理由：

- 同じ本を別目的で登録したい可能性がある
- 記事・動画・講座では同一判定が難しい
- ユーザーの登録意図をDBが厳格に判断するのは重い
- MVPでは禁止より警告で十分

ただしUIでは警告を出す。

例：

> 似たResourceがすでに登録されています。既存Resourceを使いますか？

## 4.4 Resource重複時のGalaxy API対策

同じResourceが複数ある場合、将来の集計では同一Resource候補としてまとめる余地を持つ。

ただしMVPでは、DB制約で禁止しない。

---

# 5. resource_sections テーブル方針

## 5.1 目的

Resource内の章・節・項目を管理する。

たとえば本なら目次、動画なら章立て、講座ならレッスン単位。

## 5.2 なぜResourceと分けるか

SectionはResourceに従属する存在だが、独立テーブルとして持つ。

理由：

- Sectionごとの学習状態を管理したい
- SectionごとのStudyMemoを紐づけたい
- Sectionごとの理解度を持ちたい
- 将来的にSection Tagsを持ち、Galaxy APIの接触証跡に使いたい
- Resource本体に section1, section2... のように持つと拡張性が悪い

## 5.3 Sectionは単独では存在しない

SectionはResourceなしでは意味を持たない。

そのため、必ず resource_id を持つ。

概念的には「Resourceの部品」だが、DB上は子テーブルとして分ける。

## 5.4 Section削除

ResourceSectionは論理削除にする。

deleted_at を持つ。

理由：

- ユーザーが目次登録を間違える可能性がある
- SectionにStudyMemoやSectionStudyStatusが紐づく可能性がある
- 物理削除すると履歴・証跡との整合性が難しくなる

---

# 6. progresses テーブル方針

## 6.1 目的

Progressは、ユーザーごとのResource進捗を管理する。

Resourceそのものと、ユーザーの学習状態を分けるために存在する。

## 6.2 主な役割

- status 管理
- current_level 管理
- current_section_id 管理
- started_at
- completed_at
- initial_studied_at
- last_studied_at

## 6.3 status

候補：

- NOT_STARTED
- IN_PROGRESS
- PAUSED
- ARCHIVED
- COMPLETED

ただし COMPLETED は無条件に手動更新できる状態ではない。

ResourceTypeごとの最低到達Levelを満たした場合にCompleted扱いにする。

## 6.4 current_section_id

current_section_id はProgress側に持つ。

意味：

> ユーザーが現在主に学習しているSection

最後にメモしたSectionではない。

current_section_id を変える場合、既に現在Sectionがあるときや、前のSectionが未学習の場合は、UIで確認ダイアログを出す。

これは強制順序ではなく、誤操作防止。

## 6.5 initial_studied_at

Lv.1 の Resourceレベル完了操作で使う。

ユーザーが「このResourceを一通り学習済みにする」を実行した場合に設定する。

この操作は、全Sectionの studied_at を自動で埋めない。

理由：

- Resource全体を一通り学んだ自己申告と、各Sectionを個別に学習済みにした証跡は別物
- SectionStudyStatusを自動で埋めると、個別に確認したように見えてしまう

---

# 7. section_study_statuses テーブル方針

## 7.1 目的

Sectionごとの軽い学習状態を管理する。

StudyMemoとは別。

## 7.2 主な項目

- section_study_status_id
- user_id
- resource_id
- section_id
- studied_at
- understanding_level
- created_at
- updated_at

## 7.3 understanding_level

Section単位の自己理解度。

Progress.current_level とは別。

候補：

1. ほぼ分からない
2. なんとなく分かる
3. 普通に理解した
4. 人に説明できそう
5. 実装・応用できそう

## 7.4 API方針

- GET /resources/{resourceId}/sections/{sectionId}/study-status
- PATCH /resources/{resourceId}/sections/{sectionId}/study-status

Resource詳細取得時には、sections に studyStatus を含める。

これにより、画面側でSectionごとに個別APIを叩かなくて済む。

## 7.5 更新時の自動反映

SectionStudyStatus更新時に自動反映するもの。

- Progress.status が NOT_STARTED の場合、IN_PROGRESS にする
- Progress.last_studied_at を現在時刻に更新する

自動反映しないもの。

- Progress.current_section_id
- Progress.current_level
- COMPLETED

---

# 8. study_memos テーブル方針

## 8.1 目的

StudyMemoは、学習中・学習直後にユーザーが自由に残す短い生メモ。

LearningSessionRecordとは別。

## 8.2 StudyMemoの位置づけ

StudyMemoは、Resource / Sectionに対する自由メモ。

例：

- HTTPの冪等性がまだ曖昧
- POSTとPUTの違いをあとで復習する
- この章はAPI設計に使えそう

StudyMemoはLevel到達条件ではない。

## 8.3 LearningSessionRecord.user_commentとの違い

StudyMemo:

> Resource / Sectionに対する自由な短い学習メモ

LearningSessionRecord.user_comment:

> AIが作った正式証跡に対する、ユーザーの補足・注釈・違和感

統合しない。

理由：

- 通常メモとAI評価への補足が混ざる
- どのLearningSessionRecordへのコメントか分からなくなる
- AI生成部分を編集不可にしつつ、ユーザーの補足だけ残す場所が必要

## 8.4 StudyMemoの主な項目

- study_memo_id
- user_id
- resource_id
- section_id nullable
- memo_type
- content
- tags
- deleted_at
- created_at
- updated_at

## 8.5 memo_type

候補：

- GENERAL
- LEARNED
- QUESTION
- WEAKNESS
- TODO
- IDEA
- SUMMARY

MVP UIで必須にするかは未確定。  
DB/API上は持つ。

## 8.6 content制限

1〜500文字。

UI警告：

> 本文の丸写しではなく、自分の言葉で学び・気づき・疑問を残しましょう。

## 8.7 API方針

- POST /resources/{resourceId}/memos
- GET /resources/{resourceId}/memos
- GET /resources/{resourceId}/memos/{memoId}
- PATCH /resources/{resourceId}/memos/{memoId}
- DELETE /resources/{resourceId}/memos/{memoId}

`study-memos` ではなく `memos` にする。

## 8.8 一覧取得

一覧はpreviewを返す。  
全文は詳細APIで返す。

ページングあり。

- created_at desc
- default 20
- max 50

フィルタ候補：

- sectionId
- memoType
- tag
- keyword search later

## 8.9 削除

StudyMemoは論理削除。

deleted_at を設定する。

---

# 9. learning_sessions テーブル方針

## 9.1 目的

LearningSessionは、AI Reflection / Recall の実行単位。

AIチャットログ保管ではない。

## 9.2 LearningSessionの位置づけ

LearningSessionは、Lv.2 / Lv.3 のための短いReflection / Recallセッション。

通常のStudyMemoやSection学習とは別。

## 9.3 session_type

MVPでは以下。

- IMMEDIATE_REFLECTION
- DELAYED_RECALL

IMMEDIATE_REFLECTION は Lv.2 に対応。  
DELAYED_RECALL は Lv.3 に対応。

## 9.4 LearningSessionはResource単位

MVPではSectionスコープにしない。

Section単位の通常学習は SectionStudyStatus / StudyMemo で扱う。  
LearningSessionはResource全体に対する特別イベント。

## 9.5 messagesではなくresponses

LearningSessionでは `messages` ではなく `responses` を使う。

理由：

- AIチャットログではない
- ユーザーの回答を提出していくReflection / Recallセッションだから

API例：

- POST /learning-sessions/{learningSessionId}/responses

## 9.6 raw会話ログは保存しない

正式証跡として以下は保存しない。

- AIの質問全文
- ユーザーの回答全文
- 会話ログ全部

保存するのは、完了後にユーザーが保存した LearningSessionRecord。

理由：

- 著作権・プライバシー・情報管理リスクを下げる
- SteerLogをAIチャット履歴アプリにしない
- 学習証跡アプリとしての軸を守る

## 9.7 status

候補：

- IN_PROGRESS
- COMPLETED
- RECORD_SAVED
- DISCARDED

遷移：

- IN_PROGRESS → IN_PROGRESS via responses
- IN_PROGRESS → COMPLETED via complete
- IN_PROGRESS → DISCARDED via discard
- COMPLETED → RECORD_SAVED via record
- COMPLETED → DISCARDED via discard
- RECORD_SAVED / DISCARDED は終端

## 9.8 同時セッション制約

同じ user_id + resource_id + session_type で、IN_PROGRESS または COMPLETED のLearningSessionが既にある場合、新規開始不可。

エラー：

- SESSION_ALREADY_IN_PROGRESS

## 9.9 DELAYED_RECALLの開始条件

DELAYED_RECALL は、Progress.current_level >= 2 の場合だけ開始できる。

Lv.2に到達していない状態でLv.3は開始できない。

エラー：

- LEVEL_REQUIREMENT_NOT_MET

---

# 10. learning_session_records テーブル方針

## 10.1 目的

LearningSession完了後に、ユーザーが保存した正式な学習証跡を管理する。

AIとのraw会話ログではなく、AIが整理した保存用Record。

## 10.2 MVPフィールド

- learning_session_record_id
- user_id
- resource_id
- learning_session_id
- session_type
- summary
- concept_tags
- weak_point_tags
- weak_point_summary
- next_action
- ai_assessment
- generation_basis
- user_comment
- created_at

## 10.3 confidence_levelは持たない

confidence_level はMVPから外す。

理由：

- ユーザー自信度なのか
- AIの判定自信度なのか
- 理解度なのか
- 証跡強度なのか

が曖昧だから。

## 10.4 next_action

next_action は必須。

意味は、弱点修正だけではない。

- 次の学習
- 復習
- 実装
- 説明
- 応用
- 成果物化

などを含む。

## 10.5 weakPointSummary

weak_point_summary は通常は任意。

ただし ai_assessment = NEEDS_REVIEW の場合は必須。

## 10.6 ai_assessment

候補：

- PASSED
- NEEDS_REVIEW
- OFF_TOPIC

PASSED:

> ユーザーが対象Resourceについて、自分の言葉で具体例・比較・実務接続などを含めて説明できている。

NEEDS_REVIEW:

> オンテーマだが浅い、曖昧、具体例が弱い、混同がある、弱点が見える。  
> Lv到達証跡としては有効だが、要復習ありとして表示する。

OFF_TOPIC:

> 対象Resourceと関係が薄い、短すぎる、質問に答えていない、読んでいない/分からないだけ。  
> Lv到達証跡として無効。

OFF_TOPICはLearningSessionRecord保存不可。

## 10.7 concept_tags / weak_point_tags

当初 JSONB の話が出たが、MVPでは TEXT[] でもよいと整理した。

ただし、その後のタグ設計の議論により、将来的には tags マスタ + learning_session_record_tags のような別テーブルに寄せる方針が自然になった。

MVPでは簡易的にTEXT[]でもよいが、Galaxy APIを見据えるならタグ証跡テーブル化を検討する。

## 10.8 同じResource・同じsessionTypeのRecord

MVPでは、同じ user_id + resource_id + session_type の正式LearningSessionRecordは1件だけにする方針。

理由：

- Lv.2 / Lv.3 は何度もやり直して正解を磨く場所ではない
- 初回に何が印象に残ったかを残すため
- 複数回保存するとGalaxy APIの濃淡がぼやける
- SteerLogをAIチャット履歴・学習伴走アプリに寄せないため

制約候補：

```sql
CREATE UNIQUE INDEX uq_learning_session_records_user_resource_session_type
ON learning_session_records (user_id, resource_id, session_type);
```

---

# 11. level_histories テーブル方針

## 11.1 目的

LevelHistoryは、証跡一覧ではなく、Level初回到達履歴。

Progress.current_level が現在の最高到達Level。  
LevelHistory は、いつ・何によって・どのLevelに初めて到達したかを残す。

## 11.2 LearningSessionRecordとの違い

LearningSessionRecord:

> 学習証跡そのもの

LevelHistory:

> Levelに初めて到達したイベント

## 11.3 同じResource・同じLevelは1件だけ

LevelHistoryは初回到達履歴なので、同じ user_id + resource_id + level は1件だけ。

制約候補：

```sql
CREATE UNIQUE INDEX uq_level_histories_user_id_resource_id_level
ON level_histories (user_id, resource_id, level);
```

## 11.4 source_type

候補：

- INITIAL_STUDY_COMPLETION
- SECTION_STUDY_STATUS
- LEARNING_SESSION_RECORD

## 11.5 reason_code

候補：

- INITIAL_STUDY_COMPLETED
- ALL_SECTIONS_STUDIED
- IMMEDIATE_REFLECTION_RECORD_SAVED
- DELAYED_RECALL_RECORD_SAVED

## 11.6 source_id

source_id はポリモーフィック参照になる。

つまり、source_type によって参照先が変わる。

例：

- LEARNING_SESSION_RECORD → learning_session_records.id
- SECTION_STUDY_STATUS → section_study_statuses.id
- INITIAL_STUDY_COMPLETION → null

DBの外部キーでは縛りにくいため、アプリケーション側で整合性を守る。

## 11.7 deleted_at

LevelHistoryは正式な到達履歴なので、MVPでは deleted_at を持たせない。

---

# 12. tags / tag evidence 方針

## 12.1 自由入力タグではない

SteerLogのタグは、ユーザーが自由に好きな言葉を付ける個人ラベルではない。

目的は、他のエンジニアにも伝わる一般的・標準的な技術概念として分類すること。

例：

- Java
- Spring Boot
- REST
- HTTP
- PostgreSQL
- Database Design
- Authentication
- Authorization
- API Design
- DDD
- Testing
- Refactoring
- Legacy Migration

## 12.2 タグは将来のGalaxy APIに使う

タグは以下に使う。

- Resource分類
- Section分類
- StudyMemoからの抽出
- LearningSessionRecordのconcept / weakPoint
- Artifactの技術要素
- Defense対象
- Galaxy API
- 得意・苦手可視化
- 技術プロフィール

## 12.3 Resource Tags

Resource自体が含んでいる技術・概念。

AIがResource登録時に推定する。  
ユーザーの自由入力ではなく、標準タグ候補に寄せる。

## 12.4 Section Tags

Sectionが扱っている技術・概念。

将来的に、どのSectionを学習したかから、どのタグに接触したかを推定するために使う。

## 12.5 Evidence Tags

実際の証跡から確認されたタグ。

- StudyMemo
- LearningSessionRecord
- Artifact
- Defense

から抽出・確認される。

Galaxy APIで強く使うのは、Resource Tagsそのものではなく、Evidence Tags。

## 12.6 Focus TagsはMVPでは不要

当初、Resource登録時に「今回学びたいタグ」を選ぶ案があった。

しかし議論の結果、Focus TagsはMVPでは不要とした。

理由：

- 学習したいタグと実際に学習したタグはズレる
- 何を勉強したかは、SectionStudyStatus / StudyMemo / LearningSession / Artifact から判断した方がよい
- 自己申告タグを強く使うと、Galaxy APIの信頼性が下がる

---

# 13. Galaxy APIと星の光の制御

## 13.1 基本方針

星の強さは、単にそのタグが何回出たかでは決めない。

以下の組み合わせで決める。

> 証跡の深さ + 由来の種類 + Resource内での役割

1つのResourceに含まれる全タグが均一に強く光らないようにする。

## 13.2 光の強さは5段階

- 1: 微光
- 2: 弱い光
- 3: 中くらいの光
- 4: 強い光
- 5: 最大光

ただし、タグの種類によって到達できる最大値が違う。

## 13.3 由来タイプ

星の強さだけでなく、由来タイプも持つ。

候補：

- RESOURCE_TOPIC
- SECTION_CONTACT
- MEMO_INTEREST
- CORE_ORIGIN
- RELATED_ORIGIN
- WEAK_POINT
- RECALLED
- LV3_SURFACED
- ARTIFACT_CONTEXT
- RESOURCE_DERIVED_ARTIFACT
- ARTIFACT_SURFACED
- INITIAL_DEFENSE
- GROWN_LATER
- PRIOR_KNOWLEDGE
- INCIDENTAL

## 13.4 Resourceに存在しないタグはResource由来にしない

重要制約：

> Resource / Section のタグとして存在しないタグは、そのResource由来として反映しない。

たとえArtifactに出ていても、そのResource由来の強い星にはしない。

---

# 14. 2026/05/17時点の未確定・将来検討

## 14.1 MVPでタグテーブルをどこまで入れるか

候補：

- tags
- resource_tags
- section_tags
- learning_session_record_tags
- study_memo_tags

MVPで全部入れると重い。

ただし、Galaxy APIを見据えると section_tags / learning_session_record_tags は重要。

## 14.2 LearningSessionRecordのタグをTEXT[]にするか、別テーブルにするか

MVPではTEXT[]でもよい。

ただし、将来のGalaxy APIやタグ証跡の由来管理を考えると、別テーブル化した方がよい。

## 14.3 再学習軸のDB化

将来的には、Resource直下に learning_cycle のような概念を追加する可能性がある。

例：

- INITIAL
- RELEARNING

ただしMVPでは作らない。

## 14.4 Lv.4 / Lv.5 のDB設計

将来検討。

必要になりそうなもの：

- artifacts
- artifact_tags
- defense_records
- defense_record_tags
- learning_cycle_id
- evidence_origin_type

---

# 15. まとめ

2026/05/17 の議論では、SteerLogのMVP DB設計と、将来のLevel / Galaxy API思想を接続して整理した。

MVPでは、以下を作る。

- Resource
- Section
- Progress
- SectionStudyStatus
- StudyMemo
- LearningSession
- LearningSessionRecord
- LevelHistory

将来は、以下へ拡張する。

- Tags / Evidence Tags
- Lv.4 Artifact
- Lv.5 Defense
- 初回学習軸 / 再学習軸
- Galaxy API

最終的な方向性は以下。

> SteerLogは、学習そのものを教えるアプリではなく、学習後の理解・印象・弱点・次アクションを証跡化するアプリ。  
> MVPではLv.1〜Lv.3で証跡を残す。  
> 将来的にはLv.4成果物、Lv.5 Defense、Galaxy APIで、その人の技術的興味・知識の源泉・理解の濃淡を可視化する。  
> 同じResourceは初回学習軸と再学習軸に分け、初回の印象と後から育った理解を両方残す。  
> Galaxy APIでは星の強さだけでなく、初回由来・再学習由来・成果物由来などの由来も分けて表示する。
