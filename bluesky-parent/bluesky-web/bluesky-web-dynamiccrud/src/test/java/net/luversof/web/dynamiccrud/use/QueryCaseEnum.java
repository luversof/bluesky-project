package net.luversof.web.dynamiccrud.use;

import static net.luversof.web.dynamiccrud.use.QueryCaseDbType.MARIADB;
import static net.luversof.web.dynamiccrud.use.QueryCaseDbType.MSSQL;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum QueryCaseEnum {
	
	단순쿼리(QueryCase.of(
			MARIADB,
			"""
			select * from dual as a
			""",
			MSSQL,
			"""
			select * from dual as a
			"""
	)),
	단순쿼리_PrepareStatement(QueryCase.of(
			MARIADB,
			"""
			select * from dual as a where columnA = ?
			""",
			MSSQL,
			"""
			select * from dual as a where columnA = ? ORDER BY columnA ASC
			"""
	)),
	단순쿼리_NamedParameter(QueryCase.of(
			MARIADB,
			"""
			SELECT * FROM `dual` m WHERE `columnA` = :columnA
			""",
			MSSQL,
			"""
			SELECT * FROM dual AS AAA WITH (NOLOCK)  WHERE columnA = :columnA ORDER BY columnA ASC
			"""
	)),
	단순쿼리_줄바꿈(QueryCase.of(
			MARIADB,
			"""
			SELECT 
			* 
			FROM `dual` m 
			""",
			MSSQL,
			"""
			SELECT 
			* 
			FROM dual AS AAA WITH (NOLOCK)  
			ORDER BY columnA ASC
			"""
	)),
	단순쿼리_Where조건추가(QueryCase.of(
			MARIADB,
			"""
			SELECT 
			* 
			FROM `dual` m 
			WHERE columnA = :columnA
			""",
			MSSQL,
			"""
			SELECT 
			* 
			FROM dual AS AAA WITH (NOLOCK)  
			WHERE columnA = :columnA
			ORDER BY columnA ASC
			"""
	)),
	단순쿼리_Where조건추가2(QueryCase.of(
			MARIADB,
			"""
			SELECT 
			* 
			FROM `dual` m 
			WHERE columnA = :columnA AND columnB = :columnB
			""",
			MSSQL,
			"""
			SELECT 
			* 
			FROM dual AS AAA WITH (NOLOCK)  
			WHERE columnA = :columnA AND columnB = :columnB
			ORDER BY columnA ASC
			"""
	)),
	단순쿼리_Where조건추가3(QueryCase.of(
			MARIADB,
			"""
			SELECT 
			* 
			FROM `dual` m 
			WHERE columnA = :columnA AND columnB = :columnB And columnC = :columnC
			""",
			MSSQL,
			"""
			SELECT 
			* 
			FROM dual AS AAA WITH (NOLOCK)  
			WHERE columnA = :columnA AND columnB = :columnB And columnC = :columnC
			ORDER BY columnA ASC
			"""
	)),
	단순쿼리_Where조건추가4(QueryCase.of(
			MARIADB,
			"""
			SELECT 
			* 
			FROM `dual` m 
			WHERE columnA = :columnA AND columnB = :columnB And columnC = :columnC And columnD = :columnD
			""",
			MSSQL,
			"""
			SELECT 
			* 
			FROM dual AS AAA WITH (NOLOCK)  
			WHERE columnA = :columnA AND columnB = :columnB And columnC = :columnC And columnD = :columnD
			ORDER BY columnA ASC
			"""
	)),
	단순쿼리_Where조건추가_추가조건과중첩인경우(QueryCase.of(
			MARIADB,
			"""
			SELECT 
			* 
			FROM `dual` m 
			WHERE `columnA` = :columnA AND addWhereColumnA = :addWhereColumnA
			""",
			MSSQL,
			"""
			SELECT 
			* 
			FROM dual AS AAA WITH (NOLOCK)  
			WHERE columnA = :columnA AND addWhereColumnA = :addWhereColumnA
			ORDER BY columnA ASC
			"""
	)),
	단순쿼리_Where조건추가_추가조건과중첩이면서LIKE검색인경우(QueryCase.of(
			MARIADB,
			"""
			SELECT 
			* 
			FROM `dual` m 
			WHERE `columnA` = :columnA AND addWhereColumnA LIKE :addWhereColumnA + '%'
			""",
			MSSQL,
			"""
			SELECT 
			* 
			FROM dual AS AAA WITH (NOLOCK)  
			WHERE columnA = :columnA AND addWhereColumnA LIKE :addWhereColumnA + '%'
			ORDER BY columnA ASC
			"""
	)),
	단순쿼리_줄바꿈_컬럼지정(QueryCase.of(
			MARIADB,
			"""
			SELECT 
			columnA, columnB,
			columnC as cC, count(aa)
			FROM `dual` m 
			WHERE `columnA` = :columnA
			""",
			MSSQL,
			"""
			SELECT 
			columnA, columnB,
			columnC as cC, count(aa) 
			FROM dual AS AAA WITH (NOLOCK)  
			WHERE columnA = :columnA
			ORDER BY columnA ASC
			"""
	)),
	페이징쿼리(QueryCase.of(
			MARIADB,
			"""
			SELECT 
			* 
			FROM `dual` m 
			WHERE `columnA` = :columnA
			LIMIT 22
			OFFSET 11
			""",
			MSSQL,
			"""
			SELECT 
			* 
			FROM dual AS AAA WITH (NOLOCK)  
			WHERE columnA = :columnA
			ORDER BY AAA ASC
			OFFSET 11 ROWS
			FETCH NEXT 22 ROWS ONLY
			"""
	)),
	컬럼삭제케이스예제(QueryCase.of(
		MARIADB, 
		"""
		select * from dual where 
			columnA = :columnA 
			AND columnB = :columnB 
			AND columnC = :columnC
			AND columnD = :columnD
			AND columnE = :columnE
			AND columnF = :columnF
			AND columnG = :columnG
			AND columnH = :columnH
		""",
		MSSQL,
		"""
		select * from dual WITH (NOLOCK) where 
			columnA = :columnA 
			AND columnB = :columnB 
			AND columnC = :columnC
			AND columnD = :columnD
			AND columnE = :columnE
			AND columnF = :columnF
			AND columnG = :columnG
			AND columnH = :columnH
		"""
	)),
	컬럼삭제케이스고정값예제(QueryCase.of(
		MARIADB, 
		"""
		select * from dual where 
			columnA = :columnA 
			AND columnB = 'columnB' 
			AND columnC < 123
			AND columnD > 'columnD'
			AND columnE = 'columnE'
			AND columnF = 'columnF'
			AND columnG = 'columnG'
			AND columnH = 'columnH'
		""",
		MSSQL,
		"""
		select * from dual WITH (NOLOCK) where 
			columnA = :columnA 
			AND columnB = 'columnB' 
			AND columnC < 'columnC'
			AND columnD > 'columnD'
			AND columnE = 'columnE'
			AND columnF = 'columnF'
			AND columnG = 'columnG'
			AND columnH = 'columnH'
		"""
	)),
	컬럼삭제케이스복합조건예제(QueryCase.of(
		MARIADB, 
		"""
		select * from dual where 
			columnA = :columnA 
			AND columnB like :columnB + '%'
			AND columnC like 'columnC' + '%'
			AND columnD like :columnD + '%'
			AND columnE like :columnE + '%'
			AND columnF like :columnF + '%'
			AND columnG like :columnG + '%'
			AND columnH like :columnH + '%'
		""",
		MSSQL,
		"""
		select * from dual WITH (NOLOCK) where 
			columnA = :columnA 
			AND columnB like :columnB + '%'
			AND columnC like :columnC + '%'
			AND columnD like :columnD + '%'
			AND columnE like :columnE + '%'
			AND columnF like :columnF + '%'
			AND columnG like :columnG + '%'
			AND columnH like :columnH + '%'
		"""
	)),
	동일NamedParameter중첩사용(QueryCase.of(
			MARIADB,
			"""
			SELECT * FROM someTable 
			WHERE createDate > CONVERT(DATE, :columnA) AND createDate < DATEADD(DAY, 1, CONVERT(DATE, :columnA))
			ORDER BY regId DESC
			""",
			MARIADB,
			"""
			SELECT
			 *
			FROM  someTable
			WHERE g.columnB=:columnB
			AND g.columnA > convert(VARCHAR(10), :columnA, 121)
			AND g.columnA < convert(VARCHAR(10), dateadd(d, +1, :columnA), 121)
			ORDER BY g.idx ASC
//			""",
			MARIADB,
			"""
			SELECT
			 *
			FROM  someTable
			WHERE g.columnB=:columnB
			AND g.columnA > convert(VARCHAR(10), :columnA, 121)
			AND g.columnA < convert(VARCHAR(10), dateadd(d, +1, :columnA), 121)
			AND g.columnC LIKE :g.columnC + '%'
			ORDER BY g.idx ASC
			"""
	)),
			
	;
	
	
	private List<QueryCase> queryCaseList;
}
