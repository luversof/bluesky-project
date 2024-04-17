package net.luversof.api.bookkeeping.domain.web;

import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
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
	private UUID bookkeepingId;
	
	private UUID userId;
	
	@NotNull(groups = Search.class)
	private ZonedDateTime startDate;
	
	@NotNull(groups = Search.class)
	private ZonedDateTime endDate;
	
	public interface Search {}
}
