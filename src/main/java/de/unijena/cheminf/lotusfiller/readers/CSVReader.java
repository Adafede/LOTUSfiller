package de.unijena.cheminf.lotusfiller.readers;

import de.unijena.cheminf.lotusfiller.misc.BeanUtil;
import de.unijena.cheminf.lotusfiller.misc.DatabaseTypeChecker;
import de.unijena.cheminf.lotusfiller.misc.MoleculeChecker;
import de.unijena.cheminf.lotusfiller.mongocollections.LOTUSLOTUSSourceNaturalProductRepository;
import de.unijena.cheminf.lotusfiller.mongocollections.LOTUSSourceNaturalProduct;
import de.unijena.cheminf.lotusfiller.services.AtomContainerToSourceNaturalProductService;
import net.sf.jniinchi.INCHI_OPTION;
import net.sf.jniinchi.INCHI_RET;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.exception.InvalidSmilesException;
import org.openscience.cdk.inchi.InChIGenerator;
import org.openscience.cdk.inchi.InChIGeneratorFactory;
import org.openscience.cdk.inchi.InChIToStructure;
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
import java.util.List;

public class CSVReader implements Reader {


    File file;
    ArrayList<IAtomContainer> listOfMolecules;
    private LineNumberReader inchiReader;
    LOTUSLOTUSSourceNaturalProductRepository LOTUSSourceNaturalProductRepository;
    AtomContainerToSourceNaturalProductService ac2snp;
    MoleculeChecker moleculeChecker;
    DatabaseTypeChecker databaseTypeChecker;
    String source;

    public CSVReader(){
        this.listOfMolecules = new ArrayList<IAtomContainer>();
        LOTUSSourceNaturalProductRepository = BeanUtil.getBean(LOTUSLOTUSSourceNaturalProductRepository.class);
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
                header = new ArrayList<String>(Arrays.asList(bufferedReader.readLine().split(",")));
            } else if (file.getName().toLowerCase().endsWith("tsv")) {
                header = new ArrayList<String>(Arrays.asList(bufferedReader.readLine().split("\t")));
            }

            if (header != null){
                //System.out.println(header);

                Integer indexOfID = null;

                Integer indexOfReference = null;
                Integer indexOfCitation = null;
                Integer indexOfReferenceTitle = null;
                Integer indexOfDOI = null;
                Integer indexOfSMILES = null;
                Integer indexOfInchi = null;
                Integer indexOfInchikey = null;
                Integer indexOfTaxonomy = null;

                for (String item : header) {



                    if (item.toLowerCase().contains("ref")) {
                        indexOfReference = header.indexOf(item);
                    }
                    if (item.toLowerCase().contains("citation")) {
                        indexOfCitation = header.indexOf(item);
                    }
                    if (item.contains("referenceCleanedDoi")) {
                        indexOfDOI = header.indexOf(item);
                    }
                    if (item.contains("referenceCleanedTitle")){
                        indexOfReferenceTitle = header.indexOf(item);
                    }
                    if (item.contains("structureCleanedSmiles")) {
                        indexOfSMILES = header.indexOf(item);
                    }
                    if (item.contains("structureCleanedInchi") && !item.toLowerCase().contains("inchikey")) {
                        indexOfInchi = header.indexOf(item);
                    }
                    if (item.contains("structureCleanedInchikey3D")) {
                        indexOfInchikey = header.indexOf(item);
                    }
                    if (item.contains("organismCleaned_dbTaxoTaxonomy")) {
                        indexOfTaxonomy = header.indexOf(item);
                    }



                }




                //read the rest of the file
                int count = 1;
                String line;

                while ((line = bufferedReader.readLine()) != null && count <= 600000) {


                    ArrayList<String> dataline = null ;
                    if(file.getName().toLowerCase().endsWith("csv")) {
                        dataline = new ArrayList<String>(Arrays.asList(line.split(",")));
                    }
                    else if(file.getName().toLowerCase().endsWith("tsv")){
                        dataline = new ArrayList<String>(Arrays.asList(line.split("\t")));

                    }
                    try {

                        IAtomContainer molecule = null;

                        if (indexOfSMILES != null && dataline.size() >= indexOfSMILES + 1) {

                            try {
                                molecule = sp.parseSmiles(dataline.get(indexOfSMILES));

                                molecule.setProperty("FILE_ORIGIN", file.getName().replace(".csv", ""));
                                molecule.setProperty("SOURCE", source);
                                molecule.setProperty("ORIGINAL_SMILES", dataline.get(indexOfSMILES));


                                try {
                                    if (indexOfInchi != null) {
                                        molecule.setProperty("ORIGINAL_INCHI", dataline.get(indexOfInchi));

                                    }
                                    if (indexOfInchikey != null) {
                                        molecule.setProperty("ORIGINAL_INCHIKEY", dataline.get(indexOfInchikey));
                                    }
                                } catch (IndexOutOfBoundsException e) {
                                    System.out.println("Something went wrong with indexes in " + file.getName());
                                    System.out.println(count);
                                    System.out.println(dataline.toString());
                                }
                            }catch (InvalidSmilesException e){
                                //try to read the inchi at least
                                if (indexOfInchi != null && dataline.size() >= indexOfInchi + 1){
                                    // READING InCHI
                                    InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
                                    InChIToStructure intostruct = factory.getInChIToStructure(dataline.get(indexOfInchi), DefaultChemObjectBuilder.getInstance());

                                    INCHI_RET ret = intostruct.getReturnStatus();
                                    if (ret == INCHI_RET.WARNING) {
                                        // Structure generated, but with warning message
                                        System.out.println("InChI warning: " + intostruct.getMessage());
                                    } else if (ret != INCHI_RET.OKAY) {
                                        // Structure generation failed
                                        throw new CDKException("Structure generation failed failed: " + ret.toString() + " [" + intostruct.getMessage() + "]");
                                    }

                                    molecule = intostruct.getAtomContainer();

                                    molecule.setProperty("FILE_ORIGIN", file.getName().replace(".csv", ""));
                                    molecule.setProperty("SOURCE", source);

                                }
                            }

                        } else if (indexOfInchi != null && dataline.size() >= indexOfInchi + 1) {
                            // READING InCHI
                            InChIGeneratorFactory factory = InChIGeneratorFactory.getInstance();
                            InChIToStructure intostruct = factory.getInChIToStructure(dataline.get(indexOfInchi), DefaultChemObjectBuilder.getInstance());

                            INCHI_RET ret = intostruct.getReturnStatus();
                            if (ret == INCHI_RET.WARNING) {
                                // Structure generated, but with warning message
                                System.out.println("InChI warning: " + intostruct.getMessage());
                            } else if (ret != INCHI_RET.OKAY) {
                                // Structure generation failed
                                throw new CDKException("Structure generation failed failed: " + ret.toString() + " [" + intostruct.getMessage() + "]");
                            }

                            molecule = intostruct.getAtomContainer();
                            if (indexOfInchikey != null) {
                                molecule.setProperty("ORIGINAL_INCHIKEY", dataline.get(indexOfInchikey));
                            }
                        }

                        if (molecule != null) {
                            if (indexOfID != null) {
                                molecule.setID(dataline.get(indexOfID));
                                molecule.setProperty("ID", dataline.get(indexOfID));
                            } else if (indexOfName != null) {
                                molecule.setID(dataline.get(indexOfName));
                                molecule.setProperty("ID", dataline.get(indexOfName));
                            } else {
                                molecule.setID(Integer.toString(count));
                                molecule.setProperty("ID", Integer.toString(count));
                            }

                            molecule = moleculeChecker.checkMolecule(molecule);

                            if (molecule != null) {
                                try {
                                    List options = new ArrayList();
                                    options.add(INCHI_OPTION.SNon);
                                    options.add(INCHI_OPTION.ChiralFlagOFF);
                                    options.add(INCHI_OPTION.AuxNone);
                                    InChIGenerator gen = InChIGeneratorFactory.getInstance().getInChIGenerator(molecule, options);

                                    molecule.setProperty("SIMPLE_INCHI", gen.getInchi());
                                    molecule.setProperty("SIMPLE_INCHIKEY", gen.getInchiKey());


                                } catch (CDKException e) {
                                    Integer totalBonds = molecule.getBondCount();
                                    Integer ib = 0;
                                    while (ib < totalBonds) {

                                        IBond b = molecule.getBond(ib);
                                        if (b.getOrder() == IBond.Order.UNSET) {
                                            b.setOrder(IBond.Order.SINGLE);

                                        }
                                        ib++;
                                    }
                                    List options = new ArrayList();
                                    options.add(INCHI_OPTION.SNon);
                                    options.add(INCHI_OPTION.ChiralFlagOFF);
                                    options.add(INCHI_OPTION.AuxNone);
                                    InChIGenerator gen = InChIGeneratorFactory.getInstance().getInChIGenerator(molecule, options);

                                    molecule.setProperty("SIMPLE_INCHI", gen.getInchi());
                                    molecule.setProperty("SIMPLE_INCHIKEY", gen.getInchiKey());

                                }

                                String simpleSmiles = smilesGenerator.create(molecule);
                                molecule.setProperty("SIMPLE_SMILES", simpleSmiles);
                                try {
                                    String absoluteSmiles = absoluteSmilesGenerator.create(molecule);
                                    if (!absoluteSmiles.equals(simpleSmiles) && absoluteSmiles.contains("@")) {
                                        molecule.setProperty("ABSOLUTE_SMILES", absoluteSmiles);
                                    }

                                }catch(IllegalArgumentException e){
                                    System.out.println("Could not make smiles for "+simpleSmiles);
                                }




                                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                                LocalDate localDate = LocalDate.now();

                                molecule.setProperty("ACQUISITION_DATE", dtf.format(localDate));


                                LOTUSSourceNaturalProduct lotusSourceNaturalProduct = ac2snp.createSNPlInstance(molecule);

                                String taxa = databaseTypeChecker.checkKingdom(this.source);
                                if (taxa.equals("mixed")) {
                                    //do things db by db
                                    if (source.equals("nubbedb")) {
                                        //there is a p at the beginning of each id for plants
                                        if (molecule.getID().startsWith("p.")) {
                                            taxa = "plants";
                                        } else {
                                            taxa = "animals";
                                        }
                                    } else if (source.equals("npatlas")) {
                                        if (molecule.getID().startsWith("b")) {
                                            taxa = "bacteria";
                                        } else {
                                            taxa = "fungi";
                                        }
                                    }  else if (source.equals("biofacquim")) {
                                        taxa = dataline.get(indexOfKingdom);
                                    } else {
                                        taxa = "notax";
                                    }
                                }
                                lotusSourceNaturalProduct.setOrganismText(new ArrayList<String>());
                                lotusSourceNaturalProduct.organismText.add(taxa);

                                if (indexOfKingdom != null && dataline.size() >= indexOfKingdom + 1) {
                                    if (dataline.get(indexOfKingdom).toLowerCase().contains("bacteri")) {
                                        lotusSourceNaturalProduct.organismText.add("bacteria");
                                    } else if (dataline.get(indexOfKingdom).toLowerCase().contains("fung")) {
                                        lotusSourceNaturalProduct.organismText.add("fungi");
                                    } else if (dataline.get(indexOfKingdom).toLowerCase().contains("plant")) {
                                        lotusSourceNaturalProduct.organismText.add("plants");
                                    }else if(dataline.get(indexOfKingdom).toLowerCase().contains("animal")){
                                        lotusSourceNaturalProduct.organismText.add("animals");
                                    }else{
                                        if(!dataline.get(indexOfKingdom).equals("") && !dataline.get(indexOfKingdom).equals("-") && !dataline.get(indexOfKingdom).equals(" ")) {
                                            lotusSourceNaturalProduct.organismText.add(dataline.get(indexOfKingdom));
                                        }
                                    }

                                }
                                if (indexOfGenus != null && dataline.size() >= indexOfGenus + 1) {
                                    if(source.equals("np_atlas_2019_12")){
                                        //join genus and species
                                        String realSpecies = dataline.get(indexOfGenus)+" "+dataline.get(indexOfSpecies);
                                        lotusSourceNaturalProduct.organismText.add(realSpecies);
                                    }else {
                                        lotusSourceNaturalProduct.organismText.add(dataline.get(indexOfGenus));
                                    }
                                }
                                if (indexOfSpecies != null && dataline.size() >= indexOfSpecies + 1) {
                                    if(!source.equals("np_atlas_2019_12")){
                                        if(source.equals("vietherb") || source.equals("knapsack")){
                                            String[] species = dataline.get(indexOfSpecies).split(";");
                                            for(String speciesString : species){
                                                    String spm = speciesString.replace("\'", "");
                                                    lotusSourceNaturalProduct.organismText.add(spm);


                                            }
                                        }else {
                                            lotusSourceNaturalProduct.organismText.add(dataline.get(indexOfSpecies));
                                        }

                                    }

                                }

                                if (indexOfName != null){
                                    lotusSourceNaturalProduct.setName(dataline.get(indexOfName));
                                }

                                if (indexOfSynonym != null){
                                    lotusSourceNaturalProduct.synonyms = new ArrayList<>();

                                    String[] list = dataline.get(indexOfSynonym).split(";");
                                    for(String e : list){
                                        lotusSourceNaturalProduct.synonyms.add(e);
                                    }
                                    if(indexOfName == null){
                                        lotusSourceNaturalProduct.setName(list[0]);
                                    }
                                }

                                //GEOGRAPHY
                                lotusSourceNaturalProduct.setContinent(databaseTypeChecker.checkContinent(this.source));
                                if (indexOfGeo != null && dataline.size() >= indexOfGeo + 1) {
                                    lotusSourceNaturalProduct.geographicLocation = new ArrayList<>();
                                    lotusSourceNaturalProduct.geographicLocation.add(dataline.get(indexOfGeo));
                                }

                                //CAS
                                if(indexOfCas !=null){
                                    lotusSourceNaturalProduct.setCas(dataline.get(indexOfCas));
                                }

                                //citation reference and doi
                                if ((indexOfCitation != null && dataline.size() >= indexOfCitation + 1) || (indexOfDOI != null && dataline.size() >= indexOfDOI + 1) || (indexOfReference != null && dataline.size() >= indexOfReference + 1)) {
                                    lotusSourceNaturalProduct.citation = new ArrayList<>();
                                    if (indexOfCitation != null) {
                                        lotusSourceNaturalProduct.citation.add(dataline.get(indexOfCitation));
                                    }
                                    if (indexOfDOI != null) {
                                        lotusSourceNaturalProduct.citation.add(dataline.get(indexOfDOI));
                                    }
                                    if (indexOfReference != null) {
                                        lotusSourceNaturalProduct.citation.add(dataline.get(indexOfReference));
                                    }
                                }

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
