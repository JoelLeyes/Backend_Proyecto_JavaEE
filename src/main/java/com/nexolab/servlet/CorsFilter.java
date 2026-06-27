package com.nexolab.servlet;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/*")
public class CorsFilter implements Filter {
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		HttpServletResponse resp = (HttpServletResponse) response;
		resp.setHeader("Access-Control-Allow-Origin", "*");
		resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
		resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization");
		resp.setHeader("Access-Control-Allow-Credentials", "true");

		if ("OPTIONS".equals(((HttpServletRequest) request).getMethod())) {
			resp.setStatus(200);
			return;
		}

		HttpServletRequest req = (HttpServletRequest) request;
		String contextPath = req.getContextPath();
		if (contextPath == null) {
			contextPath = "";
		}

		String requestUri = req.getRequestURI();
		if (requestUri == null) {
			requestUri = "";
		}

		String requestPath = requestUri.startsWith(contextPath)
				? requestUri.substring(contextPath.length())
				: requestUri;
		if (requestPath.startsWith("/api/")) {
			String targetPath = requestPath.substring(4);
			req.getRequestDispatcher(targetPath).forward(request, response);
			return;
		}

		chain.doFilter(request, response);
	}
}