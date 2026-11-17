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

## 1차 마무리 상태(2026-11-17)
- 미커밋: `bluesky-web/pom.xml`(`<module>` 추가) + `bluesky-web-dynamiccrud2/` 전체(`src/main/frontend/node_modules` 는 gitignore). user 역할 변경은 사용자가 커밋함(33127074).
- 배포 설정(2026-11-17, 미커밋) — 옛 dynamiccrud 와 같은 방식:
  - bluesky-argocd-applications: `bluesky-project_subchart/subchart/bluesky-web-dynamiccrud2/values.yaml`(포트 30125 · 호스트 `dynamiccrud2.web.bluesky.local` · `TZ=Asia/Seoul`, datetime-local 입력을 서버 타임존으로 해석하므로), `argocd_application/values.yaml` 에 앱 추가 `envs: [work]`(catalog · connectioninfo 연결이 work PC DB 에만 있음), `bluesky-cloud-gateway/values.yaml` ingress 호스트 추가.
  - bluesky-cloud: 게이트웨이 `application-k8sdev.yaml` 라우트 `bluesky-web-dynamiccrud2` → `lb://bluesky-web-dynamiccrud2-service`.
  - 이미지: CI(`k8s-ci` 프로필)가 jib 있는 모듈을 모두 zot 으로 push → 별도 설정 없음, buildNumber 는 `subchart_work/buildInfo-bluesky-project` 공유.
  - config server: 추가할 것 없음(앱 설정은 jar 의 application.properties, connectionInfo 읽기 설정은 공통 config).
  - 확인: `helm template` 으로 work 에만 앱 등록 · Service/Deployment(포트 · 이미지 · TZ) 렌더 확인. hosts 에 `dynamiccrud2.web.bluesky.local` 은 이미 있음.
  - 반영 순서: bluesky-project push → CI 이미지 → bluesky-cloud push → 게이트웨이 이미지 → bluesky-argocd-applications push → 루트 앱 sync(앱 생성 · 게이트웨이 재배포). 이미지보다 앱 sync 가 먼저면 ImagePullBackOff 후 이미지가 생기면 회복.
- 2차 이후: Mongo 어댑터 · Mongo connection 목록, 403 전용 화면, 변경 이력 조회 화면, MySQL/MariaDB · MSSQL 실제 DB 검증(MSSQL 주석 조회 포함), favicon.
