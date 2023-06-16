package net.luversof.api.blog.repository.mongo;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import net.luversof.api.blog.domain.mongo.Blog;

@Repository(value = "blogMongoRepository")
public interface BlogRepository extends MongoRepository<Blog, ObjectId> {

}
