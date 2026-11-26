# bluesky-web-dynamiccrud2 설계 기록

기존 `bluesky-web-dynamiccrud` 를 개선한 새 모듈. 기존 모듈과 **코드·데이터 모두 무관하게 새로 작성**한다(참고만, 데이터 이관 없음).

## 기존 DynamicCRUD 의 단점 (개선 동기)

- 메뉴 하나에 Create/Read/Update/Delete 를 하나씩만 설정할 수 있었다(`SubMenu.enableInsert/Update/Delete` 플래그 + `DbQuery` 명령 종류별 1개).
- 컬럼 설정(`DbField`)이 메뉴 단위라, 같은 테이블로 메뉴를 여러 개 만들면 컬럼 설정을 복사해 따로 관리해야 했다.
- 수정 가능 여부를 필드(`DbField.enableUpdate`)에 두어 수정 작업을 여러 개 둘 수 없었다.
- 관리툴 자체도 CRUD 설정으로 만들려고 관리툴 로직과 CRUD 로직을 공통화(`adminProjectId`)했지만, 관리툴을 추가할 일은 없고 코드만 복잡해졌다.

## 확정된 결정

### 구조
- 관리툴 로직과 CRUD 실행 로직은 공통 코드로 묶지 않는다. `adminProjectId` 없음.
- 계층: `Project > MainMenu > Menu`.

### 등록 절차
1. Project / MainMenu / Menu 를 구성한다.
2. Menu 에 connection(connectionInfo 의 connection name 목록에서 선택)과 table(해당 DB 에서 조회한 목록에서 선택)을 지정한다.
   - 이때 테이블 컬럼 정보를 **테이블 단위로** 자동 등록하고, **조회(Read) 쿼리만** 자동 생성해 등록한다.
   - Menu : Read = 1 : 1.
3. 입력/수정/삭제는 최초 자동 등록 대상이 아니다. 이후 설정에서 사용자가 추가할 때, 그 시점에 기본 쿼리를 자동 생성해 편집 화면에 보여 주고, 사용자가 고친 결과를 저장한다. 메뉴당 각각 N 개.

### 쿼리 중심 모델
- 모든 작업(Read 포함)은 쿼리 1개를 가진다. 자동 생성 쿼리를 사용자가 편집하거나 커스텀 쿼리를 직접 써서 저장한다.
- 작업이 쓰는 필드는 필드 설정이 아니라 **등록된 쿼리에서 유추**한다. 수정 화면에는 그 수정 쿼리가 쓰는 필드만 보인다 → 수정 N 개 가능.
- 쿼리는 이름 붙은 파라미터(`:colName`) 텍스트로 저장한다.
- 파라미터 추출은 SQL 파싱(JSqlParser) 대신 Spring `NamedParameterUtils.parseSqlStatement` 로 이름만 뽑는다(벤더 문법 무관). Mongo 는 JSON 텍스트의 `":param"` 을 같은 규칙으로 뽑는다.
- 파라미터 역할(입력 / 행 키 / 검색)은 **파라미터별 설정**으로 저장한다. 기본값 추정: 테이블 PK·unique 컬럼 이름과 같으면 "행 키", 아니면 "입력". 사용자가 등록 화면에서 바꿀 수 있다.
- 파라미터 설정(역할·필수 여부·입력 형식·기본값)은 쿼리 종류와 상관없이 한 형태로 통일하고, **JSON 으로 저장**한다.
- 페이징: 쿼리에 예약 파라미터 `:_limit`, `:_offset` 이 있으면 사용자가 페이징을 쓴 것으로 보고 값만 바인딩한다. 없으면 JSqlParser 로 쿼리 구조를 고쳐 방언별 `LIMIT/OFFSET` 또는 `OFFSET … FETCH` 를 붙인다(MSSQL 은 `ORDER BY` 가 없으면 `ORDER BY (SELECT NULL)`). 감싸기(`SELECT * FROM (<쿼리>) t`)는 MSSQL 이 서브쿼리 안 `ORDER BY` 를 막아서 파싱 실패 시 대체 수단으로만 쓰고, 그때 MSSQL + `ORDER BY` 면 저장 시 경고한다.
- 건수: `ORDER BY` 를 뺀 쿼리를 감싸 `COUNT(*)`.
- 목록 헤더 정렬(1차 포함): 쿼리의 `ORDER BY` 를 바꿔 끼운다. 정렬 가능한 것은 정렬 허용된 실제 컬럼(가상 필드 제외).
- 목표: 사용자가 쓰던 쿼리를 그대로 붙여 넣으면 그 쿼리를 기준으로 필드·검색 설정이 만들어져, 관리툴 사용법을 익히는 허들을 낮춘다.
- Read 쿼리의 WHERE 에 쓰인 `:param` 컬럼 = 검색 허용. 기본은 필수, 사용자가 쿼리 아래 컬럼 목록 UI 에서 선택으로 바꿀 수 있다.
- 선택 조건 처리: 저장 시 원본 쿼리를 그대로 저장한다. 실행 시 값이 빈 선택 조건만 JSqlParser 로 잘라 낸다(JSqlParser 는 이 용도로만, 파라미터 추출은 `NamedParameterUtils`).
  - 선택으로 바꿀 수 있는 조건: 최상위 `AND` 로 연결된 단순 조건(`col = :p`, `col LIKE :p`, `col >= :p` 등). `OR` 안·서브쿼리 안은 필수 고정.
  - 저장 시 파싱 실패(벤더 문법 등)면 저장은 되지만 모든 조건이 필수로 고정되고, 화면에 이유를 표시한다.
- 파라미터 문법: `:name` 형식만 허용. Spring 전용인 `:{name}`, `&name` 은 JSqlParser 와 해석이 갈리므로 저장 시 거부한다(공백 없는 `a&b` 는 Spring 이 `&b` 파라미터로 오인).
- 커스텀 쿼리 안전 검사(저장 시): 문장 1개만 허용(`;` 다중 문장 금지), Read 는 0건 실행으로 검증, `:param` 이 없는 UPDATE/DELETE 는 거부(전체 행 변경 방지). 문장 종류는 첫 키워드로 판별.
- 자동 생성 DELETE / UPDATE 의 WHERE 키: PK, 없으면 첫 unique 키. 둘 다 없으면 자동 생성하지 않고 커스텀만 허용.
- 삭제는 목록에서 여러 행 선택 가능, 한 트랜잭션에서 행마다 실행.

### 컬럼(필드)
- 필드 메타(라벨·타입·포맷·검증·도움말 등)는 테이블 단위로 공유한다. 같은 테이블을 쓰는 메뉴들은 필드를 같이 쓴다. CRUD 작업마다 라벨을 따로 두지 않는다.
- 결과 컬럼 판별: `a.title AS 제목` 은 "`article.title` 필드, 이 Read 의 표시 라벨 = 제목". 입력·수정은 원래 컬럼(`title`)으로 처리한다.
- 화면 제목 우선순위: Read 쿼리 별칭 → 필드 사용자 지정 라벨 → DB 컬럼 주석(PG `COMMENT ON`, MySQL `COLUMN_COMMENT`, MSSQL `MS_Description`) → 컬럼 이름. 별칭은 SQL 안에 있으므로 따로 저장하지 않고, 메뉴마다 달라도 충돌하지 않는다.
- 필드 등록 위치:
  - 메뉴 테이블의 실제 컬럼 → 그 테이블 필드.
  - 조인한 다른 테이블의 실제 컬럼(`u.name`) → 그 테이블(`users`)의 필드로 등록(쓰인 컬럼만), users 메뉴와 공유.
  - 계산·함수 결과(`CONCAT(...) AS full_name`) → 메뉴 테이블의 "추가 필드"(가상, 이름 = 별칭), 조회 전용.
  - 원래 컬럼을 알 수 없으면 별칭을 필드 이름으로 본다.
- 원래 컬럼 판별 순서: JSqlParser SELECT 목록(`a.title AS 제목` → `a` → `FROM article a`) → 파싱 실패 시 `ResultSetMetaData` → 그것도 안 되면 별칭. `SELECT *`·`a.*` 는 해당 테이블 컬럼 전체로 펼친다.
- 정렬 허용 여부는 필드 정보에 저장한다. 자동 등록 시 DB 인덱스 정보를 참고해 초기값을 정한다.
- 스키마 변경은 관리 화면 "컬럼 다시 읽기"로 수동 동기화한다. 새 컬럼은 추가, 사라진 컬럼은 지우지 않고 "DB에 없음"으로 표시.

### DB 지원 / 확장
- 지원: MySQL/MariaDB, MSSQL, PostgreSQL(1차), Mongo(2차). 새 DB 추가를 대비한다. 아래 Mongo 항목은 2차 작업 때 적용할 결정이다.
- DB 종류별 어댑터 인터페이스 하나: 테이블(컬렉션) 목록, 컬럼·PK·unique 조회, 기본 C/R/U/D 쿼리 생성, 파라미터 추출, 검색·페이징 적용, 실행.
- 벤더는 connection 정보로 판별한다. JDBC 3종은 공통 JDBC 어댑터 + 방언 차이만.
- Mongo 컬럼: 최근 100건 표본의 최상위 키 합집합, 타입은 최빈 타입, `_id` = PK, 중첩 문서는 한 컬럼(JSON).
- Mongo 쿼리: Read = aggregate pipeline(JSON 배열, `$lookup` 조인 가능). 입력/수정/삭제 = `{"filter": {...}, "update": {...}}` JSON. 선택 조건은 첫 `$match` 최상위 키만, 값이 비면 키 제거. 여러 행 삭제는 replica set 이면 트랜잭션, standalone 이면 행마다.
- connection 목록: connectionInfo 가 읽는 연결 정보 저장 테이블(`DataSourceConnectionConfig`)의 **전체 목록**을 조회해 보여 준다. 연결 정보가 바뀌어도 dynamiccrud2 재배포가 필요 없게 하기 위함.
  - bluesky-boot-connectioninfo 는 수정하지 않는다. connectionInfo 저장소 자체를 `connectioninfo` 라는 connection 으로 등록해 두었고(DB 에 추가 완료), dynamiccrud2 는 이 connection 의 DataSource 로 `SELECT connection FROM DataSourceConnectionConfig` 를 조회한다. 설정은 이 연결 이름 하나만 지정한다.
  - 이름만 보여 주고 url·계정은 내보내지 않는다. 벤더는 `DatabaseMetaData.getDatabaseProductName()` 으로 판별. DataSource 는 `ConnectionInfoUtil.getConnection(이름)` 이 그때그때 읽어 온다(lazy-load routing · ThreadLocal lookup key 를 쓰지 않는다, 1단계에서 변경).
  - Mongo 연결 정보는 별도 Mongo DB 의 `MongoClientConnectionConfig` 컬렉션에 있다(`MongoDbMongoClientConnectionInfoReader`).

### 설정 저장
- 설정 catalog: PostgreSQL `dynamiccrud2`.
- 저장 기술: Spring Data JDBC. 묶음(aggregate) 두 개: Menu(Read·작업·파라미터 설정 포함), 테이블(필드 메타 포함).

### 권한 / 이력 / UI / 엑셀
- 설정 화면(Project·메뉴·쿼리·필드 등록, connection·테이블 조회) 전체는 `ROLE_MASTER`/`ROLE_ADMIN` 전용, 서버 측 검증. 메뉴 사용 화면은 메뉴·작업 권한을 따른다.
- 권한: 기존 방식 그대로 메뉴 권한(역할 문자열 하나, 예: `ROLE_ADMIN`). 입력/수정/삭제 작업마다 실행 권한 칸을 두되, 비어 있으면 메뉴 권한을 따른다(비워 두면 기존과 같음).
  - 기존 dynamiccrud 는 `SubMenu.authority` 를 저장만 하고 실행 시 검사하지 않았다. dynamiccrud2 는 실제로 판정한다.
  - 판정은 서버에서, 메뉴 목록 표시·조회·엑셀·입력/수정/삭제 실행 요청마다. 화면에서 버튼만 숨기는 방식이 아니다.
  - 메뉴 권한 빈 값 = 로그인한 사용자 누구나.
  - 역할 계층 `ROLE_MASTER > ROLE_ADMIN > ROLE_USER`(`ROLE_ADMIN` 메뉴에 MASTER 도 접근).
- 변경 이력: 입력/수정/삭제 실행마다 dynamiccrud2 catalog 에 사용자·시각·메뉴/작업·바인딩 값(JSON)·영향 행 수를 남긴다.
- UI 스택: gate 와 같게 Boot 4 · JTE · htmx, gate UI 컨벤션.
- 엑셀: 1차는 Read 결과 내려받기만. 업로드(일괄 입력)는 제외.

### 인덱스 · 실행 보호 · 입력 폼
- 정렬 허용 초기값: 어떤 인덱스의 첫 번째 컬럼(PK 포함)만 허용. 그 외 컬럼·가상 필드는 불가. 수집은 JDBC `DatabaseMetaData.getIndexInfo`(Mongo 는 `listIndexes` 첫 키). 사용자가 바꾼 값은 "컬럼 다시 읽기" 후에도 유지.
- 검색 컬럼이 인덱스가 없거나 앞이 `%` 인 `LIKE` 면 쿼리 저장 화면에서 경고만 표시, 저장은 허용.
- 실행 한도(properties 기본값, 메뉴별 변경 없음): 쿼리 타임아웃 30초(JDBC `setQueryTimeout`, Mongo `maxTime`), 목록 한 페이지 최대 1000행, 엑셀 최대 10만 행.
- 필드 자동 등록 초기값(사용자 수정 가능): 입력 형식 = DB 타입 기준(문자·숫자·날짜·날짜시간·불리언·긴 글), 입력 필수 = `NOT NULL` 이면서 기본값 없음, 자동 증가 컬럼은 입력 쿼리 자동 생성에서 제외, 최대 길이 = 컬럼 길이.

### connectioninfo 연결 사용
- `connectioninfo` 연결도 선택 목록에 포함한다. connectionInfo 저장 테이블을 조회·관리하는 메뉴도 만들 수 있어야 한다. 보호는 해당 메뉴 권한을 ADMIN 전용으로 지정해서 한다 → 메뉴별 권한 관리가 실제로 동작해야 한다.

## 단계
- 1차: DataSource(JDBC: MySQL/MariaDB·MSSQL·PostgreSQL)만. 단, 어댑터 인터페이스는 Mongo·다른 DB 를 붙일 수 있게 설계한다.
- 2차 이후: Mongo 어댑터(위 Mongo 관련 결정), Mongo connection 목록 출처(별도 Mongo DB 의 `MongoClientConnectionConfig` 컬렉션 — 1차 결정 보류).

## 열린 질문
- 없음(2026-11-17 설계 인터뷰로 합의 완료).

## 1차 구현 계획 (2026-11-17)

### 계획 수립 때 확인한 사실
- 부모: `bluesky-web` (Spring Boot 4.1.0-SNAPSHOT 계열 `bluesky-parent`), jsqlparser 5.3 버전 관리됨.
- UI 스택 정정: 기존 dynamiccrud 가 `jte-spring-boot-starter-4` + htmx + theme-change, gate 는 starter-3. dynamiccrud2 는 **기존 dynamiccrud 의 스택**을 따른다.
- 보안: `ClientUserSecurityConfig` 가 `anyRequest().permitAll()`, `RoleHierarchy` 없음 → 메뉴 권한·설정 화면 권한은 dynamiccrud2 가 직접 판정해야 한다.
- 대상 DB 접근: `LazyLoadRoutingDataSource` 가 `RoutingDataSourceContextHolder` 의 lookup key(=connection 이름)로 connectionInfo 에서 DataSource 를 그때그때 만든다(실패 키는 1시간 재조회 차단).
- 사용 중 포트 30120·30122~30124·30130~30135 → dynamiccrud2 는 30125.

### 단계 (각 단계 끝에 확인 방법)
0. **모듈 뼈대**: `bluesky-web/pom.xml` 에 `<module>` 추가, pom(기존 dynamiccrud 의존성 기준, JPA·Mongo starter 대신 `spring-boot-starter-data-jdbc`), `Application`, `application.properties`(connectionInfo connections: `dynamiccrud2`, `connectioninfo`).
   - 확인: `mvn -o -pl bluesky-web/bluesky-web-dynamiccrud2 -am compile`, localdev 기동.
   - ✅ 완료(2026-11-17): 컴파일 성공, localdev 기동 4.5초(https 40125, `GET /` 404). Hikari 풀은 첫 연결 때 열리므로 `dynamiccrud2`·`connectioninfo` 연결은 아직 실제로 열어 보지 않았다 → 1단계(catalog)·3단계(connection 목록)에서 확인.
   - JTE 사전 컴파일 플러그인(`jte-maven-plugin`, `build-helper-maven-plugin`)은 템플릿이 생기는 5단계에서 추가한다(템플릿 폴더 없이 넣으면 빌드 단계만 늘어남). `spring-boot-starter-web` 대신 Boot 4 이름 `spring-boot-starter-webmvc`, springdoc·poi 는 쓰는 단계(엑셀 = 6단계)에서 추가.
1. **설정 catalog 스키마 + 저장소**: PostgreSQL DDL 스크립트(모듈 안 `src/main/resources/schema/`). 테이블: `project`, `main_menu`, `menu`(connection·테이블 참조·권한·페이지 크기·엑셀 여부), `menu_query`(종류 READ/INSERT/UPDATE/DELETE·이름·쿼리 텍스트·파라미터 설정 `jsonb`·작업 권한·순서), `db_table`(connection·catalog·schema·table 유일), `db_field`(가상 여부·라벨·DB 주석·타입·입력 형식·필수·최대 길이·자동 증가·정렬 허용·사용자 수정 여부·DB에 없음), `change_log`.
   - Spring Data JDBC 묶음: Project, MainMenu, Menu(+MenuQuery), DbTable(+DbField), ChangeLog. routingDataSource 를 쓰므로 방언은 `PostgresDialect` 로 명시(기동 시 자동 감지가 lookup key 없이 연결을 열지 않게).
   - 확인: catalog 대상 저장소 통합 테스트(저장·조회·jsonb 왕복).
   - 진행(2026-11-17): DDL = `src/main/java/net/luversof/web/dynamiccrud2/schema-create-postgresql-dynamiccrud2.sql`(stock 관례: 따옴표 camelCase · UUID · BOM+CRLF, **적용은 사용자**). 도메인 7개 · 저장소 5개 · `Dynamiccrud2DataJdbcConfig`(stock 패턴: `ConnectionInfoUtil` DataSource + 전용 트랜잭션 관리자 + jsonb↔Map 변환기 + id 콜백) · `SettingRepositoryTest`(GeneralWebTest, localdev).
   - 구현 중 바꾼 것: lazy-load routing 제거(설정 catalog · 대상 DB 모두 `ConnectionInfoUtil` 로 충분), `DbField` 는 id 없이 PK (dbTable_id, columnName)(Data JDBC 의 id 콜백이 하위 엔티티를 안 채움), `MenuQuery` id 는 콜백에서 채움(ChangeLog 가 참조), READ 1개는 부분 unique 인덱스로 보장, `uuid-creator` 의존성 추가(UuidGenerator 빈 조건).
   - ✅ 완료(2026-11-17): 사용자가 DDL 적용 후 `SettingRepositoryTest` 2/2 통과(DbTable+필드 왕복, Menu+MenuQuery jsonb 왕복, 재저장 시 MenuQuery id 유지).
2. **어댑터 계층(화면 없이, 테스트 먼저)**: `DatabaseAdapter` 인터페이스 + `JdbcDatabaseAdapter` + `SqlDialect`(MySQL/MariaDB·MSSQL·PostgreSQL).
   - 테이블 목록, 컬럼·PK·unique·인덱스 첫 컬럼·주석(벤더별 조회), 기본 C/R/U/D 생성, 파라미터 추출(`NamedParameterUtils` + `:{`·`&name` 거부), 안전 검사, Read 재작성(JSqlParser: 빈 선택 조건 제거·페이징·건수·ORDER BY 교체, 실패 시 감싸기), 결과 컬럼 → 필드 판별, 타임아웃 실행.
   - 벤더 판별은 url 대신 `DatabaseMetaData.getDatabaseProductName()`(DataSource 를 이미 얻은 뒤라 더 확실).
   - 확인: 방언별 SQL 문자열 단위 테스트(DB 없이) + 로컬 DB 통합 테스트.
   - ✅ 완료(2026-11-17): 모듈 전체 테스트 25/25 통과(단위 20 + catalog 통합 5).
     - `adapter` 패키지: `DatabaseAdapter`(DB 종류 중립 인터페이스, Sort · ReadResult) · `DatabaseAdapterResolver`(connection 이름 → 어댑터) · `ReadAnalysis` · `TableRef` · `ColumnMeta`(입력 형식 · 필수 · 최대 길이 초기값) · `TableMeta`(rowKey = PK → 첫 unique) · `QueryErrorCode`.
     - `adapter.jdbc`: `SqlDialect`(따옴표 · 이름 · 페이징 · 시스템 스키마 · MSSQL 주석) · `QueryText`(문장 종류 · 다중 문장 · 파라미터 형식 · 수정/삭제 파라미터 필수) · `ReadQueryRewriter`(JSqlParser: 조건 분석 · 빈 선택 조건 제거 · 페이징 · 건수 · 정렬 교체 · 결과 컬럼 판별, 실패 시 감싸기) · `DefaultQueryGenerator` · `JdbcMetadataReader` · `JdbcDatabaseAdapter`(타임아웃 · 행 한도, 여러 행 수정은 한 트랜잭션).
     - 통합 테스트는 설정 catalog(dynamiccrud2) 자체를 대상 DB 로 썼다: 부분 unique 인덱스(`uk_menuQuery_menuId_read`) · 식 인덱스(`uk_dbTable_location`)는 행 키에서 빠지는 것, 생성 INSERT → 선택 조건 뺀 조회 → 생성 DELETE 왕복.
     - 함정: `ConnectionInfoUtil.getConnection(이름, DataSource.class)` 는 null(레지스트리 빈이 제네릭 변수 `T` 로 등록됨) → 1인자 `getConnection(이름)` + `instanceof`. 라이브러리는 고치지 않았다.
     - 남은 것: 화면 입력 문자열 → 컬럼 타입 변환은 6단계(PG 는 `uuid = varchar` 비교를 거부한다). MySQL/MariaDB · MSSQL 은 실제 DB 로 확인하지 않았다(문자열 단위 테스트만, MSSQL 주석 조회는 미검증). 조건 안 문자열 리터럴에 `:x` 가 있으면 파라미터로 잘못 셀 수 있다.
     - 설정값 `bluesky.dynamiccrud2.query-timeout-seconds`(30) · `page-max-rows`(1000) · `excel-max-rows`(100000), `Dynamiccrud2Config` 에서 등록(테스트가 TestApplication 으로 뜨므로 Application 에 두지 않음).
3. **connection 목록**: `connectioninfo` DataSource 로 `SELECT connection FROM DataSourceConnectionConfig`.
   - 확인: 목록 조회 → 선택한 이름으로 테이블 목록까지.
   - ✅ 완료(2026-11-17): `connection.ConnectionNameService` — `bluesky.dynamiccrud2.connection-info-connection`(기본 `connectioninfo`) 연결을 어댑터로 열어 `SELECT connection FROM <DataSourceConnectionConfig> ORDER BY connection`(테이블 따옴표는 저장소 DB 종류를 따름: PG 는 `"DataSourceConnectionConfig"`, 2단계 실행 경로 그대로라 타임아웃 · 행 한도 적용). 이름만 읽는다. `ConnectionNameServiceTest`: 목록에 `dynamiccrud2` · `connectioninfo`, `connectioninfo` 로 테이블 목록에 `DataSourceConnectionConfig`. 모듈 테스트 26/26.
4. **권한**: `RoleHierarchy`(MASTER > ADMIN > USER), 설정 경로 ADMIN 전용 인터셉터, 메뉴·작업 권한 판정기를 사용 화면 모든 진입점에서 호출.
   - 확인: MockMvc 로 권한별 200/403.
   - ✅ 완료(2026-11-17): `security.MenuAuthorizer`(canRead · canRun · canManageSettings + check*, 익명 토큰은 로그인 아님, `RoleHierarchy` 는 빈이 아닌 내부 필드 — 다른 모듈의 판정에 끼어들지 않게) · `SettingAccessInterceptor`(`/setting/**` = ROLE_ADMIN, `Dynamiccrud2WebMvcConfig` 에 등록). 거부는 `AccessDeniedException` → 익명은 로그인으로, 로그인 상태는 403(ExceptionTranslationFilter 몫). `MenuAuthorizerTest` 5건, 모듈 31/31.
   - ⚠️ 5단계에서 정정: "거부는 ExceptionTranslationFilter 몫" 은 틀렸다. 공통 `CoreMvcExceptionHandler`(Throwable 처리 @ControllerAdvice)가 MVC 안에서 먼저 잡아 500 이 됐다(실측). → `security.AccessDeniedAdvice`(@Order 최우선)가 직접: 로그인 상태 403, 익명 htmx · JSON 401, 익명 화면은 `bluesky.client.user.login-url?redirectUrl=` 로 이동(ClientUserSecurityConfig 와 같은 규칙).
   - 계획과 다른 점: MockMvc 200/403 은 아직 컨트롤러가 없어 못 했다. 판정기 · 인터셉터 단위 테스트로 대신했고, 필터 체인을 거친 실제 응답(로그인 이동 · 403)은 5단계 화면이 생긴 뒤 확인한다. 사용 화면(`/use/**`)은 6단계 컨트롤러 진입점마다 `checkRead` · `checkRun` 을 부른다.
5. **설정 화면**: Project/MainMenu/Menu 관리 → 메뉴 생성(connection → 테이블 → 필드 자동 등록 + Read 자동 생성) → 쿼리 편집기(아래 컬럼 목록: 역할·필수/선택·인덱스 경고) → 작업 추가(기본 쿼리 자동 생성 후 편집) → 필드 화면(라벨·정렬 허용·컬럼 다시 읽기).
   - ✅ 완료(2026-11-17, 브라우저 눈 확인 전): 모듈 테스트 34/34.
     - 서비스: `MenuService`(트리 · Project/MainMenu/Menu 저장 · 하위 있으면 삭제 거부 · `createMenu` = 필드 등록 + 조회 생성) · `FieldService`(테이블 공유 등록 · 다시 읽기: DB 값 갱신 + 사용자 값 유지 + sortable 은 사용자 지정 전까지 인덱스로 · 조인 테이블은 쓰인 컬럼만 · 계산 컬럼은 가상 필드) · `QueryService`(초안 · 검사 · 저장 · 삭제, 역할 추정, 선택 불가 조건을 선택으로 저장하면 거부, 조회 1행 시험 실행 실패는 경고). 파라미터 설정은 `ParamSetting`(role · required · defaultValue) 을 jsonb 맵으로.
     - 화면: `SettingController` + JTE(`_layout/layout`, `setting/index · menuNew · tableOptions · menu · query · queryInspection · table`). htmx 는 connection → 테이블 목록, "쿼리 다시 읽기" 두 곳만.
     - 스타일: **daisyUI 안 씀(사용자 지시)**. Tailwind 4 단독, `src/main/frontend/main.css` 에 gate 토큰(라이트 · 다크) + 쓰는 컴포넌트만(.btn · .card · .input · .table · .badge · .alert). `npm run build` → `static/main.css`(커밋 대상, gate 와 같음). `node_modules` 는 gitignore.
     - 오류 문구: `QueryErrorCode` enum 안에 한국어 문구(.properties 한글 깨짐 회피).
     - 확인: `SettingControllerTest`(MockMvc + 가짜 사용자): 익명 → 로그인 이동 · htmx 401 · USER 403 · MASTER 200, ADMIN 으로 화면 전부 렌더 + 메뉴 생성 · 작업 저장(오류 시 편집기 다시 그림) · 컬럼 다시 읽기. `SettingServiceTest`: 등록 → 수정 작업 추가 → 조인 · 계산 컬럼 조회 저장 → 필드 등록 위치.
     - 구현 중 바꾼 것: localdev · k8sdev `bluesky.client.user.login-url=https://user.web.bluesky.local/login/redirect`(gate 와 같음, 기본 /login 은 404). Hikari 풀 `maximumPoolSize=5 · minimumIdle=0 · idleTimeout=60000`(기본 idle 10개가 공유 PG 의 `too many clients` 를 냈다, 관리툴은 여러 DB 에 풀을 연다). `spring-security-test` test 의존성 추가(bluesky-test 에서는 주석 처리됨).
     - 남은 것: QA 로그인 프로필 세션이 만료돼(OAuth GitHub 로그인 화면) 실제 브라우저로 화면을 아직 못 봤다.
     - 브라우저 확인 중 발견(2026-11-17): 로그인은 됐지만 403. 로그인 체계가 모든 GitHub 사용자에게 ROLE_USER 만 주고 ADMIN · MASTER 를 주는 곳이 없었다(기존 user DB 의 `authorities` 는 폼 로그인용 `users` 를 FK 로 물어 GitHub 사용자에게 못 씀).
       → 사용자 결정: user DB 역할 테이블. cross-repo 변경(미커밋):
         - bluesky-api-user: `schema-create-postgresql-user.sql` 에 `"UserAuthority"("userInfo_id" UUID → "UserInfo".id, "authority")`, `UserInfoService.findAuthorities`(userJdbcClient), `/api/userInfo/oauth2` 응답을 `OAuth2UserResponse`(authorities 포함, password 미포함)로.
         - bluesky-client-user: `CustomOAuth2UserService.withUserRole(oauth2User, authorities)` 가 응답의 역할을 ROLE_USER 에 더한다(호출 실패면 ROLE_USER 만).
         - 반영 순서: user DB 에 테이블 생성 + 첫 관리자 INSERT → user API 재배포 → bluesky-web-user 재배포(로그인을 수행하는 앱) → 다시 로그인. 역할은 로그인 때 세션에 들어간다. dynamiccrud2 는 바꿀 것 없음.
         - 사용자가 커밋(33127074) · 테이블 생성 · 역할 추가 후 재로그인.
     - ✅ 브라우저 확인(2026-11-17, Playwright + QA 로그인 프로필, dev.bluesky.local:40125): 설정 첫 화면 → 프로젝트 · 메인 메뉴 추가 → 메뉴 추가(connection → htmx 테이블 목록 → dynamiccrud2 · MainMenu) → 메뉴 화면(조회 자동 생성) → 수정 추가(기본 UPDATE) → 조회 편집기 "쿼리 다시 읽기"(OR 안 :id 필수 고정 · name 인덱스 경고 · 결과 컬럼 원래 위치 · 계산 컬럼) → 필드 화면 → 화면에서 메뉴 · 메인 메뉴 · 프로젝트 삭제까지 통과. 콘솔 오류는 favicon 404 하나.
       - 남은 흔적: 확인에 쓴 catalog 자체 테이블(MainMenu)의 공유 필드 행(DbTable · DbField)은 화면에 지우는 기능이 없어 남았다.
6. **사용 화면**: 권한별 메뉴 목록, 목록(검색·페이징·헤더 정렬), 입력/수정 폼(쿼리가 쓰는 필드만), 여러 행 삭제, 엑셀 내려받기, 변경 이력 기록.
   - ✅ 완료(2026-11-17): 모듈 테스트 35/35 + 브라우저 한 바퀴.
     - `use.service.UseService`: 보이는 메뉴 트리(표시 켬 + 메뉴 권한), 조회(검색 s.{param} · 선택 조건 비면 제외 · 필수 비면 조회 안 하고 안내 · 기본값 · 페이지 · 정렬은 허용 컬럼만 — 분석 결과로 미리 판정해 조회 1번), 작업 폼(INPUT 만 입력칸, 행 하나면 그 값으로 채움), 실행(ROW_KEY 는 선택 행에서, 행마다 실행을 한 트랜잭션, ChangeLog 에 사용자 · 값 · 영향 행 수), 엑셀(excelMaxRows 까지, `DatabaseAdapter.export` 추가).
     - `ValueConverter`: 화면 문자열 → uuid · BigDecimal · LocalDate · LocalDateTime · Boolean(필드 inputType · dataType 기준, PostgreSQL 은 문자열과 uuid · 숫자 비교를 거부). DB 값 → 화면 문자열(날짜시간은 초 단위 로컬, datetime-local 과 같은 모양).
     - 화면: `UseController` + `use/index · menu · actionForm · empty`. 행 값은 JSON 으로 체크박스(rows) · 행 버튼(row)에 싣고, 작업은 htmx 로 폼 조각 → 실행 → 성공 시 `HX-Redirect` 로 보던 목록(HX-Current-URL) 다시 열기, 실패면 폼에 사유.
     - 함정 2개: ① `@RequestParam List<String>` 은 값이 하나면 쉼표로 쪼갠다 → 행 JSON 이 깨져 폼이 안 채워졌다, 요청 맵에서 꺼낸다. ② 핸들러가 `HttpServletResponse` 를 인자로 받으면 요청이 "처리됨"이 돼 반환한 뷰를 안 그린다 → 헤더는 RequestContextHolder 로.
     - 브라우저 확인(Playwright · QA 로그인 프로필): 설정 화면으로 Project 테이블 메뉴 + 입력 · 수정(이름만) · 삭제 작업 구성 → 조회 name 조건 선택으로 → 사용 화면에서 등록(uuid · 숫자 · 날짜시간) → 검색 1건 → "이름 수정" 폼이 행 값으로 채워짐 → 수정 → 체크 후 선택 삭제 → 0건 → 설정 화면에서 정리. 발견해서 고친 것: 마우스 올린 `.btn-error` 가 `.btn:hover` 에 덮여 회색 바탕 + 흰 글자(CSS 만 고쳐 빌드, 재기동 후 눈 확인은 안 함).
     - 하지 않은 것: 403 전용 화면(지금은 Boot 기본 오류 화면 — localdev 는 스택트레이스까지 보임), 변경 이력 조회 화면(저장만 한다), favicon.
7. **마무리 검증**: localdev 에서 Playwright 로 실제 흐름(메뉴 생성 → 조회 → 입력·수정·삭제), 모듈 checkstyle 검사.
   - ✅ 완료(2026-11-17):
     - bluesky 저장소는 checkstyle 이 없고 spotless 가 기준 → `spotless:check` 통과.
     - `mvn -o -pl bluesky-web/bluesky-web-dynamiccrud2 verify`(localdev): 테스트 35/35, jar 생성. ⚠️ `jib:build` 가 package 단계에 묶여 있어 레지스트리 푸시를 시도한다(오프라인이라 "Cannot build to a container registry in offline mode" 로 막혔고 이미지는 올라가지 않음). 로컬 확인은 `-Djib.skip=true` 로 → BUILD SUCCESS.
     - jar 내용: 사전 컴파일 JTE 24개 · `static/main.css` · poi-ooxml · jsqlparser 포함.
     - jar 를 사전 컴파일 템플릿(`gg.jte.developmentMode=false`, `usePrecompiledTemplates=true`)으로 띄워 로그인 상태로 `/setting` · `/use` 렌더 확인, 서버 로그 ERROR 0. 확인 뒤 catalog 에 QA 데이터 없음.

## 1차 이후 고친 것(2026-11-17)
- 주소: 사용 화면 `/use` → `/menu`(`/menu/{id}` = 메뉴 화면), 관리 화면 `/setting` → `/admin`(관리자 전용이라 의미가 분명). 루트 `/` → `/menu`. 상단 메뉴 문구 "메뉴 · 관리". 자바 패키지 · 템플릿 폴더 이름(`use` · `setting`)은 그대로. 위 본문의 `/use` · `/setting` 표기는 옛 주소다.
- 상단 오른쪽 로그인 상태: 로그인이면 사용자 이름 + 로그아웃(`POST /logout`), 아니면 로그인(`login-url?redirectUrl=지금 주소`). 레이아웃이 `UserUtil` 로 직접 판단(화면마다 넘기지 않음).
- 관리 화면 순서: 새 프로젝트 · 메인 메뉴의 "순서" 입력칸을 없앴다. 새 항목은 맨 끝(최대 displayOrder + 1). 순서는 ⠿ 손잡이 드래그 앤 드롭 또는 손잡이에서 ↑ ↓ 키(`static/js/reorder.js`, 라이브러리 없음) → `POST /admin/project/order` · `/admin/project/{id}/mainMenu/order`(ids 순서대로 0, 1, 2 …, 다른 프로젝트 메인 메뉴 id 는 무시). 메뉴 순서는 메뉴 화면의 숫자 입력 그대로.
  - 관리 첫 화면 모양(사용자 요청): 삭제 · 이름 바꾸기 · 하위 추가는 이름 바로 옆 작은 단추(× · ✎ · +), 메인 메뉴는 프로젝트 아래 들여쓰기(왼쪽 선), 입력칸은 + / ✎ 를 눌렀을 때만 펼친다(`static/js/toggle.js`, aria-expanded · 첫 입력칸 초점). 새 프로젝트도 "+ 새 프로젝트" 단추로 펼친다. 메인 메뉴의 + 는 메뉴 추가 화면으로 이동.
  - 메뉴 추가의 connection 고르기: 전체 select(수백 개) 대신 검색어 입력 + 자동완성. `ConnectionNameService.search` = `LOWER(connection) LIKE LOWER(:keyword) ESCAPE '!'`(대소문자 무시, % · _ 는 글자 그대로, ESCAPE 는 MySQL 문자열 백슬래시 해석을 피해 '!'), 최대 30개 · 이름순. 화면은 `<input list>` + `<datalist>` 를 htmx 로 채움(`GET /admin/connection/search`, 입력 250ms 지연), 값이 바뀌면 테이블 select 가 `GET /admin/connection/tables` 로 다시 채움. 없는 이름이면 테이블 칸에 "connection 을 찾을 수 없습니다" 안내(오류 화면 아님).
  - 확인: `SettingControllerTest.newItemsGoLastAndReorderSaves`(맨 끝 추가 · 순서 저장 · USER 403), 브라우저(Playwright): 프로젝트 ↑ 키 · 드래그, 메인 메뉴 ↑ 키 모두 새로 고침 뒤에도 유지.

- 사용 화면 주소를 사람이 읽는 주소로(2026-11-17 사용자 결정): `/{프로젝트 code}/{메인 메뉴 code}/{메뉴 code}`, 접두어 없음. `/` = 볼 수 있는 프로젝트 목록, `/{프로젝트}` = 그 프로젝트 첫 메뉴로 이동. 사용 화면 왼쪽에 그 프로젝트의 메인 메뉴 · 메뉴(지금 메뉴 강조), 상단에 프로젝트 이름.
  - `code`: Project(전체 유일) · MainMenu(프로젝트 안 유일) · Menu(메인 메뉴 안 유일), 영문 소문자 · 숫자 · -, 1~50자. 프로젝트 code 는 앱 경로와 겹치면 거부(`MenuService.RESERVED_PROJECT_CODES`: admin · error · actuator · login · logout · oauth2 · js · webjars). 경로 변수에 같은 정규식을 걸어 점 든 정적 파일(/main.css, /js/x.js)과 안 겹친다.
  - DB: `schema-alter-postgresql-dynamiccrud2.sql` 2026-11-17 묶음(열 추가 · 기존 행 임시 code · NOT NULL · unique 인덱스), create 파일도 최종 모양으로. **적용은 사용자.**
  - 없는 code 는 404(`ResponseStatusException` 을 AccessDeniedAdvice 가 상태 코드 그대로 응답 — 공통 처리기는 500 으로 바꾼다).
  - 확인(2026-11-17, DDL 적용 뒤): 모듈 테스트 37/37, 브라우저(localdev) — 예약어 `admin` 거부 문구, `/{p}` → 첫 메뉴 이동, 사이드 메뉴 이동 · 현재 메뉴 강조, 없는 메뉴 404, 만든 QA 항목 정리.
- 패키지 정리(2026-11-17): `setting` → `admin`(controller · service), `use` → `menu`(controller · service), 공용 `setting.domain` · `setting.repository` → 최상위 `domain` · `repository`. 클래스 `SettingController` → `AdminController`, `SettingAccessInterceptor` → `AdminAccessInterceptor`, `UseController` · `UseService` → `MenuPageController` · `MenuPageService`. 템플릿 폴더 `setting/` → `admin/`, `use/` → `menu/`(`use/menu.jte` → `menu/page.jte`). git mv 로 옮겼다.
- 관리 화면 배치(2026-11-17 사용자 결정 "왼쪽 목록+오른쪽 상세"): 프로젝트가 수십 개여도 스크롤이 짧게 왼쪽은 프로젝트 이름 목록(끌어서 순서), 오른쪽은 고른 프로젝트 하나(`/admin?project={id}`, 없거나 모르는 id 면 첫 프로젝트)의 메인 메뉴 · 메뉴.
- 처리 후 이동(2026-11-17 자율 점검): 프로젝트 · 메인 메뉴 저장 / 삭제, 메뉴 삭제는 `/admin?project={id}` 로 돌아온다(전엔 늘 `/admin`). 메뉴 화면 "← 관리" · 메뉴 추가 "취소" 도 그 프로젝트로. 필드 화면은 여러 메뉴가 공유하므로 들어온 메뉴를 `?menu={id}` 로 들고 다녀 "← 메뉴" 로 돌아간다(저장 · 다시 읽기 뒤에도 유지).
- 메뉴 순서도 끌어서 바꾼다(`POST /admin/mainMenu/{id}/menu/order`), 메뉴 설정의 "순서" 입력칸은 뺐다. 새 메뉴는 맨 끝.
- 수정 · 삭제 쿼리 보호 강화: 파라미터가 하나라도 있으면 통과하던 검사를 "WHERE 뒤에 파라미터가 있어야" 로(`UPDATE t SET a = :a` 는 전체 행을 바꾸므로 거부). 저장 때뿐 아니라 실행 때도 검사해 이전에 저장된 쿼리도 막는다.
- 오류 화면: `error.jte`(상태별 안내 + 메뉴 목록 · 뒤로, 스택 트레이스 없음). 관리 화면의 없는 id(`findById().orElseThrow()`)는 500 대신 404(`AccessDeniedAdvice`). 사용 화면 빈 목록은 "조회 결과가 없습니다", 상단 · 본문 폭 `max-w-7xl` 통일, 좁은 화면에서 상단 줄바꿈, favicon 404 제거(`data:` 아이콘).
- htmx 요청 실패 알림(`static/js/htmx-errors.js`): 실패하면 화면이 그대로라 단추가 안 듣는 것처럼 보였다 - 401 은 화면을 다시 열어 로그인으로, 403 · 5xx · 연결 실패는 사유를 알린다. 오류 화면은 요청에 세션이 안 붙어 로그인해 있어도 "로그인" 이 보여 사용자 영역을 숨긴다(layout `showUser`).
- 작업 폼 레이어(2026-11-18 사용자 요청): 입력 · 수정 · 삭제 폼을 목록 위 `<dialog>` 모달로 띄운다(목록 위에 끼워 넣으면 스크롤 위치가 바뀌어 혼란). Esc · 바깥 클릭 · 닫기로 닫고, 실행 실패는 사유와 함께 열린 채 다시 그린다. 성공 응답은 `HX-Redirect`(새 이동 → 맨 위) 대신 `HX-Refresh`(새로 고침 → 스크롤 유지). 메뉴 설정 화면의 필드 화면 링크는 칸 제목 줄 "필드 · 라벨 설정" 단추로(흐린 글씨라 안 보였다).
- 목록 쪽 넘김(2026-11-18 사용자 요청): 기본 페이지 크기 20 → 15(20 행이면 화면을 넘어 쪽을 넘길 때마다 스크롤을 내려야 했고, 10 은 너무 적다는 의견. 기존 메뉴는 `schema-alter` 2026-11-18 묶음으로 20 · 10 인 것만 15), 이전 · 다음만 있던 것을 « ‹ 번호(10 쪽 묶음) › » 로 · 가운데 정렬. 관리 화면 폼 단추(저장 · 등록 · 취소 · 메뉴 삭제)는 오른쪽 정렬. 화면 폭은 제한 없이 100%(좌우 여백 px-4 · md 이상 px-6, 사용자 요청 2026-11-18), 메뉴 추가 폼은 메뉴 설정과 같은 2열.
- UI 컴포넌트(2026-11-18 사용자 지시 "컴포넌트로 관리해 재사용성을 높이는 걸 중요하게"): `jte/_components/` 에 alert · backLink · card(제목 + actions) · field(라벨 + 입력 + 도움말) · formActions(오른쪽 단추 줄) · codeInput · codeNameForm(주소 이름 + 이름 펼침 폼) · iconToggle · iconDelete · iconLink · dragHandle · emptyState · pagination · modal · typedInput(InputType 별 입력칸) · themeToggle. 화면은 이것을 불러 쓰고 같은 마크업을 다시 쓰지 않는다. 동작은 data-* 표식으로 공통 스크립트가 맡는다(`js/ui.js`: data-confirm · data-modal · data-theme-toggle, `toggle.js`, `reorder.js` - layout 이 모두 싣는다). 색은 `main.css` 토큰만 쓴다(템플릿에 색 값 0).
- 라이트 · 다크 테마(2026-11-18): 토큰은 원래 둘 다 있었다(`[data-theme="dark"]`). 상단 ☾/☀ 단추로 바꾸고 이 브라우저에 기억(localStorage `dc2-theme`), 처음엔 운영체제 설정을 따른다. 그리기 전에 head 의 짧은 스크립트가 정해 깜빡이지 않는다.
- 수정 불가 필드(2026-11-18 사용자 요청 "pk 값이나 unique 값은 보통 기본적으로 변경이 불가"): `DbField.updatable`(+ `updatableUserSet`, sortable 과 같은 규칙). PK · unique 키 · 자동 증가 컬럼은 등록 · "컬럼 다시 읽기" 때 false, 필드 화면 "수정" 칸으로 바꾼다(직접 바꾸면 다시 읽어도 유지). 수정(UPDATE) 작업에만 적용: 폼은 읽기 전용 + "수정할 수 없는 값입니다", 서버는 받은 값을 버리고 선택한 행의 원래 값을 넣는다(조회 결과에 그 컬럼이 없으면 원래 값을 몰라 거부). 입력(INSERT)은 새 행이라 모든 값을 받는다. 열이 생기기 전 등록된 필드는 NULL(안 정함)이라 사용 화면이 그 메뉴를 처음 쓸 때 한 번 다시 읽어 채운다. DDL = `schema-alter` 2026-11-18 "수정 가능" 묶음(앱보다 먼저).
- 메뉴 검색 설정 · 통합 검색(2026-11-18 사용자 결정): 쿼리를 고치지 않고 메뉴 설정 "검색" 카드에서 조회 결과의 실제 컬럼(계산 결과 · * 제외)을 검색칸으로 고른다. 메뉴마다 다르게(`Menu.searchSettings` jsonb `{"fields":[{"label","operator"}],"keyword":[label]}`, `SearchSettings`). 방식(`SearchOperator`)은 입력 형식이 정한다: 글자 = 포함(기본) · 시작 · 같음, 숫자 · 날짜 = 범위(기본, 부터~까지) · 같음, 참/거짓 = 같음. 통합 검색은 고른 글자 컬럼 중 하나라도 검색어를 포함(OR).
  - 실행: `ReadQueryRewriter` 가 WHERE 에 AND 로 붙인다. 결과 컬럼의 원래 식(`a.title`)을 써서 인덱스를 탈 수 있고, 원래 WHERE 는 괄호로 감싸 OR 와 섞이지 않는다. 해석 못 한 쿼리(UNION 등)는 감싼 바깥에서 결과 이름(`t."이름"`)으로 거르고, 그중 페이징을 직접 한(:_limit) 쿼리는 쪽 단위로 걸러져 틀리므로 거부. 글자 비교는 `LOWER(식) LIKE LOWER(:값) ESCAPE '!'`(대소문자 무시, 입력의 % _ ! 는 글자 그대로). 값 파라미터는 `_f0` · `_f0_to` · `_q`.
  - 화면: 검색 폼 하나에 통합 검색 · 설정 칸 · 쿼리 :파라미터 칸이 같이 나오고 "초기화" 가 있다. 주소 값은 `q` · `f.결과이름` · `f.결과이름.to` · `s.파라미터`, 정렬 · 쪽 · 엑셀 링크가 그대로 잇는다. 설정한 컬럼이 쿼리에서 사라지면 그 칸은 조용히 빠진다(검색 설정을 다시 저장하면 정리). 저장은 화면 값을 믿지 않고 고를 수 있는 컬럼 · 방식만 남긴다(`SearchService`).
  - DDL = `schema-alter` 2026-11-18 "메뉴 검색 설정" 묶음(앱보다 먼저).
- 2026-11-18 사용 · 관리 화면 다듬기(사용자 요청): 목록 머리 "전체 선택" 체크박스(`data-check-all`, 일부 선택이면 indeterminate). 검색해서 고르는 입력칸 `_components/combobox`(목록이 입력칸과 같은 시작 위치 · 폭, ↑↓ · Enter · Esc, 목록에서 고르지 않으면 제출 불가) - connection 은 입력마다 서버 검색, 테이블은 connection 을 고르면 한 번 받아 입력으로 거른다(테이블이 많아도). 메뉴 추가는 별도 화면 대신 관리 화면 메인 메뉴 "+" 가 펼치는 폼(`admin/menuCreateForm`, 실패하면 그 프로젝트 관리 화면에 사유). 사용자 결정: 1차 생성(주소 이름 · 이름 · connection · 테이블)은 관리 화면에서, 검색 · 작업은 생성 뒤 메뉴 설정 화면에서(한 번 별도 화면으로 되돌렸다가 이렇게 확정). 내장 브라우저로 확인(connection 검색 · 키보드 선택 · 테이블 거르기 · 전체 선택).
- 입력 순서 · 그 자리 수정(2026-11-18 사용자 요청): 화면 표시가 "이름 /주소" 라 모든 입력도 이름 → 주소 이름 순(`codeNameForm` · 메뉴 추가 · 메뉴 설정). 프로젝트 · 메인 메뉴 ✎ 는 인라인 수정: 그 줄이 같은 글꼴 · 크기의 글자 그대로 편집 상태가 된다(`codeNameForm inline`, `.inline-edit` = 테두리 없이 밑줄, 폭은 글자만큼 `field-sizing: content`). Enter · ✓ 저장, Esc · × 취소(초점은 ✎ 로). 표시 줄은 `data-hidden-by` 로 숨긴다(`toggle.js`). 함정: JTE 개발 모드는 지웠다 다시 만든 조각의 클래스를 못 찾는다(ClassNotFoundException) - 모듈의 `jte-classes` 를 지우고 재기동. 함정: 폼 안에 `<input name="id">` 가 있으면 `form.id` 가 그 입력을 돌려준다 - `getAttribute('id')` 로 읽는다.
- 메뉴 설정 화면 정리(2026-11-18 사용자 결정): 순서 기본 정보 → 작업 → 검색 → 메뉴 삭제. 기본 정보는 넓은 화면에서 이름 · 주소 이름 · 권한 · 페이지 크기(6rem) 한 줄 + 체크 둘 · 저장 한 줄. 검색은 표 대신 `_components/searchPick` 칸(☐ 컬럼 [방식] ☐ 통합)을 2~3열로, 고른 칸은 테두리 강조 · 안 고른 칸 방식은 흐리게, 저장은 카드 제목 줄. 공통 CSS: 입력칸 40→36px · 둥글기 6px · 마우스 올림 진하게 · 입력 중 기본색 테두리 + 둘레, 라벨 12px 회색, 카드 여백 20→16px · 카드 사이 16px. 평소 테두리는 3:1 대비 색 그대로.
- 인덱스 표 · 검색칸 인덱스 경고(2026-11-18 사용자 요청): 메뉴 설정에 "인덱스" 카드를 검색 위에 둔다(`_components/indexTable`, 메뉴 테이블 + 조회가 조인한 테이블, 이름 · 컬럼 순서(첫 컬럼 굵게) · PK/unique/일반 · 부분 · 식). `TableMeta.indexes`(`JdbcMetadataReader` 가 getIndexInfo 전체를 모은다). 검색 칸마다 고른 방식이 인덱스를 쓰는지 표시(`SearchService.indexUse`, 실행 쿼리 모양 기준): 포함 · 시작은 `LOWER(컬럼) LIKE` 라 못 씀, 같음 · 범위는 그 컬럼이 어떤 인덱스의 첫 컬럼이면 씀, 복합 인덱스 뒤 컬럼 · 식 인덱스는 못 씀. 방식을 바꾸면 option 의 data-* 로 문구를 바로 바꾼다(js/ui.js). 짐작이며 실제 사용은 DB 가 정한다(EXPLAIN 확인은 2차).
- 추가형 검색 설정 · 작업 편집 단추(2026-11-18 사용자 결정): 검색 설정은 고른 것만 보인다(`admin/searchSettings`). "검색칸" 은 줄(`_components/searchFieldRow`: ⠿ · 컬럼 · "검색 방식" · 인덱스 문구 · ×)을 "+ 검색칸 추가" combobox(후보 = 아직 안 고른 컬럼 + 인덱스 문구)로 더하고 ⠿ 로 순서(그 순서가 사용 화면 검색 폼 순서, 저장도 받은 순서), "통합 검색 대상" 은 칩(`_components/pickChip`). 안 고른 항목도 숨긴 채 입력 disabled 로 그려 두고 js/ui.js picklist 가 보이기 · 숨기기만 한다. PK · unique · uuid 컬럼은 기본 방식이 같음. 작업 표: 이름은 링크 색(`.link`), 줄 끝 "✎ 편집" · "삭제" 단추.
- 검색 방식 이름 · 기본값 · 단추 위치(2026-11-18 사용자 의견): 포함 · 시작 · 같음 → 부분 일치 · 앞부분 일치 · 완전 일치(범위는 그대로). 글자 컬럼 기본 방식은 완전 일치(인덱스를 쓰고, 부분 일치를 기본으로 둘 일은 드물다) - 그래서 "부분 일치는 인덱스를 못 씀" 문구는 그 방식을 고를 때만 보인다. 검색 설정 저장 단추는 다른 카드처럼 아래 오른쪽, 메뉴 삭제는 맨 위 오른쪽(스크롤 없이 보이게). combobox 거르기는 이름(data-label)만 본다(인덱스 이름이 든 설명에 걸려 엉뚱한 후보가 남던 문제).
- 쿼리 저장 전 DB 확인 · 쿼리 테스트(2026-11-18 사용자 요청 "저장할 때 해당 쿼리가 문제가 없는지 체크"): 조회는 한 행 시험 실행, 입력 · 수정 · 삭제는 실행하지 않고 검사(`DatabaseAdapter.check`: PostgreSQL · MySQL = `EXPLAIN`, MSSQL = `SET NOEXEC ON`, 파라미터는 모두 NULL). 문법 · 없는 테이블 · 컬럼(Spring `BadSqlGrammarException`)이면 오류로 저장 거부(`QUERY_CHECK_FAILED`), 그 밖의 실패(NULL 로 생기는 값 오류 등)는 경고만. 편집기 단추 "쿼리 다시 읽기" → "쿼리 테스트", 문제가 없으면 성공 안내(`QueryInspection.passed`, alert success). 관리 화면 메뉴 줄도 ✎ 로 이름 · 주소 이름 인라인 수정(`POST /admin/menu/{id}/rename`, `MenuService.renameMenu`).
- 상단 앱 이동 분리(2026-11-18 사용자 의견 "메뉴 · 관리가 등록한 메뉴 이름 옆이라 헷갈린다"): 왼쪽은 "Dynamic CRUD 2 / 프로젝트" 만, 앱 이동(전체 메뉴 · 관리)은 오른쪽에 묶은 단추(`_components/appNav`)로 테마 · 사용자 앞에 둔다. 지금 구역(경로가 /admin 이면 관리)은 눌린 모양 + aria-current.
- 메뉴 추가 뒤 흐름(2026-11-18 사용자 결정): 관리 화면 메인 메뉴 아래 폼으로 만들면 관리 화면(그 프로젝트)에 머물고 만든 메뉴가 목록에 보인다. 검색 · 작업 설정은 메뉴 이름을 눌러 메뉴 설정 화면에서.
- 테마 대비 · 화면 카드 통일 · 회귀 스크립트(2026-11-18): 다크 · 라이트 둘 다 관리 · 메뉴 설정 · 필드 · 쿼리 편집기 · 사용 화면 · 오류 화면에서 글자 대비를 쟀다(보통 4.5, 큰 글자 3). 미달은 상단 경로 구분자 "/" 하나(`text-base-content/40`, 라이트 2.63) → `hint` + `aria-hidden`. ☾ · × · ⠿ 같은 아이콘 글자는 4.18 로 비텍스트 기준(3) 통과. 필드 화면 · 쿼리 편집기도 메뉴 설정처럼 `_components/card`(제목 줄 오른쪽에 작업 단추)로: 필드 = 카드 제목 "connection · 테이블" + 읽은 시각 · 컬럼 다시 읽기, 쿼리 = "기본 정보" · "쿼리"(쿼리 테스트 단추는 제목 줄, 검사 결과는 카드 안). 전 흐름 회귀는 `~/.bluesky-qa/dc2-walk.js` 로 바꿨다: 로그인한 내장 브라우저 탭에서 실행해 실제 화면 HTML 의 폼을 읽어 값을 채우고 fetch 로 제출(Playwright 프로필 로그인은 만료, 세션 쿠키는 HttpOnly, 화면은 `X-Frame-Options: DENY` 라 iframe 불가, 내장 브라우저는 팝업도 막음). 콤보박스 · 전체 선택 · 레이어 같은 화면 JS 는 이걸로 못 보므로 내장 브라우저에서 손으로 본다. 결과 26단계 실패 0.
- 프로젝트 · 메인 메뉴 노출 · 권한별 목록(2026-11-18 사용자 요청): 메뉴의 "메뉴 목록에 표시" 처럼 `Project.enableDisplay` · `MainMenu.enableDisplay`(기본 TRUE). 관리 화면 이름 옆 눈 아이콘 단추(`_components/displayToggle`, 아이콘 = 지금 상태: 눈 보임 · 빗금 눈 숨김, 누르면 반대로. 처음엔 "숨기기 / 보이기" 글자였는데 옆 ✎ · × 와 맞춰 바꿈(사용자 의견). 이모지는 컬러라 currentColor SVG. 삭제는 × 대신 휴지통 SVG(`_components/iconDelete`, × 는 취소 · 빼기에만), 아이콘 칸 28→24px(클릭 목표 최소) + 이름 옆 아이콘은 `.icon-group` 으로 묶어 칸 사이 간격 0(그림 사이 12px → 4~8px)(사용자 의견 "아이콘 사이 간격이 넓다"). 이름 옆 "숨김" 배지는 아이콘과 겹쳐 뺐고, 단추가 없는 왼쪽 프로젝트 목록 · 메뉴 줄도 글자 대신 같은 빗금 눈(`_components/eyeIcon`, 읽는 이름 "메뉴 목록에서 숨김")(사용자 의견).
- 메뉴 설정 카드 이름(2026-11-18 사용자 의견 "더 이해하기 쉬운 단어"): 작업 → 쿼리 설정, 인덱스 → 테이블 인덱스, 검색 → 검색 설정. 관리 쪽 화면 문구의 "작업" 도 쿼리로(쿼리 추가 · 편집 화면 제목, 지울 때 확인, 필드 화면 안내, 오류 문구 STATEMENT_KIND_MISMATCH · READ_QUERY_REQUIRED). 사용 화면의 작업 레이어 이름 · 코드 주석은 그대로. `POST /admin/project/{id}/display` · `/admin/mainMenu/{id}/display`), 숨긴 것은 "숨김" 배지. 숨기면 사용 화면 전체 메뉴 · 사이드 메뉴(`MenuPageService.visibleTree`)에서 아래 메뉴까지 통째로 빠진다. 메뉴 주소로는 그대로 열리지만, 숨긴 프로젝트는 프로젝트 주소(`/{project}`)가 404 이고 메뉴 화면 사이드 메뉴가 비어 보인다. 전체 메뉴는 원래부터 메뉴 권한(`MenuAuthorizer.canRead`, 역할 계층)으로 거른다. 더해서 상단 "관리" 링크는 관리 권한(ROLE_ADMIN 이상)이 있을 때만 보인다(`appNav showAdmin`). DDL = `schema-alter` 2026-11-18 "프로젝트 · 메인 메뉴 노출 여부" 묶음(앱보다 먼저). 함정: JTE 는 boolean 값을 넣은 속성을 HTML 불리언 속성으로 그린다(`value="${flag}"` → 값 없음 → 400) - `String.valueOf` 로 넣는다. 확인: 테스트 42/42, 내장 브라우저에서 메인 메뉴 숨기기 → 전체 메뉴에서 빠짐 → 보이기로 복구, 단추 글자 대비(`.icon-btn-text` 75%) 두 테마 통과. 알게 된 것: 공유 테이블(이미 등록된 DbTable)에 DB 컬럼이 늘면 필드는 "컬럼 다시 읽기" 전까지 그대로인데, 자동 생성 쿼리는 지금 컬럼으로 만들어져 새 컬럼 값이 타입 변환 없이 문자열로 간다(boolean 컬럼이면 PostgreSQL 오류). → 고침(같은 날 사용자 요청): 이미 있는 테이블로 메뉴를 만들 때(`FieldService.registerOrGet`)와 작업을 저장할 때(`QueryService.save`) 컬럼을 다시 읽어 맞춘다(사용자가 고친 라벨 · 입력 형식 등은 유지, "컬럼 다시 읽기" 와 같은 동작). 테스트 `AdminServiceTest.sharedTablePicksUpNewColumns`(필드를 지워 옛 상태를 흉내 → 메뉴 생성 · 작업 저장 뒤 다시 생김), 43/43.
- 메인 메뉴 주소 · 추가 단추 위치(2026-11-18 사용자 지적): `/{project}/{mainMenu}` 라우트가 없어 404 였다 → 그 메인 메뉴에서 볼 수 있는 첫 메뉴로 이동(없으면 404), 관리 화면 메인 메뉴 `/코드` 도 링크. 이름 옆 `+` 는 같은 자리의 왼쪽 프로젝트 `+` 와 같은 단계로 읽혀(프로젝트 이름 옆 + 가 메인 메뉴를 만듦) 헷갈린다 → 한 단계 아래를 만드는 단추는 목록 맨 아래 글자 단추로(`_components/addToggle`): 메인 메뉴 목록 아래 "+ 메인 메뉴 추가", 각 메인 메뉴의 메뉴 목록 아래 "+ 메뉴 추가". 같은 깊이로(사용자 의견): 메인 메뉴 추가는 메인 메뉴 세로줄 목록 안 맨 아래, 메뉴 추가는 메뉴 목록과 같은 들여쓰기, `.add-row` 가 + 를 ⠿ 자리(1.75rem) · 글자를 이름 자리에 둔다(실측 x 일치). 이름 옆 아이콘은 그 항목 자체(✎ · 숨기기 · ×)만.
- 필드 관리 인덱스 · 화면 이름(2026-11-18 사용자 의견): 필드 관리 화면에도 "테이블 인덱스" 카드(필드 표 위, `_components/indexTable`, `FieldService.readMeta` 로 지금 DB 에서 읽음, 못 읽으면 표에 사유). 정렬 칸은 체크하면 그 컬럼이 어떤 인덱스의 첫 컬럼인지(`TableMeta.indexLeadingColumns`, 정렬 초기값과 같은 기준)로 "✓ 인덱스" / "⚠ 인덱스 안 씀" 을 보인다(`.sort-cell:has(input:checked) .sort-index`, CSS 만). 화면 이름: /admin = 프로젝트 관리, /admin/menu = 메뉴 관리, /admin/table = 필드 관리(제목 · ← 링크 · 메뉴 관리의 "필드 관리" 단추). 상단 앱 이동 "관리" 는 영역 이름이라 그대로.
- MySQL 인덱스 표 중복 · 데이터 출처 줄(2026-11-18 사용자 지적): MySQL 은 DB 이름이 catalog 이고 스키마가 없는데, 자동 생성 조회 쿼리의 `dynamiccrud_localdev.mainmenu` 앞부분을 스키마로 읽어 기준 테이블과 다른 테이블로 셌다(인덱스 표 2개, 조인 테이블 필드 등록 · 필드 찾기도 같은 위험) → `DbTable.schemaOf(qualifier)` 한 곳 규칙(없거나 catalog 와 같으면 이 테이블 스키마)으로 4곳(SearchService 2 · QueryService · MenuPageService) 통일, `DbTableTest`. 메뉴를 만들 때 고른 connection · 테이블과 DB 종류(`DatabaseAdapter.databaseType()`)는 메뉴 관리 기본 정보 · 필드 관리 카드 맨 위 "데이터 출처" 줄(`_components/sourceInfo`, 읽기 전용 라벨 + 값)로 - 우상단 작은 글씨는 부연처럼 보였다. "테이블" 이라는 말은 MongoDB 에선 "컬렉션" 이 맞다 → 2차 Mongo 어댑터 때 DB 종류가 대상 이름을 정하게(지금은 RDB 뿐이라 그대로).
- 위치 표시 · 대상 이름 · 메뉴 추가 폼(2026-11-18 사용자 의견): "← 돌아가기" 링크 대신 위치 표시(Breadcrumbs, `_components/breadcrumb` + `Crumb`, layout 의 h1 위)는 화면 단계: 프로젝트 관리(그 메뉴의 프로젝트를 고른 주소) › 메뉴 관리 › 테이블 필드 관리 · 쿼리 추가, 지금 화면 빼고 링크(처음엔 프로젝트 · 메인 메뉴 · 메뉴 이름 단계였는데 화면이 아닌 단계가 섞여 깊이가 이상하다는 의견으로 바꿈). 프로젝트 관리 화면은 제목과 같아 생략, 필드 관리를 메뉴 없이 열면 프로젝트 관리 › 테이블 필드 관리. `backLink` 컴포넌트는 지움. 대상 이름은 `DatabaseAdapter.targetNoun()`(기본 "테이블", 2차 Mongo 어댑터가 "컬렉션") → "테이블 필드 관리" · "테이블 인덱스" · 데이터 출처의 "테이블" 칸 · 메뉴 관리 단추. 메뉴 추가 폼은 최대 폭 64rem(max-w-5xl), 안내와 취소 · 추가 단추를 한 줄(formActions 안 왼쪽 안내).
- 안내 · 도움말 · 다국어(2026-11-19 사용자 요청): 메뉴 관리 맨 위에 접을 수 있는 "메뉴 관리 안내"(`_components/guide`: 기본 정보 → 쿼리 설정(조회 1개 + 입력 · 수정 · 삭제 여러 개, 각각 사용 화면 단추) → 검색 설정). 늘 깔리던 긴 안내 글은 원 안 물음표 ⓘ 툴팁(`_components/help`, CSS 만, aria-describedby)으로 - card · field 의 `help` 인자. 가로 스크롤 표 머리는 툴팁이 잘려 열 설명을 카드 제목 도움말로 모았다.
  - 다국어: 한국어(기본) + 영어, 관리 + 사용 화면 + 오류 문구 전부. 문구는 `messages.properties`(ko) · `messages_en.properties`, 읽기는 `config.Msg.t(key, args)`(자체 ResourceBundleMessageSource, UTF-8, 서버 기본 로케일로 새지 않음, 요청 밖은 한국어). 템플릿은 `@import static ...Msg.t` 후 `${t("key")}`, HTML 이 든 문구는 `$unsafe{}`. 함정: JTE 는 @param 기본값 식을 부르는 쪽에 넣으므로 기본값의 t() 는 정규 이름(`net.luversof.web.dynamiccrud2.config.Msg.t`)으로. 인자가 있는 문구는 MessageFormat(작은따옴표 '' · 숫자는 1,234 묶음), 인자 없는 문구는 그대로({0} 도 글자로 남아 JS 가 채움).
  - 언어 결정은 bluesky-boot `BlueskyLocaleContextResolver` BASIC(쿠키 `DC2_LOCALE` → 없으면 Accept-Language, 언어만 맞춰 en-GB → en_US). accept-header 처리기에 `set-representative-condition=overwrite-if-not-exists` 를 줘야 브라우저 언어가 먹는다(기본은 대표값이 안 됨). 상단 "English / 한국어" 단추 = `POST /locale`(`config.LocaleController`, 쿠키 저장 후 Referer 의 같은 사이트 경로로). 주소 파라미터를 안 쓰는 것은 사용 화면 검색 파라미터와 섞이지 않게.
  - 오류 코드 `QueryErrorCode` 문구도 `error.query.<이름>` 으로(예전엔 .properties 한글 깨짐을 피하려고 Java 에 뒀다) → `MessagesPropertiesTest`로 막는다. 2026-11-26 부터 파일은 ASCII 만(한글 · 기호는 유니코드 이스케이프 `\uXXXX`, 대문자 16진 - 사내 메시지 파일 관례, DMT2 `d98b9db` 와 같게): STS · Eclipse 가 .properties 를 ISO-8859-1 로 열어 날 UTF-8 한글이 깨져 보였다. 시험은 줄마다 ASCII 인지 · 풀었을 때 한글이 있는지 · U+FFFD 없는지 · ko/en 키 같은지. 새 문구는 native2ascii(또는 같은 변환)로 넣는다 - 값은 그대로라 Msg(UTF-8 로 읽음)는 바꿀 것이 없다. 저장되는 기본 이름(새 메뉴의 조회 쿼리 이름 "조회" 등)은 만든 사람 언어로 들어간다. JS 문구는 layout body 의 data-msg-*.
  - 확인: 모듈 테스트 51/51(영어 Accept-Language 렌더 · 언어 전환 쿠키 · 메시지 가드 추가). 내장 브라우저에서 English 전환 뒤 프로젝트 관리 · 메뉴 관리 · 테이블 필드 관리 · 쿼리 추가 · 사용 화면 · 전체 메뉴 · 404 를 훑어 남은 한글이 사용자 데이터뿐인지 확인. 언어 단추는 aria-label 없이 보이는 글자(English / 한국어, lang 속성)가 이름(보이는 글자와 읽히는 이름이 달랐다). 함정: STS 자동 빌드가 새 클래스(Msg)를 못 본 채 target/classes 를 덮어써 "Unresolved compilation problems" 500 - STS 프로젝트 새로 고침 뒤 재기동.
- 회귀 · 변경 이력(2026-11-19): 다국어 뒤 전 흐름 회귀(`~/.bluesky-qa/dc2-walk.js`, 화면 언어(html lang)에 따라 찾을 글자를 고름)를 한국어 · 영어 둘 다 실패 0. 스크립트는 빌드 산출물 `target/classes/static/js/` 에 잠시 복사해 페이지에서 불러 돌리고 지웠다(붙여 넣기 대신). 참/거짓 필수 선택 상자는 브라우저가 빈 값 제출을 막으므로 스크립트도 첫 값을 고른다. 대비 점검 `~/.bluesky-qa/dc2-contrast-audit.js`(색을 1px 캔버스로 읽음 - oklab 문자열을 숫자로 읽으면 가짜 1.1, 테마는 localStorage 로 바꾸고 새로 읽어 잰다) - 메뉴 관리(안내 상자 · 툴팁 펼침) 다크 · 라이트 미달 0.
  - 변경 이력 화면 `/admin/menu/{id}/history`(메뉴 관리 맨 위 "변경 이력" 단추, 위치 표시 프로젝트 관리 › 메뉴 관리 › 변경 이력): ChangeLog 를 최근 순 20건씩, 사용자(이력에 있는 사람 목록) · 작업(입력 · 수정 · 삭제 쿼리) · 기간(그날 포함)으로 거른다(GET, 쪽 넘김에 조건 유지). 넣은 값은 행마다 "라벨 값"(파라미터 이름이 컬럼이면 필드 라벨), 지운 쿼리는 "(지운 쿼리)". `ChangeLogRepository.search · countSearch · findUsernames`(조각 `ChangeLogQueries`, 아래 "이력 조회 SQL 은 코드에"), `admin.service.ChangeLogService`. 전후 비교(같은 날 사용자 요청): 수정 · 삭제는 고른 행(목록 값을 서명한 행 JSON = 사용자가 보고 바꾼 값)이 있으므로 DB 를 다시 읽지 않고 이력 bindValues 에 `before`(파라미터별 바뀌기 전 값) · `beforeRows`(고른 행 전체, 목록 제목별)를 함께 남긴다(`MenuPageService.run`). 화면은 수정 = 바뀐 칸만 `<del>이전</del> → <ins>새 값</ins>`(안 바뀐 칸은 흐리게), 삭제 = 지운 행 전체를 줄 그어, 입력 = 넣은 값(`ChangeLogService.Value`, 화면 낭독기용 "이전 값 · 새 값 · 지운 값"). 한계: 이전 값은 "목록에서 본 값" 이라 그 사이 다른 사람이 바꿨다면 DB 의 실제 직전 값과 다를 수 있다. 이 기능 전 이력은 이전 값이 없어 넣은 값만 보인다.
- 설정 변경 이력(2026-11-19 사용자 요청 "설정 변경 이력과 사용자 변경 이력을 구분해 저장 · 조회"): 데이터 변경(ChangeLog, 사용 화면 실행)과 따로 `SettingLog` 테이블에 관리 화면의 설정 변경을 남긴다. 대상(Target) = PROJECT · MAIN_MENU · MENU · QUERY · SEARCH · FIELD, 한 일(Action) = CREATE · UPDATE · DELETE · REORDER. changes = 바뀐 항목만 {항목: {before, after}}(서비스가 바꾸기 전 · 뒤 스냅숏을 떠 `SettingLogService.record` 가 비교, 바뀐 게 없는 UPDATE 는 안 남김, 쿼리 파라미터 설정은 키 순서로 맞춰 비교). 남기는 곳: MenuService(프로젝트 · 메인 메뉴 · 메뉴 만들기 · 바꾸기 · 지우기 · 순서 · 숨기기) · QueryService(저장 · 지우기) · SearchService(저장) · FieldService(필드 저장). "컬럼 다시 읽기" 는 설정 변경으로 치지 않는다. 남기기 실패(테이블 없음 등)는 로그 [SETTING_LOG_FAILED] 만 - 설정 저장은 그대로.
  - 조회: 메뉴 변경 이력 화면에 "메뉴 설정 변경 / 테이블 데이터 변경" 탭(`_components/tabNav`). 2026-11-24 사용자 결정: 메뉴 관리에서 오므로 **설정 변경이 왼쪽 · 기본**(tab 없음 = settings), 데이터 변경은 `tab=data`(예전엔 데이터가 기본 · 왼쪽). 설정 탭 범위 = 그 메뉴(메뉴 · 쿼리 · 검색) + 그 메뉴 테이블의 필드(필드는 테이블 단위라 같은 테이블을 쓰는 메뉴에 같이 보임). 프로젝트 관리의 "설정 변경 이력"(`/admin/project/{id}/history`) = 그 프로젝트 · 메인 메뉴 · 메뉴(지운 메뉴 포함) · 쿼리 · 검색. 사용자 · 대상 · 기간으로 거른다. 공용 조각 `admin/settingHistory`.
  - DDL = schema-alter 2026-11-19 "설정 변경 이력(SettingLog)" 묶음(앱보다 먼저).
  - 탭 이름(2026-11-19 사용자 결정 A안): "데이터 변경"이 누가 했는지(사용자/관리자)로 헷갈려, 무엇을 바꿨는지로 "테이블 데이터 변경"(몽고 등은 어댑터 targetNoun 으로 "컬렉션 데이터 변경") · "메뉴 설정 변경". 탭 아래 한 줄 설명(사용 화면에서 입력 · 수정 · 삭제 / 관리 화면에서 바꾼 설정). 프로젝트의 "설정 변경 이력" 은 그대로.
  - 사용자 거르기 = 고르기 상자 대신 자동완성(`_components/combobox` + `admin/userPicker`, 후보는 그 범위 이력에 있는 사람). 비우면 전체, 친 글자는 목록에서 골라야 제출(combobox `mustPick`, 정확히 다 친 이름은 고른 것으로 봄). combobox 에 처음 값(`value` · `label`) 인자 추가.
  - 입력칸 · 고르기 상자 높이 · 둥글기를 `.btn` 과 같게(2.5rem, --radius-field) - 찾기 줄에서 입력칸과 단추 높이가 달랐다. 모든 화면 공통.
- 메뉴 복사 · 조회 결과 미리 보기(2026-11-19 사용자 요청, 제안 1 · 3번):
  - 메뉴 복사 = 메뉴 관리 맨 위 "메뉴 복사"(ⓘ 설명) → 이름 · 주소 이름 펼침 폼(`_components/codeNameForm`, 기본값 "{이름} 복사본" · "{code}-copy") → `POST /admin/menu/{id}/copy`(`MenuService.copyMenu`). 같은 메인 메뉴 맨 끝에 같은 테이블 · 쿼리 전부(이름 · 원문 · 파라미터 설정 · 권한 · 순서) · 검색 설정 · 권한 · 페이지 크기 · 엑셀을 복사하고 **숨김**으로 만든다(고치기 전에 사용자 화면에 나오지 않게). 쿼리 id 는 새로(데이터 변경 이력은 원래 메뉴에 남음). 성공하면 새 메뉴 설정 화면, 주소 이름이 겹치면 원래 메뉴로 사유와 함께. 설정 변경 이력에 MENU CREATE + "복사한 메뉴".
  - 미리 보기 = 쿼리 화면 검사(`QueryService.inspect`)가 원래 하던 조회 시험 실행(1행, 결과는 버림)을 앞 `PREVIEW_ROWS`(5)행으로 늘려 결과를 `QueryInspection.preview` 로 돌려주고 "결과 미리 보기" 표로 보인다(전체 건수 함께, NULL 은 흐리게, 긴 값은 줄임 + title). 선택 조건은 빼고 필수 파라미터는 NULL 로 실행하므로 필수 파라미터가 있으면 "실제보다 적거나 없을 수 있음" 안내. 입력 · 수정 · 삭제는 실행하지 않으므로 미리 보기 없음. 저장 때도 같은 검사를 거치므로 5행 조회가 한 번 더 돈다(전체 건수 COUNT 포함 - 예전 1행 시험 실행도 같았다).
  - 확인: 모듈 테스트 56/56(`AdminServiceTest.copyMenuAndPreviewRead`), 내장 브라우저에서 복사 → 새 메뉴(숨김 · 쿼리 5개) → 조회 쿼리 미리 보기 5행(전체 6건) → 시험 복사본 삭제, 프로젝트 설정 이력에 추가 · 삭제 확인.
  - 고침(같은 날 사용자 결정 "로딩 표시 + 건수 세기 빼기"): 미리 보기는 건수를 세지 않는다(`DatabaseAdapter.sample`, total = -1, 큰 테이블 COUNT 회피 - 테스트 단추는 자주 누르므로). 저장 전 검사는 미리 보기 없이 예전처럼 `read`(건수 쿼리 포함) 한 행 - 건수 쿼리로 감쌀 때만 나는 오류(MySQL 하위 쿼리 결과 이름 중복 등)를 저장 전에 막으려고 건수는 저장 때만 센다.
- 전체 설정 변경 이력(2026-11-24 사용자 요청 "프로젝트 순서 변경이 이력에 안 보인다"): 프로젝트 순서 변경은 어느 프로젝트에도 속하지 않게(project_id 없이) 저장돼 메뉴 · 프로젝트 범위 화면 어디에도 안 걸렸다. `GET /admin/history`(`admin/allHistory`, 프로젝트 관리 오른쪽 위 그 프로젝트 "설정 변경 이력" 단추 옆 "전체 설정 변경 이력" 단추 - 처음엔 왼쪽 목록 아래 링크였다, 사용자 결정) = 범위 없이 전체(`SettingLogQueries` 범위 셋 다 null → 전체, 예전엔 아무것도 안 고름), 거르기는 같은 settingHistory 조각. 순서 바꾸기(프로젝트 · 메인 메뉴 · 메뉴)는 이제 **이전 순서도** 남겨 "이전 → 새 순서" 로 보이고, 같은 자리에 놓은 끌기는 남기지 않는다(REORDER 도 UPDATE 처럼 바뀐 게 없으면 생략).
  - 시험 정리: 전체 화면이 생기며 지운 시험 프로젝트의 이력(사용자 unknown · admin)이 드러났다. 시험이 프로젝트를 지운 직후 그 범위 이력을 지운다(`TestSettingLogs.purge`), 공유 테이블 필드를 바꿨다 되돌린 이력과 프로젝트 순서 시험 이력은 시험 시작 시각 이후 것만 지운다. 이미 쌓인 것(11-24 기준 unknown 830 · admin 67건)은 사용자가 지운다: `DELETE FROM "SettingLog" WHERE "username" IN ('unknown', 'admin');`
- 사용 화면 목록의 수정 단추 칸은 오른쪽 끝에 고정(2026-11-24 사용자 요청): 표가 가로로 스크롤돼도 늘 보이게 `.sticky-end`(position: sticky; right: 0, 배경 · 왼쪽 그림자). 삭제는 왼쪽 체크 + 목록 위 단추라 해당 없음. 메뉴 관리 화면 제목은 "메뉴 관리: 프로젝트 › 메인 메뉴 › 메뉴"(controller `menuPath`) - 메뉴 이름만으로는 어느 메뉴인지 가리기 어렵다.
- 사용 화면의 제목 · 검색 폼 · 총 건수(+ 입력/삭제/엑셀 단추) 줄은 세로 스크롤해도 화면 위에 붙어 있다(2026-11-24 사용자 요청): layout 사이드바 갈래가 제목과 `stickyHead`를 `.sticky-head`(position: sticky; top: 0, 배경 base-100, 음수 margin 으로 안 붙었을 때 자리 그대로)로 감싼다.
- 표 머리(thead)도 붙는 머리 바로 밑에 따라 붙는다(세로 스크롤은 화면 하나 - 표 상자 안 세로 스크롤은 사용자가 거절, 11-24). 가로 스크롤 상자(`.table-scroll`, overflow-x)는 세로 스크롤 상자도 되어 thead 의 position: sticky 가 화면 기준으로 듣지 않으므로, 상자를 view timeline(`view-timeline: --list`, inset 위 = `--sticky-head-h`)으로 삼고 `animation-range: exit-crossing 0px exit-crossing var(--thead-travel)` 동안 thead 를 `translateY(0 → --thead-travel)` 로 내린다. 스크롤 연동 애니메이션이라 스크롤과 같은 프레임에 움직인다(scroll 이벤트 + JS 는 한 프레임 늦어 떨린다). `--sticky-head-h` · `--thead-travel`(표 높이 - thead 높이)은 js/ui.js 가 ResizeObserver 로 잰다. `animation-timeline` 이 없는 브라우저(Firefox 기본값)는 @supports 로 빠져 thead 가 그냥 함께 올라간다. 숨긴 탭(브라우저 창 숨김)에선 렌더링이 멈춰 측정값이 늦게 바뀐다 - 시험할 때 화면을 한 번 그리게 한 뒤 재야 한다.
- 가로 스크롤은 표 상자 안에서만(2026-11-24 사용자 제보 "쿼리 편집 READ 화면에 가로 스크롤"): 원인 둘. (1) 참거짓 칸의 화면 낭독기용 글자(`sr-only` "예/아니오", position: absolute)가 기준 상자가 없어 표 스크롤 상자를 벗어나 페이지 오른쪽 끝(넓은 표의 그 위치)에 붙어 페이지 전체를 넓혔다 → 표를 감싼 `overflow-x-auto` 상자 7곳에 모두 `relative`(사용 화면 목록 · 이력 · 필드 · 컬럼 표 · 쿼리 검사 표도 같은 위험). (2) 메뉴 관리 레이어의 쿼리 폼이 `grid` 라 카드가 넓은 표 폭만큼 늘어났다 → `grid-cols-1`(minmax(0,1fr)). 확인: 단독 화면 · 레이어 · 사용 화면 모두 페이지 폭 = 화면 폭, 미리 보기 상자만 안에서 스크롤.
- 입력칸 덜 투박하게(2026-11-24 사용자 요청): 입력칸 · 고르기 상자 · 기본 단추 높이 40px → 36px(서로 같게 유지, 작은 것 32px 은 그대로), 둥글기 토큰 `--radius-field` 8px → 6px → **4px**(단추 · 입력칸 · 툴팁 등 공통), 같은 날 카드 · 안내 상자 `--radius-box` 14px → **8px**(사용자: 덜 둥글게). 배지 · 알약 모양(9999px)은 그대로. 테두리 색은 그대로 - 라이트 3.25:1 · 다크 3.34:1 로 칸 경계 하한(WCAG 1.4.11 3:1)에 붙어 있어 더 옅게 하지 않는다.
- 화면 동작 단추는 제목 줄 오른쪽 끝(2026-11-24 사용자 결정): layout 에 `titleActions`(제목 h1 줄 오른쪽). 프로젝트 관리 = "전체 설정 변경 이력"(제목 줄) · 그 프로젝트 "설정 변경 이력"(프로젝트 이름 줄 오른쪽 끝). 메뉴 관리 = 변경 이력 · 메뉴 복사(ⓘ) · 메뉴 삭제(제목 줄, 예전엔 제목 아래 따로 한 줄). 복사 입력 폼은 그 아래 펼침 그대로.
- 프로젝트 관리 아이콘 스르륵(2026-11-24 사용자 결정): 왼쪽 프로젝트 목록 · 오른쪽 트리(프로젝트 · 메인 메뉴 · 메뉴 줄)의 아이콘(✎ · 눈 · 휴지통)은 그 줄(.reveal-row)에 올리거나 초점이 들어올 때만 왼쪽에서 오른쪽으로 0.18초 페이드인(아이콘마다 0.03초씩 늦게), 벗어나면 거꾸로 사라진다(`main.css .reveal-icons`). 지킨 것: 숨김 상태 빗금 눈(displayToggle `is-hidden-state`)은 상태 표시라 늘 보임, 키보드 초점(:focus-within)에도 나타남, 마우스가 없는 기기(`@media (hover: hover)` 밖)는 늘 보임, 자리는 늘 차지해 글자가 안 밀림, 순서 손잡이 ⠿ 는 늘 보임(툴팁은 왼쪽 맞춤 - 가운데면 잘림). 사용자가 원한 효과라 reduced-motion 에서도 그대로.
- 아이콘 설명 툴팁(2026-11-24 사용자 요청): 아이콘 단추(✎ · 눈 · 휴지통 · ✓/× · 테마 · 검색칸 빼기 · 순서 손잡이)의 설명을 브라우저 title(늦게 · 다른 모양) 대신 `data-tip` 툴팁으로 - ⓘ 도움말과 같은 모양, 올리거나 키보드 초점일 때 바로 아래 가운데(`main.css .icon-btn[data-tip]`, CSS 만). 읽히는 이름은 그대로 aria-label. 흐린(비활성) 휴지통은 흐림을 그림(svg)에만 걸어 툴팁은 선명하게.
- 하위가 있으면 삭제 아이콘 비활성(2026-11-24 사용자 결정): 프로젝트는 메인 메뉴가, 메인 메뉴는 메뉴가 있으면 지울 수 없다(서버 `HAS_CHILDREN`, 함께 지우지 않음). 예전엔 휴지통을 누르면 "지울까요?" 확인 뒤에 오류가 떴다 → `_components/iconDelete` 에 `blockedReason`: 휴지통은 보이되 흐리게(aria-disabled, opacity .4), 눌러도 아무 일 없음, 까닭("메인 메뉴 N개 / 메뉴 N개를 먼저 지워야 …")은 title + aria-describedby. `disabled` 속성은 초점을 못 받아 키보드 · 낭독기가 까닭을 못 들어 쓰지 않는다. 서버 검사는 그대로(동시에 추가된 경우).
- 프로젝트 관리 화면 아이콘 정리(2026-11-24 사용자 요청): 왼쪽 프로젝트 목록 줄마다 눈 아이콘(보이기 · 숨기기, `_components/displayToggle` 에 `back` - 끝나면 보던 프로젝트로, 예전엔 숨김일 때만 빗금 눈 표시). 오른쪽 트리의 메뉴 줄도 메인 메뉴와 같은 아이콘 묶음 ✎ · 눈 · 휴지통(`POST /admin/menu/{id}/display` 새로 - `MenuService.setMenuDisplay`, 메뉴 설정 화면 "메뉴 목록에 표시" 와 같은 값 · 설정 이력 남김. 삭제는 있던 `/admin/menu/{id}/delete`, 메뉴 관리 화면과 같은 확인 문구). 확인: 모듈 테스트 63/63(`settingChangesAreLogged` 에 메뉴 눈 · 프로젝트 back 검증), 브라우저에서 다른 프로젝트를 숨겼다 되돌려도 보던 프로젝트 그대로.
  - 같은 날: 왼쪽 프로젝트 목록 주소(`/code`)를 오른쪽 끝이 아니라 이름 바로 옆에(사용자 요청), 눈 아이콘만 오른쪽 끝(`ml-auto`).
  - 같은 날 바꿈(사용자 지적 "숨긴 프로젝트 아래 메뉴가 주소로 보인다"): 숨긴 프로젝트 · 메인 메뉴 아래 메뉴는 주소로도 404(`MenuPageService.resolve` - 목록 · 폼 · 실행 · 엑셀 모두 여기를 거친다). 위 2026-11-18 항목의 "메뉴 주소로는 그대로 열린다" 는 이걸로 대체. 메뉴 자체의 "메뉴 목록에 표시" 는 그대로 목록에서만 빠지고 주소로는 열린다(복사한 메뉴가 숨김으로 만들어지므로 보이기 전에 주소로 확인). 테스트 `MenuPageControllerTest` 숨김 구간을 404 기대로.
  - 2026-11-26 바꿈(DMT2 3bd1c96 UDP-6558 반영): 메뉴 자체를 숨겨도 주소 · JSON 호출 · 엑셀 모두 404(관리자도). 예전엔 목록에서만 빠져 주소만 알면 숨긴 메뉴 데이터를 볼 수 있었다. 바로 위 "메뉴는 주소로는 열린다" 를 대체 - 복사한 메뉴(숨김으로 만들어짐)는 보이기로 바꾼 뒤 확인한다. 눈 아이콘 안내(`displayToggle menu`) · 가이드 문구도 맞춤.
- 프로젝트 관리 메뉴 줄에 복사 아이콘(2026-11-24 사용자 결정): ✎ · 복사 · 눈 · 휴지통. 아이콘 줄엔 이름 입력 폼을 펼칠 자리가 없어 묻지 않고 바로 만든다 - `POST /admin/menu/{id}/copy` 를 이름 · 주소 이름 없이 보내면 `MenuService.copyMenu(id)`: 이름 "{이름} 복사본"(100자 안), 주소 이름은 같은 메인 메뉴에서 겹치지 않게 `{code}-copy`, `-copy2`, `-copy3` …(원래 code 를 42자로 잘라 50자 안). 숨김으로 만들어 새 메뉴 설정 화면으로 간다(거기서 이름을 고치고 켠다). 메뉴 관리 화면 제목 줄 "메뉴 복사"(이름을 묻는 폼)는 그대로 둔다 - 설정을 보다가 복사하는 길. 확인: 모듈 테스트 64/64(`copyMenuAndPreviewRead` 에 src-copy 가 있으면 src-copy2), 브라우저에서 아이콘 → "테스트 메뉴 2 복사본" · testmenu2-copy · 숨김 설정 화면(확인 뒤 지움).
- 메뉴를 다른 메인 메뉴로 옮기기(2026-11-24 사용자 결정 - "다른 위치로 복사" 는 복사 뒤 옮기기로. 복사 폼에 위치 선택칸을 두는 안은 접음): 프로젝트 관리 트리에서 메뉴를 ⠿ 로 다른 메인 메뉴 목록에 끌어 놓거나, 손잡이 초점에서 목록 맨 위 · 아래에서 ↑ · ↓ 를 한 번 더 누르면 이웃 메인 메뉴(앞 목록 끝 / 뒤 목록 처음)로 간다.
  - 화면: 메뉴 목록 `<ul>` 에 `data-reorder-group="menu"`(같은 그룹 목록끼리 항목을 넘겨 줌, js/reorder.js). 메뉴가 없는 메인 메뉴도 빈 목록을 두고, 끄는 동안만 점선 놓을 자리(`body.reorder-dragging`, main.css). 옮기면 받은 목록이 순서를 보내고 성공하면 화면을 다시 그린다(사용 화면 주소 · 메인 메뉴 지우기 막힘이 바뀜) - 키보드로 옮겼으면 다시 그린 뒤 그 손잡이로 초점을 돌린다(sessionStorage `reorder-focus`).
  - 서버: 따로 주소를 두지 않고 기존 `POST /admin/mainMenu/{id}/menu/order` 가 받는다 - ids 에 다른 메인 메뉴의 메뉴가 있으면 같은 프로젝트일 때만 옮기고(다른 프로젝트 id 는 무시: 화면에 안 나오고 테이블 · 연결이 맞는지 모름), 주소 이름이 그 메인 메뉴에 이미 있으면 막는다(DUPLICATE_CODE → 409 + 사유 글자, js 가 alert 후 서버 상태로 다시 그림). 설정 변경 이력에 메뉴 UPDATE(메인 메뉴 전 · 후, `setting.field.mainMenu`) + 받은 메인 메뉴 REORDER. 떠난 메인 메뉴는 순서를 다시 매기지 않는다(남은 메뉴끼리 앞뒤는 그대로).
  - 확인: 모듈 테스트 64/64(`copyMenuAndPreviewRead` 에 옮기기 · 주소 이름 겹침 막힘), 브라우저에서 "테스트 메뉴 2" 를 키보드 ↓ 로 테스트2 맨 앞 → ↑ 로 되돌림, 끌기로 테스트2 "메뉴" 다음 → 끌어 되돌림(주소 /test/test2/testmenu2 ↔ /test/test1/testmenu2). 409 알림은 화면에서 안 봄(서비스 테스트로만).
- 테이블 필드 변경 이력(2026-11-26 사용자 결정): 필드 화면에서 고친 값은 전부터 남았다(FIELD UPDATE, 대상 id = 테이블, 프로젝트 없음 - 필드는 여러 메뉴 · 프로젝트가 공유). 메뉴 변경 이력 · 전체 이력에만 보이던 것을 필드 화면에서도 본다.
  - 필드 화면 제목 줄 오른쪽 "변경 이력" → `GET /admin/table/{id}/history`(`admin/tableHistory.jte`, `SettingLogService.tablePage` - 범위는 `target = 'FIELD' AND targetId = 테이블`). 들어온 메뉴(`menu`)는 경로 · 찾기 폼(숨은 칸) · 처음으로에 들고 다닌다. 대상이 필드 하나라 대상 고르기는 뺐다(`settingHistory` 의 `showTarget = false`).
  - "컬럼 다시 읽기"(`FieldService.resync`)도 남긴다: 전 · 후를 `SettingLogService.columns`(DB 컬럼마다 `column.{이름}` = 타입 + auto_increment, 가상 필드 · DB 에 없는 필드 제외)로 비교해 새 컬럼(전 값 없음) · 사라진 컬럼(후 값 없음) · 타입 변경을 한 건으로. 바뀐 게 없으면 남지 않는다(record 의 빈 UPDATE 건너뜀). 항목 이름은 "컬럼 {이름}"(`setting.field.column`). 쿼리 저장 때 자동으로 컬럼 · 가상 필드를 채우는 것(`registerOrGet` · `ensureColumns` · `ensureVirtual`)은 사람이 한 설정이 아니라 남기지 않는다.
  - 확인: 모듈 테스트 68/68(`columnsShowAddedRemovedAndRetyped`, `settingChangesAreLogged` 에 필드 화면 링크 · 다시 읽기 무변경 · 이력 화면 menu 유지), 브라우저에서 StockPriceHistory 필드 화면 → 변경 이력(기존 라벨 변경 1건), 다시 읽기 무변경은 안 남음. 실제 컬럼이 바뀐 다시 읽기는 단위 테스트로만.
- 표준 문자 타입이 아닌 컬럼 값(2026-11-26 사용자 요청, 개선 제안 1): jsonb 오류를 고친 뒤 inet · enum · interval · 배열 등도 같은 오류("column is of type X but expression is of type character varying")라 일반화. `ValueConverter` 는 글자 칸 값의 컬럼 타입이 문자 타입 목록(text · varchar · bpchar · char · name · nvarchar · nchar · ntext · *text …, 소문자 비교)에 없으면 `adapter.UntypedText` 로 표시만 하고, DB 별 처리는 어댑터가 한다(`JdbcDatabaseAdapter.bindable` - PostgreSQL 은 `SqlParameterValue(Types.OTHER)` 로 타입 없이 보내 서버가 컬럼 타입으로 바꾸고, MySQL · MSSQL 은 글자 그대로). 조회 · 엑셀 · 미리 보기 · 동시 수정 확인 · 입력/수정/삭제 바인딩 모두 거친다. 예전 json · jsonb 대소문자 짐작은 지웠다(MSSQL 타입 이름도 소문자라 틀릴 수 있었다). 확인: 테스트 69/69(`nonStringColumnsBindUntyped`, 실제 PG 에서 `jsonb = :v` 를 UntypedText 는 통과 · 글자는 실패 - `postgresBindsUntypedTextAsColumnType`, 읽기만).
- JSON 컬럼 입력 · 표시(2026-11-26, 개선 제안 2): json · jsonb 컬럼(`ValueConverter.isJson`, 대소문자 무관)은
  - 작업 폼: 한 줄 입력칸 대신 여러 줄(`.textarea-autogrow` · 고정폭, `_components/typedInput` 의 `json`). 열 때 보기 좋게 펼치고(들여쓰기 2), 보내기 전에 JSON 형식 검사 - 틀리면 브라우저 입력 검사 말풍선("JSON 형식이 맞지 않습니다", setCustomValidity 라 htmx 도 보내지 않는다). 빈 값은 NULL 이라 검사하지 않음. 값 목록(options)이 있으면 셀렉트가 먼저.
  - 목록 칸: 한 줄로 줄인 고정폭 작은 글자, 마우스를 올리면 펼친 전체(title, `_components/cellValue` 의 `json`).
  - 펼치기 · 검사는 js/ui.js `[data-json]`(서버는 그대로 - DB 가 틀린 JSON 을 거부하면 실행 실패로 보인다). 주의: 펼친 모양으로 저장되므로 json(글자 보존) 컬럼은 저장할 때 줄바꿈 모양이 바뀐다(jsonb 는 DB 가 정규화).
  - 확인: 테스트 69/69, 브라우저 test2/menu 수정 폼에서 searchSettings 펼침 · `{bad` 는 막힘 · 원래 값 저장 성공(서버 오류 없음).
- 행 자세히(2026-11-26, 개선 제안 3 - 컬럼이 많아 가로 스크롤이 긴 표): 줄 끝 고정 칸(.sticky-end)에 늘 "자세히" 아이콘(수정 작업이 없어도 칸을 둔다). 누르면 레이어(`rowDialog`)에 그 행 모든 컬럼을 제목 | 값 두 칸으로(좁으면 한 칸, 값은 줄바꿈 · 긴 글자 그대로, JSON 은 펼친 것). 값은 화면에 이미 있는 것(칸 title = 원래 값)이라 서버에 다시 묻지 않는다(js/row-detail.js, 이 화면만 불러옴).
  - 레이어 단추는 목록의 같은 단추를 대신 누른다: 수정 = 그 행 줄 끝 단추(`data-row-action`), 삭제 = 이 행만 고른 뒤 목록 위 선택 삭제 단추(`data-row-delete`, 글자는 "선택 삭제" 대신 작업 이름). 레이어를 닫고 작업 폼 레이어가 뜬다 - 권한 · 확인 · 실행은 기존 흐름 그대로.
  - 확인: 테스트 69/69, 브라우저 test2/menu 에서 자세히(12개 컬럼, searchSettings 펼침) → 수정 폼 뜸, → 삭제 확인 폼 뜸(이 행 하나만 골라짐, 실행은 안 함).
- 데이터 변경 되돌리기(2026-11-26, 개선 제안 4): 메뉴 관리 → 변경 이력 → 데이터 변경 탭 줄 끝 "되돌리기"(확인 후) → `POST /admin/menu/{id}/history/{logId}/revert` → `MenuPageService.revert`. 결과는 탭 위 알림(성공 · 사유).
  - 수정: 같은 수정 쿼리를 바뀌기 전 값(bindValues.before)으로 다시 실행. 그 전에 행의 지금 값이 그때 넣은 값(rows)과 같은지 본다(메뉴 테이블 컬럼인 파라미터만, 값 비교는 바인딩 값 - 숫자 1 = 1.0, JSON 은 내용으로). 다르면 "그 뒤 다시 바뀌어 되돌리지 않음", 행이 없으면 기존 "행이 없음".
  - 삭제: 지운 행(beforeRows)을 메뉴 테이블에 다시 넣는다(`DatabaseAdapter.insertRows`, 컬럼 이름은 번호 파라미터로). 목록에 있던 메뉴 테이블 컬럼만 넣으므로 목록에 없던 NOT NULL 컬럼이 있으면 DB 가 거부하고 사유가 보인다(되돌리기의 한계 - 목록에 모든 컬럼을 두면 온전히 복구). 같은 키 행이 이미 있으면 막는다. 자동 증가 값을 직접 넣지 못하는 DB(MSSQL IDENTITY · PG GENERATED ALWAYS)도 실패.
  - 입력은 되돌리지 않는다: DB 가 만든 키(자동 증가 · 기본값)를 이력이 모를 수 있어 지울 행을 확실히 못 가린다. 행 키(ROW_KEY) 없는 작업 · 전후 값이 없는 옛 이력(2026-11-19 전)도 단추 없음.
  - 권한: 관리 화면(설정 관리 권한) + 그 작업 실행 권한(`authorizer.checkRun`). 실행한 사람이 아니라 지금 실행할 수 있는 사람이 되돌린다.
  - 되돌린 것도 데이터 변경 이력(같은 쿼리 id, bindValues.revertOf = 되돌린 이력 id, 줄에 "되돌리기" 배지). 한 이력은 한 번만(`ChangeLogRepository.findRevertedIds`, 되돌린 줄은 "되돌림"), 되돌리기 이력은 다시 되돌리지 않는다. 삭제 되돌리기 이력은 다시 넣은 행(컬럼별)이라 입력처럼 보인다.
  - 확인: 테스트 69/69(`insertSearchUpdateExportDelete` 에 수정 되돌리기 · 두 번째는 "이미 되돌린" · 행이 지워진 뒤 되돌리기 막힘 · 목록에 code 가 없는 삭제 되돌리기는 DB 거부 사유), 브라우저 test2/menu 데이터 변경 탭에서 값이 같은 수정 하나를 되돌림 → "1행을 되돌렸습니다", 그 줄 "되돌림", 위에 "되돌리기" 줄.
- 쿼리 실행 계획(2026-11-26, 개선 제안 5): 쿼리 화면 "테스트" 옆 "실행 계획" → `POST /admin/menu/{id}/query/explain`(`QueryService.explain` → `DatabaseAdapter.explain`) → 입력칸 아래 `#q-explain` 에 계획 줄(`admin/queryExplain.jte`). 실행하지 않는다(ANALYZE 없음) - 수정 · 삭제 쿼리도 계획만.
  - PostgreSQL: 파라미터가 있으면 값과 무관한 일반 계획 - `PREPARE dc2_explain AS …($n)` + `SET plan_cache_mode = force_generic_plan` + `EXPLAIN EXECUTE dc2_explain(NULL, …)`, 끝나면 DEALLOCATE · RESET(풀 연결). NULL 을 그냥 넣으면 플래너가 `= NULL` 을 접어 "One-Time Filter: false" 만 보여 쓸모가 없다. `EXPLAIN (GENERIC_PLAN)`(16+)은 PgJDBC 가 확장 프로토콜이라 "bind message supplies 0 parameters" 로 실패해 쓰지 않는다. 안 되면(12 전 · 타입을 못 정하는 파라미터) NULL 로 본 계획 + "실제와 다를 수 있음" 안내. 시도마다 트랜잭션 없이(PG 는 실패한 트랜잭션을 더 못 쓴다 - 한 트랜잭션으로 묶었다가 연결이 닫혔다).
  - MySQL · MariaDB: `EXPLAIN FORMAT=TREE`(MySQL 8.0.16+), 안 되면 표 EXPLAIN 을 "열=값" 줄로, 파라미터는 NULL(안내 붙음). 실제 MySQL 로는 아직 확인 안 함. MSSQL 은 "지원하지 않음"(SHOWPLAN 을 따로 보내야 함).
  - `:이름 → $n` 은 `QueryText.toNumberedParameters`(문자열 · 주석 · `::` 형 변환 제외, 같은 이름 같은 번호).
  - 확인: 테스트 71/71(`toNumberedParametersKeepsLiteralsAndCasts`, 실제 PG `explainShowsPlanWithoutRunning` - 파라미터 조건이 `$1` 로 보임 · UPDATE 도 계획만), 브라우저 testmenu1 조회 쿼리 → `Index Scan using "StockPriceHistory_pkey"`.
- 엑셀로 여러 행 넣기(2026-11-26, 개선 제안 6): 사용 화면 입력 작업 단추 옆 "엑셀로 {작업}" → 레이어(`menu/bulkForm`): ① 양식 내려받기(`GET …/action/{id}/bulk-template`, 첫 줄 = 작업 폼과 같은 칸 제목) ② 둘째 줄부터 한 줄 = 한 행(최대 `BULK_MAX_ROWS` 1000) ③ 올리기(`POST …/action/{id}/bulk`, multipart).
  - 처리(`MenuPageService.runBulk`): 칸 이름은 화면 제목이나 파라미터 이름. 먼저 모든 행을 작업 폼과 같은 규칙(기본값 · 필수 · 형식 - `inputValue` 를 run 과 같이 씀)으로 검사해 틀린 행이 하나라도 있으면 아무것도 넣지 않고 "N행: 사유" 목록(N = 엑셀 줄 번호). 다 맞으면 한 트랜잭션으로 넣고 이력 한 건(bindValues.rows 전부 + bulk=true). DB 가 거부하면(중복 키 등) 전부 취소되고 사유.
  - 행을 고르는(ROW_KEY) 작업 · 입력이 아닌 작업은 400(폼 · 양식 · 실행 모두). 권한은 그 작업 실행 권한.
  - 칸 값 변환(`menu/controller/ExcelRows`): 날짜 칸 → 2026-11-26(시각 있으면 2026-11-26T13:20), 수 → 지수 없는 글자(3.0 → 3), 참거짓 → true/false, 수식 → 계산된 값, 모두 빈 줄은 뺀다. xlsx 가 아니면 "읽을 수 없음". 파일 크기는 Spring 기본 multipart 한도(1MB).
  - 확인: 테스트 73/73(`ExcelRowsTest`, `insertSearchUpdateExportDelete` 에 단추 · 양식 · 수정 작업 400 · 틀린 행 1개면 아무것도 안 넣음("3행:") · 맞으면 2행 넣음). 화면: 테스트 프로젝트 메뉴들엔 입력 작업이 없어 브라우저로는 못 봄(조각 렌더링은 MockMvc 로 확인).
- 식별자를 늘 따옴표로(2026-11-26, DMT2 `636d45c` UDP-6559 와 같게 - 사용자 요청): `SqlDialect.quote` 가 단순 이름도 감싼다(`"id"` · `` `code` `` · `[name]`). 예약어 컬럼(MySQL 8 의 virtual, order, user …)에서 자동 생성 쿼리 · 정렬 · 검색 · DB 값 전체 · 동시 수정 확인 · 되돌리기(insertRows)가 문법 오류 나던 것. 감싸도 MySQL · MSSQL 은 대소문자 규칙이 같고, PostgreSQL 은 메타데이터의 실제 이름을 감싸므로 같은 이름. 예전 "예약어는 안 감싼다" shortcut 은 지웠다.
  - JSqlParser 에 `withSquareBracketQuotation(true)`(파싱 · 검색 조건 붙이기 둘 다): 기본값은 `[이름]` 을 배열 첨자로 읽어 MSSQL 쿼리 재작성(건수 쿼리 ORDER BY 제거 등)이 빠졌다.
  - 이미 저장된 메뉴 쿼리는 바뀌지 않는다(예약어 컬럼이 있으면 쿼리 편집에서 직접 감쌀 것). 새로 만드는 쿼리 초안만 모두 따옴표.
  - 확인: 테스트 74/74(기대 문자열 갱신 + `parsesSquareBracketNames`), 브라우저 테스트 프로젝트 6개 메뉴 목록 · 정렬 이상 없음.
- API 문서(2026-11-26 사용자 요청): springdoc-openapi(`springdoc-openapi-starter-webmvc-ui`, 버전은 상위 BOM - 3.2.1). Swagger UI `/swagger-ui.html`, 명세 `/v3/api-docs/{use|admin}`.
  - 묶음 두 개(`Dynamiccrud2SpringDocConfig`): use = 사용 화면 JSON 호출(`MenuApiController`), admin = `/admin/api/**`. 둘 다 `@RestController` 만 싣는다(화면 @Controller 주소 · 엑셀은 빠짐).
  - 사용 화면 API 설명: 조회의 검색값(s. · f. · q)은 이름이 메뉴마다 달라 쿼리 파라미터로 싣지 않고 설명에 적었다(메뉴별 실제 이름은 메뉴 관리 "JSON 호출" 카드). 오류는 `{"error": 사유}` + 400/401/403/404.
  - 가이드(/guide) JSON 호출 절에 API 문서 링크.
  - 호출은 로그인 세션(쿠키). CSRF 는 공통 보안 설정(`ClientUserSecurityConfig`)이 꺼 두었다 - 화면 폼과 같은 조건이라 따로 바꾸지 않았다.
  - 확인: 테스트 75/75(`apiDocsListOnlyJsonCalls` - use 명세에 작업 주소 · 화면 주소 없음 · Swagger UI 200).
- 관리 JSON 호출 `/admin/api/**`(2026-11-26 사용자 요청 - 관리 화면은 폼 · redirect 뿐이라 호출용이 없었다): `admin/api` 패키지에 컨트롤러 셋, 관리 화면과 같은 서비스 · 검사 · 설정 변경 이력을 그대로 쓴다. `/admin/**` 라 `AdminAccessInterceptor` 가 설정 관리 권한으로 막고, 되돌리기는 그 작업 실행 권한도 본다.
  - 구조(`AdminStructureApiController`): GET /projects(트리), 프로젝트 · 메인 메뉴 POST · PUT · DELETE · PUT …/display · PUT …/order, GET /connections · /connections/{name}/tables, 메뉴 POST /main-menus/{id}/menus · GET · PUT /menus/{id} · PUT …/display · POST …/copy(본문 없으면 자동 이름) · DELETE · PUT /main-menus/{id}/menus/order(다른 메인 메뉴 메뉴 id 면 옮기기).
  - 쿼리 · 검색 · 필드(`AdminQueryApiController`): GET …/queries/draft?kind · POST …/queries/inspect · …/explain · POST · PUT · DELETE …/queries/{id}, GET · PUT /menus/{id}/search, GET /tables/{id} · PUT /tables/{id}/fields/{column} · POST /tables/{id}/resync.
  - 이력(`AdminHistoryApiController`): GET /history · /projects/{id}/history · /menus/{id}/history/settings · /menus/{id}/history/data · /tables/{id}/history(화면과 같은 거르기 · 20건 쪽), POST /menus/{id}/history/{logId}/revert.
  - 응답은 엔티티가 아니라 `AdminApiViews` 레코드(ProjectView · MenuView(작업 · 검색 설정 포함) · TableView …) - 저장 구조가 바뀌어도 호출 모양은 그대로. 만들기 201, 상태만 바꾸는 것 · 지우기 204.
  - 오류(`AdminApiAdvice`, 이 패키지만 · 공통 처리기보다 먼저): `{"error": 사유}` - 400(검사 · 주소 이름 겹침 · 하위가 있어 못 지움 · 되돌리기 불가 · DB 거부 · 본문을 못 읽음), 404(없는 id - 서비스의 orElseThrow). 401 · 403 은 AccessDeniedAdvice. 사용 화면 API 도 본문을 못 읽으면 같은 `{"error"}`(예전엔 공통 ProblemDetail).
  - 함정: Jackson 3 은 빠진 기본형(boolean) 값을 거부한다 - 검색칸 입력은 `Boolean required` 레코드(`SearchField`)로 받아 바꾼다. 메뉴 바꾸기 · 필드 바꾸기 본문의 boolean · int 는 다 보내야 한다(Swagger 예시 그대로).
  - 가이드 JSON 호출 절에 관리 API 줄.
  - 확인: 테스트 77/77(`AdminApiControllerTest` - 구조 전 과정 · 형식 오류 400 · 하위 있음 400 · 없는 id 404 · USER 403, 쿼리 초안 → 검사 → 추가 → 바꾸기 → WHERE 없는 수정 거부 → 지우기, 검색 저장 · 틀린 방식 400, 필드 라벨 바꾸고 되돌림, 각 이력 · 없는 이력 되돌리기 400; `apiDocsListOnlyJsonCalls` 에 admin 묶음), 브라우저 Swagger UI 묶음 두 개 · 태그 셋, `GET /admin/api/projects` 200.
- 메뉴 추가 연결 실패 안내(2026-11-26, DMT2 `3badce9` · `d98b9db` UDP-6560 과 같게): 대상 connection 에 닿지 않거나 계정이 거부되면 오류 화면(500) 대신 "connection {0} 에 연결하지 못했습니다. 서버 주소 · 계정 설정을 확인하세요"(`admin.menuCreate.connFailed`), 원인은 WARN 로그.
  - 테이블 목록(`/admin/connection/tables`): 목록 자리에 안내(대상 DB 만 읽으므로 DataAccessException 전부).
  - 메뉴 저장(`createMenu` - 테이블 컬럼 읽기): 프로젝트 관리 화면으로 돌아가 안내. `DataAccessResourceFailureException`(연결 실패 · CannotGetJdbcConnection)만 - 설정 DB 오류(다른 DataAccessException)는 대상 연결 탓이 아니라 그대로 오류.
  - 관리 JSON 호출(`GET /admin/api/connections/{name}/tables` · `POST /admin/api/main-menus/{id}/menus`)도 같은 안내를 400 `{"error"}` 로. DMT2 와 달리 Mongo 어댑터가 없어 MongoException 은 없다.
  - 확인: 테스트 79/79(`AdminTableOptionsTest` - 목으로 연결 실패, 예외 이름 · 원문이 안 보임, 설정 DB 오류는 그대로).
- connection 자동완성에 읽은 출처(2026-11-26, DMT2 `40ed7c9` UDP-6560 과 같은 자리): 이름 옆 흐린 글자(`_components/pickOption` 의 `combobox-sub`, 고르면 입력칸엔 이름만) = 그 연결 정보를 읽는 connectionInfo **reader key**(지금은 `postgresql-datasource`).
  - 처음엔 url 앞부분으로 DB 종류(postgresql · mariadb …)를 보였다가 사용자 결정으로 바꿨다: Mongo(`mongodb-mongoclient`) 등 저장소가 늘 수 있으니 "어디서 읽었나" 를 보인다 - DMT2 의 mongodb · dbconn 이 바로 출처 표시다.
  - `ConnectionNameService.searchWithSource`: JDBC 표(`DataSourceConnectionConfig`)에서 찾은 이름에 켜진 `*-datasource` reader key(`bluesky-boot.connection-info.readers.*.enabled`, `ConnectionInfoProperties` 빈)를 붙인다. 여럿 켜져 있으면 이어서(어느 reader 표인지 이름만으로 못 가림), 없으면 표시 없음. Mongo 출처를 합칠 때(2차) 그쪽 이름은 그 reader key 로.
  - 관리 JSON 호출 `GET /admin/api/connections` 는 `[{name, source}]`.
- 사용 화면 높이 줄이기(2026-11-26, DMT2 `ed3997b` UDP-6561 반영): 왼쪽 메뉴가 있는 화면은 제목(메뉴 이름) h1 을 `sr-only` 로(화면 낭독기 · 탭 제목에는 남김). 검색 폼은 카드 상자 없이 한 줄, 라벨은 입력 왼쪽(`_components/field inline`). 같은 메인 메뉴의 다른 메뉴 링크에 지금 검색 조건(`searchQuery` = q · s. · f.)을 붙여 넘긴다(그 메뉴에 없는 칸은 무시). 테스트: 링크 `href=".../menu?s.name=..."`, 변경 이력은 같은 시각이면 순서가 바뀌어 작업 id 로 찾게.
- 표 머리 따라 붙기를 JS 로(2026-11-26, DMT2 `c2baba7` UDP-6561 반영): 위 11-24 항목의 CSS view timeline 방식을 대체. DMT2 에서 애니메이션이 진행되지 않아 바꾼 것과 같게, `js/ui.js` 가 scroll 마다(그 자리에서 - 다음 프레임으로 미루면 떨림) 붙는 머리 아래 끝 - 목록 상자 윗변 만큼 thead 를 `translateY`(표 아래 끝까지), resize · ResizeObserver 는 rAF 로 모은다. 머리 칸 글자 한 줄(`thead th { white-space: nowrap }`). 확인: 브라우저에서 스크롤 1000px → 머리 아래 끝 144 = thead 위 144. 망 표시(net-profile 배지)는 DMT 전용 망 개념이라 옮기지 않았다.
- MongoDB 메뉴(2026-11-26, DMT2 `9dfa599` UDP-6560 이식 - 사용자 결정 "java 코드만, mongo connectioninfo 는 설정 안 된 상태로 가정"): `adapter/mongo/MongoDatabaseAdapter` · `MongoQuery` 는 DMT2 HEAD 판 그대로(distinctValues · insertRows · 실행 계획 미지원 안내 포함).
  - 작업 원문 = JSON 하나(collection · operation[find · insertOne · updateMany · deleteMany] · filter · sort · projection · document · update). `:param` 은 파싱 뒤 타입 있는 BSON 값으로 바인딩. 필드 = 최근 문서 100개의 최상위 필드, 행 키 `_id`(글자 → ObjectId, `ValueConverter` dataType `objectId`). 수정 · 삭제는 filter 에 파라미터 필수(`NO_PARAMETER_IN_MONGO_FILTER`).
  - 연결: bluesky-boot-autoconfigure-connectioninfo-mongodb. Mongo 이름 = `bluesky-boot.connection-info.mongodb.connection-map` 키(properties-mongoclient reader), 데이터베이스도 그 설정에서(MongoClient 는 모른다). `DatabaseAdapterResolver` 가 이름이 그 목록에 있으면 `ConnectionInfoUtil.getConnection(name, MongoClient.class)` 로 Mongo 어댑터, 아니면 JDBC. 앱 설정엔 Mongo 연결이 없다(application.properties 에 켜는 법만 주석) - 지금은 모두 JDBC. 연결 정보를 Mongo 컬렉션에서 읽는 mongodb-mongoclient reader 는 database 를 알 수 없어 쓰지 않는다(shortcut 주석).
  - 연결 자동완성에 Mongo 이름을 합친다(출처 글자 `properties-mongoclient`, 같은 이름이면 Mongo 가 이긴다). 메뉴 추가 테이블 목록 · 메뉴 저장 · 관리 API 의 연결 실패 안내는 `MongoException` 도 잡는다. 쿼리 편집 안내는 원문이 `{` 로 시작하면 Mongo 안내.
  - 시험: `MongoQueryTest`, `MongoDatabaseAdapterWriteTest`(Testcontainers mongo:8.2), `MongoMenuFlowTest`(시험에서만 reader · loader · connection-map 을 켜고 관리 등록 → 사용 화면 조회 · 검색 · 입력 · 수정 · 삭제). Docker 가 없으면 건너뛴다. 91/91. DMT2 의 sb 실제 Mongo 읽기 시험은 옮기지 않았다.
- 상단 프로젝트 고르기 열고 닫기 효과(2026-11-26 사용자 요청 "사라질 땐 효과가 없다"): `.nav-dropdown::details-content` opacity + `content-visibility .15s allow-discrete`(닫혀도 전환 동안 보인다), 목록만 `translate`(::details-content 에 주면 absolute 목록 기준 상자가 바뀜), 열 때는 `@starting-style`. 짧은 흐림이라 reduced-motion 에서도 둔다. 확인: headless Chrome(파일 페이지)으로 표집 - 열기 30ms opacity .35 → 200ms 1, 닫기 30ms .89 → 60ms .26 → 200ms 0 · hidden. 내장 브라우저 창이 숨겨지면 문서 시간이 멈춰 전환을 잴 수 없다.
- 왼쪽 메뉴 접기 · 툴팁(2026-11-26, DMT2 `69e8830` · `fa9702f` UDP-6561 반영): 왼쪽 메뉴 위 단추로 접고 편다 - 접으면 단추만 남는 2rem 띠(폭 · 투명도 전환), 화살표가 돈다, 좁은 화면은 내용만 숨김. 상태는 localStorage `dc2-sidebar`, 그리기 전에 layout 머리 스크립트가 `<html data-sidebar="collapsed">` 로 맞춘다(깜빡임 없음). 프로젝트 고르기 ▾ 는 펼치면 뒤집힌다(`.nav-dropdown-caret`), 이름 글자 선택 막음(`select-none`). DMT2 의 목록 keyframes 효과 · 닫힘 JS(`ba3e5cb`)는 위 `::details-content` 전환으로 이미 되어 옮기지 않았다. 목록 오른쪽 고정 칸(`.sticky-end`) 단추 툴팁은 가로 스크롤 상자에 잘려 단추 왼쪽에 띄운다. DMT2 `2cc57ac` · `c86b572`(seasoning select 폭 100%)는 dc2 `.select` 가 폭을 정하지 않아 해당 없음.
- 목록 표시 여부(2026-11-26, DMT2 `80c8003` · `57aa061` · `b78cadd` UDP-6597 의 1차 가져오기 외 부분): `Menu.listHidden`(TEXT, 결과 이름 한 줄에 하나, NULL = 모두 보임). 끈 칸은 사용 화면 목록의 `th` · `td` 에 `hidden` - 행 자세히 · 수정 · 엑셀에는 그대로(개인정보처럼 아예 감출 칸은 조회 쿼리에서 뺀다는 안내). 메뉴 설정 카드 "목록 표시 여부"(체크 = 보임, `all` - `show` 를 저장, `MenuPageService.listColumns` = 조회 결과 이름 · `*` 면 테이블 필드 · 이미 숨긴 이름, 쿼리 실행 없음, 연결 실패면 카드 뺌), 설정 이력 스냅숏 · 메뉴 복사에 포함. 메뉴 설정 화면 순서: 쿼리 · 목록 표시 여부 · 검색 설정 · 테이블 정보 · JSON 호출. DDL: `schema-alter-postgresql-dynamiccrud2.sql` 2026-11-26 묶음(**적용은 사용자**, 앱 배포 전).
- 기본 쿼리 다시 만들기(2026-11-26, DMT2 `90c5952`): 쿼리 편집 단추 → 확인 → `POST /admin/menu/{id}/query/draft`(`QueryService.draft`, 관리 API 의 draft 와 같음)로 DB 테이블의 지금 컬럼으로 그 종류 기본 쿼리를 만들어 입력칸 조각(`admin/queryTextBox`)만 바꾼다. 저장은 [저장]. 못 만들면(PK · unique 없음 · 연결 실패) 입력칸은 그대로 두고 사유 안내.
- 조회 쿼리 고침 둘(2026-11-26, DMT2 `e4fec06` · `59fa9fd` UDP-6597): ① 해석 못 한 조회(JSqlParser 5.4 는 컬럼 이름 `number` 를 못 읽는다)는 `SELECT * FROM (원문) t` 로 감싸는데, 끝 ORDER BY 가 이름 정렬이면 바깥(`t.이름`)으로 옮기고 건수 쿼리에서는 뺀다 - MSSQL 이 감싼 안쪽 ORDER BY 를 막아 조회가 실패하던 것(`ReadQueryRewriter.TrailingOrderBy`, 괄호 · 따옴표 밖 마지막 ORDER BY, 뒤에 페이징이 있거나 식 정렬이면 그대로, 화면 정렬이 있으면 원래 정렬은 버림). ② FROM 이 하위 쿼리 하나인 조회 분석이 `NoSuchElementException` 으로 죽던 것 - 테이블 수를 하위 쿼리까지 세는데 목록은 비어 있었다. 구버전 MSSQL ROW_NUMBER 쪽은 dc2 에 없어 해당 없음.
- 칸 제목 규칙 보완(2026-11-26, DMT2 `6327f68` 의 `FieldService` 부분): 공백 · 한글 등 이름에 못 쓰는 글자가 든 별칭(`AS 'Job 실행 ID'`)은 사람이 읽으라고 쓴 메뉴의 칸 제목이라 테이블 필드 라벨(여러 메뉴 공유)보다 먼저. 이름 모양 별칭(`AS applyTime`)은 예전처럼 라벨이 먼저(위 11-19 제목 순서의 예외).
- 목록 표시 여부에서 칸 순서 바꾸기(2026-11-26, DMT2 `26e7b32`): 카드의 칸마다 ⠿ 손잡이(`_components/dragHandle`, `js/reorder.js` - `data-reorder-url=""` 라 바로 저장하지 않고 [저장] 때 `all` 순서로 보낸다). 저장하면 순서가 바뀐 경우 조회 쿼리 SELECT 칸 순서를 바꿔 `QueryService.save`(저장 검사 그대로)로 저장한다 - 칸 순서는 쿼리 순서 그대로라는 원칙 유지. SELECT 칸 글자 처리(칸 나누기 · 결과 이름 · 순서, 원래 서식 보존)는 `adapter/jdbc/SelectColumns`. `SELECT *` · Mongo · 별칭 없는 식이 있는 쿼리는 손잡이 없이 안내 문구(`admin.menu.listHidden.helpNoOrder`).
- 넓은 레이어 폭(2026-11-26, DMT2 `b102543`): 쿼리 편집 레이어(`.modal-wide`) 72rem → 80rem(화면 폭 따라 최대 1280px). DMT2 는 seasoning lg(900px)에서 검사 표 글자가 세로로 꺾이던 문제였고, dc2 는 이미 1152px 라 증상은 없었지만 폭을 맞췄다.
  - 확인: 테스트 80/80(`sourceIsEnabledDatasourceReaderKey` - 꺼진 · Mongo reader 는 빼고 여럿이면 이음, 자동완성 조각에 `dynamiccrud2 <span class="combobox-sub">postgresql-datasource</span>`), 브라우저 실제 연결 16개 모두 `postgresql-datasource`.
  - 확인 중 환경(Boot 4.2 로 올린 뒤): `~/.m2` 의 `bluesky-test` jar 가 Logback 1.6 대응 전 판이라 테스트 컨텍스트가 "Circular variable reference ... CONSOLE_LOG_PATTERN" 로 실패 → `mvn -pl bluesky-test install` 로 해결. 앱은 예전 bluesky-boot 기준으로 컴파일된 `bluesky-client-common` jar 때문에 화면이 500("BlueskyClientResponseErrorHandler does not implement ResponseErrorHandler") - 다른 서비스가 그 jar 를 잡고 있어 설치가 막혀, dc2 만 client-common · client-user target/classes 를 `additional-classpath-elements` 앞에 두고 띄웠다. 다른 서비스를 내린 뒤 `mvn -pl bluesky-client/bluesky-client-user -am install` 하면 이 우회는 필요 없다.
- 쿼리 작성 화면 정리(2026-11-23 사용자 요청): 테이블 인덱스 표를 메뉴 관리 화면에서 쿼리 추가 · 편집 화면(`admin/queryEditor`, 레이어 · 단독 둘 다)의 쿼리 입력칸 위로 옮겼다 - WHERE · ORDER BY 를 쓸 때 보도록. 표는 조회 쿼리가 쓰는 테이블들(`SearchService.setup(menu).tables()`, AdminController.editor). 메뉴 관리 안내(검색 설정)는 "검색칸마다 보이는 인덱스 사용 여부" 로 고쳤다. 쿼리 입력칸은 처음 세 줄, 줄이 늘면 내용만큼 커진다(`.textarea-autogrow`: CSS `field-sizing: content`, 최대 70vh 뒤로는 스크롤, 모르는 브라우저는 js/ui.js 가 높이를 맞춤).
  - 같은 날 추가 요청: 인덱스와 함께 **컬럼 정보**도(입력 · 수정 · 삭제 쿼리를 그 기준으로 쓰도록). 카드 "테이블 정보" 에 테이블마다 컬럼 표(`_components/columnTable`: 이름 · 타입(길이 - 글자 · 이진 · 고정 소수 타입만) · NULL 허용(NOT NULL 배지) · 기본값 · 키(PK · unique · 자동 증가) · 주석) → 인덱스 표 순서. `SearchService.TableIndexes` 에 읽은 `TableMeta`(meta) 를 함께 싣는다(못 읽으면 null). `indexTable` 에 heading 인자. 쓰지 않게 된 `admin.menu.index.help` 문구는 지웠다.
  - 테이블 정보 카드는 접을 수 있다(`_components/card` 에 collapsible · open - details/summary, 제목 줄 ⓘ 는 눌러도 접히지 않게 js/ui.js). 카드는 `_components/tableInfo` 하나로 두 곳: 쿼리 추가 · 편집 화면(펼침)과 메뉴 관리 화면 검색 설정 바로 위(접힘). 접고 편 상태는 브라우저에 기억해 다음에도 그대로(사용자 요청 같은 날: card `rememberKey` → `data-remember-open`, js/ui.js 가 localStorage `card-open:tableInfo` 에 저장 · 복원, 레이어 조각은 htmx:afterSwap 에서 복원). 두 곳이 상태 하나를 같이 쓴다(사용자 결정 - 한 번 접으면 어디서나 접힘). 기억이 없을 때만 처음 상태(쿼리 화면 펼침 · 메뉴 관리 접힘), 저장소를 못 써도 처음 상태.
  - 검색 설정 위치 결정(사용자 동의): 메뉴 관리에 그대로. 검색칸은 메뉴 설정이고(쿼리는 두고 검색만 자주 바꿈), 후보가 **저장된** 조회 쿼리의 결과 컬럼에서 나오므로 쿼리를 확정한 뒤 다듬는 순서가 맞다. 쿼리 편집으로 옮기면 화면이 너무 길어진다. 대신 검색 설정 위에 테이블 정보(접힘)를 둔다 - 검색칸마다 인덱스 사용 여부는 이미 보인다.
  - 확인 중 발견(환경): `bluesky-boot-connectioninfo` 가 11-22 커밋(타입 지정 조회를 상위 타입까지)으로 바뀌었는데 `~/.m2` jar 는 11-11 판이라, 모듈 테스트가 `dynamiccrud2TransactionManager` "Property 'dataSource' is required" 로 컨텍스트 기동 실패(dc2 가 `getConnection(key, DataSource.class)` 로 null). 코드 문제 아님. 진짜 원인(같은 날 확인): CI 는 reposilite(http://reposilite.bluesky.local/snapshots/)에 11-22 판을 올렸지만 로컬 Maven 3.8+ 이 **http 저장소를 기본 차단**(`maven-default-http-blocker`)해 reposilite 를 조회하지 않고 central-portal-snapshots 의 11-11 판을 썼다. 해결(사용자 결정): reposilite 는 https 로도 열려 있어 pom 의 주소를 `https://reposilite.bluesky.local/...` 로 바꿨다(bluesky-parent · bluesky-boot-dependencies · bluesky-cloud-parent, CI 는 k8s 내부 미러를 써서 영향 없음). 설정 파일 없이 `-U` 로 reposilite 판을 받고 모듈 테스트 62/62. 갱신 중엔 떠 있는 서비스가 `*-SNAPSHOT.jar` 를 잡아 "Could not transfer"(.tmp 남음)가 나므로 서비스를 내리고 한다.
- 테이블 필드 관리 "필수" 열 제거(2026-11-19 사용자 결정): 필드의 required 는 처음 등록 때 DB NOT NULL 을 읽어 넣을 뿐 어디에도 쓰이지 않았다(입력 폼 필수 = 쿼리 화면 파라미터 설정, 기본값도 DB 메타에서 추정). 입력 필수는 작업마다 다를 수 있어(입력 필수 · 수정 선택) 쿼리 화면 한 곳에서 정한다. 화면 · 저장(`updateField`) · 설정 이력 스냅숏에서 뺐고, DB 열 · 엔티티는 DB NOT NULL 기록으로 남김(DDL 없음). 열 설명은 표 머리 ⓘ 툴팁(라벨 · 입력 형식 · 최대 길이 · 정렬 · 수정, 오른쪽 두 열은 `help alignEnd` 로 왼쪽으로 펼침), 카드 도움말은 공통 설명 + "필수는 쿼리 화면에서".
- 조회 ↔ 테이블 필드 관리 연계(2026-11-19 사용자 요청 1~3번): 쿼리는 "무엇을 가져올지", 필드는 "어떻게 보일지".
  - 필드 찾기 · 제목 규칙을 `FieldService.lookup(base)`(결과 컬럼 → 필드 · 그 테이블, 조인 테이블은 한 번만 읽음) · `FieldService.headerOf(field, alias, fallback)` 하나로 모았다 - 사용 화면(MenuPageService) · 검색 설정(SearchService) · 쿼리 화면(QueryService)이 같은 규칙. 예전엔 앞 둘에 같은 코드가 따로 있었다.
  - 제목 순서 변경: 필드 라벨 → 별칭 → DB 주석 → 컬럼 이름(예전: 별칭이 라벨을 말없이 덮음). 별칭은 라벨이 없을 때만 제목, 결과 이름(검색 · 정렬 키)은 그대로 별칭. 별칭 · 라벨 둘 다 있는 기존 메뉴는 열 제목이 라벨로 바뀐다(엑셀 머리글도).
  - 쿼리 화면 결과 컬럼 표: 화면 제목 · 입력 형식 · 정렬(사용 화면과 같은 규칙, `QueryService.ColumnView`). 필드 설정은 테이블 단위라 줄마다 링크가 아니라 "결과 컬럼" 제목 오른쪽 "테이블 필드 관리" 단추 하나(조인해 등록된 테이블이 여럿이면 테이블마다 ": 이름", `QueryInspection.fieldTables`) - 사용자 결정(처음엔 줄마다 "필드 설정" 링크였다). 별칭이 라벨에 밀리면 그 안내, 아직 필드가 없으면 "저장하면 등록", * 는 설정을 보일 수 없다는 안내. 결과 미리 보기도 같은 제목 · 표시 형식.
  - 목록 표시 형식 `_components/cellValue`(필드 입력 형식대로, 보이기만 - 행 JSON · 엑셀 · 이력은 원래 값): 일시 "날짜 시각", 날짜 앞 10자, 참거짓 ✓ / –(낭독 예 / 아니오), 숫자 오른쪽 정렬. **자릿수 구분(1,234)은 하지 않는다** - id · 연도 · 코드 같은 숫자 컬럼이 많아 오히려 읽기 어렵다(사용자에게 알림, 필요하면 필드별 설정으로).
  - 확인: 모듈 테스트 57/57(`FieldHeaderTest` 제목 순서, `AdminServiceTest` 결과 컬럼 연결), 브라우저에서 쿼리 화면 표 · 링크(createdDate 줄 강조) · 사용 화면 목록(숫자 오른쪽, ✓/–, 일시) 확인.
- 처리 중 표시(2026-11-19, 앱 공통 `js/ui.js` + `main.css [aria-busy]`): 누른 단추에 aria-busy → `.btn` 글자 앞에 도는 고리 + 다시 누르기 막음(pointer-events). 폼 제출은 입력 검사 · 확인([data-confirm])을 통과한 submit 만, 단추를 disabled 로 하지 않는다(name · value 가 빠짐) - 대신 같은 폼 두 번째 submit 을 막고 bfcache 로 돌아오면 푼다. htmx 는 요청 보낸 단추(폼이면 누른 단추)에만, 입력칸(자동완성) 요청은 표시 안 함. 사용자가 원한 표시라 reduced-motion 에서도 멈추지 않고 천천히(1.6초) 돈다. 엑셀은 링크라 해당 없음.
- 이력 조회 SQL 은 코드에(2026-11-19 사용자 요청 "코드로 옮겨줘"): STS 가 `@Query` 문자열을 MySQL 문법으로 검사해 PostgreSQL 큰따옴표 이름 · `CAST(:x AS UUID) IS NULL OR ...` 를 [SQL_SYNTAX] 오류로 표시했다(실행은 정상). Spring Data JDBC 저장소 조각(`XxxQueries` + `XxxQueriesImpl`, 저장소가 조각 인터페이스를 함께 extends)으로 옮겨 `NamedParameterJdbcOperations` 로 실행한다: `ChangeLogRepository`(search · countSearch · findUsernames), `SettingLogRepository`(같음), `DbTableRepository.findIdByLocation`. 메서드 모양은 그대로라 호출부 · 테스트 무변경. 거르기는 `repository.SqlWhere` 가 값 있는 조건만 AND 로 잇는다(CAST 꼼수 불필요, Instant 는 Timestamp 로 묶음). 목록은 정렬 · 쪽으로 id 만 고른 뒤 `JdbcAggregateOperations.findAllById` 로 읽고 id 순서로 되돌린다 - jsonb · enum 변환을 Data JDBC 그대로 쓰려고. 확인: 모듈 테스트 55/55, 재기동 뒤 설정 이력 거르기(대상 · 기간) 결과 확인.
- 확인(2026-11-17): 모듈 테스트 37/37(메뉴 순서 · 없는 id 404 · WHERE 검사 사례 추가), 브라우저 전 흐름 28단계(페이지 넘김 · 정렬 유지 포함)(관리 → 작업 쿼리 3종 → 필드 → 사용 화면 입력 실패 안내 · 입력 · 검색 · 정렬 · 엑셀 · 수정 · 선택 삭제 → 정리).
- 알려진 것(코드 아님): 로컬 실행은 `~/.m2` 의 bluesky-client-user jar(2026-10-28)를 쓰는데 이 jar 는 세션 속성 삭제를 서버에 보내지 않는다(2026-11-03 수정 이전) → 읽은 오류 안내(flash)가 만료(180초)까지 다시 보일 수 있다. client-user 를 새로 설치하거나 reactor 로 빌드하면 사라진다. 2026-11-18 재확인: 다른 로컬 서비스가 그 jar 를 잡고 있어 `install` 이 덮어쓰지 못했다 → dc2 만 `-Dspring-boot.run.additional-classpath-elements=<bluesky-client-user/target/classes>` 로 새 클래스를 앞에 두고 띄우면 오류 안내가 한 번만 보인다(확인함, `directories` 는 Boot 4 플러그인에서 안 먹음).
- 테스트는 프로파일이 필요하다: `mvn -o -pl bluesky-web/bluesky-web-dynamiccrud2 test "-DargLine=-Dspring.profiles.active=localdev"`(없으면 configserver import 누락으로 컨텍스트 기동 실패).

## 1차 마무리 상태(2026-11-17)
- 미커밋: `bluesky-web/pom.xml`(`<module>` 추가) + `bluesky-web-dynamiccrud2/` 전체(`src/main/frontend/node_modules` 는 gitignore). user 역할 변경은 사용자가 커밋함(33127074).
- 배포 설정(2026-11-17, 미커밋) — 옛 dynamiccrud 와 같은 방식:
  - bluesky-argocd-applications: `bluesky-project_subchart/subchart/bluesky-web-dynamiccrud2/values.yaml`(포트 30125 · 호스트 `dynamiccrud2.web.bluesky.local` · `TZ=Asia/Seoul`, datetime-local 입력을 서버 타임존으로 해석하므로), `argocd_application/values.yaml` 에 앱 추가 `envs: [work]`(catalog · connectioninfo 연결이 work PC DB 에만 있음), `bluesky-cloud-gateway/values.yaml` ingress 호스트 추가.
  - bluesky-cloud: 게이트웨이 `application-k8sdev.yaml` 라우트 `bluesky-web-dynamiccrud2` → `lb://bluesky-web-dynamiccrud2-service`.
  - 이미지: CI(`k8s-ci` 프로필)가 jib 있는 모듈을 모두 zot 으로 push → 별도 설정 없음, buildNumber 는 `subchart_work/buildInfo-bluesky-project` 공유.
  - config server: 추가할 것 없음(앱 설정은 jar 의 application.properties, connectionInfo 읽기 설정은 공통 config).
  - 확인: `helm template` 으로 work 에만 앱 등록 · Service/Deployment(포트 · 이미지 · TZ) 렌더 확인. hosts 에 `dynamiccrud2.web.bluesky.local` 은 이미 있음.
  - 반영 순서: bluesky-project push → CI 이미지 → bluesky-cloud push → 게이트웨이 이미지 → bluesky-argocd-applications push → 루트 앱 sync(앱 생성 · 게이트웨이 재배포). 이미지보다 앱 sync 가 먼저면 ImagePullBackOff 후 이미지가 생기면 회복.
- 2차 이후: Mongo 어댑터 · Mongo connection 목록, 변경 이력 조회 화면, MySQL/MariaDB · MSSQL 실제 DB 검증(MSSQL 주석 조회 포함). (403 화면 · favicon 은 2026-11-17 오류 화면 · data: 아이콘으로 처리)
