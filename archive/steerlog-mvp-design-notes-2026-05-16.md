# SteerLog DB設計方針まとめ：2026/05/16
## 目的
SteerLog MVP のDB設計に入る前に、DB選定・ID設計・削除方針・日時カラム・enum管理・user_id / 外部キー / index方針を整理する。
今回の目的は、単にテーブル定義を作ることではなく、**なぜそのDB設計にするのかを説明できる状態にすること**である。
SteerLogは、単なる学習ログアプリではなく、学習Resource、Progress、StudyMemo、LearningSessionRecord、LevelHistoryなどを関係づけて、学習状態・技術志向・スキル形成の根拠を説明可能にするアプリである。
---
# 1. DB選定
## 採用DB
SteerLog MVPでは **PostgreSQL** を採用する。
## 理由
SteerLogは、以下のような関連データを扱う。
- Resource
- ResourceSection
- Progress
- SectionStudyStatus
- StudyMemo
- LearningSession
- LearningSessionRecord
- LevelHistory
これらは単体で完結するデータではなく、互いに関係して意味を持つ。
たとえば、StudyMemoは「誰の、どのResourceの、どのSectionに対するメモか」が重要であり、LevelHistoryは「どのResourceが、いつ、何を根拠にLevel到達したか」を示す。
そのため、データ同士の関係性と整合性を扱いやすいリレーショナルDBが適している。
PostgreSQLを選ぶ理由は以下。
- Resource / Progress / Memo / LearningSessionRecord / LevelHistory の関係性を扱いやすい
- 外部キー・一意制約・トランザクションなどで整合性を保ちやすい
- UUID型との相性がよい
- 将来的なタグ検索、弱点分析、学習証跡の集計、プロフィール可視化と相性がよい
- JSON的な拡張余地もあり、将来のGalaxy構想とも説明をつなげやすい
- ポートフォリオとして技術選定理由を説明しやすい
## MySQLとの比較
MySQLでもSteerLogは実装可能である。
MySQLを選ぶ理由としては、以下がある。
- Webアプリケーションでの利用実績が多い
- Spring Bootとの情報量が多い
- CRUD中心のMVPであれば十分対応可能
- 運用ノウハウが多い
ただし、SteerLogでは将来的にタグ検索・弱点分析・学習証跡の集計・プロフィール可視化などを考えているため、PostgreSQLの方がアプリの方向性と説明を結びつけやすい。
## NoSQL / MongoDBを採用しない理由
MongoDBなどのNoSQLは、関係・整合性・履歴を扱えないわけではない。
ただし、SteerLogのように Resource、Progress、Memo、LearningSessionRecord、LevelHistory を関係づけて証跡として扱うアプリでは、RDBの方が自然である。
NoSQLでは、関係管理や整合性担保の責任がアプリケーション側に寄りやすい。
SteerLogのMVPでは、以下の理由からRDBを採用する。
- 存在しないResourceにMemoが紐づくような不整合を避けたい
- LevelHistoryやLearningSessionRecordの根拠関係を明確にしたい
- JOINや集計を使ってResource詳細、Progress、Memo、LevelHistoryを扱いたい
- 学習証跡アプリとしてデータの信頼性を保ちたい
## NewSQLを採用しない理由
NewSQL / Distributed SQL は、SQLとACIDトランザクションを保ちつつ、水平スケールや分散処理を狙うDBである。
ただし、SteerLog MVPでは以下のような要件はまだない。
- 複数リージョンでの強整合性
- 大量アクセスへの水平スケール
- 単一DBでは処理できない規模
- グローバルSaaSレベルの高可用性
現在の課題は、分散スケールではなく、Resource、Progress、Memo、LearningSessionRecord、LevelHistory の関係性と証跡整合性を正しく設計することである。
そのため、MVPではPostgreSQLで十分と判断する。
---
# 2. ID設計
## 基本方針
SteerLogでは、内部の学習管理データと外部公開データでID方針を分ける。
```text
内部学習データ:
BIGINT ID
外部公開データ:
UUID public_id または handle / slug

⸻

内部学習データはBIGINTを採用する

対象テーブル例。

* resources
* resource_sections
* progresses
* section_study_statuses
* study_memos
* learning_sessions
* learning_session_records
* level_histories

これらは、基本的にログインユーザー本人だけが閲覧・操作する詳細学習データである。

そのため、DB内部の主キーにはBIGINTを採用する。

BIGINTを採用する理由

* 短く扱いやすい
* URLやログで見やすい
* RedmineやIssue管理のように数値IDとして扱いやすい
* DB内部のJOINやデバッグがしやすい
* 主キー・外部キーとしてシンプル
* MVP実装がわかりやすい

内部APIでは以下のようなURLでもよい。

GET /resources/123
GET /resources/123/memos/456
GET /learning-sessions/789

ただし、BIGINTは推測可能なIDであるため、必ず認可チェックを行う。

select *
from resources
where id = :resourceId
  and user_id = :currentUserId
  and deleted_at is null;

UUIDは認可の代わりではない

UUIDは推測されにくくするための補助であり、認可チェックの代わりにはならない。

BIGINTでもUUIDでも、以下は必須である。

* そのデータがログインユーザー本人のものか確認する
* そのユーザーがそのデータを閲覧・更新・削除してよいか確認する
* 公開データの場合はvisibilityを確認する

⸻

外部公開データはUUID public_id または handleを使う

対象例。

* public_profiles
* share_links
* Galaxy API の公開エンドポイント
* public-facing profile identifier

外部公開URLでは、連番IDによってユーザー数・登録順・プロフィールIDが推測されることを避けたい。

そのため、公開用にはUUIDまたはhandle / slugを使う。

例。

GET /profiles/{publicProfileId}/galaxy
GET /profiles/@wushijiudao/galaxy

公開APIでは内部IDを返さない

Galaxy APIや公開プロフィールでは、内部の詳細学習ログをそのまま見せるのではなく、集計済みのプロフィール情報を返す。

返すもの。

* skillCluster
* tags
* evidenceCount
* highestLevel
* 技術志向
* 弱点傾向
* Galaxy表示用の集計データ

返さないもの。

* resource_id
* study_memo_id
* learning_session_record_id
* 生のStudyMemo本文
* 今どの章を勉強しているか
* 詳細な内部学習ログ

ID設計の結論

内部詳細データはBIGINTで管理する。
外部公開プロフィールやGalaxy APIではUUID public_id または handle を使う。
BIGINTでもUUIDでも認可チェックは必須。
公開APIでは内部IDや生ログを返さず、集計済みデータを返す。

⸻

3. 認証・認可

認証とは

認証とは、あなたは誰ですか？を確認することである。

例。

* メールアドレス + パスワード
* Googleログイン
* GitHubログイン

認証によって、アプリケーションは「このリクエストは user_id = 10 のユーザーによるもの」と判断できる。

認可とは

認可とは、その人が、そのデータを見たり変更したりしてよいか確認することである。

例。

GET /resources/123

このリクエストに対して、認証だけでは「誰がアクセスしているか」しか分からない。

認可では以下を確認する。

resource_id = 123 は、ログイン中ユーザーのResourceか？

SQL例。

select *
from resources
where id = :resourceId
  and user_id = :currentUserId
  and deleted_at is null;

認証と認可の違い。

認証:
誰かを確認する
認可:
その人がその操作をしてよいか確認する

⸻

4. 削除方針

基本方針

SteerLogでは、ユーザーが作成・編集する主要データは論理削除を基本とする。

論理削除では、DBからレコードを物理的に削除せず、deleted_at に削除日時を入れる。

update resources
set deleted_at = now(),
    updated_at = now()
where id = :resourceId
  and user_id = :currentUserId
  and deleted_at is null;

通常の取得では deleted_at is null を条件にする。

select *
from resources
where user_id = :currentUserId
  and deleted_at is null;

⸻

論理削除対象

以下のテーブルは、MVPでは論理削除対象とする。

* resources
* resource_sections
* study_memos

理由。

* ユーザーが直接作成・編集・削除するデータである
* 誤削除や復元余地を考慮できる
* 学習証跡との関係を壊さない
* Resourceを物理削除すると、Progress、Memo、LearningSessionRecord、LevelHistoryの意味が壊れる可能性がある

⸻

単体削除しない対象

以下はMVPでは単体削除しない。

* progresses
* section_study_statuses

理由。

* ProgressはResourceに従属する現在状態である
* SectionStudyStatusはSectionに従属する状態である
* 親Resource / Sectionの表示状態に従えばよい
* 単体削除APIはMVPでは不要

⸻

learning_sessions

learning_sessions は deleted_at ではなく、status = DISCARDED によって破棄を表現する。

LearningSessionはAI Reflection / Recall の実行単位であり、正式な証跡ではない。

正式な証跡は learning_session_records に保存する。

そのため、MVPでは以下の状態で管理する。

* IN_PROGRESS
* COMPLETED
* RECORD_SAVED
* DISCARDED

⸻

削除しない証跡データ

以下はMVPではユーザー削除機能を持たせない。

* learning_session_records
* level_histories

理由。

* learning_session_records は正式な学習証跡である
* level_histories はLevel到達履歴である
* 削除可能にするとProgress.currentLevelや到達履歴との整合性が難しくなる
* SteerLogの信頼性を保つため、証跡・履歴は安易に削除しない

⸻

物理削除

MVPの通常操作では、物理削除は原則使わない。

ただし、将来的には以下で物理削除または匿名化を検討する。

* 退会
* 完全削除要求
* 一時データ整理
* 期限切れトークン削除
* プライバシー対応
* 保存不要な一時AIデータの削除

削除方針まとめ

resources / resource_sections / study_memos は論理削除。
progresses / section_study_statuses は単体削除なし。
learning_sessions は status = DISCARDED で破棄表現。
learning_session_records / level_histories はMVPでは削除なし。
物理削除は将来の退会・完全削除要求・一時データ整理で検討する。

⸻

5. 日時カラム方針

共通方針

全主要テーブルに以下を持たせる。

* created_at
* updated_at

論理削除対象テーブルには以下も持たせる。

* deleted_at

created_at

レコードが作成された日時。

例。

* Resourceを登録した日時
* Memoを書いた日時
* LearningSessionRecordを保存した日時
* LevelHistoryが作られた日時

updated_at

レコードが最後に更新された日時。

例。

* Resourceタイトルを変更した
* Progress.statusを変更した
* StudyMemoを編集した
* SectionStudyStatusを更新した

deleted_at

論理削除された日時。

対象。

* resources
* resource_sections
* study_memos

⸻

PostgreSQLの日時型

PostgreSQLでは timestamptz を使う。

例。

created_at timestamptz not null
updated_at timestamptz not null
deleted_at timestamptz

理由。

* タイムゾーン差異に対応しやすい
* サーバー環境が変わっても絶対時刻として扱いやすい
* 将来的な公開プロフィールや外部利用でも扱いやすい

⸻

日時の設定責務

created_at / updated_at は基本的にJava / Spring Boot側で設定する。

JPA Auditingなどを利用する想定。

DB側では not null 制約を持たせる。

⸻

システム日時と業務日時を分ける

created_at / updated_at はシステム共通の日時である。

一方、SteerLogには業務上の意味を持つ日時がある。

例。

* progresses.started_at
* progresses.completed_at
* progresses.last_studied_at
* progresses.initial_studied_at
* section_study_statuses.studied_at
* learning_sessions.completed_at

これらは created_at / updated_at とは意味が違うため、別カラムとして持つ。

learning_sessions の開始日時

MVPでは、learning_sessions.created_at をセッション開始日時として扱ってよい。

セッション完了日時は completed_at で表現する。

日時方針まとめ

全主要テーブルに created_at / updated_at を持たせる。
resources / resource_sections / study_memos には deleted_at を持たせる。
PostgreSQLでは timestamptz を使う。
created_at / updated_at はJava/Spring Boot側で設定する。
業務日時は専用カラムで表現する。

⸻

6. enum / 固定値の扱い

基本方針

MVPではDB enumは使わず、VARCHAR + Java enumで管理する。

理由。

* MVP段階ではResourceType、MemoType、LearningSessionTypeなどの値が変わる可能性がある
* DB enumで固定すると、値追加・変更時にDBマイグレーションが重くなる
* Java enumとDB enumの二重管理になりやすい
* MVPでは柔軟性を優先する

⸻

DBカラム

enum的な値は varchar で保持する。

例。

resource_type varchar(50) not null
status varchar(50) not null
memo_type varchar(50) not null
session_type varchar(50) not null
ai_assessment varchar(50) not null
source_type varchar(50) not null
reason_code varchar(100) not null

reason_code は値が長くなる可能性があるため、varchar(100) とする。

⸻

Java側

Java側ではenumとして定義する。

例。

public enum ResourceType {
    BOOK,
    ARTICLE,
    VIDEO,
    COURSE,
    DOCUMENTATION,
    PROBLEM,
    IMPLEMENTATION
}

JPAでは必ず EnumType.STRING を使う。

@Enumerated(EnumType.STRING)
@Column(name = "resource_type", nullable = false, length = 50)
private ResourceType resourceType;

EnumType.ORDINAL は使わない。

EnumType.ORDINALを使わない理由

EnumType.ORDINAL はenumの順番を数値として保存する。

例。

BOOK, ARTICLE, VIDEO

が以下のように保存される。

BOOK = 0
ARTICLE = 1
VIDEO = 2

あとからenumの順番を変えると、DBに保存された数値の意味が壊れる。

そのため、DBには文字列で保存する。

⸻

CHECK制約

MVP初期ではCHECK制約は必須にしない。

値が安定してきた段階で、必要に応じてCHECK制約またはDB enumを検討する。

段階。

MVP初期:
VARCHAR + Java enum
値が安定した後:
VARCHAR + Java enum + CHECK制約
さらに厳密にしたい場合:
DB enumを検討

⸻

SteerLogでenumになる主な項目

ResourceType

物理名。

resources.resource_type

論理名。

リソース種別

候補値。

* BOOK
* ARTICLE
* VIDEO
* COURSE
* DOCUMENTATION
* PROBLEM
* IMPLEMENTATION

⸻

ProgressStatus

物理名。

progresses.status

論理名。

進捗ステータス

候補値。

* NOT_STARTED
* IN_PROGRESS
* PAUSED
* COMPLETED
* ARCHIVED

⸻

MemoType

物理名。

study_memos.memo_type

論理名。

メモ種別

候補値。

* GENERAL
* LEARNED
* QUESTION
* WEAKNESS
* TODO
* IDEA
* SUMMARY

⸻

LearningSessionType

物理名。

learning_sessions.session_type
learning_session_records.session_type

論理名。

学習セッション種別

候補値。

* IMMEDIATE_REFLECTION
* DELAYED_RECALL

⸻

LearningSessionStatus

物理名。

learning_sessions.status

論理名。

学習セッション状態

候補値。

* IN_PROGRESS
* COMPLETED
* RECORD_SAVED
* DISCARDED

⸻

AiAssessment

物理名。

learning_session_records.ai_assessment

論理名。

AI評価

候補値。

* PASSED
* NEEDS_REVIEW
* OFF_TOPIC

⸻

LevelHistorySourceType

物理名。

level_histories.source_type

論理名。

レベル到達元種別

候補値。

* INITIAL_STUDY_COMPLETION
* SECTION_STUDY_STATUS
* LEARNING_SESSION_RECORD

⸻

LevelHistoryReasonCode

物理名。

level_histories.reason_code

論理名。

レベル到達理由コード

候補値。

* INITIAL_STUDY_COMPLETED
* ALL_SECTIONS_STUDIED
* IMMEDIATE_REFLECTION_RECORD_SAVED
* DELAYED_RECALL_RECORD_SAVED

⸻

enum方針まとめ

MVPではDB enumは使わない。
DBにはVARCHARで保存し、Java enumで管理する。
JPAではEnumType.STRINGを使う。
EnumType.ORDINALは使わない。
CHECK制約はMVP初期では必須にせず、値が安定してから検討する。

⸻

7. user_id / 認可 / 外部キー方針

user_idを主要テーブルに持たせる

SteerLogでは、ユーザーに紐づく主要テーブルに user_id を持たせる。

対象。

* resources
* resource_sections
* progresses
* section_study_statuses
* study_memos
* learning_sessions
* learning_session_records
* level_histories

resource_idから辿れる場合でもuser_idを持つ理由

たとえば、study_memos は resource_id を持っているため、resources にJOINすればuser_idを取得できる。

しかし、SteerLogでは各主要テーブルに user_id を持たせる。

理由。

* 認可チェックしやすい
* ユーザー単位の検索・集計がしやすい
* Galaxy APIやプロフィール生成時にユーザー単位で材料を集めやすい
* データ分離の意識が明確になる
* SQLや実装が簡潔になる

例。

select *
from study_memos
where id = :memoId
  and resource_id = :resourceId
  and user_id = :currentUserId
  and deleted_at is null;

user_id がない場合は resources とのJOINが必要になる。

select sm.*
from study_memos sm
join resources r on sm.resource_id = r.id
where sm.id = :memoId
  and sm.resource_id = :resourceId
  and r.user_id = :currentUserId
  and sm.deleted_at is null;

ここでいう「重い」は、主に実装やSQLが少し複雑になるという意味であり、SteerLog規模では性能差を大きく気にする必要はない。

⸻

user_idを持たせるデメリット

デメリットもある。

* データが少し重複する
* resource_idとuser_idの整合性を守る必要がある
* insert時にuser_idを正しくセットする必要がある

例として、以下は不整合である。

resources.id = 123, user_id = 10
study_memos.resource_id = 123
study_memos.user_id = 20

このような不整合を避けるため、アプリケーション側で以下を徹底する。

* Memo作成時に対象Resourceがログインユーザーのものか確認する
* study_memos.user_id には currentUserId を設定する
* APIでは必ず user_id による所有者チェックを行う

⸻

8. 外部キー制約

外部キー制約とは

外部キー制約とは、テーブル同士の親子関係をDBが守ってくれる仕組みである。

例。

study_memos.resource_id
→ resources.id

この関係がある場合、存在しないResourceに紐づくMemoを作れないようにする。

外部キー制約がないと、以下のような壊れたデータが入る可能性がある。

study_memos.id = 1
study_memos.resource_id = 999999

しかし、resources.id = 999999 が存在しなければ、そのMemoは何に対するメモなのか分からない。

これを孤児データと呼ぶ。

⸻

SteerLogで外部キーを張る理由

SteerLogは学習証跡アプリであり、データ同士の関係が壊れると価値が下がる。

そのため、外部キー制約は基本的に設定する。

理由。

* 存在しないResourceにMemoが作られるのを防ぐ
* 存在しないLearningSessionにRecordが作られるのを防ぐ
* LevelHistoryの根拠関係を守る
* 学習証跡としての信頼性を保つ
* テーブル同士の関係をDB上でも明確にする

⸻

外部キー例

resources.user_id
→ users.id
resource_sections.resource_id
→ resources.id
progresses.resource_id
→ resources.id
study_memos.resource_id
→ resources.id
learning_sessions.resource_id
→ resources.id
learning_session_records.learning_session_id
→ learning_sessions.id
level_histories.resource_id
→ resources.id

⸻

CASCADE DELETEは原則使わない

CASCADE DELETEは、親データを物理削除したときに子データも自動で削除する仕組みである。

SteerLogでは、通常削除は論理削除を基本とする。

また、LearningSessionRecordやLevelHistoryは正式な証跡・履歴である。

そのため、親データ削除に合わせて証跡データを自動削除することは避ける。

方針。

CASCADE DELETE は原則使わない。
削除はアプリケーション側で明示的に制御する。
通常の削除は deleted_at または status で表現する。

⸻

9. 一意制約

基本方針

1対1に近い関係や、重複してはいけないデータには一意制約を設定する。

progresses

ResourceごとのProgressは、ユーザーごとに1つだけ。

unique (user_id, resource_id)

これにより、同じユーザー・同じResourceにProgressが複数作られることを防ぐ。

⸻

section_study_statuses

SectionごとのStudyStatusは、ユーザー・Resource・Sectionごとに1つだけ。

unique (user_id, resource_id, section_id)

⸻

learning_session_records

LearningSessionに対するRecordは1つだけ。

unique (learning_session_id)

⸻

level_histories

LevelHistoryは、そのResourceがそのLevelに初回到達した履歴である。

同じResource・同じLevelの履歴は1つだけ。

unique (user_id, resource_id, level)

⸻

10. index方針

基本方針

SteerLogでは、MVP段階で大量データを前提にした積極的なindex設計は行わない。

理由。

* SteerLogは全ユーザー横断検索をコア機能としない
* 詳細学習データは基本的に本人だけが見る
* Galaxy APIも内部の生ログを大量検索するのではなく、将来的には集計済みデータを返す想定
* 個人利用・チーム利用中心であれば、10万件超えを前提にした最適化は不要
* 過剰なindexはINSERT / UPDATE / DELETE時の更新コストを増やす

⸻

indexの考え方

indexは検索を速くする索引である。

ただし、indexは増やせばよいものではない。

メリット。

* SELECTが速くなる可能性がある
* WHERE / JOIN / ORDER BY でよく使う列に有効

デメリット。

* INSERT / UPDATE / DELETE時にindex更新コストが増える
* 管理対象が増える
* 過剰なindexは無駄になる

⸻

SteerLogでの判断

SteerLogは、ユーザーが使うときに最も更新が多そうなのはStudyMemoである。

ただし、Memoでさえ頻繁に大量更新するわけではない。

一方で、画面表示では以下のようなSELECTが繰り返し使われる。

* 自分のResource一覧を取得する
* Resourceに紐づくMemo一覧を取得する
* ResourceのProgressを取得する
* LevelHistoryを取得する
* LearningSessionRecordを取得する

そのため、更新よりも検索条件として頻出する箇所を最低限優先する。

⸻

MVPでのindex方針

MVPでは、以下を基本とする。

* 主キーに伴うindex
* 一意制約に伴うindex
* user_id / resource_id など、認可・親子取得で頻繁に使う最低限のindex

created_atや複合indexは、実際に一覧取得や集計で性能課題が見えてから追加する。

index方針まとめ

SteerLogは全ユーザー横断検索をコア機能としない。
そのため、10万件超えを前提にした積極的なindex設計は行わない。
MVPでは、主キー・一意制約に伴うindexを基本とし、
追加indexは user_id / resource_id など頻出条件に限定する。
created_at や複合indexは後回しにし、実際に必要になった段階で追加する。

⸻

11. Galaxy API / 公開プロフィールの考え方

全ユーザー横断検索はコア機能にしない

SteerLogの思想は、全ユーザーから人材検索するサービスではない。

コアは以下。

自分のスキルは、どの本・記事・実装・対話・証跡から作られたのかを可視化する

そのため、初期から以下のような機能を前提にしない。

* 全ユーザーからJavaが得意な人を検索
* 全ユーザーの技術志向ランキング
* 採用サービス的な横断検索
* 大規模レコメンド

これらを作ると、SteerLogの重心が「学習証跡・技術プロフィール可視化」から「エンジニア検索 / 採用 / マッチングサービス」に寄ってしまう。

⸻

公開プロフィールは共有型にする

最初に目指す公開機能は、全ユーザー検索ではなく、共有プロフィールである。

例。

* 面接官に見せる
* チームメンバーに見せる
* 一緒に開発する人に見せる
* メンターに見せる

公開するのは、詳細学習ログそのものではなく、集計された技術志向・スキル傾向である。

Galaxy APIの位置づけ

Galaxy APIは、内部学習データを直接公開するAPIではない。

内部データをもとに、プロフィール・スキル傾向・証跡数・タグ傾向を集計して返すAPIである。

公開APIで返すもの。

* skillCluster
* evidenceCount
* highestLevel
* conceptTags
* weakPoint傾向
* 技術志向
* 学習証跡の概要

公開APIで返さないもの。

* Resourceの内部ID
* Memoの内部ID
* LearningSessionRecordの内部ID
* StudyMemo本文
* 詳細な進捗
* 今どの章を勉強しているか

⸻

将来的な段階

MVP:
個人の学習証跡管理
次段階:
自分用Galaxyプロフィール生成
その次:
共有リンク / 公開プロフィール
さらに次:
チーム内共有・チーム内検索
かなり先:
全ユーザー横断検索・推薦

現時点では、全ユーザー横断検索を前提にしない。

ただし、将来必要になっても壊れないように、以下は守る。

* user_idを主要テーブルに持つ
* 公開プロフィールと内部詳細データを分ける
* 公開APIでは内部IDや生ログを返さない
* 必要になれば集計テーブルを追加できる構造にする

⸻

12. DB設計前提まとめ

確定方針

1. DB
   - PostgreSQL
2. ID
   - 内部学習データは BIGINT
   - 外部公開プロフィール / Galaxy API / 共有リンクは UUID public_id または handle
3. 認証・認可
   - 認証は「誰か」を確認する
   - 認可は「その人がそのデータを操作してよいか」を確認する
   - 内部APIでは必ず user_id による所有者チェックを行う
4. 削除
   - resources / resource_sections / study_memos は論理削除
   - progresses / section_study_statuses は単体削除なし
   - learning_sessions は status = DISCARDED
   - learning_session_records / level_histories は削除なし
   - 物理削除は将来の退会・完全削除要求・一時データ整理で検討
5. 日時
   - 全主要テーブルに created_at / updated_at
   - 論理削除対象に deleted_at
   - PostgreSQLでは timestamptz
   - created_at / updated_at はJava/Spring Boot側で設定
   - 業務日時は専用カラムで表現する
6. enum / 固定値
   - DB enumは使わない
   - VARCHAR + Java enumで管理
   - JPAでは EnumType.STRING
   - EnumType.ORDINAL は使わない
   - CHECK制約はMVP初期では必須にしない
7. user_id
   - ユーザーに紐づく主要テーブルには user_id を持たせる
   - 認可、検索、集計、Galaxy生成をしやすくするため
8. 外部キー
   - 基本的に設定する
   - 存在しない親データに紐づく孤児データを防ぐ
   - CASCADE DELETE は原則使わない
9. 一意制約
   - progresses: unique(user_id, resource_id)
   - section_study_statuses: unique(user_id, resource_id, section_id)
   - learning_session_records: unique(learning_session_id)
   - level_histories: unique(user_id, resource_id, level)
10. index
   - MVPでは最小限
   - 主キー・一意制約に伴うindexを基本とする
   - 追加indexは user_id / resource_id など頻出条件に限定
   - created_at / 複合indexは後回し

⸻

13. 次にやること

次回からは、実テーブル設計に入る。

設計順。

1. resources
2. resource_sections
3. progresses
4. section_study_statuses
5. study_memos
6. learning_sessions
7. learning_session_records
8. level_histories

各テーブルで決めること。

* 論理名
* 物理名
* カラム名
* 型
* NULL許可
* 制約
* 外部キー
* 一意制約
* 論理削除有無
* created_at / updated_at / deleted_at
* APIとの対応関係

