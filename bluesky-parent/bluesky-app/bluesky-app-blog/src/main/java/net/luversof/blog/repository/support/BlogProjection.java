package net.luversof.blog.repository.support;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.rest.core.config.Projection;

import net.luversof.blog.domain.Blog;

@Projection(name = "blogProjection", types = Blog.class)
public interface BlogProjection {
	UUID getId();
	LocalDateTime getCreatedDate();
}
