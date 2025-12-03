package tom.config;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

public class SpaRedirectFilter implements Filter {

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) req;

		String path = request.getRequestURI().substring(request.getContextPath().length());

		// 1. Let API calls through
		if (path.startsWith("/api/") || path.equals("/api")) {
			chain.doFilter(req, res);
			return;
		}

		// 2. Let static resources through (anything that contains a dot)
		// e.g. /static/css/app.css, /assets/img/logo.png, /favicon.ico
		if (path.contains(".")) {
			chain.doFilter(req, res);
			return;
		}

		// 3. Everything else – forward to Angular’s index.html
		request.getRequestDispatcher("/index.html").forward(req, res);
	}

}
