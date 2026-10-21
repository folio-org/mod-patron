# mod-patron

Copyright (C) 2018-2023 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Table of contents

* [Introduction](#introduction)
* [Configuration](#configuration)
  * [Configuration using `patron.settings` interface](#configuration-using-patronsettings-interface)
  * [Supported configuration settings](#supported-configuration-settings)
* [GET /patron/account/{accountId}](#get-patronaccountaccountid)
  * [Sorting](#sorting)
  * [Multi-Item requests (Batch requests)](#multi-item-requests-batch-requests)
    * [Prerequisites](#prerequisites)
    * [`includeBatches`](#includebatches)
    * [`batchesDaysBack`](#batchesdaysback)
    * [Limitations and notes](#limitations-and-notes)
* [Issue tracker](#issue-tracker)
* [Other documentation](#other-documentation)

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

## GET /patron/account/{accountId}

Returns the patron account: totals, and optionally the lists of charges, holds, loans and
batch request status records (Multi-Item requests).

| Query parameter   | Default      | Description                                                                                                                         |
|-------------------|--------------|-------------------------------------------------------------------------------------------------------------------------------------|
| `includeLoans`    | `false`      | Include the `loans` list                                                                                                            |
| `includeCharges`  | `false`      | Include the `charges` list                                                                                                          |
| `includeHolds`    | `false`      | Include the `holds` list                                                                                                            |
| `includeBatches`  | `false`      | Include the `batches` list of batch request status records, see [Multi-Item requests](#multi-item-requests-batch-requests)          |
| `batchesDaysBack` | `30`         | Look-back window in days for batch requests without an open request, see [Multi-Item requests](#multi-item-requests-batch-requests) |
| `sortBy`          | not set      | CQL sort of the `holds`, `charges` and `loans` lists, see [Sorting](#sorting)                                                       |
| `offset`          | `0`          | Number of records to skip                                                                                                           |
| `limit`           | `2147483647` | Maximum number of records per list                                                                                                  |

### Sorting

The endpoint has optional query parameter 'sortBy'
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

### Multi-Item requests (Batch requests)

Controlled by the `includeBatches` and `batchesDaysBack` query parameters.

A Multi-Item request (batch request) is a set of item requests submitted together with
`POST /patron/account/{accountId}/instance/{instanceId}/batch-request`. The requests that are created for its items
are ordinary holds. The `batches` list of the account does not contain the batch requests themselves, it contains
batch request status records: one record per batch request, describing its processing status and the outcome
for each of its items (see the example below).

The records are linked to the holds by id: for a hold created as part of a batch request, `batchRequestInfo.batchRequestId`
is equal to the `batchRequestId` of the corresponding record in `batches`.

#### Prerequisites

* The `isMultiItemRequestingFeatureEnabled` setting must exist and be enabled, see
  [Supported configuration settings](#supported-configuration-settings). With `includeBatches=true` and the setting
  missing, disabled or invalid, the endpoint responds with `422` and no account data.
* The `circulation-bff` interfaces `circulation-bff-requests` and `circulation-bff-requests-batch` must be available.

#### `includeBatches`

* Type: boolean, optional, default `false`. With `false` the `batches` property in the response is an empty list,
  holds have no `batchRequestInfo`, and the `batchesDaysBack` parameter is ignored.
* With `true`:
  * The `batches` list of batch request status records is added to the response. Each record contains the
    `batchRequestId`, the `status` (`In progress` or `Completed`; a batch request where every item failed is
    `Completed`), `submittedAt`, `completedAt` (only when completed), the counters `itemsTotal`, `itemsRequested`,
    `itemsPending`, `itemsFailed` and the per-item lists `itemsRequestedDetails`, `itemsPendingDetails`,
    `itemsFailedDetails`.
  * Each hold created as part of a batch request gets the `batchRequestInfo` property (`batchRequestId` and
    `batchRequestSubmittedAt`) that points to its record in `batches` by `batchRequestId`. It is present only
    when `includeHolds=true`, as the holds list is empty otherwise.
  * Fetching the requests is a bit heavier than doing the same when `includeBatches` is `false`, as getting the
    batch request related info requires additional upstream calls.
  * `includeBatches=true` is enough to get the `batches`, `includeHolds` is not required.

The `batches` list contains the union of the records for:

1. Batch requests of the holds returned in the response, in other words of the page of requests selected with
   `offset`, `limit` and `sortBy`. Every such record is referenced by at least one hold through `batchRequestInfo`,
   when `includeHolds=true`. Their item details have `instanceId` and `title`.
2. Batch requests of the patron that have no open request, so no hold references them and they are not reachable
   through the requests: batch requests that are pending, in progress or failed (for example, every item failed)
   and were submitted within the last `batchesDaysBack` days. Their item details have no `instanceId` and may
   have no `title`. `offset`, `limit` and `sortBy` do not apply to them.

A batch request found both ways has one record. The records are ordered by submission date (`submittedAt`),
newest first, regardless of `sortBy`.

#### `batchesDaysBack`

* Type: integer, optional, default `30`, minimum `0`, maximum `365`. Values out of the range are rejected with `400`.
* Used only when `includeBatches=true`, ignored otherwise.
* Defines the look-back window for the batch requests without an open request (group 2 above): only those
  submitted at or after "now minus `batchesDaysBack` days" (UTC) get a record in `batches`.
  It does not filter the records of the batch requests referenced by the holds in the response (group 1).
* `0` means the cut-off is the current moment, so effectively no records of group 2 are returned.
* Completed batch requests (all items successfully requested) are not added by this parameter.
  They appear only if one of their requests is still open, as it is then referenced by a hold.

#### Limitations and notes

* At most 100 batch requests without an open request are loaded per call, the newest ones. Older ones within
  the `batchesDaysBack` window are not returned if there are more than 100 such ones in the window.
  The cap is not configurable.
* Each record costs at least two additional upstream calls (the batch request and its details), all made
  concurrently. The cap bounds the number of these calls, so `includeBatches=true` has a noticeable cost for
  patrons with many batch requests.
* A failure to load the status or details of a batch request without an open request is logged and its record
  is left out of the response instead of failing the whole request. A failure for a batch request referenced by a
  returned hold, or for the query of the batch requests themselves, fails the request.
* The `batches` list itself is not paged. `offset` and `limit` only affect which requests are returned, and
  therefore which records of group 1 are included.
* For an `In progress` record the counters are calculated from the batch request details, as they are the most
  up-to-date. For a `Completed` record they are taken from the batch request statistics.

Example of the request:

```
GET /patron/account/{accountId}?includeHolds=true&includeBatches=true&batchesDaysBack=60
```

Example of a record of the `batches` list, for a batch request without an open request:

```json
{
  "batchRequestId": "5203c035-005e-4a70-b555-ddaa3094c521",
  "status": "In progress",
  "submittedAt": "2025-09-10T10:00:00.123+00:00",
  "itemsTotal": 2,
  "itemsRequested": 0,
  "itemsPending": 2,
  "itemsFailed": 0,
  "itemsPendingDetails": [
    {
      "itemId": "7e881333-3a3a-501c-a6d4-03b81e3c7e79",
      "pickUpLocationId": "ebab9ccc-4ece-4f35-bc82-01f3325abed8"
    },
    {
      "itemId": "8e881333-3a3a-501c-a6d4-03b81e3c7e79",
      "pickUpLocationId": "ebab9ccc-4ece-4f35-bc82-01f3325abed8"
    }
  ],
  "itemsFailedDetails": [],
  "itemsRequestedDetails": []
}
```

## Issue tracker

See project [MODPATRON](https://issues.folio.org/browse/MODPATRON)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

## Other documentation

Other [modules](https://dev.folio.org/source-code/#server-side) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)
