package net.luversof.bookkeeping.domain.web;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;

import lombok.Data;

/**
 * Entry 입력시 기존 입력 정보 참고에 필요한 파라메터를 관리하기 위한 클래스
 * 월 단위로 무조건 고정하여 한달씩 보여준다.
 * @author bluesky
 *
 */
@Data
public class EntryRequestParam {

	/**
	 * user의 bookkeeping table id 값
	 */
	private String bookkeepingId;
	
	private String userId;
	
	@NotNull(groups = Search.class)
	private LocalDate startLocalDate;
	
	@NotNull(groups = Search.class)
	private LocalDate endLocalDate;
	
	public interface Search {}
}
