################################# DEPENDENCIES #################################
packages_cran <-
  c("curl",
    "dplyr",
    "purrr",
    "readr",
    "rvest")
packages_bioconductor <- NULL
packages_github <- NULL

################################## VARIABLES ##################################

ZENODO_FROZEN <- "https://doi.org/10.5281/zenodo.5794106"
ZENODO_METADATA <- "https://doi.org/10.5281/zenodo.6378223"

PATTERN_FROZEN <- "frozen_metadata.csv.gz"
PATTERN_METADATA_ORGANISMS <- "organism_metadata"
PATTERN_METADATA_REFERENCES <- "reference_metadata"
PATTERN_METADATA_STRUCTURES <- "structure_metadata"

PATH_EXPORT_LOTUS <- "data/lotus.tsv" #' @Maria can your reader read gzip?
PATH_EXPORT_LOTUS_METADATA <- "data/lotus_metadata.tsv"

################################## FUNCTIONS ##################################

#' Title
#'
#' @param df
#'
#' @return
#' @export
#'
#' @examples
check_and_load_packages <- function(cran = packages_cran,
                                    bioconductor = packages_bioconductor,
                                    github = packages_github) {
  installed_packages <- rownames(installed.packages())
  installed_packages_cran <- cran %in% installed_packages
  installed_packages_bioconductor <-
    bioconductor %in% installed_packages
  installed_packages_github <- github %in% installed_packages
  
  if (!is.null(bioconductor)) {
    cran <- cran |>
      append("BiocManager")
  }
  if (!is.null(github)) {
    cran <- cran |>
      append("remotes")
  }
  
  if (any(installed_packages_cran == FALSE)) {
    install.packages(cran[!installed_packages_cran])
  }
  if (any(installed_packages_bioconductor == FALSE)) {
    BiocManager::install(bioconductor[!installed_packages_bioconductor])
  }
  if (any(installed_packages_github == FALSE)) {
    lapply(X = github[!installed_packages_github], FUN = remotes::install_github)
  }
  
  return(lapply(c(
    cran,
    bioconductor,
    gsub(
      pattern = ".*/",
      replacement = "",
      x = github
    )
  ),
  require,
  character.only = TRUE) |>
    invisible())
}

#' Title
#'
#' @param session
#' @param text
#' @param ...
#'
#' @return
#' @export
#'
#' @examples
follow_next <- function(session,
                        text = "Next",
                        ...) {
  link <- rvest::html_element(x = session,
                              xpath = sprintf("//*[text()[contains(.,'%s')]]", text))
  
  url <- rvest::html_attr(link, "href") |>
    trimws() |>
    gsub(pattern = "^\\.{1}/", replacement = "")
  
  message("Navigating to ", url)
  
  rvest::session_jump_to(session, url, ...)
}

#' Title
#'
#' @param url
#' @param pattern
#'
#' @return
#' @export
#'
#' @examples
get_last_file_from_zenodo <- function(url, pattern) {
  file <- rvest::session(url = url) |>
    follow_next(text = pattern) |>
    purrr::pluck("url") |>
    curl::curl_download(destfile = tempfile()) |>
    readr::read_delim()
  
  return(file)
}

################################### PROGRAM ###################################

start <- Sys.time()

#' (Down)load dependencies
check_and_load_packages()

#' Get last versions of each file from Zenodo
last_frozen <- get_last_file_from_zenodo(url = ZENODO_FROZEN,
                                         pattern = PATTERN_FROZEN)
last_metadata_organisms <-
  get_last_file_from_zenodo(url = ZENODO_METADATA,
                            pattern = PATTERN_METADATA_ORGANISMS) |>
  dplyr::select(
    organismCleaned,
    organismCleaned_id,
    organismCleaned_dbTaxo,
    organismCleaned_dbTaxoTaxonRanks,
    organismCleaned_dbTaxoTaxonomy
  ) |>
  dplyr::distinct()
last_metadata_references <-
  get_last_file_from_zenodo(url = ZENODO_METADATA,
                            pattern = PATTERN_METADATA_REFERENCES) |>
  dplyr::select(
    referenceCleanedDoi,
    referenceCleanedPmcid,
    referenceCleanedPmid,
    referenceCleanedTitle
  ) |>
  dplyr::distinct()
last_metadata_structures <-
  get_last_file_from_zenodo(url = ZENODO_METADATA,
                            pattern = PATTERN_METADATA_STRUCTURES) |>
  dplyr::select(
    structureCleanedInchikey,
    structureCleanedInchi,
    structureCleanedSmiles,
    structureCleaned_inchikey2D,
    structureCleaned_inchi2D,
  ) |>
  dplyr::distinct()

#' Tidy everything
lotus_metadata <- last_frozen |>
  dplyr::select(
    # organismType,
    # organismValue,
    # structureType,
    # structureValue,
    # referenceType,
    # referenceValue,
    organismCleaned = organism_name,
    # organismCleaned_id,
    # organismCleaned_dbTaxo,
    # organismCleaned_dbTaxoTaxonRanks,
    # organismCleaned_dbTaxoTaxonomy,
    structureCleanedInchikey = structure_inchikey,
    structureCleanedInchi = structure_inchi,
    structureCleanedSmiles = structure_smiles,
    # structureCleaned_inchikey2D,
    # structureCleaned_inchi2D,
    structureCleaned_smiles2D = structure_smiles_2D,
    structureCleaned_molecularFormula = structure_molecular_formula,
    # structureCleaned_exactMass = structure_exact_mass
    structureCleaned_nameIupac = structure_nameIupac,
    structureCleaned_nameTraditional = structure_nameTraditional,
    structureCleaned_stereocenters_total = structure_stereocenters_total,
    structureCleaned_stereocenters_unspecified = structure_stereocenters_unspecified,
    referenceCleanedDoi = reference_doi,
    # referenceCleanedPmcid,
    # referenceCleanedPmid,
    # referenceCleanedTitle
  ) |>
  dplyr::left_join(last_metadata_organisms) |>
  dplyr::left_join(last_metadata_structures) |>
  dplyr::left_join(last_metadata_references)

#' Export
# readr::write_delim(x = last_frozen,
#                    file = PATH_EXPORT_LOTUS,
#                    delim = "\t")
# readr::write_delim(x = lotus_metadata,
#                    file = PATH_EXPORT_LOTUS_METADATA,
#                    delim = "\t")

end <- Sys.time()

message("Finished in ", format(end - start))
