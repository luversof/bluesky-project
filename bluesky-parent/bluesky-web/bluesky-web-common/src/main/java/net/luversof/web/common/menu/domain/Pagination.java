package net.luversof.web.common.menu.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;

public class Pagination {
	private int currentPage;
	private int totalPage;

	private List<Nav> navList;

	private Nav firstNav;
	private Nav prevNav;
	private Nav nextNav;
	private Nav lastNav;

	public int getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	public int getTotalPage() {
		return totalPage;
	}

	public void setTotalPage(int totalPage) {
		this.totalPage = totalPage;
	}

	public List<Nav> getNavList() {
		return navList;
	}

	public void setNavList(List<Nav> navList) {
		this.navList = navList;
	}

	public Nav getFirstNav() {
		return firstNav;
	}

	public void setFirstNav(Nav firstNav) {
		this.firstNav = firstNav;
	}

	public Nav getPrevNav() {
		return prevNav;
	}

	public void setPrevNav(Nav prevNav) {
		this.prevNav = prevNav;
	}

	public Nav getNextNav() {
		return nextNav;
	}

	public void setNextNav(Nav nextNav) {
		this.nextNav = nextNav;
	}

	public Nav getLastNav() {
		return lastNav;
	}

	public void setLastNav(Nav lastNav) {
		this.lastNav = lastNav;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pagination other = (Pagination) obj;
		return currentPage == other.currentPage && Objects.equals(firstNav, other.firstNav)
				&& Objects.equals(lastNav, other.lastNav) && Objects.equals(navList, other.navList)
				&& Objects.equals(nextNav, other.nextNav) && Objects.equals(prevNav, other.prevNav)
				&& totalPage == other.totalPage;
	}

	@Override
	public int hashCode() {
		return Objects.hash(currentPage, firstNav, lastNav, navList, nextNav, prevNav, totalPage);
	}

	@Override
	public String toString() {
		return "Pagination [currentPage=" + currentPage + ", totalPage=" + totalPage + ", navList=" + navList
				+ ", firstNav=" + firstNav + ", prevNav=" + prevNav + ", nextNav=" + nextNav + ", lastNav=" + lastNav
				+ "]";
	}

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

	public record Nav(int page, boolean isActive) {
	}
}