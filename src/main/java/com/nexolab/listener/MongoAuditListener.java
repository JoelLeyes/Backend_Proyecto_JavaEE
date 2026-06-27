package com.nexolab.listener;

import com.nexolab.service.MongoAuditService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

@WebListener
public class MongoAuditListener implements ServletContextListener {
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		MongoAuditService.getInstance();
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		MongoAuditService.shutdown();
	}
}