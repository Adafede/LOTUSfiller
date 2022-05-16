# LOTUS filler for MongoDB

To fill all from scratch:

```
docker run -it -p 27017:27017 mongo 
```

```
mvn package
```

```
java -jar target/lotusfiller-0.0.2-SNAPSHOT.jar data/test.tsv fragments/fragment_without_sugar.txt fragments/fragment_with_sugar.txt > log/latest.logs.may16.txt
```

```
mongo --port 27017
```

```
use NPOC2021
```

```
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
weights: { name:10, synonyms:5 }
}

    ]

}
)

db.lotusUniqueNaturalProduct.createIndex( {npl_score:1})

db.lotusUniqueNaturalProduct.createIndex( { pubchemBits : "hashed" } )

db.lotusUniqueNaturalProduct.createIndex( {deep_smiles: "hashed"})
db.lotusUniqueNaturalProduct.createIndex( { "pfCounts.bits" :1} )
db.lotusUniqueNaturalProduct.createIndex( { "pfCounts.count" : 1 })
exit
```

```
java -jar target/lotusfiller-0.0.2-SNAPSHOT.jar data/test.tsv fragments/fragment_without_sugar.txt fragments/fragment_with_sugar.txt > log/latest.logs.may16_2.txt
```

```
java -jar target/lotusfiller-0.0.2-SNAPSHOT.jar cleanRecomputeMissing fragments/fragment_without_sugar.txt fragments/fragment_with_sugar.txt
```

```
mongo --port 27017
```

```
use NPOC2021
```

indexes:

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

db.lotusUniqueNaturalProduct.createIndex( {traditional_name:"hashed"})
```

```
db.runCommand(
{
createIndexes: 'lotusUniqueNaturalProduct',
indexes: [
{
key: {
iupac_name:"text", traditional_name:"text", allTaxa:"text", allChemClassifications:"text", allWikidataIds:"text"
},
name: "superTextIndex",
weights: { traditional_name:10, allTaxa:5 }
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
