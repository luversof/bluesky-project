package net.luversof.web.dynamiccrud.use;

import static net.luversof.web.dynamiccrud.use.QueryCaseDbType.MARIADB;
import static net.luversof.web.dynamiccrud.use.QueryCaseDbType.MSSQL;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum QueryCaseEnum {

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
			WHERE `columnA` = :columnA
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
			
	;
	
	
	private List<QueryCase> queryCaseList;
}
