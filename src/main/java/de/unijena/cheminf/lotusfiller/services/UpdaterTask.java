package de.unijena.cheminf.lotusfiller.services;

import de.unijena.cheminf.lotusfiller.misc.BeanUtil;
import de.unijena.cheminf.lotusfiller.mongocollections.LOTUSLOTUSSourceNaturalProductRepository;
import de.unijena.cheminf.lotusfiller.mongocollections.LOTUSSourceNaturalProduct;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusUniqueNaturalProduct;
import de.unijena.cheminf.lotusfiller.mongocollections.LotusLotusUniqueNaturalProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Transient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
public class UpdaterTask implements Runnable {

    @Autowired
    @Transient
    LotusLotusUniqueNaturalProductRepository lotusUniqueNaturalProductRepository;

    @Autowired
    @Transient
    LOTUSLOTUSSourceNaturalProductRepository LOTUSSourceNaturalProductRepository;

    List<LotusUniqueNaturalProduct> batchOfMolecules;

    Integer taskid;


    @Override
    public void run() {
        this.lotusUniqueNaturalProductRepository = BeanUtil.getBean(LotusLotusUniqueNaturalProductRepository.class);
        this.LOTUSSourceNaturalProductRepository = BeanUtil.getBean(LOTUSLOTUSSourceNaturalProductRepository.class);


        for(LotusUniqueNaturalProduct unp : batchOfMolecules){

            //find all SourceNaturalProducts that correspond to this UniqueNaturalproduct

            List<LOTUSSourceNaturalProduct> allSNP = LOTUSSourceNaturalProductRepository.findBySimpleInchiKey(unp.inchikey);
            for(LOTUSSourceNaturalProduct snp : allSNP){
                snp.setLotusUniqueNaturalProduct(unp);
                LOTUSSourceNaturalProductRepository.save(snp);
            }

        }
        Date date = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        System.out.println("Task "+taskid+" finished at: "+formatter.format(date)+"\n");
    }


    public List<LotusUniqueNaturalProduct> getBatchOfMolecules() {
        return batchOfMolecules;
    }

    public void setBatchOfMolecules(List<LotusUniqueNaturalProduct> batchOfMolecules) {
        this.batchOfMolecules = batchOfMolecules;
    }

    public Integer getTaskid() {
        return taskid;
    }

    public void setTaskid(Integer taskid) {
        this.taskid = taskid;
    }
}
