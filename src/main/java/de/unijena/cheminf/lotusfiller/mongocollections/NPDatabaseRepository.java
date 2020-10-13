package de.unijena.cheminf.lotusfiller.mongocollections;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface NPDatabaseRepository extends MongoRepository<NPDatabase, String> {
}
