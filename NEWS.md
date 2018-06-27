## 1.0.0 Unreleased
 * Update the circulation dependency to 3.3 that contains the loan renew by id
   functionality and implement the patron initiated loan renewal endpoint.
 * Fix dueDate problem when the field is not present.
 * Update mod-feesfines with the latest schema changes.
 * Fix raml-cop warnings in examples.
 * Integrate patron account charges with data from mod-feesfines - WIP.
 * Removed some RAML descriptions causing the generated API doc to include
   endpoints that don't exist.
 * Fixed a field name typo in the JSON schema
 * Added endpoints for hold management at the instance level (currently not
   supported by FOLIO) for future integration
 * RAML cleanup
 * Initial work
