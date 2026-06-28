package com.nexolab.service;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

import java.time.Instant;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class MongoAuditService {
	private static final Logger LOGGER = Logger.getLogger(MongoAuditService.class.getName());
	private static final String DEFAULT_DB_NAME = "java2026_logs";
	private static final String USERS_COLLECTION = "users";
	private static final String CHATS_COLLECTION = "chats";
	private static final String GROUPS_COLLECTION = "groups";
	private static final String MESSAGES_COLLECTION = "messages";

	private static volatile MongoAuditService instance;

	private final boolean enabled;
	private final MongoClient client;
	private final MongoCollection<Document> usersCollection;
	private final MongoCollection<Document> chatsCollection;
	private final MongoCollection<Document> groupsCollection;
	private final MongoCollection<Document> messagesCollection;

	private MongoAuditService(boolean enabled, MongoClient client, MongoCollection<Document> usersCollection,
					 MongoCollection<Document> chatsCollection, MongoCollection<Document> groupsCollection,
					 MongoCollection<Document> messagesCollection) {
		this.enabled = enabled;
		this.client = client;
		this.usersCollection = usersCollection;
		this.chatsCollection = chatsCollection;
		this.groupsCollection = groupsCollection;
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
					  jakarta.servlet.http.HttpServletRequest request, String summary, String errorMessage,
						  Map<String, Object> details) {
		if (!enabled) {
			return;
		}

		Document document = baseDocument(source, action, success, summary, errorMessage)
				.append("userId", userId)
				.append("email", normalize(email));

		insertSafely(usersCollection, document, USERS_COLLECTION);
	}

	public void logChatAction(String source, String action, boolean success, Long chatId, String chatName,
					 Long creatorId, String creatorEmail, Long recipientId, String recipientEmail,
					 String summary, String errorMessage) {
		if (!enabled) {
			return;
		}

		Document document = baseDocument(source, action, success, summary, errorMessage)
				.append("chatId", chatId)
				.append("chatName", normalize(chatName))
				.append("creatorId", creatorId)
				.append("creatorEmail", normalize(creatorEmail));

		if (recipientId != null) {
			document.append("recipientId", recipientId);
		}
		if (recipientEmail != null) {
			document.append("recipientEmail", normalize(recipientEmail));
		}

		insertSafely(chatsCollection, document, CHATS_COLLECTION);
	}

	public void logMessageAction(String source, String action, boolean success, Long chatId, String chatName,
					   String chatType, Long senderId, String senderEmail,
					   String recipientEmail, List<String> recipientEmails,
					   String summary, String errorMessage) {
		if (!enabled) {
			return;
		}

		Document document = baseDocument(source, action, success, summary, errorMessage)
				.append("chatId", chatId)
				.append("chatName", normalize(chatName))
				.append("chatType", normalize(chatType))
				.append("senderId", senderId)
				.append("senderEmail", normalize(senderEmail));

		if (recipientEmail != null) {
			document.append("recipientEmail", normalize(recipientEmail));
		}
		if (recipientEmails != null && !recipientEmails.isEmpty()) {
			document.append("recipientEmails", recipientEmails.stream().map(MongoAuditService::normalize).collect(Collectors.toList()));
		}

		insertSafely(messagesCollection, document, MESSAGES_COLLECTION);
	}

	public void logGroupAction(String source, String action, boolean success, Long groupId, String groupName,
					   Long creatorId, String creatorEmail, Integer memberCount,
					   jakarta.servlet.http.HttpServletRequest request, String summary, String errorMessage,
					   Map<String, Object> details) {
		if (!enabled) {
			return;
		}

		Document document = baseDocument(source, action, success, summary, errorMessage)
				.append("groupId", groupId)
				.append("groupName", normalize(groupName))
				.append("creatorId", creatorId)
				.append("creatorEmail", normalize(creatorEmail))
				.append("memberCount", memberCount);

		insertSafely(groupsCollection, document, GROUPS_COLLECTION);
	}

	private static MongoAuditService create() {
		String uri = System.getenv("ATLAS_MONGODB_URI");
		if (uri == null || uri.isBlank()) {
			LOGGER.warning("ATLAS_MONGODB_URI no configurado. Los logs de Atlas quedarán deshabilitados.");
			return new MongoAuditService(false, null, null, null, null, null);
		}

		String dbName = System.getenv().getOrDefault("ATLAS_MONGODB_DB", DEFAULT_DB_NAME).trim();
		try {
			MongoClient client = MongoClients.create(new ConnectionString(uri.trim()));
			MongoDatabase database = client.getDatabase(dbName);
			database.runCommand(new Document("ping", 1));

			MongoCollection<Document> usersCollection = database.getCollection(USERS_COLLECTION);
			MongoCollection<Document> chatsCollection = database.getCollection(CHATS_COLLECTION);
			MongoCollection<Document> groupsCollection = database.getCollection(GROUPS_COLLECTION);
			MongoCollection<Document> messagesCollection = database.getCollection(MESSAGES_COLLECTION);
			ensureIndexes(usersCollection, chatsCollection, groupsCollection, messagesCollection);

			LOGGER.info("Atlas logs habilitados en la base " + dbName + ".");
			return new MongoAuditService(true, client, usersCollection, chatsCollection, groupsCollection, messagesCollection);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "No se pudo inicializar Atlas logs. El backend seguirá funcionando sin auditoría.", e);
			return new MongoAuditService(false, null, null, null, null, null);
		}
	}

	private static void ensureIndexes(MongoCollection<Document> usersCollection, MongoCollection<Document> chatsCollection,
									 MongoCollection<Document> groupsCollection, MongoCollection<Document> messagesCollection) {
		usersCollection.createIndex(Indexes.descending("createdAt"));
		usersCollection.createIndex(Indexes.ascending("action"));
		usersCollection.createIndex(Indexes.ascending("userId"));
		usersCollection.createIndex(Indexes.ascending("email"));

		chatsCollection.createIndex(Indexes.descending("createdAt"));
		chatsCollection.createIndex(Indexes.ascending("action"));
		chatsCollection.createIndex(Indexes.ascending("chatId"));
		chatsCollection.createIndex(Indexes.ascending("creatorId"));
		chatsCollection.createIndex(Indexes.ascending("recipientEmail"));

		groupsCollection.createIndex(Indexes.descending("createdAt"));
		groupsCollection.createIndex(Indexes.ascending("action"));
		groupsCollection.createIndex(Indexes.ascending("groupId"));
		groupsCollection.createIndex(Indexes.ascending("creatorId"));

		messagesCollection.createIndex(Indexes.descending("createdAt"));
		messagesCollection.createIndex(Indexes.ascending("action"));
		messagesCollection.createIndex(Indexes.ascending("chatId"));
		messagesCollection.createIndex(Indexes.ascending("senderId"));
		messagesCollection.createIndex(Indexes.ascending("recipientEmail"));
		messagesCollection.createIndex(Indexes.ascending("recipientEmails"));
	}

	private static Document baseDocument(String source, String action, boolean success, String summary, String errorMessage) {
		return new Document("createdAt", Date.from(Instant.now()))
				.append("source", normalize(source))
				.append("action", normalize(action))
				.append("success", success)
				.append("outcome", success ? "SUCCESS" : "ERROR")
				.append("summary", normalize(summary))
				.append("errorMessage", normalize(errorMessage));
	}

	public static List<String> recipientEmails(com.nexolab.model.Chat chat, Long senderId) {
		if (chat == null || chat.getParticipantes() == null) {
			return List.of();
		}

		return chat.getParticipantes().stream()
				.filter(u -> u != null && u.getEmail() != null && (senderId == null || u.getIdUsuario() == null || !u.getIdUsuario().equals(senderId)))
				.map(com.nexolab.model.Usuario::getEmail)
				.map(MongoAuditService::normalize)
				.filter(email -> email != null && !email.isBlank())
				.collect(Collectors.toList());
	}

	public static String primaryRecipientEmail(com.nexolab.model.Chat chat, Long senderId) {
		List<String> emails = recipientEmails(chat, senderId);
		return emails.isEmpty() ? null : emails.get(0);
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