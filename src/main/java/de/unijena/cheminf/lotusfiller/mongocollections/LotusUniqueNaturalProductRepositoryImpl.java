package de.unijena.cheminf.lotusfiller.mongocollections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

public class LotusUniqueNaturalProductRepositoryImpl implements LotusUniqueNaturalProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public LotusUniqueNaturalProductRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<String> findAllLotusIds() {

        List<String> lotus_ids_list = mongoTemplate.query(LotusUniqueNaturalProduct.class)
                .distinct("lotus_id")
                .as(String.class)
                .all();
        return lotus_ids_list;
    }

    @Override
    public List<String> findAllInchiKeys() {

        List<String> inchikeys_list = mongoTemplate.query(LotusUniqueNaturalProduct.class)
                .distinct("inchikey3D")
                .as(String.class)
                .all();
        return inchikeys_list;
    }
}
