package de.unijena.cheminf.lotusfiller;

//import com.mongodb.MongoClientOptions;
import de.unijena.cheminf.lotusfiller.readers.ReaderService;
import de.unijena.cheminf.lotusfiller.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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


    public static void main(String[] args) {
        SpringApplication.run(LOTUSfillerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println("Code version from 13th October 2020");



    }
}
