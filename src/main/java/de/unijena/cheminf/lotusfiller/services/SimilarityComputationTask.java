package de.unijena.cheminf.lotusfiller.services;


import de.unijena.cheminf.lotusfiller.misc.BeanUtil;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusLotusUniqueNaturalProductRepository;
import de.unijena.cheminf.lotusfiller.mongocollections.NPSimilarity;
import de.unijena.cheminf.lotusfiller.mongocollections.NPSimilarityRepository;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusUniqueNaturalProduct;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.Fingerprinter;
import org.openscience.cdk.fingerprint.IBitFingerprint;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.similarity.Tanimoto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Transient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;


@Service
@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
public class SimilarityComputationTask implements Runnable {


    @Autowired
    @Transient
    NPSimilarityRepository npSimilarityRepository;

    @Autowired
    @Transient
    AtomContainerToUniqueNaturalProductService atomContainerToUniqueNaturalProductService;

    @Autowired
    @Transient
    LotusLotusUniqueNaturalProductRepository lotusUniqueNaturalProductRepository;


    ArrayList<List<String>> npInchikeyPairsToCompute;

    Integer taskid;

    @Override
    public void run() {

        this.npSimilarityRepository = BeanUtil.getBean(NPSimilarityRepository.class);
        this.atomContainerToUniqueNaturalProductService = BeanUtil.getBean(AtomContainerToUniqueNaturalProductService.class);
        this.lotusUniqueNaturalProductRepository = BeanUtil.getBean(LotusLotusUniqueNaturalProductRepository.class);

        Fingerprinter fingerprinter = new Fingerprinter();
        System.out.println("Computing similarities for task "+taskid);

        for(List spair : npInchikeyPairsToCompute){
            List<String> pair = new ArrayList<String>(spair);
            LotusUniqueNaturalProduct unp1 = lotusUniqueNaturalProductRepository.findByInchikey(pair.get(0)).get(0);
            LotusUniqueNaturalProduct unp2 = lotusUniqueNaturalProductRepository.findByInchikey(pair.get(1)).get(0);


            IAtomContainer mol1 = atomContainerToUniqueNaturalProductService.createAtomContainer(unp1);
            IAtomContainer mol2 = atomContainerToUniqueNaturalProductService.createAtomContainer(unp2);

            IBitFingerprint fingerprint1 = null;
            IBitFingerprint fingerprint2 = null;
            try {
                fingerprint1 = fingerprinter.getBitFingerprint(mol1);
                fingerprint2 = fingerprinter.getBitFingerprint(mol2);
                double tanimoto_coefficient = Tanimoto.calculate(fingerprint1, fingerprint2);

                if (tanimoto_coefficient>=0.5){

                    NPSimilarity newSimilarity = new NPSimilarity();
                    newSimilarity.setUniqueNaturalProductID1(unp1.getId());
                    newSimilarity.setUniqueNaturalProductID2(unp2.getId());
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


    public void setNpPairsToCompute(ArrayList<List<String>> npInchikeyPairsToCompute){
        this.npInchikeyPairsToCompute = npInchikeyPairsToCompute;
    }


}
