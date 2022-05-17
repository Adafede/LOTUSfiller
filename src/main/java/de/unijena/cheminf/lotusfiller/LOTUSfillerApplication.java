package de.unijena.cheminf.lotusfiller;

//import com.mongodb.MongoClientOptions;
import de.unijena.cheminf.lotusfiller.readers.ReaderService;
import de.unijena.cheminf.lotusfiller.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.lang.System.exit;

@SpringBootApplication
public class LOTUSfillerApplication implements CommandLineRunner {


    @Autowired
    ReaderService readerService;


    @Autowired
    NPUnificationService npUnificationService;

    @Autowired
    FragmentReaderService fragmentReaderService;

    @Autowired
    FragmentCalculatorService fragmentCalculatorService;

    @Autowired
    MolecularFeaturesComputationService molecularFeaturesComputationService;

    @Autowired
    org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Autowired
    SimilarityComputationService similarityComputationService;

    @Autowired
    UpdaterService updaterService;

    @Autowired
    CreateCNPidService createCNPidService;


    @Autowired
    ExportService exportService;

    @Autowired
    AnnotationLevelService annotationLevelService;

    @Autowired
    NamingService namingService;


    @Autowired
    FingerprintsCountsFiller fingerprintsCountsFiller;

    @Autowired
    ReadAndLoadMetadata readAndLoadMetadata;

    public static void main(String[] args) {
        SpringApplication.run(LOTUSfillerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println("Code version from 17th May 2022");


        if (args.length > 0) {


            if (args[0].equals("recomputeMissing")){
                fragmentCalculatorService.doWorkRecompute();
                molecularFeaturesComputationService.doWorkRecompute();
                updaterService.updateSourceNaturalProductsParallelized(40);
                while (!updaterService.processFinished()) {
                    System.out.println("I'm waiting");
                    TimeUnit.MINUTES.sleep(1);
                }

            }
            else if (args[0].equals("addMetadata")){
                System.out.println("Adding metadata from " + args[1]);


                readAndLoadMetadata.addWDandChemOntoData(args[1]);

            }
            else if (args[0].equals("cleanTaxonomies")){
                readAndLoadMetadata.cleanDuplicateUncomplicatedTaxonomies();
            }
            else if (args[0].equals("cleanRecomputeMissing")){

                System.out.println("reading fragments");

                fragmentReaderService.doWork(0, args[1]);
                fragmentReaderService.doWork(1, args[2]);


                System.out.println("Fragmenting everything from scratch");
                fragmentCalculatorService.doParallelizedWork(42);

                System.out.println("Done fragmenting natural products");
                SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                System.out.println("at: "+formatter.format(new Date())+"\n");


                //evaluate annotation level
                //System.out.println("evaluating annotation levels");
                //annotationLevelService.doWorkForAll();
                //System.out.println("done");

                // Compute additional features
                molecularFeaturesComputationService.doWork();
                //updaterService.updateSourceNaturalProductsParallelized(42);


            }
            else if (args[0].equals("addCNPid")){

                System.out.println("Creating de novo LOTUS IDs");
                createCNPidService.createDeNovoIDs();
                updaterService.updateSourceNaturalProductsParallelized(40);
                while (!updaterService.processFinished()) {
                    System.out.println("I'm waiting");
                    TimeUnit.MINUTES.sleep(1);
                }

            }
            else if (args[0].equals("updateCNPid")){
                System.out.println("Updating LOTUS ids");
                createCNPidService.clearIDs();
                createCNPidService.importIDs("lotus_ids_march2021.txt");
                createCNPidService.createIDforNewMolecules();

                updaterService.updateSourceNaturalProductsParallelized(40);
                while (!updaterService.processFinished()) {
                    System.out.println("I'm waiting");
                    TimeUnit.MINUTES.sleep(1);
                }
                System.out.println("done");
            }
            else if (args[0].equals("runOnlySimilarity")){
                //compute similarities between natural products
                similarityComputationService.generateAllPairs();
                // //similarityComputationService.computeSimilarities();
                similarityComputationService.doParallelizedWork(40);
                while (!similarityComputationService.processFinished()) {
                    System.out.println("I'm waiting");
                    TimeUnit.MINUTES.sleep(1);
                }
            }
            else if (args[0].equals("onlyAddSM")){
                //read and insert synthetic molecules
                //readerService.readSyntheticMoleculesAndInsertInMongo(args[1]); //tsv file
                //molecularFeaturesComputationService.doWorkForSM();
                System.out.println("Need to reimplement");
            }
            else if (args[0].equals("generateSDF")){

                exportService.generateSDF("LOTUS_2021_03.sdf");

            }
            else if (args[0].equals("generateSimpleSDF")){
                exportService.generateSimpleSDF("LOTUS_2021_03_simple.sdf", "LOTUS_2021_03_metadata.tsv");
            }
            else if (args[0].equals("generateTSV")){
                exportService.generateTSV("LOTUS_2021_03.tsv");
            }
            else if (args[0].equals("onlyImportLotusIds")){
                int index_of_id_file = Arrays.asList(args).indexOf("onlyImportLotusIds")+1;
                createCNPidService.importIDs(args[index_of_id_file]);
                createCNPidService.createIDforNewMolecules();

            }
            else if (args[0].equals("updateBitFingerprints")){

                molecularFeaturesComputationService.convertToBitSet();

            }
            else if (args[0].equals("createPubchemBitCounts")) {
                molecularFeaturesComputationService.createPubchemBitCounts();

            }
            else if (args[0].equals("namesToLowerCase")){
                namingService.namesToLowcase();
            }

            else { //Filling from scratch
                //cleaning the DB before filling it



                //String dataDirectory = args[0];
                boolean canContinue = false;

                String platinumFile = args[0];
                if (!platinumFile.equals("")){
                    canContinue=true;
                }

                //boolean canContinue = readerService.directoryContainsMolecularFiles(dataDirectory);


                if (canContinue) {
                    //insert in mongodb


                    mongoTemplate.getDb().drop();

                    readerService.readMolecularFileAndInsertInMongo(platinumFile);

                    //unify
                    //npUnificationService.fetchSourceNames();
                    npUnificationService.doWork();



                    fragmentReaderService.doWork(0, args[1]);
                    fragmentReaderService.doWork(1, args[2]);


                    //fragmentCalculatorService.doWork();

                    fragmentCalculatorService.doParallelizedWork(40);


                    System.out.println("Done fragmenting natural products");
                    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
                    System.out.println("at: "+formatter.format(new Date())+"\n");

                    if (Arrays.asList(args).contains("importLOTUSids")) {
                        //coconut_ids_april2020.csv
                        System.out.println("importing old LOTUS ids");
                        int index_of_id_file = Arrays.asList(args).indexOf("importLOTUSids")+1;
                        createCNPidService.clearIDs();
                        createCNPidService.importIDs(args[index_of_id_file]);
                        createCNPidService.createIDforNewMolecules();

                        System.out.println("done importing IDS and generating news ones");
                    }
                    else{

                        createCNPidService.clearIDs();

                        if (Arrays.asList(args).contains("idPrefix")){
                            int index_of_prefix = Arrays.asList(args).indexOf("idPrefix")+1;
                            createCNPidService.createDeNovoIDs(args[index_of_prefix]);
                        }
                        else {

                            createCNPidService.createDeNovoIDs();
                        }
                    }



                    // Compute additional features
                    molecularFeaturesComputationService.doWork();
                    //updaterService.updateSourceNaturalProductsParallelized(42);

                    namingService.namesToLowcase();

                    fingerprintsCountsFiller.doWork();



                    molecularFeaturesComputationService.createPubchemBitCounts();

                    if (Arrays.asList(args).contains("addMetadata")) {

                        int index_of_meta_file = Arrays.asList(args).indexOf("addMetadata")+1;

                        System.out.println("Adding metadata from " + args[index_of_meta_file]);
                        readAndLoadMetadata.addWDandChemOntoData(args[index_of_meta_file]);
                    }



                }
                else {
                    System.out.println("Could not find files with molecules in the provided directory!");
                    exit(0);
                }

            }





            System.out.println("Normal exit");
            exit(0);

        }



    }
}
