package de.unijena.cheminf.lotusfiller.services;

import com.google.common.collect.Lists;
import de.unijena.cheminf.lotusfiller.mongocollections.LOTUSLOTUSSourceNaturalProductRepository;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusUniqueNaturalProduct;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusLotusUniqueNaturalProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

@Service
public class UpdaterService {

    @Autowired
    LOTUSLOTUSSourceNaturalProductRepository LOTUSSourceNaturalProductRepository;

    @Autowired
    LotusLotusUniqueNaturalProductRepository lotusUniqueNaturalProductRepository;

    List<Future<?>> futures = new ArrayList<Future<?>>();

    /*
    public void updateSourceNaturalProducts(){

        //get all sourceNaturalProduct
        System.out.println("Updating links...");

        List<LOTUSSourceNaturalProduct> allSourceNaturalProducts = LOTUSLOTUSSourceNaturalProductRepository.findAll();

        for(LOTUSSourceNaturalProduct snp : allSourceNaturalProducts){
            String unpid = snp.getLotusUniqueNaturalProduct().getId();
            Optional<LotusUniqueNaturalProduct> unp = lotusUniqueNaturalProductRepository.findById(unpid);
            if(unp.isPresent()){
                LotusUniqueNaturalProduct np = unp.get();
                snp.setLotusUniqueNaturalProduct(np);
                LOTUSLOTUSSourceNaturalProductRepository.save(snp);
            }

        }
        System.out.println("done");
    }
*/


    public void updateSourceNaturalProductsParallelized(int nbThreads){

        System.out.println("Updating links...");

        //List<LOTUSSourceNaturalProduct> allSourceNaturalProducts = LOTUSLOTUSSourceNaturalProductRepository.findAll();
        List<LotusUniqueNaturalProduct> allLotusUniqueNaturalProducts = lotusUniqueNaturalProductRepository.findAll();


        try{

            System.out.println("Number of links to update: "+ allLotusUniqueNaturalProducts.size());

            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(nbThreads);


            List<List<LotusUniqueNaturalProduct>> moleculeBatches =  Lists.partition(allLotusUniqueNaturalProducts, 1000);

            List<Callable<Object>> todo = new ArrayList<Callable<Object>>(moleculeBatches.size());
            System.out.println("Total number of tasks:" + moleculeBatches.size());

            int taskcount = 0;



            for(List<LotusUniqueNaturalProduct> oneBatch : moleculeBatches){

                UpdaterTask task = new UpdaterTask();

                task.setBatchOfMolecules(oneBatch);
                taskcount++;

                task.taskid=taskcount;

                Future<?> f = executor.submit(task);

                futures.add(f);

                //executor.execute(task);

                System.out.println("Task "+taskcount+" executing");

            }


            executor.shutdown();


        } catch (Exception e) {
        e.printStackTrace();
        }


        System.out.println("done");

    }


    public boolean processFinished(){

        boolean allFuturesDone = true;

        for(Future<?> future : this.futures){

            allFuturesDone &= future.isDone();

        }
        //System.out.println("Finished parallel computation of fragments");
        return allFuturesDone;
    }

}
