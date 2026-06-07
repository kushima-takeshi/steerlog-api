# SteerLog API

SteerLog API は、エンジニアの学習 Resource、進捗、Section、メモ、Level 履歴を管理する API です。  
単なる学習時間管理ではなく、**学習証跡と理解段階を記録する** ことが目的です。

現在は MVP 前半として **Lv.1 / StudyMemo まで実装済み** です。

---

## 技術スタック

- Java 21
- Spring Boot 3.4.5
- Maven
- PostgreSQL
- Flyway
- Spring Data JPA
- JUnit 5
- Mockito
- MockMvc
- Docker Compose
- Lombok なし

認証は未実装。Controller では `TEMP_USER_ID = 1L` 固定。

---

## 実装済み機能

### Resource

- `POST /resources`
- `GET /resources`
- `GET /resources/{resourceId}`
- `PATCH /resources/{resourceId}`
- `DELETE /resources/{resourceId}`

### Progress / Lv.1 / LevelHistory

- `GET /resources/{resourceId}/progress`
- `POST /resources/{resourceId}/progress/complete-initial-study`
- `GET /resources/{resourceId}/level-histories`
- Lv.1 明示到達
- 全 Section 学習済みによる Lv.1 自動到達
- LevelHistory 重複防止

### ResourceSection / SectionStudyStatus

- `POST /resources/{resourceId}/sections`
- `GET /resources/{resourceId}/sections`
- `PATCH /resources/{resourceId}/sections/{sectionId}/study-status`
- Section 作成時に SectionStudyStatus 自動作成

### StudyMemo

- `POST /resources/{resourceId}/memos`
- `GET /resources/{resourceId}/memos`
- `PATCH /resources/{resourceId}/memos/{memoId}`
- `DELETE /resources/{resourceId}/memos/{memoId}`
- StudyMemo 作成時に `Progress.lastStudiedAt` 更新

---

## 起動方法

```bash
docker compose up -d
mvn spring-boot:run
```

---

## テスト実行方法

```bash
mvn test
```

---

## 手動動作確認

[docs/09-manual-api-check.md](docs/09-manual-api-check.md) を参照してください。

Resource → Section → Lv.1 → LevelHistory → StudyMemo CRUD の流れを curl で順番に確認できます。  
2026-06-07 にローカル環境で手動確認済みです。

---

## 主要ドキュメント

- [docs/README.md](docs/README.md) - docs全体の案内
- [docs/00-product-principles.md](docs/00-product-principles.md) - プロダクト思想
- [docs/01-mvp-scope.md](docs/01-mvp-scope.md) - MVP範囲
- [docs/02-db-design.md](docs/02-db-design.md) - DB設計
- [docs/03-api-design.md](docs/03-api-design.md) - API設計
- [docs/04-level-rules.md](docs/04-level-rules.md) - Level到達ルール
- [docs/05-learning-session-flow.md](docs/05-learning-session-flow.md) - LearningSession設計
- [docs/06-implementation-rules.md](docs/06-implementation-rules.md) - 実装ルール
- [docs/07-implementation-order.md](docs/07-implementation-order.md) - 実装順序
- [docs/08-ai-development-workflow.md](docs/08-ai-development-workflow.md) - AI開発フロー
- [docs/09-manual-api-check.md](docs/09-manual-api-check.md) - 手動API確認

---

## 未実装 / Next

- 認証
- LearningSession
- LearningSessionRecord
- Lv.2 / Lv.3
- Resource 詳細への統合表示
- StudyMemo tags / important flag
