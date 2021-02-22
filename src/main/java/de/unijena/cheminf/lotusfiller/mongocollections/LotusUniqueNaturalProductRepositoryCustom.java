package de.unijena.cheminf.lotusfiller.mongocollections;

import java.util.List;

public interface LotusUniqueNaturalProductRepositoryCustom {

    List<String> findAllLotusIds();

    List<String> findAllInchiKeys();
}
