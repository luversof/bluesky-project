package net.luversof.web.common.menu.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Data;

@Data
public class Pagination {
	private int currentPage;
	private int totalPage;
	
	private List<Nav> navList;
	
	private Nav firstNav;
	private Nav prevNav;
	private Nav nextNav;
	private Nav lastNav;
	
	public Pagination(Page<?> page) {
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
	
	record Nav(int page, boolean isActive) {
	}
}