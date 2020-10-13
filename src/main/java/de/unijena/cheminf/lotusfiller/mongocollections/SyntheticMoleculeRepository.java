package de.unijena.cheminf.lotusfiller.mongocollections;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SyntheticMoleculeRepository  extends MongoRepository<SyntheticMolecule, String> {
}
