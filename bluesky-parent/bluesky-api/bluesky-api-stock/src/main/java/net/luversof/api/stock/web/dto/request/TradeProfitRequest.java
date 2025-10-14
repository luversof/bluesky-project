package net.luversof.api.stock.web.dto.request;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.util.CollectionUtils;

import net.luversof.api.stock.constant.StockErrorCode;

/**
 * 주식 손익 계산 요청 DTO
 * 조회 기준 조합
 * 1. userId (유저의 모든 계좌, 모든 종목)
 * 2. userId, accountId (유저의 특정 계좌, 모든 종목)
 * 3. userId, stockItemId (유저의 모든 계좌, 특정 종목) 
 * 4. userId, accountId, stockItemId (유저의 특정 계좌, 특정 종목)
 * + 위 조건에 추가로 fromDate, toDate가 있으면 해당 기간 내의 거래만 조회
 * 
 * web 요청 dto이나 service 단에 전파 
 * 이렇게 사용하는게 올바른 설계가 아니지만 간결함을 위해 전파함
 * 다만 service 단에서 최소한으로 사용하려고 함.
 * 
 * 근데 복수 userId나 accountId를 지원해야 할까?
 * 유저간 비교 같은? (이건 적용 가능성이 낮음)
 * 복수 계좌 조회는 있을 수 있을 거 같음.
 * 
 * stockITemId 기준 처리가 맞을까? ticker가 기준이 되는게 더 나을지도?
 */
public record TradeProfitRequest(
	UUID userId,
	List<UUID> accountIdList,
	List<UUID> stockItemIdList,
	OffsetDateTime startDate,
	OffsetDateTime endDate,
	TradeProfitRequestGroup groupBy
	) {
	
	public TradeProfitRequest {
		if (groupBy == null) {
			groupBy = TradeProfitRequestGroup.ACCOUNT_AND_STOCKITEM;
		}
	}
	
	public TradeProfitRequestType getRequestType() {
		if (userId == null) {
			StockErrorCode.NOT_EXIST_USER_ID.throwException();
		}
		
		if (accountIdList == null) {
			return CollectionUtils.isEmpty(stockItemIdList) ? TradeProfitRequestType.USER : TradeProfitRequestType.USER_STOCKITEM;
		}
		
		return CollectionUtils.isEmpty(stockItemIdList) ? TradeProfitRequestType.USER_ACCOUNT : TradeProfitRequestType.USER_ACCOUNT_STOCKITEM;
	}
	
	public boolean hasDateRange() {
		return startDate != null && endDate != null;
	}
}
