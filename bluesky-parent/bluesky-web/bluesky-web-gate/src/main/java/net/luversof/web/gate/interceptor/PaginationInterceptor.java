package net.luversof.web.gate.interceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

/** model에 Page 객체가 있는 경우 Pagination를 추가해주는 interceptor */
public class PaginationInterceptor implements WebRequestInterceptor {

    @Override
    public void preHandle(WebRequest request) throws Exception {}

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
    public void afterCompletion(WebRequest request, Exception ex) throws Exception {}

    public static class Pagination {

        private int currentPage;
        private int totalPage;

        private List<Nav> navList;

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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pagination that = (Pagination) o;
            return currentPage == that.currentPage
                    && totalPage == that.totalPage
                    && (navList != null ? navList.equals(that.navList) : that.navList == null);
        }

        @Override
        public int hashCode() {
            int result = currentPage;
            result = 31 * result + totalPage;
            result = 31 * result + (navList != null ? navList.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "Pagination{"
                    + "currentPage="
                    + currentPage
                    + ", totalPage="
                    + totalPage
                    + ", navList="
                    + navList
                    + '}';
        }

        private Nav firstNav;
        private Nav prevNav;
        private Nav nextNav;
        private Nav lastNav;

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
    }

    public record Nav(int page, boolean isActive) {}
}
