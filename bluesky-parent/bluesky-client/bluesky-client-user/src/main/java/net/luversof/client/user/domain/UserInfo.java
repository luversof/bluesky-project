package net.luversof.client.user.domain;

import java.util.UUID;

public record UserInfo(UUID id, String username, String password) {}
