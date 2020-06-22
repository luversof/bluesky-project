package net.luversof.blog.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import net.luversof.blog.domain.mongo.Blog;

@Repository(value = "blogMongoRepository")
public interface BlogRepository extends MongoRepository<Blog, String> {

}
