## 1.2.0 Unreleased
 * Update schema files with a top level description (MODPATRON-11)
 * Requires either `circulation` 3.3, 4.0 or 5.0 (MODPATRON-12)

## 1.1.0 2018-09-07
 * Requires either `circulation` 3.3 or 4.0 (MODPATRON-9, CIRC-136)

## 1.0.0 2018-08-17
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
