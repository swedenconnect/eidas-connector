### Swedish eIDAS Proxy Service

This is the Swedish test proxy sevice of the Swedish eIDAS Pilot

- Application ID: **${proxy-service.applicationId}**
- Supported eIDAS protocol versions: **${proxy-service.eidasVersions}**

**Relevant resources:**
<table class="table table-striped table-links table-responsive">

<tr><td>Sweden Connect Home Page</td>
<td><a href="https://swedenconnect.se">https://swedenconnect.se</a></td></tr>

<tr><td>eIDAS Proxy Service Metadata</td>
<td><a href="ServiceMetadata">${proxy-service.domain.prefix}/ServiceMetadata</a></td></tr>

<tr><td>National SP Metadata</td>
<td><a href="nat-metadata">${proxy-service.domain.prefix}/nat-metadata</a></td></tr>

<tr><td>Private SP National Metadata</td>
<td><a href="nat-metadata${proxy-service.private-sp.suffix}">${proxy-service.domain.prefix}/nat-metadata${proxy-service.private-sp.suffix}</a></td></tr>

</table>

**Metadata validation certificate:**
<div style="margin-left:20px; font-size:small">

```
${proxy.service.metadata.cert}
```
</div>

**Developers:**

>Stefan Santesson  <a href="mailto:stefan@aaa-sec.com"><stefan@aaa-sec.com></a><br/>
>Martin Lindström  <a href="mailto:martin.lindstrom@litsec.se"><martin.lindstrom@litsec.se></a>
