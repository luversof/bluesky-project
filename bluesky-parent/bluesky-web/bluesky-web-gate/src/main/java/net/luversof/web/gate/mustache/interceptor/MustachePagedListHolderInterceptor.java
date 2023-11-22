package net.luversof.web.gate.mustache.interceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

import lombok.Data;

/**
 * model에 Page 객체가 있는 경우 PagedListHolder를 추가해주는 interceptor
 */
public class MustachePagedListHolderInterceptor implements WebRequestInterceptor {

	@Override
	public void preHandle(WebRequest request) throws Exception {
	}

	@Override
	public void postHandle(WebRequest request, ModelMap model) throws Exception {
		if (model == null) {
			return;
		}
		
		var navigationMap = new HashMap<String, Navigation>();
		for (var entrySet : model.entrySet()) {
			// page model이 있는 경우 해당 객체에 대응되는 navigation 객체를 model key + "Navigation" 이름으로 반환
			if (entrySet.getValue() instanceof Page page) {
				navigationMap.put(entrySet.getKey() + "Navigation", new Navigation(page));
			}
		}
		model.putAll(navigationMap);
		
	}

	@Override
	public void afterCompletion(WebRequest request, Exception ex) throws Exception {
	}

	@Data
	class Navigation {
		
		private int pageNumber;
		
		private int totalPages;
		
		private List<Nav> navList;
		
		private int prevPage;
		
		Navigation(Page<?> page) {
			pageNumber = page.getPageable().getPageNumber();
			totalPages = page.getTotalPages();
			
			int navSize = 9;
			
			int startPage = (int) (Math.floor(pageNumber / navSize) * navSize + 1);
			int tempEndPage = startPage + navSize - 1;
			int endPage = tempEndPage > totalPages ? totalPages : tempEndPage;
			
			prevPage = startPage > 1 ? startPage - 1 : -1;
			
			navList = new ArrayList<>();
			for (int i = startPage; i <= endPage; i++) {
				navList.add(new Nav(i, i == pageNumber + 1));
			}
		}
	}
	
	record Nav(int page, boolean isCurrentPage) {
	}
}
