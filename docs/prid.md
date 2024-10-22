![Logo](images/sweden-connect.png)

# eIDAS Connector Provisional Identifier (PRID) Calculation

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

---

<a name="Country Policy Configuration"></a>
## Country Policy Configuration

The PRID-feature of the eIDAS Connector uses a PRID policy where each supported country points to an algorithm and a persistence level. The format is as follows:

```
policy.<Country code>.algorithm=default-eIDAS|colresist-eIDAS|special-characters-eIDAS
policy.<Country code>.persistenceClass=A|B|C
```

Below is an example of the policy configuration file:

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
policy.FR.persistenceClass=A

policy.IS.algorithm=default-eIDAS
policy.IS.persistenceClass=A
```

See `config/policy.properties`.

The PRID service will reload the PRID policy file every 10 minutes. It is also possible to force a reload and verify that the update was correct. See "**Refresh of policy configuration - /manage/refresh**" below.

---

Copyright &copy; 2017-2024, [Myndigheten för digital förvaltning - Swedish Agency for Digital Government (DIGG)](http://www.digg.se). Licensed under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0).
