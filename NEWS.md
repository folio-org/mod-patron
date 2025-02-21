# 6.2.9 2025-02-21

* Delegate creation of mediated requests to mod-circulation-bff (MODPATRON-212)

# 6.2.8 2025-02-11

* Fix IndexOutOfBounds exception for single tenant environment (MODPATRON-219)

# 6.2.7 2025-02-06

* Add mapping for ecsRequestPhase field (MODPATRON-216)

# 6.2.6 2025-01-22

* Add missing required interfaces (MODPATRON-208)

# 6.2.5 2025-01-13

* Changes for the externalSystemId for external patron-registration (MODPATRON-198)
* New API added for put /patron/{externalSystemId} (MODPATRON-202)
* Changes for get /patron/registrationStatus/{identifier} (MODPATRON-205)

# 6.2.4 2024-12-17

* Modify extended timeouts for `POST` requests and `GET` allowed service points (MOPATRON-197)

# 6.2.3 2024-12-12

* Add extended timeouts for `POST` requests and `GET` allowed service points (MODPATRON-197)

# 6.2.2 2024-12-09

* Create mediated requests in secure tenant (MODPATRON-194)
* Request types - failsafe approach for ECS requesting (MODPATRON-192)

# 6.2.1 2024-12-01

* Create request: switch to mod-circulation-bff (MODPATRON-183)
* Review and cleanup Module Descriptor for mod-patron (MODPATRON-184)

# 6.2.0 2024-10-30

* Update to RMB 35.3.0 (MODPATRON-189)
* Implementation of POST: `/patron` API (MODPATRON-191)
* Modify existing getByEmail API to return patron details as per new design (MODPATRON-190)
* Added new endpoint to get service points based on the item (MODPATRON-181)
* Add interface `inventory` 14.0 (MODPATRON-186)
* Allowed service points: switch to mod-circulation-bff (MODPATRON-178)
* Added debugging logs for `getPatronAccountById` API (MODPATRON-182)
* Fixed `enrolmentDate` field is not saving in database (MODPATRON-180)
* Enhance the error response to ENUM constant for LC User registration APIs (MODPATRON-179)
* Fixed PreferredEmailCommunication `Service` to `Services` (MODPATRON-177)
* Fix vulnerabilities (MODPATRON-170, MODPATRON-173)
* Removed address0 (MODPATRON-171)
* Added GET endpoint for expired LC users (MODPATRON-169)
* Added PUT endpoint for LC user (MODPATRON-168)
* Validation errors return 422 HTTP code (MODPATRON-156)
* Added GET API for external patron (MODPATRON-166)
* Added POST API for external patron (MODPATRON-165)

# 6.1.0 2024-03-21

* Upgrade to RMB 35.2.0, Vertx 4.5.5, Spring 6.1.5 (MODPATRON-160)
* Raise feesfines interface version to 19.0 (MODPATRON-149)

# 6.0.0 2023-10-13

* Add allowed SP permission (MODPATRON-147)
* Add query parameters to allowed SP call (MODPATRON-147)
* Add allowed-service-points endpoint (MODPATRON-147)
* Move to Java 17 (MODPATRON-146)
* Fix holdId description
* Use GitHub Workflows api-lint and api-schema-lint and api-doc (MODPATRON-139)
* Change spelling of parameter (MODPATRON-137)
* Updating feesfines interface to 18.0 (MODPATRON-133)
* Fix HTTP 500 error when cancelling TLR without an item (MODPATRON-131)
* Upgrade dependencies for Orchid (RMB, Vertx, Log4j, lombok) (MODPATRON-126)

# 5.5.0 2023-02-15

* Requires `inventory` `5.2 6.0 7.0 8.0 9.0 10.0 11.0 12.0 or 13.0` ([MODPATRON-124](https://issues.folio.org/browse/MODPATRON-124))

## 5.4.0 2022-10-19

* Upgraded RMB to 35.0.0 ([MODPATRON-122](https://issues.folio.org/browse/MODPATRON-122))
* Now checks localization settings and uses local currency code. (MODPATRON-7)
* Adding timeout and error handlers for requests to external modules (MODPATRON-119)
* Upgrade user interface to 16.0 ([MODPATRON-116](https://issues.folio.org/browse/MODPATRON-116))
* Requires `inventory` `5.2 6.0 7.0 8.0 9.0 10.0 11.0 or 12.0`([MODPATRON-120](https://issues.folio.org/browse/MODPATRON-120))

## 5.3.0 2022-06-15

* Upgrade to RMB 34.0.0 (MODPATRON-114)
* Missing statuses added (MODPATRON-22)
* Title-level requests properly created (MODPATRON-104)

## 5.2.0 2022-02-22

* Upgrade to RMB 33.2.4 (MODPATRON-103)

## 5.1.0 2022-01-12

* Inactive patrons can now view their account details (MODPATRON-69)
* Upgrade to RMB 33.0.4 and Log4J to 2.16.0. (CVE-2021-44228) (MODPATRON-94)
* [MODPATRON-79](https://issues.folio.org/browse/MODPATRON-79): Support sorting of patron account info
* [MODPATRON-80](https://issues.folio.org/browse/MODPATRON-80): Support paging of patron account info
* [MODPATRON-100](https://issues.folio.org/browse/MODPATRON-100): Inconsistent handling of invalid limit parameter
* [MODPATRON-74](https://issues.folio.org/browse/MODPATRON-74): Update request schema according to request data migration changes
* [MODPATRON-92](https://issues.folio.org/browse/MODPATRON-92): Update interface circulation version to 12.0

## 5.0.0 2021-10-05

* `embed_postgres` command line option is no longer supported (MODPATRON-53)
* Upgrades to RAML Module Builder 33.0.2 (MODPATRON-53, MODPATRON-66)
* Provides `patron 4.3`
* Requires `inventory` `5.2 6.0 7.0 8.0 9.0 or 10.0`
* Requires `feesfines 14.0, 15.0, 16.0 or 17.0`

## 4.5.1 2021-06-28

 * [MODPATRON-61](https://issues.folio.org/browse/MODPATRON-61): Patron Comments- Not seeing comments in response OR on FOLIO
   request record when user creates request via opac or discovery

## 4.5.0 2021-06-18

 * Accounts without item data attached no longer result in errors when using "includeCharges" flag (MODPATRON-59)
 * Accounts now include correct accrual date when using the "includeCharges" flag (MODPATRON-62)
 * Requires `circulation 9.5 10.0 or 11.0` ([MODPATRON-52](https://issues.folio.org/browse/MODPATRON-52), [MODPATRON-54](https://issues.folio.org/browse/MODPATRON-54))


## 4.4.0 2021-03-15

 * Introduces `patron comments` for requests ([MODPATRON-4](https://issues.folio.org/browse/MODPATRON-4))
 * Upgrades to RAML Module Builder 32.1.0
 * Upgrades to vert.x 4.0.0
 * Provides `patron 4.2`
 * Requires `circulation 9.5`
 * Requires `feesfines 14.0, 15.0 or 16.0`


## 4.3.0 2020-10-20
 * [MODPATRON-45](https://issues.folio.org/browse/MODPATRON-45): MODPATRON-45: Upgrade to RMB v31.1.2 and JDK 11

## 4.2.0 2020-06-12
 * [MODPATRON-37](https://issues.folio.org/browse/MODPATRON-37): MODPATRON-37: Migrate to RMB 30.0.2

## 4.1.0 2020-03-18
 * [MODPATRON-36](https://issues.folio.org/browse/MODPATRON-36): MODPATRON-36: Migrate to item-storage 8.0 and inventory 10.0

## 4.0.0 2019-12-06
 * [MODPATRON-29](https://issues.folio.org/browse/MODPATRON-29): Fixing the impact of the API change regarding CIRC-405
 * [FOLIO-2235](https://issues.folio.org/browse/FOLIO-2235): Add LaunchDescriptor settings to each backend non-core module repository
 * [MODPATRON-32](https://issues.folio.org/browse/MODPATRON-32): Update holdings-storage API version to 4.0
 * [FOLIO-2358](https://issues.folio.org/browse/FOLIO-2358): Use JVM features (UseContainerSupport, MaxRAMPercentage) to manage container memory
 * [MODPATRON-34](https://issues.folio.org/browse/MODPATRON-34): Implement Cancel Requests, updated RMB version to 29.1.0 and vertx-junit version to 3.8.4

## 3.0.2 2019-07-25
 * [MODPATRON-26](https://issues.folio.org/browse/MODPATRON-26): Determine request type to use when
   making an item level request

## 3.0.1 2019-07-11
 * [MODPATRON-25](https://issues.folio.org/browse/MODPATRON-25): Validation changes for hold.json

## 3.0.0 2019-06-10
 * [MODPATRON-8](https://issues.folio.org/browse/MODPATRON-8): Title level requests
 * [MODPATRON-2](https://issues.folio.org/browse/MODPATRON-2): Support for returning the request queue position in requests
 * [MODPATRON-5](https://issues.folio.org/browse/MODPATRON-5): Support for pickup location when placing or returning a request
 * [MODPATRON-13](https://issues.folio.org/browse/MODPATRON-13): Update to RAML 1.0 and the latest RMB

## 2.0.0 2019-03-22
 * [MODPATRON-18](https://issues.folio.org/browse/MODPATRON-18): Support for
   `circulation` 7.0. This interface update includes potentially breaking
    changes in the `expirationDate` field in the `hold` schema. A time
    component was added, which clients might not be expecting.

## 1.2.0 2018-12-04
 * Requires either `feesfines` 14.0 or 15.0 (MODPATRON-16)
 * Update schema files with a top level description (MODPATRON-11)
 * Requires either `circulation` 3.3, 4.0 or 5.0 (MODPATRON-12)
 * Requires `inventory` `5.2 6.0 7.0 8.0` (MODPATRON-14)
 * Requires `holdings-storage` version `1.2 2.0 3.0` (MODPATRON-15)

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
