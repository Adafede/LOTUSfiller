package de.unijena.cheminf.lotusfiller.mongocollections;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface LOTUSLOTUSSourceNaturalProductRepository extends MongoRepository<LOTUSSourceNaturalProduct, String>, LOTUSSourceNaturalProductRepositoryCustom {


    List<LOTUSSourceNaturalProduct> findBySimpleInchiKey(String inchikey);

}
