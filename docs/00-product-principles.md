# 00-product-principles.md

# SteerLog Product Principles

## 目的

このドキュメントは、SteerLogのプロダクト思想・設計原則を定義する。  
AIコード生成、設計判断、実装レビューでは、この内容を最上位方針として扱う。

このファイルは、SteerLogを単なるCRUDアプリ、読書ログ、学習時間管理アプリ、AIクイズアプリ、NotebookLM代替として実装しないための基準である。

---

# 1. SteerLogとは何か

SteerLogは、エンジニアの学習対象・学習状態・理解の変化・弱点・次アクション・証跡を整理し、説明可能にするための **学習証跡アプリ** である。

SteerLogが扱う中心は、単なる「勉強した記録」ではない。

```text
何を学習対象にしたか
どのResourceに触れたか
どのSectionまで進んだか
何を理解したつもりか
どこが曖昧か
時間を置いて何を思い出せたか
どんな次アクションが出たか
どの証跡によってLevelに到達したか
```

を扱う。

---

# 2. SteerLogが目指すもの

SteerLogは、以下を目指す。

```text
学習の積み重ねを証跡として残す
学習状態をResource単位で説明可能にする
弱点や次アクションを見える化する
学習した技術・本・記事・考え方の由来を残す
将来的に、技術的関心や学習の源泉をプロフィールとして表現できるようにする
```

SteerLogは、ユーザーが後から以下を説明できる状態を作る。

```text
このResourceをなぜ登録したのか
どこまで触れたのか
何を理解したのか
何がまだ弱いのか
どのタイミングで振り返ったのか
どの証跡でLv.1〜Lv.3に到達したのか
次に何をすべきか
```

---

# 3. SteerLogではないもの

SteerLogは、以下ではない。

```text
単なる読書ログアプリ
単なる学習時間管理アプリ
Pomodoro管理アプリ
NotebookLMの代替
本文QAアプリ
AIクイズアプリ
認定試験アプリ
自動採点アプリ
チャットログ保存アプリ
外部LLMログ保管アプリ
```

特に、以下の方向には寄せない。

```text
本や記事の本文を大量に取り込む
著作物本文をAIに読ませて問題生成する
正答率だけで理解度を判定する
AIとの会話ログ全文を保存する
学習時間やPomodoroを中心指標にする
```

---

# 4. 学習思想

SteerLogのLevel設計は、以下の学習思想に基づく。

## 4.1 初回学習は「地図を作る」段階

初回学習では、深い理解まで到達できなくてもよい。

初めて学ぶResourceでは、まだ何が重要で、何が分からないかも分からない。  
そのため、1周目はまず全体像を掴み、頭の中に地図を作る段階とする。

この思想はLv.1に反映される。

```text
Lv.1 = 初回学習済み / 一通り触れた状態
```

Lv.1は完全理解を意味しない。

---

## 4.2 理解は振り返りで深まる

学習直後に、自分の言葉で説明しようとすると、理解できている部分と曖昧な部分が見える。

この思想はLv.2に反映される。

```text
Lv.2 = Immediate Reflectionの正式証跡がある状態
```

Lv.2は「完全に理解した」ではなく、即時振り返りの証跡が存在する状態を意味する。

---

## 4.3 定着には時間を置いた想起が必要

学習直後に分かったつもりでも、時間が経つと思い出せないことがある。  
そのため、時間を置いて思い出す機会が必要である。

この思想はLv.3に反映される。

```text
Lv.3 = Delayed Recallの正式証跡がある状態
```

Lv.3も完全理解を保証しない。  
時間を置いた想起の証跡が存在する状態を意味する。

---

## 4.4 エンジニアには出力が必要

エンジニアの学習は、読んだ・分かっただけでは終わらない。  
実装、設計、成果物、説明に接続されることで価値が高まる。

この思想は将来のLv.4に対応する。

```text
Lv.4 = Artifact / 成果物による証跡
```

ただし、Lv.4はMVP外とする。

---

## 4.5 最終的には説明・Defenseできることが重要

学んだ内容を他者に説明し、質問に答え、自分の設計判断を防衛できることは、エンジニアとして重要である。

この思想は将来のLv.5に対応する。

```text
Lv.5 = Defense / 説明・防衛できる状態
```

ただし、Lv.5はMVP外とする。

---

# 5. Levelの基本思想

SteerLogのLevelは、理解保証ではなく **証跡段階** である。

```text
Level = そのResourceに対して、どの種類の学習証跡が存在するか
```

であり、

```text
Level = その技術を完全に理解した証明
```

ではない。

そのため、LearningSessionRecordの `aiAssessment` が `NEEDS_REVIEW` であっても、証跡として有効であればLv.2 / Lv.3到達候補になる。

ただし、UIやプロフィールでは、`PASSED` と `NEEDS_REVIEW` を区別して表示する。

---

# 6. AIの役割

SteerLogにおけるAIの役割は、教師・採点者・認定機関ではない。

AIの主な役割は、以下である。

```text
学習後のReflectionを促す
時間を置いたRecallを促す
ユーザー回答から要約・概念タグ・弱点・次アクションを整理する
学習証跡の候補を作る
```

AIは、学習内容そのものを完全に教える存在ではない。  
AIは、ユーザーが学習した内容を振り返り、証跡化するための補助者である。

---

# 7. AI生成証跡の扱い

AIが生成したLearningSessionRecordの内容は、正式証跡として扱う。

そのため、AI生成部分はユーザーが直接編集できない。

編集不可：

```text
summary
conceptTags
weakPointTags
weakPointSummary
nextAction
aiAssessment
generationBasis
```

ユーザーが追加できるのは、補足コメントのみ。

```text
userComment
```

ただし、AI生成結果を自動保存してはいけない。  
complete後にresultDraftを画面に表示し、ユーザーが確認して納得した場合のみ正式保存する。

保存しない場合、Level更新もLevelHistory作成も行わない。

---

# 8. rawログを保存しない原則

SteerLogはチャットログ保存アプリではない。

MVPでは、以下を正式証跡として保存しない。

```text
ユーザーの生回答全文
AIとの全会話ログ
AI質問履歴全文
回答履歴全文
外部LLMログ全文
```

保存するのは、AIが整理した正式証跡である。

```text
LearningSessionRecord
```

および、その初到達履歴である。

```text
LevelHistory
```

---

# 9. StudyMemoの位置づけ

StudyMemoは、ユーザーが学習中に残す短い生メモである。

```text
気づき
疑問
弱点
TODO
実装アイデア
自分なりの要約
```

を残すために使う。

StudyMemoは正式なLevel到達条件ではない。  
StudyMemoを書いたからといって、Lv.1〜Lv.3に到達するわけではない。

StudyMemoは、学習中の思考の断片を残す補助的な証跡である。

また、StudyMemoを長文ノートや本文丸写しの場所にしない。

---

# 10. Progressの思想

Progressは、Resource単位の学習状態を表す。

Progressは以下を持つ。

```text
現在の学習状態
現在の主な学習Section
現在到達Level
初回学習完了日時
最終学習日時
アーカイブ状態
```

`currentLevel` はユーザーが自由に上げるものではない。  
Lv.1〜Lv.3の到達条件を満たしたとき、システムが更新する。

---

# 11. ARCHIVEDの思想

`ARCHIVED` は削除ではない。

`ARCHIVED` は、そのResourceを現在の学習対象から外した状態である。

例：

```text
途中まで読んだが、今は優先度を下げた
仕事の内容が変わったため、今は学習対象から外した
別Resourceで学び直すことにした
古くなったので今は追わない
```

`archivedAt` と `archiveReason` は、学習方針の変化を残す判断履歴として扱う。

---

# 12. Resourceと証跡の関係

SteerLogでは、Resourceは学習対象そのものを表す。

Resourceに対して、以下の証跡や状態が紐づく。

```text
Progress
ResourceSection
SectionStudyStatus
StudyMemo
LearningSession
LearningSessionRecord
LevelHistory
```

Resourceは、単なるタイトル情報ではなく、ユーザーの学習証跡の中心である。

---

# 13. 将来の方向性

MVPでは実装しないが、SteerLogは将来的に以下へ拡張できるようにする。

```text
Lv.4 Artifact
Lv.5 Defense
Galaxy API
技術的関心・学習源泉の可視化
タグ証跡の標準化
初回学習由来と再学習由来の区別
Artifact由来の証跡
プロフィール公開
```

ただし、MVPではこれらを実装しない。  
MVPでは、Lv.1〜Lv.3の学習証跡を堅実に作ることを優先する。

---

# 14. 実装AIへの最重要指示

AIコード生成時は、以下を必ず守る。

```text
SteerLogをAIクイズアプリとして実装しない
SteerLogをNotebookLM代替として実装しない
StudyMemoをLevel到達条件にしない
LearningSessionRecordをユーザー編集可能にしない
AI生成結果をユーザー確認なしで保存しない
raw回答ログを正式保存しない
currentLevelを外部から直接更新させない
LevelHistoryを一般CRUD対象にしない
学習時間/PomodoroをMVPに入れない
Galaxy API / Lv.4 / Lv.5をMVPに入れない
```

---

# 15. まとめ

SteerLogは、学習の量を測るアプリではなく、学習の証跡を整理するアプリである。

SteerLogは、ユーザーが学習したResourceについて、

```text
一通り触れた
直後に振り返った
時間を置いて思い出した
弱点が分かった
次にやることが分かった
```

という状態を、説明可能な形で残す。

MVPでは、この思想を崩さず、Resource・Progress・StudyMemo・LearningSession・LearningSessionRecord・LevelHistoryを中心に、Lv.1〜Lv.3までを実装する。
