# 가계부(bookkeeping) 개편 설계

2026-11-03 합의. 1차 목표는 "편한 가계부" 수준의 웹 가계부.
옛 `bluesky-api-bookkeeping` 도메인과 게이트 bookkeeping 코드는 버리고 새로 만든다.

## 1. 범위

| 구분 | 내용 |
|---|---|
| 1차 | 수입·지출·이체 입력/수정/삭제, 대·소분류, 자산별 잔액, 월 달력·일별 목록, 월 합계, 분류 통계·12개월 추이, 월 시작일, 검색, 여러 가계부, 멤버 공유(초대 링크) |
| 2차 | 예산, 카드 할부·결제 예정액, 반복 내역, 즐겨찾기, CSV 가져오기·내보내기, 기준통화 환산(수동 월 환율 `BookFxRate`), OWNER 이양 |
| 보류 | 앱(앱 안에서 독립 실행·관리, 나중에 동기화해 웹에서 조회). 1차 API 는 앱을 고려하지 않는다 |
| 제외 | 문자·푸시 자동 입력, 사진 첨부, 잠금 |

## 2. 옛 구조를 버리는 이유

- 수입·지출을 숨은 `CONTRA_ASSET` 계좌와의 이체로 표현했다. 복식부기도 단식도 아니고, 지출 1건 검증에 조회 6번이 든다.
- `AssetType` 테이블과 `AssetTypeCode` enum 이 겹친다.
- 분류가 1단계뿐이다.
- 게이트 화면이 없다. 2025-12-31 에 옛 `bluesky-web-default` 화면을 지운 뒤 jte 로 옮기지 않았다.
- 게이트 클라이언트 5개 중 4개가 API 에 없는 경로를 부른다.
- 소유권 검사가 없다.
- 기존 데이터는 보존하지 않는다.

## 3. 결정 사항

### 공유·권한
- 소유와 공유는 `BookMember(book_id, user_id, role)` 로만 표현한다. `Book` 에는 user_id 가 없다.
- 역할은 OWNER, MEMBER 두 가지다.
  - OWNER 는 가계부당 1명이다. 가계부 삭제·설정·멤버 관리를 맡는다.
  - MEMBER 는 내역·자산·분류를 편집한다.
- 가계부 전체를 공유한다. 자산별 비공개는 없다.
- 멤버십·역할 검사는 API 가 요청마다 한다. 게이트는 세션 userId 를 `@RequestParam UUID userId` 로 넘기기만 한다(stock 과 같은 방식).
- 초대
  - OWNER 가 1회용 링크(`BookInvite`)를 발급한다. 7일 뒤 만료된다.
  - 링크를 받은 사람이 로그인한 상태에서 열면 MEMBER 로 추가된다.
  - api-user 에 이메일·username 조회 경로를 새로 열지 않는다.
- 멤버 나가기·추방
  - MEMBER 는 스스로 나갈 수 있다. OWNER 는 MEMBER 를 추방할 수 있다.
  - OWNER 는 나갈 수 없다. 가계부 삭제만 할 수 있다.
  - 나간 멤버가 쓴 내역은 남는다(`createdBy` 유지).
- 멤버 이름 표시는 api-user `findByIdIn` 으로 user id 를 username 으로 바꾼다. 게시판과 같은 방식이다.
- 동시 편집은 마지막 저장이 이긴다. 잠금은 없다.

### 통화·금액
- 통화는 자산마다 둔다(`Asset.currency`, ISO 4217). 가계부에는 기준통화(`Book.baseCurrency`)만 둔다.
- 내역 금액은 해당 자산의 통화를 따른다.
- 이체는 `toAmount` 를 둔다.
  - 통화가 같으면 `toAmount = amount` 로 강제한다.
  - 통화가 다르면 두 값을 따로 받는다.
- 금액 칼럼은 `NUMERIC(19,4)` 이다.
  - 입력값의 소수 자릿수는 `Currency.getDefaultFractionDigits()` 로 검증한다.
  - 금액은 0 보다 커야 한다. 부호는 `type` 이 정한다.
- 기준통화는 언제든 바꿀 수 있다. 1차에서는 표시용이다.
- 자산 통화는 내역이 하나라도 있으면 바꿀 수 없다.
- 1차 합계·통계는 통화별로 따로 낸다. 환산은 하지 않는다.

### 내역·잔액
- 단식 기록이다. `type` 은 INCOME, EXPENSE, TRANSFER 중 하나다.

  | type | asset_id | toAsset_id | category_id |
  |---|---|---|---|
  | INCOME | 받는 자산 | null | 수입 분류 |
  | EXPENSE | 나가는 자산 | null | 지출 분류 |
  | TRANSFER | 나가는 자산 | 받는 자산(≠ asset_id) | null |

- 잔액은 저장하지 않는다. 다음 식으로 계산한다.
  - `잔액 = openingBalance + Σ INCOME − Σ EXPENSE − Σ TRANSFER 나감(amount) + Σ TRANSFER 들어옴(toAmount)`
- 신용카드
  - 카드로 쓴 지출이 쌓여 잔액이 음수(부채)가 된다.
  - 결제는 은행에서 카드로 가는 이체로 기록한다.
  - 1차에는 `paymentDay` 만 둔다.
- 날짜는 `entryDate DATE` 와 `entryTime TIME`(선택)이다. 시간대는 없다.
- 월 범위
  - `monthStartDay` 는 1~28 이다.
  - 시작일이 25 이면 "11월" 은 10/25 ~ 11/24 다. 월 이름은 기간이 끝나는 달을 따른다.
- 내역에는 `createdBy`, `createdDate` 를 남긴다.

### 분류·자산·기본 데이터
- 분류는 `parent_id` 로 대·소 2단계로 고정한다. 3단계는 막는다.
- 내역은 소분류를 가리킨다. 소분류가 없는 대분류는 대분류를 직접 가리켜도 된다.
- 분류의 kind(INCOME/EXPENSE)는 내역 type 과 맞아야 한다.
  - 소분류의 kind 는 부모와 같아야 한다.
- 가계부를 만들 때 기본 데이터를 넣는다. 이름은 메시지 파일에서 꺼내 문자열로 저장하고, 이후 사용자가 고칠 수 있다.
  - 수입 분류: 급여·상여·이자·기타
  - 지출 분류: 식비·교통·생활·교육·문화·경조사·관리비·기타
  - 소분류는 없다.
  - 자산: "현금" 1개, 통화는 가계부 기준통화.
- 삭제
  - 내역이 있는 자산·분류는 `hidden=true` 로 숨기기만 한다.
  - 내역이 없으면 실제로 지운다.
  - 하위 분류가 있는 대분류도 숨김만 된다.
- 순서는 `displayOrder` 로 정하고 위/아래 단추로 바꾼다.

## 4. 스키마

명명은 기존 repo 관례를 따른다. 따옴표로 감싼 camelCase 이고, FK 칼럼은 `xxx_id` 다.
파일은 `src/main/java/net/luversof/api/bookkeeping/` 아래에 둔다.
- `schema-create-postgresql-bookkeeping.sql`: 최종 스키마
- `schema-alter-postgresql-bookkeeping.sql`: 옛 테이블 DROP 과 새 CREATE 묶음

실행은 사용자가 한다.

| 테이블 | 칼럼 |
|---|---|
| `Book` | id UUID PK, name, baseCurrency CHAR(3), monthStartDay SMALLINT(1~28), createdDate TIMESTAMPTZ |
| `BookMember` | book_id, user_id, role VARCHAR(10), joinedDate · PK(book_id, user_id) · idx(user_id) |
| `BookInvite` | token VARCHAR PK, book_id, createdBy, expiresAt TIMESTAMPTZ, usedBy UUID null, usedDate null |
| `Asset` | id, book_id, kind VARCHAR(20), name, currency CHAR(3), openingBalance NUMERIC(19,4), paymentDay SMALLINT null, displayOrder INT, hidden BOOL · idx(book_id) |
| `Category` | id, book_id, parent_id null, kind VARCHAR(10), name, displayOrder INT, hidden BOOL · idx(book_id) |
| `Entry` | id, book_id, type VARCHAR(10), entryDate DATE, entryTime TIME null, amount NUMERIC(19,4), toAmount NUMERIC(19,4) null, asset_id, toAsset_id null, category_id null, content VARCHAR(200), memo VARCHAR(1000) null, createdBy UUID, createdDate TIMESTAMPTZ · idx(book_id, entryDate), idx(asset_id), idx(toAsset_id), idx(category_id) |

- `Asset.kind` 는 CASH, BANK, CREDIT_CARD, INVESTMENT, LOAN, ETC 중 하나다. 체크카드 kind 는 두지 않는다(9절).
- 금액은 `CHECK (amount > 0)` 로 막는다.
- type 별 칼럼 조합(이체 칼럼 · 분류 유무 · 자기 자신으로 이체 금지)은 DB `ck_entry_typeColumns` 로도 막는다. OWNER 1명은 부분 유니크 인덱스(`uk_bookMember_owner`)로 막는다.
- 같은 가계부 여부, 분류 kind 일치, 같은 통화 이체의 `toAmount = amount` 는 서비스에서 검증한다.
- FK 제약은 두지 않는다(repo 관례).

## 5. API

모든 요청은 `userId` query param 을 받는다. 멤버가 아니면 403, 역할이 모자라도 403 이다.
응답은 도메인 record 를 그대로 쓴다. 따로 계약을 두지 않는다.

| 경로 | 메서드 | 권한 | 내용 |
|---|---|---|---|
| `/api/books` | GET · POST | 로그인 | 내 가계부 목록(역할 포함) · 만들기(기본 데이터 포함) |
| `/api/books/{bookId}` | GET · PUT · DELETE | MEMBER · OWNER · OWNER | 조회 · 이름·기준통화·월 시작일 수정 · 전체 삭제 |
| `/api/books/{bookId}/members` | GET | MEMBER | 멤버 목록 |
| `/api/books/{bookId}/members/{memberUserId}` | DELETE | 본인 또는 OWNER | 나가기 · 추방 |
| `/api/books/{bookId}/invites` | POST | OWNER | 초대 링크 발급 |
| `/api/invites/{token}` | GET | 로그인 | 미리보기(가계부 이름, 만료 여부) |
| `/api/invites/{token}/accept` | POST | 로그인 | 수락 → MEMBER |
| `/api/books/{bookId}/assets` | GET · POST | MEMBER | 목록(잔액 포함, hidden 제외 선택) · 추가 |
| `/api/books/{bookId}/assets/{id}` | PUT · DELETE | MEMBER | 수정 · 삭제(내역 있으면 숨김) |
| `/api/books/{bookId}/assets/{id}/move` | POST `?direction=up\|down` | MEMBER | 순서 |
| `/api/books/{bookId}/categories` (`/{id}`, `/{id}/move`) | 자산과 같음 | MEMBER | 분류 |
| `/api/books/{bookId}/entries` | GET · POST | MEMBER | `?from&to&q&categoryId&assetId` 검색 · 추가 |
| `/api/books/{bookId}/entries/{id}` | PUT · DELETE | MEMBER | 수정 · 삭제 |
| `/api/books/{bookId}/stats/month` | GET `?month=2026-11` | MEMBER | 기간, 일별 수입·지출 합, 분류별 합(통화별) |
| `/api/books/{bookId}/stats/trend` | GET `?months=12` | MEMBER | 월별 수입·지출 합(통화별) |

- `q` 는 내용·메모를 부분 일치(`ILIKE`)로 찾는다.
- 기간 계산(`month` 를 `from~to` 로 바꾸는 일)은 API 가 `monthStartDay` 로 한 곳에서 한다.

## 6. 게이트 화면 (jte + htmx)

| 경로 | 화면 |
|---|---|
| `/bookkeeping` | 내 가계부 목록·만들기. 1권이면 바로 들어가고, 0권이면 만들기 화면 |
| `/bookkeeping/invite/{token}` | 초대 미리보기·수락 |
| `/bookkeeping/{bookId}` | 월 화면: 달력(날짜별 수입·지출 합), 일별 내역, 통화별 월 합계, 검색(기간·내용·메모·분류·자산), 이전/다음 달 |
| `/bookkeeping/{bookId}/assets` | 그룹(kind)별 자산·잔액, 자산을 누르면 그 자산의 내역 |
| `/bookkeeping/{bookId}/stats` | 대분류 원 그래프(누르면 소분류), 12개월 수입·지출 막대, 통화 선택 |
| `/bookkeeping/{bookId}/settings` | 가계부 정보, 분류·자산 편집(위/아래 단추), 멤버·초대 링크 |

- 구현 방식(2026-11-03, 6-1 · 6-2):
  - 화면은 서버가 다 그리고, 쓰기는 일반 폼 POST 뒤 월 화면으로 돌려보낸다(PRG). 실패 사유는 플래시로 보여 준다(`StockViewSupport.failureMessage`).
  - 모달은 `?new=1` · `?edit=<entryId>` 로 서버가 열어 그린다. 종류 버튼은 같은 모달을 그 종류로 다시 여는 링크다. JS 는 없다.
  - 이 게이트는 daisyUI 플러그인을 쓰지 않는다(`main.css` 가 컴포넌트를 직접 정의하고, modal 은 없음). 그래서 모달은 유틸리티로 그렸다. 새 유틸리티 클래스를 쓰면 `npm run build:css` 로 `static/main.css` 를 다시 만든다.
  - 금액에는 색을 박지 않고 부호(+ 수입, - 지출)로 가른다(`AmountColorConventionTest`). 수입 · 지출 구분은 라벨과 유형 배지로 한다.
  - 문구는 `bookkeepingUiMessage(_ko).properties` 에 둔다(`spring.messages.basename` 에 추가). 렌더 · 도우미 시험은 `BookkeepingScreenTest`.
  - 실패는 리다이렉트 + 플래시로 넘기지 않고 그 자리에서 다시 그린다(400 등). 게이트 세션 저장소(api-user `createSession`)가 지운 속성을 지우지 않아 플래시가 계속 남았다(2026-11-03 실측). 이 저장소 문제는 별도 작업으로 고쳤다(미배포).
- 6-3 자산(`BookkeepingAssetViewController`, `AssetGroups`, `assets.jte`): 종류별 묶음 · 통화별 소계 · 보이는 자산 총액(음수 포함), 숨긴 자산은 접어 둔다. 자산을 누르면 월 화면의 자산 필터. 월 이동 링크는 필터(검색어 · 분류 · 자산)를 유지한다.
- 6-5 통계(`BookkeepingStatsViewController`, `StatsView`, `stats.jte`): 한 통화씩, 대분류별 도넛 + 같은 수치의 표(줄을 누르면 그 대분류의 소분류 · "직접" 몫), 최근 12개월 수입 · 지출 막대. 차트 데이터는 canvas `data-bk-data`(JSON)로 싣고 nonce 인라인 스크립트가 Chart.js(`/js/vendor/chart.umd.min.js`)로 그린다. 표가 키보드 경로다.
- 6-4 설정(`BookkeepingSettingsViewController`, `settings.jte` + 조각 5개): 성공은 `?notice=saved|deleted|hidden` 으로 PRG, 수정 줄은 `?editAsset=` · `?editCategory=`. 초대 링크는 만든 직후 한 번만 보인다. 멤버 이름은 `BoardUserInfoService.fetchUsernames`. 가계부 삭제는 확인 칸 필수.
- 내역 입력·수정은 수입/지출/이체 탭이 있는 모달 하나로 한다.
  - 날짜는 오늘 또는 달력에서 고른 날로 채운다.
  - "저장 후 계속 입력" 단추를 둔다.
  - 삭제는 모달 안 단추로 한다.
- 기존 게이트 관례를 따른다: `_components/ui`, 테마 light/dark, 구역 교체 방식, Chart.js.
- 상단 메뉴의 가계부는 노출한다. `/bookkeeping` 화면이 들어가는 단계에서 application.properties 의 숨김을 푼다.
- 옛 게이트 bookkeeping 코드는 지운다: httpexchange 5개, domain, constant(`EntryGroupType` 등), `BookkeepingApiController`, 기존 시험(2026-11-03 완료).
  - devcheck(`BookkeepingDevCheckController`)는 새 클라이언트로 다시 써서 남겼다. 게이트 → API 연결을 확인하는 데 쓰고, `WriteEndpointUserGuardTest` 가 `bookkeeping/controller` 폴더를 훑기 때문이기도 하다.
  - 새 클라이언트는 `httpexchange/` 의 6개이고, 응답 · 입력 record 는 각 인터페이스 안에 둔다. 날짜 파라미터는 포맷 설정을 타지 않게 문자열(`yyyy-MM-dd`, `yyyy-MM`)로 넘긴다.
  - 멤버 이름은 `BoardUserInfoService.fetchUsernames` 를 쓴다(public 으로 바꿈).

## 7. 시험

- API 서비스 단위 시험
  - 잔액 계산(이체 다통화 포함)
  - 내역 검증: type 별 칼럼 조합, 분류 kind 일치, 다른 가계부의 자산·분류 거절, 소수 자릿수
  - 멤버십·역할 검사
  - 초대 수락·만료·재사용 거절
  - 월 범위 계산(시작일 1·25·28, 연말 경계)
- 게이트: 남의 bookId 로 들어오면 403 이 나는지.
- DB 통합은 `BookkeepingDbTest` 로 확인한다. 시스템 속성 `bookkeeping.db.url` 이 있을 때만 돈다(평소 빌드에서는 건너뜀). 버리는 DB 에서만 돌린다.

  ```bash
  docker run -d --name bk-it -e POSTGRES_PASSWORD=x -p 55432:5432 postgres:18.4
  mvn -o -pl bluesky-api/bluesky-api-bookkeeping -am test -Dtest='RulesTest,BookkeepingDbTest' -Dsurefire.failIfNoSpecifiedTests=false -Dbookkeeping.db.url=jdbc:postgresql://localhost:55432/postgres -Dbookkeeping.db.password=x
  docker rm -f bk-it
  ```

## 8. 구현 순서

각 단계는 그 단계만으로 컴파일·시험이 통과하는 지점에서 끝낸다.

1. **스키마**: `schema-create` 를 새로 쓰고 `schema-alter` 에 DROP 과 CREATE 를 넣는다. 사용자가 로컬 DB 에 적용한다.
2. **API 바탕**
   - 옛 domain·constant·converter·repository·service·controller 와 옛 시험을 지운다.
   - 새 record 도메인 6개와 repository 를 만든다.
   - `BookkeepingErrorCode` 와 메시지 파일을 새로 쓴다.
3. **API 서비스와 시험**: 다음 순서로 만든다.
   1. `BookAccess`(멤버십·역할)
   2. Book(만들기 + 기본 데이터·수정·삭제)
   3. Asset·Category(CRUD·숨김·순서)
   4. Entry(검증·CRUD·검색)
   5. 잔액
   6. 월 범위·통계
   7. 초대·멤버
4. **API 컨트롤러**: 5절 경로를 만들고, 모듈 check(spotless·checkstyle)를 돌린다.
5. **게이트 바탕**
   - 옛 bookkeeping 코드를 지운다.
   - 새 HttpExchange 클라이언트·record 와 api-user `findByIdIn` 연결을 만든다.
   - 게이트 test-compile 까지 확인한다.
6. **게이트 화면**: 다음 순서로 만든다.
   1. 목록·만들기·초대 수락. 이때 메뉴를 노출한다.
   2. 월 화면과 입력 모달
   3. 자산
   4. 설정
   5. 통계
7. **마무리**: 게이트 403 시험을 넣고, 로컬에서 띄워 시각 QA(Playwright 로그인 프로필, light/dark, 좁은 폭)를 한다.

## 9. 추가 결정

- **체크카드**(2026-11-03): 체크카드는 자산 kind 로 두지 않는다. 체크카드로 쓴 돈은 연결 계좌에서 바로 빠지므로, 그 지출은 은행 자산에 바로 기록한다.
  - 연결 계좌(`linkedAsset_id`)로 잔액을 합산하는 방식은 단순함을 위해 택하지 않았다.
