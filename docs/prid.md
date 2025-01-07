![Logo](images/sweden-connect.png)

# eIDAS Connector Provisional Identifier (PRID) Calculation

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

<a name="country-policy-configuration"></a>
## Country Policy Configuration

The PRID-feature of the eIDAS Connector uses a PRID policy where each supported country points to an algorithm and a persistence level. Algorithms and persistence levels are described in detail in the [eIDAS Constructed Attributes Specification for the Swedish eID Framework](https://docs.swedenconnect.se/technical-framework/latest/11_-_eIDAS_Constructed_Attributes_Specification_for_the_Swedish_eID_Framework.html) document.

The PRID policy file must be either a Java Properties file (`.properties`) or a YAML file (`.yml` or `.yaml`).

For Properties files the format is as follows:

```
policy.<Country code>.algorithm=default-eIDAS|colresist-eIDAS|special-characters-eIDAS
policy.<Country code>.persistenceClass=A|B|C
```

For YAML-file the format is thus:

```
policy:
  <Country code>:
    algorithm: default-eIDAS|colresist-eIDAS|special-characters-eIDAS
    persistenceClass: A|B|C
  <Country code>: ...```

Below is an example of the policy configuration file in Properties format:

```
policy.SE.algorithm=default-eIDAS
policy.SE.persistenceClass=A

policy.DK.algorithm=default-eIDAS
policy.DK.persistenceClass=A

policy.NO.algorithm=default-eIDAS
policy.NO.persistenceClass=A

policy.AT.algorithm=special-characters-eIDAS
policy.AT.persistenceClass=A

policy.FR.algorithm=colresist-eIDAS
policy.FR.persistenceClass=B

policy.IS.algorithm=default-eIDAS
policy.IS.persistenceClass=A
```

And the same in YAML-format:

```yaml
policy:
  SE:
    algorithm: default-eIDAS
    persistenceClass: A
  DK:
    algorithm: default-eIDAS
    persistenceClass: A
  NO:
    algorithm: default-eIDAS
    persistenceClass: A
  AT:
    algorithm: special-characters-eIDAS
    persistenceClass: A
  FR:
    algorithm: colresist-eIDAS
    persistenceClass: B
  IS:
    algorithm: default-eIDAS
    persistenceClass: A
```

## Updating the PRID Policy Configuration

The PRID service will reload the PRID policy file every 10 minutes. It is also possible to force a reload and verify that the update was correct. See "**Refresh of policy configuration - /manage/refresh**" below.

---

Copyright &copy; 2017-2025, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
