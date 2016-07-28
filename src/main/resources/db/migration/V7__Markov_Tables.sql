CREATE TABLE markovSuffixes (
  chainName  VARCHAR_IGNORECASE,
  prefix     VARCHAR_IGNORECASE,
  suffix     VARCHAR_IGNORECASE,

  PRIMARY KEY (chainName, prefix, suffix)
);

CREATE TABLE markovStarts (
  chainName  VARCHAR_IGNORECASE,
  prefix     VARCHAR_IGNORECASE,

  PRIMARY KEY (chainName, prefix)
)