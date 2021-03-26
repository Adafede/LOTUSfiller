package de.unijena.cheminf.lotusfiller.services;

import de.unijena.cheminf.lotusfiller.mongocollections.LotusUniqueNaturalProduct;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusUniqueNaturalProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class CreateCNPidService {

    @Autowired
    LotusUniqueNaturalProductRepository lotusUniqueNaturalProductRepository;


    public String prefix= "LTS";


    public void clearIDs(){

        System.out.println("clearing LOTUS ids");

        List<LotusUniqueNaturalProduct> allunp = lotusUniqueNaturalProductRepository.findAll();

        for(LotusUniqueNaturalProduct unp : allunp){

            unp.setLotus_id("");

            lotusUniqueNaturalProductRepository.save(unp);


        }

        System.out.println("done");

    }

    public void importIDs(String filename) {

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader( new File(filename)));


            int count = 1;
            String line;


            while ((line = bufferedReader.readLine()) != null){
                if(!line.startsWith("lotus_id")) {
                    //ArrayList<String> dataline = new ArrayList<String>(Arrays.asList(line.split(","))); //coconut_id = 0, inchikey = 1
                    String[] dataTab = line.split(",") ;

                    List<LotusUniqueNaturalProduct> unplist = lotusUniqueNaturalProductRepository.findByInchikey(dataTab[1]);


                    if (!unplist.isEmpty()) {
                        for (LotusUniqueNaturalProduct unp : unplist) {
                            unp.setLotus_id(dataTab[0]);
                            lotusUniqueNaturalProductRepository.save(unp);
                        }

                    } else {
                        System.out.println("BAD! Could not find " + dataTab[0] + " in the new version of LOTUS!");
                    }


                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void createDeNovoIDs(String prefix){

        this.prefix = prefix;

        List<LotusUniqueNaturalProduct> allunp = lotusUniqueNaturalProductRepository.findAll();

        int count = 1;
        for(LotusUniqueNaturalProduct unp : allunp){


            String lotus_id = prefix + StringUtils.repeat("0", 7-StringUtils.length(count)) + count;

            unp.setLotus_id(lotus_id);

            lotusUniqueNaturalProductRepository.save(unp);

            count++;

        }

    }


    public void createDeNovoIDs(){

        List<LotusUniqueNaturalProduct> allunp = lotusUniqueNaturalProductRepository.findAll();

        int count = 1;
        for(LotusUniqueNaturalProduct unp : allunp){


            String lotus_id = prefix + StringUtils.repeat("0", 7-StringUtils.length(count)) + count;

            unp.setLotus_id(lotus_id);

            lotusUniqueNaturalProductRepository.save(unp);

            count++;

        }

    }

    public void createIDforNewMolecules(){

        int max_id = 0;
        List<LotusUniqueNaturalProduct> allnp = lotusUniqueNaturalProductRepository.findAll();

        ArrayList<LotusUniqueNaturalProduct> unpWithoutId = new ArrayList<>();

        for(LotusUniqueNaturalProduct np : allnp){

            if(np.lotus_id.equals("")){
                unpWithoutId.add(np);

            }else if(np.lotus_id.startsWith("LTS")){
                int coconut_tmp = Integer.parseInt( np.getLotus_id().split("TS")[1] );
                if(coconut_tmp>max_id ){
                    max_id = coconut_tmp;
                }
            }

        }


        max_id+=1;
        for(LotusUniqueNaturalProduct ildnp : unpWithoutId){
            String coconut_id = prefix + StringUtils.repeat("0", 7-StringUtils.length(max_id)) + max_id;
            ildnp.setLotus_id(coconut_id);
            lotusUniqueNaturalProductRepository.save(ildnp);
            max_id+=1;
        }

    }
}
