package de.unijena.cheminf.lotusfiller.services;


import de.unijena.cheminf.lotusfiller.mongocollections.LotusUniqueNaturalProduct;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusUniqueNaturalProductRepository;
import de.unijena.cheminf.lotusfiller.mongocollections.UncomplicatedTaxonomy;
import org.openscience.cdk.exception.CDKException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class ReadAndLoadMetadata {

    @Autowired
    LotusUniqueNaturalProductRepository lotusUniqueNaturalProductRepository;



    public void cleanDuplicateUncomplicatedTaxonomies(){

        List<String> lotus_ids = lotusUniqueNaturalProductRepository.findAllLotusIds();

        for(String lotus_id : lotus_ids){

            LotusUniqueNaturalProduct lunp = lotusUniqueNaturalProductRepository.findByLotus_id(lotus_id).get(0);

            Set<String> allDois = lunp.taxonomyReferenceObjects.keySet();

            for(String doi : allDois){
                Set<String> allTaxDbs = lunp.taxonomyReferenceObjects.get(doi).keySet();
                for(String taxDb : allTaxDbs){
                    HashSet<UncomplicatedTaxonomy> newUTlist = new HashSet<>();
                    HashSet<String> addedOrgids = new HashSet<>();

                    for(UncomplicatedTaxonomy ut : lunp.taxonomyReferenceObjects.get(doi).get(taxDb)){
                        if(!addedOrgids.contains(ut.getCleaned_organism_id()) ){
                            newUTlist.add(ut);
                            addedOrgids.add(ut.getCleaned_organism_id());
                        }
                    }
                    lunp.taxonomyReferenceObjects.get(doi).put(taxDb, newUTlist);


                }

            }
            lotusUniqueNaturalProductRepository.save(lunp);

        }




    }

    public void addWDandChemOntoData(String fileName){

        File file = new File(fileName);

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

            // if the first line is the header
            ArrayList<String> header = null;

            if (file.getName().toLowerCase().endsWith("csv")) {
                header = new ArrayList<String>(Arrays.asList(bufferedReader.readLine().replace("\"", "").split(",")));
            } else if (file.getName().toLowerCase().endsWith("tsv")) {
                header = new ArrayList<String>(Arrays.asList(bufferedReader.readLine().replace("\"", "").split("\t")));
            }


            if (header != null) {

                // read the header
                Integer indexOfInchikey = null;
                Integer indexOfStructureWikidata = null; //the simplest to add
                Integer indexOfstructure_taxonomy_npclassifier_01pathway = null;
                Integer indexOfstructure_taxonomy_npclassifier_02superclass = null;
                Integer indexOfstructure_taxonomy_npclassifier_03class = null;
                Integer indexOfstructure_taxonomy_classyfire_01kingdom = null;
                Integer indexOfstructure_taxonomy_classyfire_02superclass = null;
                Integer indexOfstructure_taxonomy_classyfire_03class = null;
                Integer indexOfstructure_taxonomy_classyfire_04directparent = null;
                Integer indexOforganism_wikidata = null;
                Integer indexOforganism_name = null;
                Integer indexOfreference_wikidata = null;
                Integer indexOfreference_doi = null;


                for (String item : header) {

                    if (item.equals("structure_inchikey")) {
                        indexOfInchikey = header.indexOf(item);
                    }

                    if (item.equals("structure_wikidata")) {
                        indexOfStructureWikidata = header.indexOf(item);
                    }
                    if (item.equals("structure_taxonomy_npclassifier_01pathway")) {
                        indexOfstructure_taxonomy_npclassifier_01pathway = header.indexOf(item);
                    }
                    if (item.equals("structure_taxonomy_npclassifier_02superclass")) {
                        indexOfstructure_taxonomy_npclassifier_02superclass = header.indexOf(item);
                    }
                    if (item.equals("structure_taxonomy_npclassifier_03class")) {
                        indexOfstructure_taxonomy_npclassifier_03class = header.indexOf(item);
                    }
                    if (item.equals("structure_taxonomy_classyfire_01kingdom")) {
                        indexOfstructure_taxonomy_classyfire_01kingdom = header.indexOf(item);
                    }
                    if (item.equals("structure_taxonomy_classyfire_02superclass")) {
                        indexOfstructure_taxonomy_classyfire_02superclass = header.indexOf(item);
                    }
                    if (item.equals("structure_taxonomy_classyfire_03class")) {
                        indexOfstructure_taxonomy_classyfire_03class = header.indexOf(item);
                    }
                    if (item.equals("structure_taxonomy_classyfire_04directparent")) {
                        indexOfstructure_taxonomy_classyfire_04directparent = header.indexOf(item);
                    }
                    if (item.equals("organism_wikidata")) {
                        indexOforganism_wikidata = header.indexOf(item);
                    }
                    if (item.equals("organism_name")) {
                        indexOforganism_name = header.indexOf(item);
                    }
                    if (item.equals("reference_wikidata")) {
                        indexOfreference_wikidata = header.indexOf(item);
                    }
                    if (item.equals("reference_doi")) {
                        indexOfreference_doi = header.indexOf(item);
                    }


                }


                //read the rest of the file
                int count = 1;
                String line;

                while ((line = bufferedReader.readLine()) != null && count <= 6000000) {


                    ArrayList<String> dataline = null;
                    if (file.getName().toLowerCase().endsWith("csv")) {
                        dataline = new ArrayList<String>(Arrays.asList(line.replace("\"", "").split(",")));
                    } else if (file.getName().toLowerCase().endsWith("tsv")) {
                        dataline = new ArrayList<String>(Arrays.asList(line.replace("\"", "").split("\t")));

                    }



                    if(indexOfInchikey != null){
                        //search for the inchikey in the database
                        List<LotusUniqueNaturalProduct> lotusUniqueNaturalProducts = lotusUniqueNaturalProductRepository.findByInchikey(dataline.get(indexOfInchikey));
                        if(!lotusUniqueNaturalProducts.isEmpty()) {
                            LotusUniqueNaturalProduct lunp = lotusUniqueNaturalProducts.get(0);
                            System.out.println(lunp.getLotus_id()+"  "+lunp.getInchikey());

                            if(lunp.allWikidataIds==null){
                                lunp.allWikidataIds = new HashSet<>();
                            }

                            if(indexOfStructureWikidata != null){
                                lunp.setWikidata_id(dataline.get(indexOfStructureWikidata));
                                lunp.allWikidataIds.add(dataline.get(indexOfStructureWikidata).split("/")[dataline.get(indexOfStructureWikidata).split("/").length-1]);
                            }

                            System.out.println(dataline.get(indexOfStructureWikidata));

                            //
                            lunp.allChemClassifications = new HashSet<>();

                            if (indexOfstructure_taxonomy_npclassifier_01pathway != null){
                                lunp.setChemicalTaxonomyNPclassifierPathway(dataline.get(indexOfstructure_taxonomy_npclassifier_01pathway));
                                lunp.allChemClassifications.add(dataline.get(indexOfstructure_taxonomy_npclassifier_01pathway));
                            }

                            if(indexOfstructure_taxonomy_npclassifier_02superclass != null){
                                lunp.setChemicalTaxonomyNPclassifierSuperclass(dataline.get(indexOfstructure_taxonomy_npclassifier_02superclass));
                                lunp.allChemClassifications.add(dataline.get(indexOfstructure_taxonomy_npclassifier_02superclass));
                            }

                            if(indexOfstructure_taxonomy_npclassifier_03class != null){
                                lunp.setChemicalTaxonomyNPclassifierClass(dataline.get(indexOfstructure_taxonomy_npclassifier_03class));
                                lunp.allChemClassifications.add(dataline.get(indexOfstructure_taxonomy_npclassifier_03class));
                            }


                            // add the classyfire values
                            if(indexOfstructure_taxonomy_classyfire_01kingdom != null){
                                lunp.setChemicalTaxonomyClassyfireKingdom(dataline.get(indexOfstructure_taxonomy_classyfire_01kingdom));
                                lunp.allChemClassifications.add(dataline.get(indexOfstructure_taxonomy_classyfire_01kingdom));
                            }

                            if(indexOfstructure_taxonomy_classyfire_02superclass != null){
                                lunp.setChemicalTaxonomyClassyfireSuperclass(dataline.get(indexOfstructure_taxonomy_classyfire_02superclass));
                                lunp.allChemClassifications.add(dataline.get(indexOfstructure_taxonomy_classyfire_02superclass));
                            }

                            if(indexOfstructure_taxonomy_classyfire_03class != null){
                                lunp.setChemicalTaxonomyClassyfireClass(dataline.get(indexOfstructure_taxonomy_classyfire_03class));
                                lunp.allChemClassifications.add(dataline.get(indexOfstructure_taxonomy_classyfire_03class));
                            }

                            if(indexOfstructure_taxonomy_classyfire_04directparent != null){
                                lunp.setChemicalTaxonomyClassyfireDirectParent(dataline.get(indexOfstructure_taxonomy_classyfire_04directparent));
                                lunp.allChemClassifications.add(dataline.get(indexOfstructure_taxonomy_classyfire_04directparent));
                            }



                            //assign wikidata ids to organisms and references

                            if(indexOfreference_wikidata!= null && indexOfreference_doi != null && indexOforganism_wikidata!= null && indexOforganism_name != null) {
                                String adaptedDOI = dataline.get(indexOfreference_doi).replace(".", "$x$x$");
                                if (lunp.taxonomyReferenceObjects.containsKey(adaptedDOI) && lunp.taxonomyReferenceObjects.get(adaptedDOI).containsKey("Open Tree of Life")){
                                    HashSet<UncomplicatedTaxonomy> utList = lunp.taxonomyReferenceObjects.get(adaptedDOI).get("Open Tree of Life");
                                    for (UncomplicatedTaxonomy ut : utList) {
                                        if (dataline.get(indexOforganism_name).equals(ut.getOrganism_value())) {
                                            ut.setReference_wikidata_id(dataline.get(indexOfreference_wikidata));
                                            ut.setWikidata_id(dataline.get(indexOforganism_wikidata));

                                            lunp.allWikidataIds.add(dataline.get(indexOforganism_wikidata).split("/")[dataline.get(indexOforganism_wikidata).split("/").length-1]);
                                            lunp.allWikidataIds.add(dataline.get(indexOfreference_wikidata).split("/")[dataline.get(indexOfreference_wikidata).split("/").length-1]);

                                        }
                                    }
                                }
                            }


                            System.out.println(lunp.allChemClassifications);
                            System.out.println(lunp.allWikidataIds);
                            System.out.println(lunp.wikidata_id);


                            lotusUniqueNaturalProductRepository.save(lunp);

                        }

                    }





                    count++;
                }

            }


        } catch (IOException e) {
            e.printStackTrace();
        }


    }


}
