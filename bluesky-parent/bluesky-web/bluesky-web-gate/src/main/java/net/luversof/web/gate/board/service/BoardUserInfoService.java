package net.luversof.web.gate.board.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.data.domain.PageResponse;
import net.luversof.client.user.httpexchange.UserInfoApiClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.board.domain.BoardArticleComment;
import net.luversof.web.gate.board.httpexchange.BoardArticleCommentClient;

/**
 * Helper service that enriches board resources with human readable usernames
 * fetched from bluesky-api-user.
 */
@Service
public class BoardUserInfoService {

	private static final Logger log = LoggerFactory.getLogger(BoardUserInfoService.class);

	private static final String ANONYMOUS = "익명";

	private final UserInfoApiClient userInfoApiClient;
	private final BoardArticleCommentClient boardArticleCommentClient;

	public BoardUserInfoService(UserInfoApiClient userInfoApiClient,
			BoardArticleCommentClient boardArticleCommentClient) {
		this.userInfoApiClient = userInfoApiClient;
		this.boardArticleCommentClient = boardArticleCommentClient;
	}
	
	public PageResponse<BoardArticle> enrich(PageResponse<BoardArticle> pageResponse) {
		if (pageResponse == null || pageResponse.empty()) {
			return pageResponse;
		}
		var boardArticleList = pageResponse.content();
		var userIdList = boardArticleList.stream()
				.map(BoardArticle::userId)
				.collect(Collectors.toList());
		var usernameMap = fetchUsernames(userIdList);
		for (int i = 0; i < pageResponse.content().size(); i++) {
			var boardArticle = boardArticleList.get(i);
			boardArticleList.set(i, applyUsername(boardArticle, usernameMap));
		}
		return pageResponse;
	}

	public BoardArticle enrich(BoardArticle boardArticle) {
		if (boardArticle == null) {
			return boardArticle;
		}
		var usernameMap = fetchUsernames(List.of(boardArticle.userId()));
		return applyUsername(boardArticle, usernameMap);
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

	private Map<UUID, String> fetchUsernames(List<UUID> userIdList) {
		if (userIdList == null || userIdList.isEmpty()) {
			return Collections.emptyMap();
		}
		var distinctIds = userIdList.stream()
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		if (distinctIds.isEmpty()) {
			return Collections.emptyMap();
		}
		try {
			var responses = userInfoApiClient.findByIdIn(List.copyOf(distinctIds));
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
