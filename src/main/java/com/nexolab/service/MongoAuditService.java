package com.nexolab.service;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import com.nexolab.model.Chat;
import com.nexolab.model.Mensaje;
import com.nexolab.model.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import org.bson.Document;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class MongoAuditService {
	private static final Logger LOGGER = Logger.getLogger(MongoAuditService.class.getName());
	private static final String DEFAULT_DB_NAME = "java2026_logs";
	private static final String USERS_COLLECTION = "users";
	private static final String MESSAGES_COLLECTION = "messages";

	private static volatile MongoAuditService instance;

	private final boolean enabled;
	private final MongoClient client;
	private final MongoCollection<Document> usersCollection;
	private final MongoCollection<Document> messagesCollection;

	private MongoAuditService(boolean enabled, MongoClient client, MongoCollection<Document> usersCollection, MongoCollection<Document> messagesCollection) {
		this.enabled = enabled;
		this.client = client;
		this.usersCollection = usersCollection;
		this.messagesCollection = messagesCollection;
	}

	public static MongoAuditService getInstance() {
		MongoAuditService current = instance;
		if (current != null) {
			return current;
		}

		synchronized (MongoAuditService.class) {
			current = instance;
			if (current == null) {
				current = create();
				instance = current;
			}
			return current;
		}
	}

	public static void shutdown() {
		synchronized (MongoAuditService.class) {
			if (instance != null) {
				instance.close();
				instance = null;
			}
		}
	}

	public void logUserAction(String source, String action, boolean success, Long userId, String email,
						  HttpServletRequest request, String summary, String errorMessage,
						  Map<String, Object> details) {
		if (!enabled) {
			return;
		}

		Document document = baseDocument(source, action, success, request, summary, errorMessage)
				.append("userId", userId)
				.append("email", normalize(email))
				.append("details", details == null ? new Document() : new Document(details));

		insertSafely(usersCollection, document, USERS_COLLECTION);
	}

	public void logMessageAction(String source, String action, boolean success, Long chatId, String chatName,
						   String chatType, Long messageId, Long senderId, String senderEmail,
						   Long replyToMessageId, int contentLength, boolean hasAttachments, int attachmentCount,
						   HttpServletRequest request, String summary, String errorMessage,
						   Map<String, Object> details) {
		if (!enabled) {
			return;
		}

		Document document = baseDocument(source, action, success, request, summary, errorMessage)
				.append("chatId", chatId)
				.append("chatName", normalize(chatName))
				.append("chatType", normalize(chatType))
				.append("messageId", messageId)
				.append("senderId", senderId)
				.append("senderEmail", normalize(senderEmail))
				.append("replyToMessageId", replyToMessageId)
				.append("contentLength", contentLength)
				.append("hasAttachments", hasAttachments)
				.append("attachmentCount", attachmentCount)
				.append("details", details == null ? new Document() : new Document(details));

		insertSafely(messagesCollection, document, MESSAGES_COLLECTION);
	}

	private static MongoAuditService create() {
		String uri = System.getenv("ATLAS_MONGODB_URI");
		if (uri == null || uri.isBlank()) {
			LOGGER.warning("ATLAS_MONGODB_URI no configurado. Los logs de Atlas quedarán deshabilitados.");
			return new MongoAuditService(false, null, null, null);
		}

		String dbName = System.getenv().getOrDefault("ATLAS_MONGODB_DB", DEFAULT_DB_NAME).trim();
		try {
			MongoClient client = MongoClients.create(new ConnectionString(uri.trim()));
			MongoDatabase database = client.getDatabase(dbName);
			database.runCommand(new Document("ping", 1));

			MongoCollection<Document> usersCollection = database.getCollection(USERS_COLLECTION);
			MongoCollection<Document> messagesCollection = database.getCollection(MESSAGES_COLLECTION);
			ensureIndexes(usersCollection, messagesCollection);

			LOGGER.info("Atlas logs habilitados en la base " + dbName + ".");
			return new MongoAuditService(true, client, usersCollection, messagesCollection);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "No se pudo inicializar Atlas logs. El backend seguirá funcionando sin auditoría.", e);
			return new MongoAuditService(false, null, null, null);
		}
	}

	private static void ensureIndexes(MongoCollection<Document> usersCollection, MongoCollection<Document> messagesCollection) {
		usersCollection.createIndex(Indexes.descending("createdAt"));
		usersCollection.createIndex(Indexes.ascending("action"));
		usersCollection.createIndex(Indexes.ascending("userId"));
		usersCollection.createIndex(Indexes.ascending("email"));

		messagesCollection.createIndex(Indexes.descending("createdAt"));
		messagesCollection.createIndex(Indexes.ascending("action"));
		messagesCollection.createIndex(Indexes.ascending("chatId"));
		messagesCollection.createIndex(Indexes.ascending("messageId"));
		messagesCollection.createIndex(Indexes.ascending("senderId"));
	}

	private static Document baseDocument(String source, String action, boolean success, HttpServletRequest request,
						    String summary, String errorMessage) {
		return new Document("createdAt", Date.from(Instant.now()))
				.append("source", normalize(source))
				.append("action", normalize(action))
				.append("success", success)
				.append("outcome", success ? "SUCCESS" : "ERROR")
				.append("summary", normalize(summary))
				.append("errorMessage", normalize(errorMessage))
				.append("endpoint", request == null ? null : normalize(request.getRequestURI()))
				.append("method", request == null ? null : normalize(request.getMethod()))
				.append("ipAddress", request == null ? null : normalize(extractIpAddress(request)))
				.append("userAgent", request == null ? null : normalize(request.getHeader("User-Agent")));
	}

	private static String extractIpAddress(HttpServletRequest request) {
		String forwardedFor = request.getHeader("X-Forwarded-For");
		if (forwardedFor != null && !forwardedFor.isBlank()) {
			return forwardedFor.split(",")[0].trim();
		}
		String realIp = request.getHeader("X-Real-IP");
		if (realIp != null && !realIp.isBlank()) {
			return realIp.trim();
		}
		return request.getRemoteAddr();
	}

	private static void insertSafely(MongoCollection<Document> collection, Document document, String collectionName) {
		if (collection == null) {
			return;
		}

		try {
			collection.insertOne(document);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "No se pudo guardar un evento en Atlas (" + collectionName + ")", e);
		}
	}

	private static String normalize(String value) {
		return value == null ? null : value.trim();
	}

	private void close() {
		if (client != null) {
			client.close();
		}
	}

	@SuppressWarnings("unused")
	public boolean isEnabled() {
		return enabled;
	}
}