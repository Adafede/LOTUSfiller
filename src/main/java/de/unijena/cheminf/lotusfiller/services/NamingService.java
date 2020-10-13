package de.unijena.cheminf.lotusfiller.services;

import de.unijena.cheminf.lotusfiller.mongocollections.LotusUniqueNaturalProduct;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusLotusUniqueNaturalProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NamingService {

    @Autowired
    LotusLotusUniqueNaturalProductRepository lotusUniqueNaturalProductRepository;

    public void namesToLowcase(){

        System.out.println("Converting names to low case");
        List<String> allCoconutIds = lotusUniqueNaturalProductRepository.findAllCoconutIds();

        for(String coconut_id : allCoconutIds){
            this.npNameToLowcase(coconut_id);
        }
        System.out.println("done");

    }


    public void npNameToLowcase(String coconut_id){

        LotusUniqueNaturalProduct np = lotusUniqueNaturalProductRepository.findByCoconut_id(coconut_id).get(0);

        if(np != null) {

            if(np.name != null && np.name != ""){

                np.name = np.name.toLowerCase();

                if(!np.synonyms.isEmpty()){
                    List<String> lowSynonyms = np.synonyms.stream()
                            .map(String::toLowerCase)
                            .collect(Collectors.toList());
                    np.synonyms = new HashSet<>(lowSynonyms);
                }

            }
            lotusUniqueNaturalProductRepository.save(np);
        }

    }
}
