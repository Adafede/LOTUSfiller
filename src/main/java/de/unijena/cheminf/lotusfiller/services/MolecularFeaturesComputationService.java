package de.unijena.cheminf.lotusfiller.services;

import de.unijena.cheminf.lotusfiller.mongocollections.*;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.aromaticity.Aromaticity;
import org.openscience.cdk.aromaticity.ElectronDonation;
import org.openscience.cdk.atomtype.CDKAtomTypeMatcher;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.fingerprint.CircularFingerprinter;
import org.openscience.cdk.fingerprint.ExtendedFingerprinter;
import org.openscience.cdk.fingerprint.PubchemFingerprinter;
import org.openscience.cdk.fingerprint.SubstructureFingerprinter;
import org.openscience.cdk.graph.Cycles;
import org.openscience.cdk.hash.MoleculeHashGenerator;
import org.openscience.cdk.interfaces.*;
import org.openscience.cdk.qsar.DescriptorValue;
import org.openscience.cdk.qsar.descriptors.molecular.*;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.silent.SilentChemObjectBuilder;
import org.openscience.cdk.smiles.SmiFlavor;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.CDKHydrogenAdder;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinder;
import org.openscience.cdk.tools.ErtlFunctionalGroupsFinderUtility;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.AtomTypeManipulator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class MolecularFeaturesComputationService {

    @Autowired
    LotusUniqueNaturalProductRepository lotusUniqueNaturalProductRepository;

    @Autowired
    AtomContainerToUniqueNaturalProductService atomContainerToUniqueNaturalProductService;




    PubchemFingerprinter pubchemFingerprinter = new PubchemFingerprinter( SilentChemObjectBuilder.getInstance() );

    CircularFingerprinter circularFingerprinter = new CircularFingerprinter(CircularFingerprinter.CLASS_ECFP4);


    SubstructureFingerprinter substructureFingerprinter = new SubstructureFingerprinter();

    ExtendedFingerprinter extendedFingerprinter = new ExtendedFingerprinter();

    Aromaticity aromaticityModel = new Aromaticity(ElectronDonation.daylight(), Cycles.or(Cycles.all(), Cycles.cdkAromaticSet()));
    ErtlFunctionalGroupsFinder ertlFunctionalGroupsFinder  = ErtlFunctionalGroupsFinderUtility.getErtlFunctionalGroupsFinderGeneralizingMode();
    MoleculeHashGenerator efgHashGenerator = ErtlFunctionalGroupsFinderUtility.getFunctionalGroupHashGenerator();
    SmilesGenerator efgSmilesGenerator = new SmilesGenerator(SmiFlavor.Unique | SmiFlavor.UseAromaticSymbols);

    SmilesGenerator uniqueSmilesGenerator = new SmilesGenerator(SmiFlavor.Unique);


    public void createPubchemBitCounts(){
        System.out.println("Creating bit counts");

        List<String> allLotusIds = lotusUniqueNaturalProductRepository.findAllLotusIds();
        System.out.println("all lotus ids:");
        System.out.println(allLotusIds);

        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());

        for(String lotus_id : allLotusIds){

            LotusUniqueNaturalProduct unp = lotusUniqueNaturalProductRepository.findByLotus_id(lotus_id).get(0);

            IAtomContainer molecule = null;
            try {
                molecule = sp.parseSmiles(unp.smiles);



                BitSet s = pubchemFingerprinter.getBitFingerprint(molecule).asBitSet();
                ArrayList<Integer> indexes = new ArrayList<>();
                for (int i = s.nextSetBit(0); i != -1; i = s.nextSetBit(i + 1)) {
                    indexes.add(i);
                }

                PubchemFingerPrintsCounts pcClass = new PubchemFingerPrintsCounts(indexes.size(),indexes );

                unp.pfCounts = pcClass;

                unp.pubfp = null;

                lotusUniqueNaturalProductRepository.save(unp);

            } catch (CDKException e) {
                e.printStackTrace();
            }


        }



        System.out.println("done");

    }

/*
    public void generateUniqueSmiles(){
        System.out.println("Generating nice smiles");
        List<String> allInchiKeys = lotusUniqueNaturalProductRepository.findAllInchiKeys();
        System.out.println("here are the retrieved ids:");
        System.out.println(allInchiKeys);

        SmilesParser sp = new SmilesParser(DefaultChemObjectBuilder.getInstance());

        for(String inchikey : allInchiKeys){

            LotusUniqueNaturalProduct unp = lotusUniqueNaturalProductRepository.findByInchikey(inchikey).get(0);

            IAtomContainer molecule = null;
            try {
                molecule = sp.parseSmiles(unp.smiles3D);

                AtomContainerManipulator.suppressHydrogens(molecule); // removing explicit hydrogens

                unp.unique_smiles = uniqueSmilesGenerator.create(molecule);

                lotusUniqueNaturalProductRepository.save(unp);

            } catch (CDKException e) {
                e.printStackTrace();
            }


        }
        System.out.println("done");
    }
*/


    public void convertToBitSet(){
        System.out.println("Convert bit fingerprints to bitsets");

        List<LotusUniqueNaturalProduct> allNP = lotusUniqueNaturalProductRepository.findAll();

        for(LotusUniqueNaturalProduct np : allNP) {
            if ( np.pubchemBitsString == null || np.pubchemBitsString == "") {

                IAtomContainer ac = atomContainerToUniqueNaturalProductService.createAtomContainer(np);

                // Addition of implicit hydrogens & atom typer
                CDKAtomTypeMatcher matcher = CDKAtomTypeMatcher.getInstance(ac.getBuilder());
                CDKHydrogenAdder adder = CDKHydrogenAdder.getInstance(ac.getBuilder());
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
                AtomContainerManipulator.convertImplicitToExplicitHydrogens(ac);
                AtomContainerManipulator.removeNonChiralHydrogens(ac);


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

                    np = computeFingerprints(np);

                    lotusUniqueNaturalProductRepository.save(np);


                } catch (CDKException | UnsupportedOperationException e) {
                    e.printStackTrace();
                }
            }

        }

        System.out.println("done");
    }



    public void doWorkRecompute(){
        System.out.println("Calculating additional features for unique molecules (only incomplete)");

        List<LotusUniqueNaturalProduct> allNP = lotusUniqueNaturalProductRepository.findAllByApolComputed();
        for(LotusUniqueNaturalProduct np : allNP){

            np = computeFeatures(np);
            np = computeFingerprints(np);

            lotusUniqueNaturalProductRepository.save(np);
        }
        System.out.println("done");
    }




    public void doWork(){
        System.out.println("Calculating additional features for unique molecules");


        List<LotusUniqueNaturalProduct> allNP = lotusUniqueNaturalProductRepository.findAll();

        for(LotusUniqueNaturalProduct np : allNP){

            np = computeFeatures(np);
            //np = computeFingerprints(np);
            np = computeErtlFunctionalGroups(np);

            lotusUniqueNaturalProductRepository.save(np);
        }



        System.out.println("done");
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
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(ac);
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

            s = substructureFingerprinter.getBitFingerprint(ac).asBitSet().toString();
            pcl = new ArrayList<>();
            s = s.replace(" ", "");s = s.replace("\"", "");s = s.replace("{", "");s = s.replace("}", "");
            sl = s.split(",");
            for(String c : sl){
                try {
                    pcl.add(Integer.parseInt(c));
                }catch (NumberFormatException e){ e.printStackTrace(); }
            }
            np.setSubstructureFingerprint(pcl);


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

                np.setPubchemBits( bitsOn.toByteArray());
                np.setPubchemBitsString(pubchemBitString);

                np = computeFingerprints(np);


            } catch (CDKException | UnsupportedOperationException e) {
                e.printStackTrace();
            }


        } catch (CDKException | UnsupportedOperationException e) {
            e.printStackTrace();
        }
        return np;
    }


    public LotusUniqueNaturalProduct computeErtlFunctionalGroups(LotusUniqueNaturalProduct np){

        IAtomContainer ac = atomContainerToUniqueNaturalProductService.createAtomContainer(np);
        List<IAtomContainer> functionalGroupsGeneralized;

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
        AtomContainerManipulator.convertImplicitToExplicitHydrogens(ac);
        AtomContainerManipulator.removeNonChiralHydrogens(ac);

        try{

            ac = ErtlFunctionalGroupsFinderUtility.applyFiltersAndPreprocessing(ac, aromaticityModel);
            functionalGroupsGeneralized = ertlFunctionalGroupsFinder.find(ac, false);
            if (!functionalGroupsGeneralized.isEmpty()) {

                np.ertlFunctionalFragments = new Hashtable<>();
                np.ertlFunctionalFragmentsPseudoSmiles = new Hashtable<>();

                HashMap<Long, IAtomContainer> tmpResultsMap = new HashMap<>(functionalGroupsGeneralized.size(), 1);
                for (IAtomContainer functionalGroup : functionalGroupsGeneralized) {
                    Long hashCode = efgHashGenerator.generate(functionalGroup);
                    if (tmpResultsMap.keySet().contains(hashCode)) {
                        int tmpFrequency = tmpResultsMap.get(hashCode).getProperty("FREQUENCY");
                        tmpResultsMap.get(hashCode).setProperty("FREQUENCY", tmpFrequency + 1);
                    } else {
                        functionalGroup.setProperty("FREQUENCY", 1);
                        tmpResultsMap.put(hashCode, functionalGroup);
                    }
                }


                for (Long tmpHashCode : tmpResultsMap.keySet()) {
                    IAtomContainer tmpFunctionalGroup = tmpResultsMap.get(tmpHashCode);
                    String tmpFGSmilesCode = null;
                    try {
                        tmpFGSmilesCode = efgSmilesGenerator.create(tmpFunctionalGroup);

                        String tmpFGPseudoSmilesCode = ErtlFunctionalGroupsFinderUtility.createPseudoSmilesCode(tmpFunctionalGroup);


                        int tmpFrequency = tmpFunctionalGroup.getProperty("FREQUENCY");

                        np.ertlFunctionalFragments.put(tmpFGSmilesCode, tmpFrequency);
                        np.ertlFunctionalFragmentsPseudoSmiles.put(tmpFGPseudoSmilesCode, tmpFrequency);

                    } catch (CDKException e) {
                        e.printStackTrace();
                    }
                }




            }
    }catch(NullPointerException e){
            System.out.println("Failed to compute the Ertl functional groups for "+np.getInchikey());
        }





        return np;

    }


    public void predictStereochemistry(){
        //TODO - eventually one day, but very high combinatorics
    }




    public LotusUniqueNaturalProduct computeFeatures(LotusUniqueNaturalProduct np){

        //Constructors for descriptors
        IChemObjectBuilder builder = DefaultChemObjectBuilder.getInstance();

        IAtomContainer acFull = atomContainerToUniqueNaturalProductService.createAtomContainer(np);

        try {
            //AlogP
            ALOGPDescriptor alogpDescriptor = new ALOGPDescriptor();
            alogpDescriptor.initialise(builder);
            DescriptorValue alogpvalue = alogpDescriptor.calculate(acFull);
            DoubleArrayResult alogpresults = (DoubleArrayResult) alogpvalue.getValue();
            np.setAlogp(alogpresults.get(0));
            np.setAlogp2(alogpresults.get(1));
            np.setAmralogp(alogpresults.get(2));


            APolDescriptor aPolDescriptor = new APolDescriptor();
            aPolDescriptor.initialise(builder);
            DescriptorValue apolvalue = aPolDescriptor.calculate(acFull);
            DoubleResult apolresult = (DoubleResult) apolvalue.getValue();
            np.setApol(apolresult.doubleValue());

            try {
                BCUTDescriptor bcutDescriptor = new BCUTDescriptor();
                bcutDescriptor.initialise(builder);
                np.bcutDescriptor = new ArrayList<>();
                DescriptorValue bcutvalue = bcutDescriptor.calculate(acFull);
                DoubleArrayResult bcutResults = (DoubleArrayResult) bcutvalue.getValue();
                for (int i = 0; i < 6; i++) {
                    np.bcutDescriptor.add(bcutResults.get(i));
                }
            }catch(ArrayIndexOutOfBoundsException e){
                e.printStackTrace();
            }

            BPolDescriptor bPolDescriptor = new BPolDescriptor();
            bPolDescriptor.initialise(builder);
            DescriptorValue bpolvalue = bPolDescriptor.calculate(acFull);
            DoubleResult bpolresult = (DoubleResult) bpolvalue.getValue();
            np.setBpol(bpolresult.doubleValue());

            EccentricConnectivityIndexDescriptor eccentricConnectivityIndexDescriptor = new EccentricConnectivityIndexDescriptor();
            eccentricConnectivityIndexDescriptor.initialise(builder);
            DescriptorValue eccenValue = eccentricConnectivityIndexDescriptor.calculate(acFull);
            IntegerResult eccenResult = (IntegerResult) eccenValue.getValue();
            np.setEccentricConnectivityIndexDescriptor(eccenResult.intValue());

            FMFDescriptor fmfDescriptor = new FMFDescriptor();
            fmfDescriptor.initialise(builder);
            DescriptorValue fmfValue = fmfDescriptor.calculate(acFull);
            DoubleResult fmfResult = (DoubleResult) fmfValue.getValue();
            np.setFmfDescriptor(fmfResult.doubleValue());

            FractionalCSP3Descriptor fractionalCSP3Descriptor = new FractionalCSP3Descriptor();
            fractionalCSP3Descriptor.initialise(builder);
            DescriptorValue fsp3value = fractionalCSP3Descriptor.calculate(acFull);
            DoubleResult fsp3result = (DoubleResult) fsp3value.getValue();
            np.setFsp3(fsp3result.doubleValue());

            FragmentComplexityDescriptor fragmentComplexityDescriptor = new FragmentComplexityDescriptor();
            fragmentComplexityDescriptor.initialise(builder);
            DescriptorValue fcdValue = fragmentComplexityDescriptor.calculate(acFull);
            DoubleResult fcdResults = (DoubleResult) fcdValue.getValue();
            np.setFragmentComplexityDescriptor(fcdResults.doubleValue());


            GravitationalIndexDescriptor gravitationalIndexDescriptor = new GravitationalIndexDescriptor();
            gravitationalIndexDescriptor.initialise(builder);
            DescriptorValue gravValue = gravitationalIndexDescriptor.calculate(acFull);
            DoubleArrayResult gravResults = (DoubleArrayResult) gravValue.getValue();
            np.setGravitationalIndexHeavyAtoms(gravResults.get(0));

            HBondAcceptorCountDescriptor hBondAcceptorCountDescriptor = new HBondAcceptorCountDescriptor();
            hBondAcceptorCountDescriptor.initialise(builder);
            DescriptorValue nhbaccValue = hBondAcceptorCountDescriptor.calculate(acFull);
            IntegerResult nhbaccResult = (IntegerResult) nhbaccValue.getValue();
            np.sethBondAcceptorCount(nhbaccResult.intValue());

            HBondDonorCountDescriptor hBondDonorCountDescriptor = new HBondDonorCountDescriptor();
            hBondDonorCountDescriptor.initialise(builder);
            DescriptorValue nhbdonValue = hBondDonorCountDescriptor.calculate(acFull);
            IntegerResult nhbdonResult = (IntegerResult) nhbdonValue.getValue();
            np.sethBondDonorCount(nhbdonResult.intValue());

            HybridizationRatioDescriptor hybridizationRatioDescriptor = new HybridizationRatioDescriptor();
            hybridizationRatioDescriptor.initialise(builder);
            DescriptorValue hybridRatioValue = hybridizationRatioDescriptor.calculate(acFull);
            DoubleResult hybridRationResult = (DoubleResult) hybridRatioValue.getValue();
            np.setHybridizationRatioDescriptor(hybridRationResult.doubleValue());

            KappaShapeIndicesDescriptor kappaShapeIndicesDescriptor = new KappaShapeIndicesDescriptor();
            kappaShapeIndicesDescriptor.initialise(builder);
            DescriptorValue kappaShapeValues = kappaShapeIndicesDescriptor.calculate(acFull);
            DoubleArrayResult kappaShapeResults = (DoubleArrayResult) kappaShapeValues.getValue();
            np.setKappaShapeIndex1(kappaShapeResults.get(0));
            np.setKappaShapeIndex2(kappaShapeResults.get(1));
            np.setKappaShapeIndex3(kappaShapeResults.get(2));

            MannholdLogPDescriptor mannholdLogPDescriptor = new MannholdLogPDescriptor();
            mannholdLogPDescriptor.initialise(builder);
            DescriptorValue manholdLogpValues = mannholdLogPDescriptor.calculate(acFull);
            DoubleResult manholdLogpResult = (DoubleResult) manholdLogpValues.getValue();
            np.setManholdlogp(manholdLogpResult.doubleValue());

            PetitjeanNumberDescriptor petitjeanNumberDescriptor = new PetitjeanNumberDescriptor();
            petitjeanNumberDescriptor.initialise(builder);
            DescriptorValue petitjeanNumnerValue = petitjeanNumberDescriptor.calculate(acFull);
            DoubleResult petitjeanResult = (DoubleResult) petitjeanNumnerValue.getValue();
            np.setPetitjeanNumber(petitjeanResult.doubleValue());

            PetitjeanShapeIndexDescriptor petitjeanShapeIndexDescriptor = new PetitjeanShapeIndexDescriptor();
            petitjeanShapeIndexDescriptor.initialise(builder);
            DescriptorValue petitjeanShapeValues = petitjeanShapeIndexDescriptor.calculate(acFull);
            DoubleArrayResult petitjeanShapeResults = (DoubleArrayResult) petitjeanShapeValues.getValue();
            np.setPetitjeanShapeTopo(petitjeanShapeResults.get(0));
            np.setPetitjeanShapeGeom(petitjeanShapeResults.get(1));

            RuleOfFiveDescriptor ruleOfFiveDescriptor = new RuleOfFiveDescriptor();
            ruleOfFiveDescriptor.initialise(builder);
            DescriptorValue ruleOfFiveValue = ruleOfFiveDescriptor.calculate(acFull);
            IntegerResult ruleOfFiveResult = (IntegerResult) ruleOfFiveValue.getValue();
            np.setLipinskiRuleOf5Failures(ruleOfFiveResult.intValue());

                /*SmallRingDescriptor smallRingDescriptor = new SmallRingDescriptor();
                smallRingDescriptor.initialise(builder);
                DescriptorValue smallRingValue = smallRingDescriptor.calculate(acFull);
                IntegerResult smallRingResult = (IntegerResult) smallRingValue.getValue();
                np.setNumberSmallRingsDescriptor(smallRingResult.intValue());*/


            SpiroAtomCountDescriptor spiroAtomCountDescriptor = new SpiroAtomCountDescriptor();
            spiroAtomCountDescriptor.initialise(builder);
            DescriptorValue spiroatomValue = spiroAtomCountDescriptor.calculate(acFull);
            IntegerResult spiroatomResult = (IntegerResult) spiroatomValue.getValue();
            np.setNumberSpiroAtoms(spiroatomResult.intValue());

            VABCDescriptor vabcDescriptor = new VABCDescriptor();
            vabcDescriptor.initialise(builder);
            DescriptorValue vabcValue = vabcDescriptor.calculate(acFull);
            DoubleResult vabcResult = (DoubleResult) vabcValue.getValue();
            np.setVabcDescriptor(vabcResult.doubleValue());

            VAdjMaDescriptor vAdjMaDescriptor = new VAdjMaDescriptor();
            vAdjMaDescriptor.initialise(builder);
            DescriptorValue vadjmaValue = vAdjMaDescriptor.calculate(acFull);
            DoubleResult vadjmaResult = (DoubleResult) vadjmaValue.getValue();
            np.setVertexAdjMagnitude(vadjmaResult.doubleValue());

            WienerNumbersDescriptor wienerNumbersDescriptor = new WienerNumbersDescriptor();
            wienerNumbersDescriptor.initialise(builder);
            DescriptorValue wienerNumbersValue = wienerNumbersDescriptor.calculate(acFull);
            DoubleArrayResult wienerNumbersResult = (DoubleArrayResult) wienerNumbersValue.getValue();
            np.setWeinerPathNumber(wienerNumbersResult.get(0));
            np.setWeinerPolarityNumber(wienerNumbersResult.get(1));

            XLogPDescriptor xLogPDescriptor = new XLogPDescriptor();
            xLogPDescriptor.initialise(builder);
            DescriptorValue xlogpValues = xLogPDescriptor.calculate(acFull);
            DoubleResult xlogpResult = (DoubleResult) xlogpValues.getValue();
            np.setXlogp(xlogpResult.doubleValue());

            ZagrebIndexDescriptor zagrebIndexDescriptor = new ZagrebIndexDescriptor();
            zagrebIndexDescriptor.initialise(builder);
            DescriptorValue zagrebIndexValue = zagrebIndexDescriptor.calculate(acFull);
            DoubleResult zagrebIndexresult = (DoubleResult) zagrebIndexValue.getValue();
            np.setZagrebIndex(zagrebIndexresult.doubleValue());

            TPSADescriptor tpsaDescriptor = new TPSADescriptor();
            tpsaDescriptor.initialise(builder);
            DescriptorValue tpsaValue = tpsaDescriptor.calculate(acFull);
            DoubleResult tpsaResult = (DoubleResult) tpsaValue.getValue();
            np.setTopoPSA(tpsaResult.doubleValue());

            FractionalPSADescriptor fractionalPSADescriptor = new FractionalPSADescriptor();
            fractionalPSADescriptor.initialise(builder);
            DescriptorValue ftpsaValue = fractionalPSADescriptor.calculate(acFull);
            DoubleResult ftpsaResult = (DoubleResult) ftpsaValue.getValue();
            np.setTpsaEfficiency(ftpsaResult.doubleValue());



            lotusUniqueNaturalProductRepository.save(np);



        } catch (CDKException | OutOfMemoryError e) {
            e.printStackTrace();
        }
        return np;


    }





}
