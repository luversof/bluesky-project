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
