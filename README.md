# mod-patron

Copyright (C) 2018-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Microservice to allow 3rd party discovery services to perform FOLIO patron
actions via the discovery service's UI.

## Configuration

Some of the features of mod-patron may rely on specific configurations. mod-patron stores such configurations in its own 
database using local settings API with interface: `patron.settings`. The module provides CRUD API for managing it.

### Configuration using `patron.settings` interface

Required parameters:
* `key`
* `scope`
* `value`

### Supported configuration settings

| Configuration name                           | Key                                   | Scope        | Value                                                           | Description                                                                            |
|----------------------------------------------|---------------------------------------|--------------|-----------------------------------------------------------------|----------------------------------------------------------------------------------------|
| Multi-Item Requesting Feature enable/disable | `isMultiItemRequestingFeatureEnabled` | `mod-patron` | Object with property name `enabled` and value `true` or `false` | Allows to query batch request details using `GET /patron/account/{accountId}` endpoint |


Multi-Item Requesting Feature configuration example:

Request body that can be used to enable the Multi-Item Requesting Feature using `POST /patron/settings` endpoint:

```json
{
  "id": "65de6432-be11-48ba-9686-a65101634040",
  "scope": "mod-patron",
  "key": "isMultiItemRequestingFeatureEnabled",
  "value": {
    "enabled": "true"
  },
  "_version": 1
}
```

When modifying the configuration setting using `PUT /patron/settings/{id}`, ensure that the `value` property is a valid JSON object
and `_version` property is provided with the correct version number.

## Additional information
The endpoint GET '/patron/account/{accountId}' has optional query parameter 'sortBy'
that indicates the order of records within the lists of holds, charges, loans.
The value of 'sortBy' parameter is appended as a part of a CQL query that's evaluated
during separate corresponding calls to retrieve holds, charges and loans.
Often, a given value of 'sortBy' will only work with one type of record (holds, loans,
or charges), e.g. item.title works for holds, but not loans/charges.  The expectation is
that when using the 'sortBy' parameter, separate calls will be made for retrieving
holds, loans, and charges.  In cases where multiple 'include*' parameters are 'true' and
'sortBy' is used, it's possible (if not probable) that some of those lists will not be sorted
as desired.

Examples of requests: 
   - get holds sorted by item.title field 
     in ascending order: /patron/account/{accountId}?includeHold=true&&sortBy=item.title/sort.ascending
   - get charges sorted by title field 
     in ascending order: /patron/account/{accountId}?includeCharges=true&&sortBy=title/sort.ascending
   - get loans sorted by loanDate field
     in ascending order: /patron/account/{accountId}?includeCharges=true&&sortBy=loanDate/sort.ascending  
### Issue tracker

See project [MODPATRON](https://issues.folio.org/browse/MODPATRON)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)
