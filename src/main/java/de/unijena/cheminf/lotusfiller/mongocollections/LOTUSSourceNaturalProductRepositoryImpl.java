package de.unijena.cheminf.lotusfiller.mongocollections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

public class LOTUSSourceNaturalProductRepositoryImpl implements LOTUSSourceNaturalProductRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Autowired
    public LOTUSSourceNaturalProductRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }


    @Override
    public List<String> findUniqueOriginalInchiKeys(){

        GroupOperation groupByInchikey = group("inchikey3D");

        Aggregation aggregation = newAggregation(groupByInchikey);
        AggregationResults<String> groupResults = mongoTemplate.aggregate(aggregation, "lOTUSSourceNaturalProduct", String.class);

        List<String> result =  groupResults.getMappedResults();

        return result;
    }
    //COMMENT AR: same short inchikey thing...shall I generate them upstream in the file? 
    // Or shall we do a small grep of the first 14 chars? Or drop them?
    /*public List<Object> findUniqueInchiKeys() {
        return mongoTemplate.query(LOTUSSourceNaturalProduct.class).distinct("simpleInchiKey").all()  ;
    }*/

    @Override
    public List<Object> findUniqueSourceNames(){
        return mongoTemplate.query(LOTUSSourceNaturalProduct.class).distinct("source").all() ;
    }


}
