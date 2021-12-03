# mod-patron

Copyright (C) 2018-2019 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Microservice to allow 3rd party discovery services to perform FOLIO patron
actions via the discovery service's UI.

## Additional information
The endpoint GET '/patron/account/{accountId}' has optional query parameter 'sortBy' 
that indicates the order of records within the lists of holds, charges, loans.
The value of 'sortBy' parameter is appended as a part of a CQL query that's evaluated 
during separate corresponding calls to retrieve holds, charges and loans.
If 'sortBy' is specified then it only can be used to sort one type of records, 
and it can not be applied to all holds, charges and loans records at the same time.
Currently, it doesn't work for loans.item object.

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
