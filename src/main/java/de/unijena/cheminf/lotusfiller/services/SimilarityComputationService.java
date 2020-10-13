package de.unijena.cheminf.lotusfiller.services;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusUniqueNaturalProduct;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusLotusUniqueNaturalProductRepository;
import de.unijena.cheminf.lotusfiller.mongocollections.NPSimilarityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class SimilarityComputationService {

    @Autowired
    LotusLotusUniqueNaturalProductRepository lotusUniqueNaturalProductRepository;

    @Autowired
    NPSimilarityRepository npSimilarityRepository;

    @Autowired
    AtomContainerToUniqueNaturalProductService atomContainerToUniqueNaturalProductService;

    private Set<Set<String>> npPairs;

    private Hashtable<Integer,List<String>> hashtableInchikeyPairs = new Hashtable<>();



    List<Future<?>> futures = new ArrayList<Future<?>>();


    /*

    public void computeSimilarities(){
        Fingerprinter fingerprinter = new Fingerprinter();

        System.out.println("Computing similarities");


        for(Set spair : npPairs){
            List<LotusUniqueNaturalProduct> pair = new ArrayList<LotusUniqueNaturalProduct>(spair);

            IAtomContainer mol1 = atomContainerToUniqueNaturalProductService.createAtomContainer(pair.get(0));
            IAtomContainer mol2 = atomContainerToUniqueNaturalProductService.createAtomContainer(pair.get(1));

            IBitFingerprint fingerprint1 = null;
            IBitFingerprint fingerprint2 = null;
            try {
                fingerprint1 = fingerprinter.getBitFingerprint(mol1);
                fingerprint2 = fingerprinter.getBitFingerprint(mol2);
                double tanimoto_coefficient = Tanimoto.calculate(fingerprint1, fingerprint2);

                if (tanimoto_coefficient>=0.5){

                    NPSimilarity newSimilarity = new NPSimilarity();
                    newSimilarity.setUniqueNaturalProductID1(pair.get(0).getId());
                    newSimilarity.setUniqueNaturalProductID2(pair.get(1).getId());
                    newSimilarity.setTanimoto(tanimoto_coefficient);
                    //newSimilarity.setDistanceMoment(distance_moment);

                    npSimilarityRepository.save(newSimilarity);
                }

            } catch (CDKException e) {
                e.printStackTrace();
            }

        }
        System.out.println("done");

    }
*/

    public void generateAllPairs(){
        System.out.println("Computing pairs of NPs");

        List<LotusUniqueNaturalProduct> allNP = lotusUniqueNaturalProductRepository.findAll();


        Set<String> npset = new HashSet<>();

        for(LotusUniqueNaturalProduct np: allNP){
            npset.add(np.inchikey);
        }

        allNP=null;
        this.npPairs = Sets.combinations( npset, 2);

        npset=null;

        int npcount = 0;
        for(Set spair : this.npPairs) {
            this.hashtableInchikeyPairs.put(npcount, new ArrayList<String>(spair));
            npcount++;

        }

        System.out.println("created hashtable for np pair partition");

        System.out.println("done");

    }


    public void doParallelizedWork( Integer numberOfThreads){

        System.out.println("Start parallel computation of Tanimoto");

        try{
            ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numberOfThreads);



            List<List<Integer>> npPairBatch =  Lists.partition(new ArrayList<Integer>(hashtableInchikeyPairs.keySet()), 10000);

            System.out.println("created partition");

            int taskcount = 0;


            System.out.println("Total number of tasks:" + npPairBatch.size());

            for(List<Integer> intNPBatch : npPairBatch){
                SimilarityComputationTask task  = new SimilarityComputationTask();

                ArrayList<List<String>> pairBatch= new ArrayList<>();

                for(Integer s : intNPBatch){
                    pairBatch.add(hashtableInchikeyPairs.get(s));
                }

                task.setNpPairsToCompute(pairBatch);
                taskcount++;

                System.out.println("Task "+taskcount+" created");
                task.taskid=taskcount;

                Future<?> f = executor.submit(task);

                futures.add(f);

                System.out.println("Task "+taskcount+" executing");

            }

            executor.shutdown();
            //executor.awaitTermination(210, TimeUnit.SECONDS);

            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                System.out.println("Interrupted exectuion of the molecular similarity calculation! please check consistency of results!");
            }




        } catch (Exception e) {
            e.printStackTrace();
        }


    }


    public boolean processFinished(){

        boolean allFuturesDone = true;

        for(Future<?> future : this.futures){

            allFuturesDone &= future.isDone();

        }


        System.out.println("Finished parallel computation of Tanimoto");
        return allFuturesDone;
    }
}
