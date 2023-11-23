package net.luversof.web.gate.interceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

import lombok.Data;

/**
 * model에 Page 객체가 있는 경우 Pagination를 추가해주는 interceptor
 */
public class PaginationInterceptor implements WebRequestInterceptor {

	@Override
	public void preHandle(WebRequest request) throws Exception {
	}

	@Override
	public void postHandle(WebRequest request, ModelMap model) throws Exception {
		if (model == null) {
			return;
		}
		
		var paginationMap = new HashMap<String, Pagination>();
		for (var entrySet : model.entrySet()) {
			// page model이 있는 경우 해당 객체에 대응되는 pagination 객체를 model key + "Navigation" 이름으로 반환
			if (entrySet.getValue() instanceof Page page) {
				paginationMap.put(entrySet.getKey() + "Pagination", new Pagination(page));
			}
		}
		model.putAll(paginationMap);
		
	}

	@Override
	public void afterCompletion(WebRequest request, Exception ex) throws Exception {
	}

	@Data
	class Pagination {
		
		private int currentPage;
		private int totalPage;
		
		private List<Nav> navList;
		
		private Nav firstNav;
		private Nav prevNav;
		private Nav nextNav;
		private Nav lastNav;
		
		Pagination(Page<?> page) {
			currentPage = page.getPageable().getPageNumber();
			totalPage = page.getTotalPages();
			
			int navSize = 10;
			
			int startPage = (int) (Math.floor(currentPage / navSize) * navSize + 1);
			int tempEndPage = startPage + navSize - 1;
			int endPage = tempEndPage > totalPage ? totalPage : tempEndPage;
			
			var prevPage = startPage > 1 ? startPage - 1 : -1;
			var nextPage = endPage < totalPage - 1 ? endPage + 1 : -1;
			
			navList = new ArrayList<>();
			for (int i = startPage; i <= endPage; i++) {
				navList.add(new Nav(i, i == currentPage + 1));
			}
			
			firstNav = new Nav(1, prevPage > 0);
			prevNav = new Nav(prevPage, prevPage > 0);
			nextNav = new Nav(nextPage, nextPage > 0);
			lastNav = new Nav(totalPage, nextPage > 0);
		}
	}
	
	record Nav(int page, boolean isActive) {
	}
}
