package de.unijena.cheminf.lotusfiller.services;

import de.unijena.cheminf.lotusfiller.mongocollections.*;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.ringsearch.AllRingsFinder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NPUnificationService {

    @Autowired
    LOTUSSourceNaturalProductRepository LOTUSSourceNaturalProductRepository;

    @Autowired
    LotusUniqueNaturalProductRepository lotusUniqueNaturalProductRepository;

    @Autowired
    AtomContainerToUniqueNaturalProductService atomContainerToUniqueNaturalProductService;


    PubchemFingerprinter pubchemFingerprinter = new PubchemFingerprinter( SilentChemObjectBuilder.getInstance() );

    CircularFingerprinter circularFingerprinter = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4);


    SubstructureFingerprinter substructureFingerprinter = new SubstructureFingerprinter();

    ExtendedFingerprinter extendedFingerprinter = new ExtendedFingerprinter();



    private ArrayList<String> sourceNames;

    private Hashtable<String, String> sourceURLs;



    public void doWork(){

        System.out.println("NP unification InChi-key based");

        //sourceNames = this.fetchSourceNames();

        //sourceURLs = this.createSourceURLS();



        System.out.println("SOURCES  "+sourceNames);

        List<String> uniqueInchiKeys = LOTUSSourceNaturalProductRepository.findUniqueOriginalInchiKeys(); //find all distinct original inchi keys
        System.out.println("Total number of unique InchiKeys " + uniqueInchiKeys.size());

        for(String oinchikey: uniqueInchiKeys){


            String inchikey3D =  oinchikey.split("\"")[3];  //oinchikey.toString();
            System.out.println(inchikey3D);
            List<LOTUSSourceNaturalProduct> snpList = LOTUSSourceNaturalProductRepository.findByInchikey3D(inchikey3D);
            //System.out.println(snpList.get(0).simpleInchiKey+" "+snpList.get(0).source);

            //create a new UniqueNatural product

            LotusUniqueNaturalProduct unp = new LotusUniqueNaturalProduct();

            unp.setInchikey(inchikey3D);
            unp.setInchi(snpList.get(0).inchi3D);
            unp.setSmiles(snpList.get(0).smiles3d);

            unp.setInchikey2D(snpList.get(0).inchikey2D);
            unp.setInchi2D(snpList.get(0).inchi2D);
            unp.setSmiles2D(snpList.get(0).smiles2d);




            unp.setTotal_atom_number(snpList.get(0).getTotalAtomNumber());
            unp.setHeavy_atom_number(snpList.get(0).getHeavyAtomNumber());




            unp.traditional_name = snpList.get(0).traditionalName;
            unp.iupac_name = snpList.get(0).iupacName;

            unp = lotusUniqueNaturalProductRepository.save(unp);

            unp.synonyms = new HashSet<>();
            unp.allTaxa = new HashSet<>();

            HashSet<TaxonomyReferenceObject> taxonomyReferenceObjects = new HashSet<>();




            unp.xrefs = new HashSet<>();
            //unp.absolute_smiles = new Hashtable<>();


            // deal with taxonomy reference objects : unify and add urls
            unp.taxonomyReferenceObjects = new Hashtable<>(); // key = DOI, value = another HT (db bame -> uncomplicatedTaxonomy)




            //associate the LotusUniqueNaturalProduct entry to each of the sources
            for(LOTUSSourceNaturalProduct snp : snpList){
                //snp.setLotusUniqueNaturalProduct(unp);
                //LOTUSSourceNaturalProductRepository.save(snp);

                //add annotations from SourceNaturalProducts

                String snpDOI = snp.taxonomyReferenceObject.getReferenceDOI();
                String taxoDB = snp.taxonomyReferenceObject.getOrganism_taxo_db();

                if (unp.taxonomyReferenceObjects.containsKey(snpDOI)){
                    // need to check if DB already in or not

                    if ( unp.taxonomyReferenceObjects.get(snpDOI).containsKey(taxoDB)){
                        UncomplicatedTaxonomy unt = makeUncomplicatedTaxonomyFromSourceNP(snp.getTaxonomyReferenceObject(), unp);
                        unp.taxonomyReferenceObjects.get(snpDOI).get(taxoDB).add(unt);

                    }else{
                        HashSet<UncomplicatedTaxonomy> taxoObjectsList = new HashSet<>();
                        UncomplicatedTaxonomy unt = makeUncomplicatedTaxonomyFromSourceNP(snp.getTaxonomyReferenceObject(), unp);
                        unp.taxonomyReferenceObjects.get(snpDOI).put(taxoDB, taxoObjectsList);
                        unp.taxonomyReferenceObjects.get(snpDOI).get(taxoDB).add(unt);


                    }

                }else{
                    // need to add the new doi and the taxoDB and create arrayList and add the uncomplicated taxonomy
                    Hashtable<String, HashSet<UncomplicatedTaxonomy>> ht2 = new Hashtable<>();
                    HashSet<UncomplicatedTaxonomy> taxoObjectsList = new HashSet<>();
                    UncomplicatedTaxonomy unt = makeUncomplicatedTaxonomyFromSourceNP(snp.getTaxonomyReferenceObject(), unp);
                    taxoObjectsList.add(unt);
                    ht2.put(taxoDB, taxoObjectsList);

                    unp.taxonomyReferenceObjects.put(snpDOI, ht2);




                }







                //synonyms
                if (snp.getSynonyms() != null){

                    String[] synonyms;

                    for(String sy : snp.getSynonyms()){

                        String[] names = sy.split("\\\n");
                        for (int i = 0; i < names.length; i++) {
                            unp.synonyms.add(names[i].trim());
                        }
                    }

                }

                //cas
                if (snp.getCas() != null && snp.getCas() != ""){
                    unp.setCas(snp.getCas() );
                }





            }

            unp = lotusUniqueNaturalProductRepository.save(unp);

            //compute molecular parameters for the LotusUniqueNaturalProduct
            unp = computeFingerprints(unp);
            unp = computeAdditionalMolecularFeatures(unp);
            lotusUniqueNaturalProductRepository.save(unp);

        }



    }









    public LotusUniqueNaturalProduct computeAdditionalMolecularFeatures(LotusUniqueNaturalProduct m){
        AllRingsFinder arf = new AllRingsFinder();
        MolecularFormulaManipulator mfm = new MolecularFormulaManipulator();
        AtomContainerManipulator acm = new AtomContainerManipulator();

        IAtomContainer im = atomContainerToUniqueNaturalProductService.createAtomContainer(m);


        // count rings
        try {
            IRingSet rs = arf.findAllRings(im, 20);

            m.setMax_number_of_rings(rs.getAtomContainerCount());

            Cycles   cycles = Cycles.sssr(im);
            IRingSet rings  = cycles.toRingSet();
            m.setMin_number_of_rings(rings.getAtomContainerCount()); //SSSR


        } catch (CDKException e) {
            System.out.println("Too complex: "+m.getSmiles2D());
        }

        //compute molecular formula
        m.setMolecular_formula(mfm.getString(mfm.getMolecularFormula(im) ));

        //compute number of carbons, of nitrogens, of oxygens
        m.setNumber_of_carbons(mfm.getElementCount(mfm.getMolecularFormula(im), "C"));

        m.setNumber_of_oxygens(mfm.getElementCount(mfm.getMolecularFormula(im), "O"));

        m.setNumber_of_nitrogens(mfm.getElementCount(mfm.getMolecularFormula(im), "N"));

        m.setMolecular_weight( acm.getMass(im) );


        // cleaning the NaNs
        if (m.getMolecular_weight().isNaN()){
            m.setMolecular_weight(0.0);
        }


        //get bond count
        IBond[] bonds = acm.getBondArray(im);
        int bondCount = 0;
        for(IBond b : bonds){
            if (b.getAtomCount() == 2){
                if (!b.getAtom(0).getSymbol().equals("H") && !b.getAtom(1).getSymbol().equals("H")){
                    bondCount++;
                }
            }
        }
        m.setBond_count(bondCount);




        return(m);
    }




    public ArrayList fetchSourceNames(){
        List<Object> uniqueSourceNames = LOTUSSourceNaturalProductRepository.findUniqueSourceNames();
        ArrayList<String> tmpArray = new ArrayList<>();

        for(Object usn: uniqueSourceNames) {
            tmpArray.add(usn.toString().toLowerCase());
        }
        return tmpArray;
    }





    public LotusUniqueNaturalProduct computeFingerprints(LotusUniqueNaturalProduct np){



        IAtomContainer ac = atomContainerToUniqueNaturalProductService.createAtomContainer(np);

        // Addition of implicit hydrogens & atom typer
        CDKAtomTypeMatcher matcher = CDKAtomTypeMatcher.getInstance(ac.getBuilder());
        CDKHydrogenAdder adder = CDKHydrogenAdder.getInstance(ac.getBuilder() );
        for (int j = 0; j < ac.getAtomCount(); j++) {
            IAtom atom = ac.getAtom(j);
            IAtomType type = null;
            try {
                type = matcher.findMatchingAtomType(ac, atom);
                AtomTypeManipulator.configure(atom, type);
            } catch (CDKException e) {
                e.printStackTrace();
            }

        }

        try {
            adder.addImplicitHydrogens(ac);
        } catch (CDKException e) {
            e.printStackTrace();
        }
        //AtomContainerManipulator.convertImplicitToExplicitHydrogens(ac);
        AtomContainerManipulator.removeNonChiralHydrogens(ac);

        try {

            String s = pubchemFingerprinter.getBitFingerprint(ac).asBitSet().toString();
            ArrayList<Integer> pcl = new ArrayList<>();
            s = s.replace(" ", "");s = s.replace("\"", "");s = s.replace("{", "");s = s.replace("}", "");
            String[] sl = s.split(",");
            for(String c : sl){
                try {
                    pcl.add(Integer.parseInt(c));
                }catch (NumberFormatException e){ e.printStackTrace(); }
            }
            np.setPubchemFingerprint(pcl);

            //np.pubfp = new HashMap<>();
            //np.pubfp.put(new Integer(pcl.size()), pcl);


            s = circularFingerprinter.getBitFingerprint(ac).asBitSet().toString();
            pcl = new ArrayList<>();
            s = s.replace(" ", "");s = s.replace("\"", "");s = s.replace("{", "");s = s.replace("}", "");
            sl = s.split(",");
            for(String c : sl){
                try {
                    pcl.add(Integer.parseInt(c));
                }catch (NumberFormatException e){ e.printStackTrace(); }
            }
            np.setCircularFingerprint(pcl);

            s = extendedFingerprinter.getBitFingerprint(ac).asBitSet().toString();
            pcl = new ArrayList<>();
            s = s.replace(" ", "");s = s.replace("\"", "");s = s.replace("{", "");s = s.replace("}", "");
            sl = s.split(",");
            for(String c : sl){
                try {
                    pcl.add(Integer.parseInt(c));
                }catch (NumberFormatException e){ e.printStackTrace(); }
            }
            np.setExtendedFingerprint(pcl);



            //Bits and String for PubChem

            try {
                //for PubChem

                BitSet bitsOn = pubchemFingerprinter.getBitFingerprint(ac).asBitSet();
                String pubchemBitString = "";

                for (int i = 0; i <= bitsOn.length(); i++) {
                    if (bitsOn.get(i)) {
                        pubchemBitString += "1";
                    } else {
                        pubchemBitString += "0";
                    }
                }

                np.setPubchemBits(bitsOn.toByteArray());
                np.setPubchemBitsString(pubchemBitString);



            } catch (CDKException | UnsupportedOperationException e) {
                e.printStackTrace();
            }


        } catch (CDKException | UnsupportedOperationException e) {
            e.printStackTrace();
        }
        return np;
    }


    public Hashtable createSourceURLS(){
        Hashtable<String, String> urls = new Hashtable<>();

        if ( this.sourceNames != null && !this.sourceNames.isEmpty()){
            for(String sourceName : this.sourceNames){
                /*if (sourceName.equals("biofacquim")){
                    urls.put("biofacquim", "https://biofacquim.herokuapp.com/");
                }*/

                if (sourceName.equals("bitterdb")){
                    urls.put("bitterdb", "http://bitterdb.agri.huji.ac.il/compound.php?id=");
                }

                if (sourceName.equals("carotenoids")){
                    urls.put("carotenoids", "http://carotenoiddb.jp/Entries/");
                }

                if (sourceName.equals("chebi_np")){
                    urls.put("chebi_np", "https://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI:");
                }

                if (sourceName.equals("chembl_np")){
                    urls.put("chembl_np", "https://www.ebi.ac.uk/chembl/compound_report_card/");
                }


                if (sourceName.equals("cmaup")){
                    urls.put("cmaup", "http://bidd2.nus.edu.sg/CMAUP/searchresults.php?keyword_search=");
                }

                if (sourceName.equals("pubchem_tested_np")){
                    urls.put("pubchem_tested_np", "https://pubchem.ncbi.nlm.nih.gov/compound/");
                }

                if (sourceName.equals("drugbanknp")){
                    urls.put("drugbanknp", "https://www.drugbank.ca/drugs/");
                }


                if (sourceName.equals("chemspidernp")){
                    urls.put("chemspidernp", "http://www.chemspider.com/Chemical-Structure.");
                }

                if (sourceName.equals("np_atlas_2019_12") ){
                    urls.put("np_atlas_2019_12", "https://www.npatlas.org/joomla/index.php/explore/compounds#npaid=");
                }

                if (sourceName.equals("npatlas")){
                    urls.put("npatlas", "https://www.npatlas.org/joomla/index.php/explore/compounds#npaid=");
                }




                if (sourceName.equals("exposome-explorer")){
                    urls.put("exposome-explorer", "http://exposome-explorer.iarc.fr/compounds/");
                }

                if (sourceName.equals("fooddb")){
                    urls.put("fooddb", "https://foodb.ca/compounds/");
                }

                if (sourceName.equals("knapsack")){
                    urls.put("knapsack", "http://www.knapsackfamily.com/knapsack_core/information.php?mode=r&word=");
                }

                if (sourceName.equals("npass")){
                    urls.put("npass", "http://bidd2.nus.edu.sg/NPASS/browse_np.php?compound=");
                }

                if (sourceName.equals("nubbe")){
                    urls.put("nubbe", "https://nubbe.iq.unesp.br/portal/nubbe-search.html");
                }

                if (sourceName.equals("phenolexplorer")){
                    urls.put("phenolexplorer", "http://phenol-explorer.eu/compounds/");
                }


                if (sourceName.equals("sancdb")){
                    urls.put("sancdb", "https://sancdb.rubi.ru.ac.za/compounds/");
                }


                if (sourceName.equals("supernatural2")){
                    urls.put("supernatural2", "http://bioinf-applied.charite.de/supernatural_new/index.php?site=compound_search&id=");
                }


                if (sourceName.equals("tcmdb_taiwan")){
                    urls.put("tcmdb_taiwan", "http://tcm.cmu.edu.tw/");
                }

                if (sourceName.equals("tppt")){
                    urls.put("tppt", "https://www.agroscope.admin.ch/agroscope/en/home/publications/apps/tppt.html");
                }

                if (sourceName.equals("vietherb")){
                    urls.put("vietherb", "https://vietherb.com.vn/metabolites/");
                }

                if (sourceName.equals("streptomedb")){
                    urls.put("streptomedb", "http://132.230.56.4/streptomedb2/get_drugcard/");
                }

            }
        }



        return urls;

    }


    public UncomplicatedTaxonomy makeUncomplicatedTaxonomyFromSourceNP(TaxonomyReferenceObject tro, LotusUniqueNaturalProduct unp){
        UncomplicatedTaxonomy uncomplicatedTaxonomy = new UncomplicatedTaxonomy();

        uncomplicatedTaxonomy.setCleaned_organism_id(tro.getCleaned_organism_id());

        uncomplicatedTaxonomy.setOrganism_value(tro.getOrganism_value());
        uncomplicatedTaxonomy.setOrganism_url(tro.getOrganism_url());


        unp.allTaxa.add(tro.getOrganism_value());


        if (tro.getDomain() != null && tro.getDomain() != "" && !tro.getDomain().equals("null")){
            uncomplicatedTaxonomy.setDomain(tro.getDomain());

            unp.allTaxa.add(tro.getDomain());
        }

        if (tro.getSuperkingdom() != null && tro.getSuperkingdom() != "" && !tro.getSuperkingdom().equals("null")){
            uncomplicatedTaxonomy.setSuperkingdom(tro.getSuperkingdom());
            unp.allTaxa.add(tro.getSuperkingdom());
        }

        if (tro.getKingdom() != null && tro.getKingdom() != "" && !tro.getKingdom().equals("null")){
            uncomplicatedTaxonomy.setKingdom(tro.getKingdom());
            unp.allTaxa.add(tro.getKingdom());
        }

        if (tro.getPhylum() != null && tro.getPhylum() != "" && !tro.getPhylum().equals("null")){
            uncomplicatedTaxonomy.setPhylum(tro.getPhylum());
            unp.allTaxa.add(tro.getPhylum());
        }

        if (tro.getClassx() != null && tro.getClassx() != "" && !tro.getClassx().equals("null")){
            uncomplicatedTaxonomy.setClassx(tro.getClassx());
            unp.allTaxa.add(tro.getClassx());
        }

        if (tro.getOrder()!=null && tro.getOrder() != "" && !tro.getOrder().equals("null")){
            uncomplicatedTaxonomy.setOrder(tro.getOrder());
            unp.allTaxa.add(tro.getOrder());
        }

        if (tro.getFamily() != null && tro.getFamily() != "" && !tro.getFamily().equals("null")){
            uncomplicatedTaxonomy.setFamily(tro.getFamily());
            unp.allTaxa.add(tro.getFamily());
        }

        if (tro.getGenus() != null && tro.getGenus() != "" && !tro.getGenus().equals("null")){
            uncomplicatedTaxonomy.setGenus(tro.getGenus());
            unp.allTaxa.add(tro.getGenus());
        }

        if (tro.getSpecies() != null && tro.getSpecies() != "" && !tro.getSpecies().equals("null")){
            uncomplicatedTaxonomy.setSpecies(tro.getSpecies());
            unp.allTaxa.add(tro.getSpecies());
        }



        return uncomplicatedTaxonomy;
    }



}
