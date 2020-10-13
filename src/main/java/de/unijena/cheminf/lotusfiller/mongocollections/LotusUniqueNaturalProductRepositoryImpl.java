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
    public List<String> findAllCoconutIds() {

        List<String> coconut_ids_list = mongoTemplate.query(LotusUniqueNaturalProduct.class)
                .distinct("coconut_id")
                .as(String.class)
                .all();
        return coconut_ids_list;
    }
}
