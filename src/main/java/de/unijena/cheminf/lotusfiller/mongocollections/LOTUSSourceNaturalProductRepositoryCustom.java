package de.unijena.cheminf.lotusfiller.mongocollections;

import java.util.List;

public interface LOTUSSourceNaturalProductRepositoryCustom {

    List<String> findUniqueInchiKeys();

    List<Object> findUniqueSourceNames();

}
