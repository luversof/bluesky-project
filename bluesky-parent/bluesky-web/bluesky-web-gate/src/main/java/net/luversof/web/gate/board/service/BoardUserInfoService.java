package net.luversof.web.gate.board.service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.luversof.client.user.openfeign.UserApiClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.board.domain.BoardArticleComment;

/**
 * Helper service that enriches board resources with human readable usernames
 * fetched from bluesky-api-user.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardUserInfoService {

	private static final String ANONYMOUS = "익명";

	private final UserApiClient userApiClient;

	public BoardArticle enrich(BoardArticle boardArticle) {
		if (boardArticle == null) {
			return null;
		}
		var usernameMap = fetchUsernames(Collections.singletonList(boardArticle.userId()));
		return applyUsername(boardArticle, usernameMap);
	}

	public Page<BoardArticle> enrich(Page<BoardArticle> page) {
		if (page == null || page.isEmpty()) {
			return page;
		}
		var userIds = page.getContent().stream()
				.map(BoardArticle::userId)
				.collect(Collectors.toList());
		var usernameMap = fetchUsernames(userIds);
		return page.map(article -> applyUsername(article, usernameMap));
	}

	public BoardArticleComment enrich(BoardArticleComment comment) {
		if (comment == null) {
			return null;
		}
		var usernameMap = fetchUsernames(Collections.singletonList(comment.userId()));
		return applyUsername(comment, usernameMap);
	}

	public Page<BoardArticleComment> enrichComments(Page<BoardArticleComment> page) {
		if (page == null || page.isEmpty()) {
			return page;
		}
		var userIds = page.getContent().stream()
				.map(BoardArticleComment::userId)
				.collect(Collectors.toList());
		var usernameMap = fetchUsernames(userIds);
		return page.map(comment -> applyUsername(comment, usernameMap));
	}

	private Map<UUID, String> fetchUsernames(Collection<UUID> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return Collections.emptyMap();
		}
		var distinctIds = userIds.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (distinctIds.isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			var responses = userApiClient.findByIdIn(List.copyOf(distinctIds));
			if (responses == null || responses.isEmpty()) {
				return Collections.emptyMap();
			}
			Map<UUID, String> usernameMap = new HashMap<>();
			for (var response : responses) {
				if (response == null || response.id() == null) {
					continue;
				}
				try {
					var uuid = UUID.fromString(response.id());
					if (!usernameMap.containsKey(uuid) && StringUtils.hasText(response.username())) {
						usernameMap.put(uuid, response.username());
					}
				} catch (IllegalArgumentException ex) {
					log.warn("Invalid user id {} returned from api-user", response.id());
				}
			}
			return usernameMap;
		} catch (Exception ex) {
			log.warn("Failed to fetch user info for {} : {}", distinctIds, ex.getMessage());
			return Collections.emptyMap();
		}
	}

	private BoardArticle applyUsername(BoardArticle article, Map<UUID, String> usernameMap) {
		if (article == null) {
			return null;
		}
		return article.toBuilder()
				.username(resolveUsername(article.userId(), article.username(), usernameMap))
				.build();
	}

	private BoardArticleComment applyUsername(BoardArticleComment comment, Map<UUID, String> usernameMap) {
		if (comment == null) {
			return null;
		}
		return comment.toBuilder()
				.username(resolveUsername(comment.userId(), comment.username(), usernameMap))
				.build();
	}

	private String resolveUsername(UUID userId, String existing, Map<UUID, String> usernameMap) {
		if (StringUtils.hasText(existing) && !existing.equalsIgnoreCase(asString(userId))) {
			return existing;
		}
		if (userId != null) {
			var fetched = usernameMap.get(userId);
			if (StringUtils.hasText(fetched)) {
				return fetched;
			}
			return userId.toString();
		}
		return ANONYMOUS;
	}

	private String asString(UUID userId) {
		return userId != null ? userId.toString() : null;
	}
}
