package de.unijena.cheminf.lotusfiller.readers;

import de.unijena.cheminf.lotusfiller.misc.BeanUtil;
import de.unijena.cheminf.lotusfiller.misc.DatabaseTypeChecker;
import de.unijena.cheminf.lotusfiller.misc.MoleculeChecker;
import de.unijena.cheminf.lotusfiller.mongocollections.LOTUSSourceNaturalProductRepository;
import de.unijena.cheminf.lotusfiller.mongocollections.LOTUSSourceNaturalProduct;
import de.unijena.cheminf.lotusfiller.mongocollections.TaxonomyReferenceObject;
import de.unijena.cheminf.lotusfiller.services.AtomContainerToSourceNaturalProductService;
import net.sf.jniinchi.INCHI_OPTION;
import net.sf.jniinchi.INCHI_RET;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;

import java.io.*;
//import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

public class CSVReader implements Reader {


    File file;
    ArrayList<IAtomContainer> listOfMolecules;
    private LineNumberReader inchiReader;
    LOTUSSourceNaturalProductRepository LOTUSSourceNaturalProductRepository;
    AtomContainerToSourceNaturalProductService ac2snp;
    MoleculeChecker moleculeChecker;
    DatabaseTypeChecker databaseTypeChecker;
    String source;

    public CSVReader(){
        this.listOfMolecules = new ArrayList<IAtomContainer>();
        LOTUSSourceNaturalProductRepository = BeanUtil.getBean(LOTUSSourceNaturalProductRepository.class);
        ac2snp = BeanUtil.getBean(AtomContainerToSourceNaturalProductService.class);
        moleculeChecker = BeanUtil.getBean(MoleculeChecker.class);
        databaseTypeChecker = BeanUtil.getBean(DatabaseTypeChecker.class);
    }


    @Override
    public void readFile(File file) {

        SmilesGenerator smilesGenerator = new SmilesGenerator(SmiFlavor.Unique );
        SmilesGenerator absoluteSmilesGenerator = new SmilesGenerator(SmiFlavor.Absolute );
        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
        this.file = file;
        if(file.getName().toLowerCase().endsWith("csv")){
            this.source = file.getName().toLowerCase().replace(".csv", "");
        }
        else if(file.getName().toLowerCase().endsWith("tsv")){
            this.source = file.getName().toLowerCase().replace(".tsv", "");
        }



        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(this.file));
            //read the header

            // if the first line is the header
            ArrayList<String> header = null;


            if (file.getName().toLowerCase().endsWith("csv")) {
                header = new ArrayList<String>(Arrays.asList(bufferedReader.readLine().replace("\"", "").split(",")));
            } else if (file.getName().toLowerCase().endsWith("tsv")) {
                header = new ArrayList<String>(Arrays.asList(bufferedReader.readLine().replace("\"", "").split("\t")));
            }

            //System.out.println(header);

            if (header != null){
                //System.out.println(header);



                Integer indexOfOrganismType =null;
                Integer indexOfOrganismValue = null;
                Integer indexOfReferenceValue = null;
                Integer indexOfOrganismCleaned = null;
                Integer indexOfOrganismCleanedId = null; //id in the taxonomy database (gbif, NCBI or open tree of life)
                Integer indexOfOrganismCleanedDBTaxo = null;
                Integer indexOfOrganismCleaned_dbTaxoTaxonRanks = null; //to split on |
                Integer indexOfOrganismCleaned_dbTaxoTaxonomy = null; //to split on |
                Integer indexOfStructureCleanedInchikey = null; //the inchi key to unify on
                Integer indexOfStructureCleanedInchi = null;
                Integer indexOfStructureCleanedSmiles = null; //3D smiles
                Integer indexOfstructureCleaned_inchikey2D = null; //2D
                Integer indexOfstructureCleaned_inchi2D = null; //2D
                Integer indexOfstructureCleaned_smiles2D = null; //2D
                Integer indexOfstructureCleaned_nameIupac = null;
                Integer indexOfstructureCleaned_nameTraditional = null; //THE name to use
                Integer indexOfReferenceCleanedDOI = null;
                Integer indexOfReferenceCleanedTitle = null;




                /**
                 * previous curated header version:
                 */

                /*
                Integer indexOflinkToWDstructure = null;
                Integer indexOfInchikey = null;
                Integer indexOfInchi = null;
                Integer indexOfCanonicalSMILES = null;
                Integer indexOfCAS = null;
                Integer indexOfChebi = null;
                Integer indexOfChembl = null;
                Integer indexOfPubchem = null;
                Integer indexOfWDtaxonLink = null;
                Integer indexOfTaxonName = null;
                Integer indexOfTaxonIdGbif  = null;
                Integer indexOfTaxonIdNCBI  = null;
                Integer indexOfReferenceWDLink  = null;
                Integer indexOfReferenceDOI  = null;
                Integer indexOfReferenceTitle  = null;
                Integer indexOfUniqueName  = null;
                Integer indexOfOttId  = null;
                Integer indexOfOttLink  = null;
                Integer indexOfNameDomain  = null;
                Integer indexOfUniqueNameDomain  = null;
                Integer indexOfOttDomain  = null;
                Integer indexOfNameKingdom  = null;
                Integer indexOfUniqueNameKingdom  = null;
                Integer indexOfOttKingdom  = null;
                Integer indexOfNamePhylum  = null;
                Integer indexOfUniqueNamePhylum  = null;
                Integer indexOfOttPhylum  = null;
                Integer indexOfNameClass  = null;
                Integer indexOfUniqueNameClass  = null;
                Integer indexOfOttClass  = null;
                Integer indexOfNameOrder  = null;
                Integer indexOfUniqueNameOrder  = null;
                Integer indexOfOttOrder  = null;
                Integer indexOfNameFamily  = null;
                Integer indexOfUniqueNameFamily  = null;
                Integer indexOfOttFamily  = null;
                Integer indexOfNameGenus  = null;
                Integer indexOfUniqueNameGenus  = null;
                Integer indexOfOttGenus  = null;
                Integer indexOfNameSpecies  = null;
                Integer indexOfUniqueNameSpecies  = null;
                Integer indexOfOttSpecies  = null;

    */


                for (String item : header) {



                    if (item.equals("organismType")) {
                        indexOfOrganismType = header.indexOf(item);
                    }

                    if (item.equals("organismValue")) {
                        indexOfOrganismValue = header.indexOf(item);
                    }

                    if (item.equals("referenceValue")) {
                        indexOfReferenceValue = header.indexOf(item);
                    }

                    if (item.equals("organismCleaned")) {
                        indexOfOrganismCleaned = header.indexOf(item);
                    }

                    if (item.equals("organismCleaned_id")) {
                        indexOfOrganismCleanedId = header.indexOf(item);
                    }

                    if (item.equals("organismCleaned_dbTaxo")) {
                        indexOfOrganismCleanedDBTaxo = header.indexOf(item);
                    }

                    if (item.equals("organismCleaned_dbTaxoTaxonRanks")) {
                        indexOfOrganismCleaned_dbTaxoTaxonRanks = header.indexOf(item);
                    }

                    if (item.equals("organismCleaned_dbTaxoTaxonomy")) {
                        indexOfOrganismCleaned_dbTaxoTaxonomy = header.indexOf(item);
                    }

                    if (item.equals("structureCleanedInchikey")) {
                        indexOfStructureCleanedInchikey = header.indexOf(item);
                    }

                    if (item.equals("structureCleanedInchi")) {
                        indexOfStructureCleanedInchi = header.indexOf(item);
                    }

                    if (item.equals("structureCleanedSmiles")) {
                        indexOfStructureCleanedSmiles = header.indexOf(item);
                    }

                    if (item.equals("structureCleaned_inchikey2D")) {
                        indexOfstructureCleaned_inchikey2D = header.indexOf(item);
                    }

                    if (item.equals("structureCleaned_inchi2D")) {
                        indexOfstructureCleaned_inchi2D = header.indexOf(item);
                    }

                    if (item.equals("structureCleaned_smiles2D")) {
                        indexOfstructureCleaned_smiles2D = header.indexOf(item);
                    }

                    if (item.equals("structureCleaned_nameIupac")) {
                        indexOfstructureCleaned_nameIupac = header.indexOf(item);
                    }

                    if (item.equals("structureCleaned_nameTraditional")) {
                        indexOfstructureCleaned_nameTraditional = header.indexOf(item);
                    }

                    if (item.equals("referenceCleanedDoi")) {
                        indexOfReferenceCleanedDOI = header.indexOf(item);
                    }

                    if (item.equals("referenceCleanedTitle")) {
                        indexOfReferenceCleanedTitle = header.indexOf(item);
                    }





/*
                    if (item.toLowerCase().equals("structure")) {
                        indexOflinkToWDstructure = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("structure_inchikey")) {
                        indexOfInchikey = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("structure_inchi")) {
                        indexOfInchi = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("structure_smiles_canonical")) {
                        indexOfCanonicalSMILES = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("structure_cas")) {
                        indexOfCAS = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("structure_chebi")) {
                        indexOfChebi = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("structure_chembl")) {
                        indexOfChembl = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("structure_pubchem")) {
                        indexOfPubchem = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("taxon")) {
                        indexOfWDtaxonLink = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("taxon_name")) {
                        indexOfTaxonName = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("taxon_id_gbif")) {
                        indexOfTaxonIdGbif = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("taxon_id_ncbi")) {
                        indexOfTaxonIdNCBI = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("reference")) {
                        indexOfReferenceWDLink = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("reference_doi")) {
                        indexOfReferenceDOI = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("reference_title")) {
                        indexOfReferenceTitle = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("unique_name")) {
                        indexOfUniqueName = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("ott_id")) {
                        indexOfOttId = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("link")) {
                        indexOfOttLink = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("name.domain")) {
                        indexOfNameDomain = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("unique_name.domain")) {
                        indexOfUniqueNameDomain = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("ott_id.domain")) {
                        indexOfOttDomain = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("name.kingdom")) {
                        indexOfNameKingdom = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("unique_name.kingdom")) {
                        indexOfUniqueNameKingdom = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("ott_id.kingdom")) {
                        indexOfOttKingdom = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("name.phylum")) {
                        indexOfNamePhylum = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("unique_name.phylum")) {
                        indexOfUniqueNamePhylum = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("ott_id.phylum")) {
                        indexOfOttPhylum = header.indexOf(item);
                    }


                    if (item.toLowerCase().equals("name.class")) {
                        indexOfNameClass = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("unique_name.class")) {
                        indexOfUniqueNameClass = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("ott_id.class")) {
                        indexOfOttClass = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("name.order")) {
                        indexOfNameOrder = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("unique_name.order")) {
                        indexOfUniqueNameOrder = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("ott_id.order")) {
                        indexOfOttOrder = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("name.family")) {
                        indexOfNameFamily = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("unique_name.family")) {
                        indexOfUniqueNameFamily = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("ott_id.family")) {
                        indexOfOttFamily = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("name.genus")) {
                        indexOfNameGenus = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("unique_name.genus")) {
                        indexOfUniqueNameGenus = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("ott_id.genus")) {
                        indexOfOttGenus = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("name.species")) {
                        indexOfNameSpecies = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("unique_name.species")) {
                        indexOfUniqueNameSpecies = header.indexOf(item);
                    }

                    if (item.toLowerCase().equals("ott_id.species")) {
                        indexOfOttSpecies = header.indexOf(item);
                    }
*/

                }




                //read the rest of the file
                int count = 1;
                String line;

                while ((line = bufferedReader.readLine()) != null && count <= 6000000) {


                    ArrayList<String> dataline = null ;
                    if(file.getName().toLowerCase().endsWith("csv")) {
                        dataline = new ArrayList<String>(Arrays.asList(line.replace("\"", "").split(",")));
                    }
                    else if(file.getName().toLowerCase().endsWith("tsv")){
                        dataline = new ArrayList<String>(Arrays.asList(line.replace("\"", "").split("\t")));

                    }
                    try {


                        //getting the 3D structure


                        IAtomContainer molecule = null;

                        String smiles=null;
                        String inchi=null;
                        String inchikey=null;

                        String smiles2D=null;
                        String inchi2D=null;
                        String inchikey2D = null;

                        if (indexOfStructureCleanedSmiles != null && dataline.size() >= indexOfStructureCleanedSmiles + 1) {

                            try {
                                molecule = sp.parseSmiles(dataline.get(indexOfStructureCleanedSmiles));

                                molecule.setProperty("3D_SMILES", dataline.get(indexOfStructureCleanedSmiles));
                                smiles = dataline.get(indexOfStructureCleanedSmiles);


                                try {
                                    if (indexOfStructureCleanedInchi != null) {
                                        molecule.setProperty("3D_INCHI", dataline.get(indexOfStructureCleanedInchi));
                                        inchi =  dataline.get(indexOfStructureCleanedInchi);

                                    }
                                    if (indexOfStructureCleanedInchikey != null) {
                                        molecule.setProperty("3D_INCHIKEY", dataline.get(indexOfStructureCleanedInchikey));
                                        inchikey =  dataline.get(indexOfStructureCleanedInchikey);
                                    }
                                } catch (IndexOutOfBoundsException e) {
                                    System.out.println("Something went wrong with indexes in " + file.getName());
                                    System.out.println(count);
                                    System.out.println(dataline.toString());
                                }
                            }catch (InvalidSmilesException e){
                                //try to read the inchi at least
                                if (indexOfStructureCleanedInchi != null && dataline.size() >= indexOfStructureCleanedInchi + 1){
                                    // READING InCHI
                                    InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
                                    InChIToStructure intostruct = factory.getInChIToStructure(dataline.get(indexOfStructureCleanedInchi), DefaultChemObjectBuilder.getInstance());

                                    INCHI_RET ret = intostruct.getReturnStatus();
                                    if (ret == INCHI_RET.WARNING) {
                                        // Structure generated, but with warning message
                                        System.out.println("InChI warning: " + intostruct.getMessage());
                                    } else if (ret != INCHI_RET.OKAY) {
                                        // Structure generation failed
                                        throw new CDKException("Structure generation failed failed: " + ret.toString() + " [" + intostruct.getMessage() + "]");
                                    }

                                    molecule = intostruct.getAtomContainer();

                                    molecule.setProperty("3D_SMILES", dataline.get(indexOfStructureCleanedSmiles));
                                    smiles = dataline.get(indexOfStructureCleanedSmiles);
                                    molecule.setProperty("3D_INCHI", dataline.get(indexOfStructureCleanedInchi));
                                    inchi =  dataline.get(indexOfStructureCleanedInchi);
                                    molecule.setProperty("3D_INCHIKEY", dataline.get(indexOfStructureCleanedInchikey));
                                    inchikey =  dataline.get(indexOfStructureCleanedInchikey);




                                }
                            }

                        } else if (indexOfStructureCleanedInchi != null && dataline.size() >= indexOfStructureCleanedInchi + 1) {
                            // READING InCHI
                            InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
                            InChIToStructure intostruct = factory.getInChIToStructure(dataline.get(indexOfStructureCleanedInchi), DefaultChemObjectBuilder.getInstance());

                            INCHI_RET ret = intostruct.getReturnStatus();
                            if (ret == INCHI_RET.WARNING) {
                                // Structure generated, but with warning message
                                System.out.println("InChI warning: " + intostruct.getMessage());
                            } else if (ret != INCHI_RET.OKAY) {
                                // Structure generation failed
                                throw new CDKException("Structure generation failed failed: " + ret.toString() + " [" + intostruct.getMessage() + "]");
                            }

                            molecule = intostruct.getAtomContainer();
                            inchi =  dataline.get(indexOfStructureCleanedInchi);

                            if (indexOfStructureCleanedInchikey != null) {
                                molecule.setProperty("3D_INCHIKEY", dataline.get(indexOfStructureCleanedInchikey));
                                inchikey =  dataline.get(indexOfStructureCleanedInchikey);

                            }
                        }





                        if (molecule != null) {


                            molecule = moleculeChecker.checkMolecule(molecule);



                            if (molecule != null) {

                                LOTUSSourceNaturalProduct lotusSourceNaturalProduct = new LOTUSSourceNaturalProduct();

                                lotusSourceNaturalProduct.setSmiles3d(smiles);
                                lotusSourceNaturalProduct.setInchi3D(inchi);
                                lotusSourceNaturalProduct.setInchikey3D(inchikey);



                                lotusSourceNaturalProduct.setTotalAtomNumber(molecule.getAtomCount());

                                int heavyAtomCount = 0;
                                for(IAtom a : molecule.atoms()){
                                    if(!a.getSymbol().equals("H")){
                                        heavyAtomCount=heavyAtomCount+1;
                                    }
                                }
                                lotusSourceNaturalProduct.setHeavyAtomNumber(heavyAtomCount);






                                try {
                                    String absoluteSmiles = absoluteSmilesGenerator.create(molecule);
                                    lotusSourceNaturalProduct.setAbsoluteSmiles(absoluteSmiles);


                                }catch(IllegalArgumentException e){
                                    System.out.println("Could not make smiles for "+inchikey);
                                }

                                //Adding 2D information

                                if (indexOfstructureCleaned_smiles2D != null) {
                                    molecule.setProperty("2D_SMILES", dataline.get(indexOfstructureCleaned_smiles2D));
                                    smiles2D =  dataline.get(indexOfstructureCleaned_smiles2D);

                                }

                                if (indexOfstructureCleaned_inchi2D != null) {
                                    molecule.setProperty("2D_INCHI", dataline.get(indexOfstructureCleaned_inchi2D));
                                    inchi2D =  dataline.get(indexOfstructureCleaned_inchi2D);

                                }

                                if (indexOfstructureCleaned_inchikey2D != null) {
                                    molecule.setProperty("2D_INCHIKEY", dataline.get(indexOfstructureCleaned_inchikey2D));
                                    inchikey2D =  dataline.get(indexOfstructureCleaned_inchikey2D);

                                }

                                lotusSourceNaturalProduct.setSmiles2d(smiles2D);
                                lotusSourceNaturalProduct.setInchi2D(inchi2D);
                                lotusSourceNaturalProduct.setInchikey2D(inchikey2D);

                                //System.out.println(smiles2D);





                                //getting the molecule name
                                if(indexOfstructureCleaned_nameTraditional != null){
                                    lotusSourceNaturalProduct.setTraditionalName( dataline.get(indexOfstructureCleaned_nameTraditional));
                                }

                                if(indexOfstructureCleaned_nameIupac != null){
                                    lotusSourceNaturalProduct.setIupacName(dataline.get(indexOfstructureCleaned_nameIupac));
                                }

                                /*
                                //getting cross-refs
                                lotusSourceNaturalProduct.setLinkToWDstructure(dataline.get(indexOflinkToWDstructure));
                                if(indexOfCAS != null){
                                    lotusSourceNaturalProduct.setCas(dataline.get(indexOfCAS));
                                }

                                lotusSourceNaturalProduct.xrefs = new Hashtable<>();
                                if(indexOfChebi != null){
                                    lotusSourceNaturalProduct.xrefs.put("CHEBI", dataline.get(indexOfChebi));
                                }
                                if(indexOfChembl != null){
                                    lotusSourceNaturalProduct.xrefs.put("CHEMBL", dataline.get(indexOfChembl));
                                }
                                if(indexOfPubchem != null){
                                    lotusSourceNaturalProduct.xrefs.put("PUBCHEM", dataline.get(indexOfPubchem));
                                }

                                   */
                                TaxonomyReferenceObject taxonomyReferenceObject = new TaxonomyReferenceObject();



                                //taxonomy & reference

                                Integer ixSuperkingdom = null;
                                Integer ixKingdom = null;
                                Integer ixDomain = null;
                                Integer ixPhylum = null;
                                Integer ixClass = null;
                                Integer ixOrder = null;
                                Integer ixFamily = null;
                                Integer ixGenus = null;
                                Integer ixSpecies =null;

                                if(indexOfReferenceCleanedDOI != null){
                                    taxonomyReferenceObject.setReferenceDOI(dataline.get(indexOfReferenceCleanedDOI).replace(".", "$x$x$"));
                                }

                                if(indexOfReferenceCleanedTitle != null){
                                    taxonomyReferenceObject.setReferenceTitle(dataline.get(indexOfReferenceCleanedTitle));
                                }

                                if(indexOfOrganismCleaned_dbTaxoTaxonRanks != null){
                                    taxonomyReferenceObject.setRanks(dataline.get(indexOfOrganismCleaned_dbTaxoTaxonRanks));



                                    ArrayList<String> ranks = new ArrayList<String>(Arrays.asList(dataline.get(indexOfOrganismCleaned_dbTaxoTaxonRanks).split("\\|")));


                                    for(String r : ranks){
                                        if(r.equals("superkingdom")){
                                            ixSuperkingdom = ranks.indexOf(r);
                                        }
                                        if(r.equals("kingdom")){
                                            ixKingdom = ranks.indexOf(r);
                                        }
                                        if(r.equals("domain")){
                                            ixDomain = ranks.indexOf(r);
                                        }
                                        if(r.equals("phylum")){
                                            ixPhylum = ranks.indexOf(r);
                                        }
                                        if(r.equals("class")){
                                            ixClass = ranks.indexOf(r);
                                        }
                                        if(r.equals("order")){
                                            ixOrder = ranks.indexOf(r);
                                        }
                                        if(r.equals("family")){
                                            ixFamily = ranks.indexOf(r);
                                        }
                                        if(r.equals("genus")){
                                            ixGenus = ranks.indexOf(r);
                                        }
                                        if(r.equals("species")){
                                            ixSpecies = ranks.indexOf(r);
                                        }

                                    }
                                }

                                if(indexOfOrganismCleaned_dbTaxoTaxonomy != null){
                                    taxonomyReferenceObject.setAll_taxonomy(dataline.get(indexOfOrganismCleaned_dbTaxoTaxonomy));

                                    ArrayList<String> taxo = new ArrayList<String>(Arrays.asList(dataline.get(indexOfOrganismCleaned_dbTaxoTaxonomy).split("\\|")));

                                    if(ixSuperkingdom != null){
                                        taxonomyReferenceObject.setSuperkingdom(taxo.get(ixSuperkingdom));
                                    }
                                    if(ixKingdom != null){
                                        taxonomyReferenceObject.setKingdom(taxo.get(ixKingdom));
                                    }
                                    if(ixDomain != null){
                                        taxonomyReferenceObject.setDomain(taxo.get(ixDomain));
                                    }
                                    if(ixPhylum != null){
                                        taxonomyReferenceObject.setPhylum(taxo.get(ixPhylum));
                                    }
                                    if(ixClass != null){
                                        taxonomyReferenceObject.setClassx(taxo.get(ixClass));
                                    }
                                    if(ixOrder != null){
                                        taxonomyReferenceObject.setOrganism_taxo_db(taxo.get(ixOrder));
                                    }
                                    if(ixFamily != null){
                                        taxonomyReferenceObject.setFamily(taxo.get(ixFamily));
                                    }
                                    if(ixGenus != null){
                                        taxonomyReferenceObject.setGenus(taxo.get(ixGenus));
                                    }
                                    if(ixSpecies != null){
                                        taxonomyReferenceObject.setSpecies(taxo.get(ixSpecies));
                                    }

                                }

                                if (indexOfOrganismCleanedId != null) {
                                    taxonomyReferenceObject.setCleaned_organism_id(dataline.get(indexOfOrganismCleanedId));
                                }

                                if(indexOfOrganismCleaned != null){
                                    taxonomyReferenceObject.setOrganism_value(dataline.get(indexOfOrganismCleaned));
                                }

                                if(indexOfOrganismCleanedDBTaxo != null){
                                    taxonomyReferenceObject.setOrganism_taxo_db(dataline.get(indexOfOrganismCleanedDBTaxo));
                                }








                                /*
                                //citation reference and doi
                                if(indexOfReferenceWDLink != null) {
                                    taxonomyReferenceObject.setReference_wd(dataline.get(indexOfReferenceWDLink));
                                }
                                if(indexOfReferenceDOI != null){
                                    taxonomyReferenceObject.setReference_doi(dataline.get(indexOfReferenceDOI));
                                }

                                if(indexOfReferenceTitle != null){
                                    taxonomyReferenceObject.setReference_title(dataline.get(indexOfReferenceTitle));
                                }

                                */


                                lotusSourceNaturalProduct.taxonomyReferenceObject = taxonomyReferenceObject;
                                //timestamp
                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                                LocalDate localDate = LocalDate.now();

                                molecule.setProperty("ACQUISITION_DATE", dtf.format(localDate));

                                //LOTUSSourceNaturalProduct lotusSourceNaturalProduct = ac2snp.createSNPlInstance(molecule);


                                if (!moleculeChecker.isForbiddenMolecule(molecule)) {
                                    LOTUSSourceNaturalProductRepository.save(lotusSourceNaturalProduct);
                                }
                            }
                        } else {
                            System.out.println("No molecular structure detected");
                        }


                    } catch (CDKException e) {
                        e.printStackTrace();
                        System.out.println(line);
                    }

                    count++;
                }
            }

        } catch (IOException e ) {
            e.printStackTrace();
        }



    }

    @Override
    public ArrayList<IAtomContainer> returnCorrectMolecules() {
        return this.listOfMolecules;
    }

    @Override
    public String returnSource() {
        return this.source;
    }
}
