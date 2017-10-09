# TODO List for the eIDAS connector

* In Shib-base: Improve error handling for the external authentication flow.
	* We should define one base exception class that 

* In Shib-base: Perform initialization of an AuthnRequest by setting up a number of contexts and performing initial checks.
	* SignMessage decryption and assignment of a SignMessageContext.
	* Calculation of requested AuthnContextClassRef URI:s to check if they match and so on. Also add those to a easy to use context.
	
* Possibly change system/flows/authn/external-authn-flow.xml so that we catch exceptions and end the flow in a controlled way.