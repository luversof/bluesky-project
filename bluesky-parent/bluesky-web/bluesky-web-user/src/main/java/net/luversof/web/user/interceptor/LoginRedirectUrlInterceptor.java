package net.luversof.web.user.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class LoginRedirectUrlInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		String redirectUrl = request.getParameter("redirectUrl");
		if (redirectUrl != null) {
			HttpSession session = request.getSession();
			session.setAttribute("redirectUrl", redirectUrl);
		}
		return true;
	}

}
