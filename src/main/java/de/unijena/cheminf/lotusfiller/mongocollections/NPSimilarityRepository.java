package de.unijena.cheminf.lotusfiller.mongocollections;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface NPSimilarityRepository extends MongoRepository<NPSimilarity, String> {
}
