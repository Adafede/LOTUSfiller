# LOTUS filler for MongoDB

To fill all from scratch:

```
docker run -it -p 27017:27017 mongo 
```

```
mvn package
```

```
java -jar target/lotusfiller-0.0.2-SNAPSHOT.jar data/test_old.tsv fragments/fragment_without_sugar.txt fragments/fragment_with_sugar.txt > log/latest.logs.may17_1.txt
```

```
java -jar target/lotusfiller-0.0.2-SNAPSHOT.jar addMetadata data/test_metadata_old.tsv  > log/latest.logs.may17_2.txt
```

```
mongo --port 27017
```

```
use NPOC2021
```

```
db.lOTUSSourceNaturalProduct.createIndex( {inchi3D:"hashed"})
db.lOTUSSourceNaturalProduct.createIndex( {inchikey3D: "hashed"})
db.lOTUSSourceNaturalProduct.createIndex( {inchikey2D:1})
db.lOTUSSourceNaturalProduct.createIndex( {smiles3D:"hashed"})
db.lOTUSSourceNaturalProduct.createIndex( {smiles2D:"hashed"})

db.lotusUniqueNaturalProduct.createIndex( {inchi:"hashed"})
db.lotusUniqueNaturalProduct.createIndex( {inchikey:1})
db.lotusUniqueNaturalProduct.createIndex( {smiles: "hashed"})
db.lotusUniqueNaturalProduct.createIndex( {inchi2D:"hashed"})
db.lotusUniqueNaturalProduct.createIndex( {inchikey2D:1})
db.lotusUniqueNaturalProduct.createIndex( {smiles2D: "hashed"})
db.lotusUniqueNaturalProduct.createIndex( {molecular_formula:1})
db.lotusUniqueNaturalProduct.createIndex( {lotus_id:1})
db.lotusUniqueNaturalProduct.createIndex( {fragmentsWithSugar:"hashed"})
db.lotusUniqueNaturalProduct.createIndex( {fragments:"hashed"})
```

```
db.runCommand(
  {
    createIndexes: 'lotusUniqueNaturalProduct',
    indexes: [
        {
            key: {
                iupac_name:"text", traditional_name:"text", allTaxa:"text"
            },
            name: "superTextIndex",
        weights: { name:10, synonyms:5  }
        }

    ]
  }
)

db.lotusUniqueNaturalProduct.createIndex( {npl_score:1})
db.lotusUniqueNaturalProduct.createIndex( { pubchemBits : "hashed" } )
db.lotusUniqueNaturalProduct.createIndex( {deep_smiles: "hashed"})
db.lotusUniqueNaturalProduct.createIndex( { "pfCounts.bits" :1} )
db.lotusUniqueNaturalProduct.createIndex( { "pfCounts.count" : 1 })

db.fragment.createIndex({signature:1})
db.fragment.createIndex({signature:1, withsugar:-1})


db.fragment.createIndex({signature:1})
db.fragment.aggregate([
{ "$group": {
"_id": { "signature": "$signature", "withsugar":"$withsugar" },
"dups": { "$push": "$_id" },
"count": { "$sum": 1 }
}},
{ "$match": { "count": { "$gt": 1 } }}
]).forEach(function(doc) {
doc.dups.shift();
db.fragment.remove({ "_id": {"$in": doc.dups }});
});
db.fragment.createIndex({signature:1, withsugar:-1}, {unique:true, dropDups : true})
exit
```

```
java -jar target/lotusfiller-0.0.2-SNAPSHOT.jar cleanRecomputeMissing fragments/fragment_without_sugar.txt fragments/fragment_with_sugar.txt > log/latest.logs.may17_3.txt
```
