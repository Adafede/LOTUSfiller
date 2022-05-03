package de.unijena.cheminf.lotusfiller.mongocollections;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;


public interface LOTUSSourceNaturalProductRepository extends MongoRepository<LOTUSSourceNaturalProduct, String>, LOTUSSourceNaturalProductRepositoryCustom {

    //COMMENT AR: What was behind this? first inchikey layer?
    //List<LOTUSSourceNaturalProduct> findBySimpleInchiKey(String inchikey);

    List<LOTUSSourceNaturalProduct> findByInchikey3D(String inchikey3D);

}
