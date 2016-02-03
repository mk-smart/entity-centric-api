# CouchDB configuration for MK:Smart entity-centric API

The following documentation shows how to create the CouchDB Documents (JSON objects) where the configuration of the API is stored.

## Defining integration rules

Data integration rules are defined in two ways:
1. dataset-based
2. datatype-based

### Dataset specification documents

These documents are used to describe how a dataset can be accessed, what types of data it supports/provides, and how they can be retrieved.

| Option                                   | Required | Description |
| ---------------------------------------- | -------- | ----------- |
| `id `                                    | yes      | can be any string, but it should uniquely identify the dataset |
| `type`                                   | yes      | __must__ be `provider-spec` |
| `http://rdfs.org/ns/void#sparqlEndpoint` | no       | __must__ be the physical URI of the query service |
| `mks:types`                              | no       | an object whose keys are of the form `type/global:id/{nameoftype}`, and that describes which are the types of data whose descriptions can be retrieved. |
| `mks:primitives`                         | no       | an object whose keys are of the form `{scope}:{nameofproperty}`, and that describes which property-based queries can be satisfied by this dataset. |

#### mks:types

* `localise` : a JavaScript function that takes up to four parameters `stype`,`authority`,`uidcat` and `uid`; must return a string;
* `query_text` : a SPARQL SELECT query that returns a list of bindings with attributes `p` and `o` (also extensible to `p1`, `o1`, `p2`, `o2` etc.
* `fetch_query` : a SPARQL SELECT query that returns a list of bindings with attribute `s`

### Datatype specification documents

These documents are used to describe what types of data the API should actively be looking for, and their relationships with other types of data.

| Option      | Required | Description |
| ------------| -------- | ----------- |
| `id`       | yes      | if `type` is `type-spec`, then it should be a URI; if instead it is `global-type-spec`, then it should be of the form `type/global:id/{nameoftype}` |
| `type`      | yes      | __must__ be either `type-spec`  or `global-type-spec` |
| `mks:super` | no       | a supertype identifier, using the same syntax as the `id` of this type |
| `globalise` | no       | a JavaScript function that takes a single parameter `luri` and returns a string. |
| `localise`  | no       | a JavaScript function that takes up to four parameters `stype`,`authority`,`uidcat` and `uid` and returns a string. |

### Namespace rewrite rules

### Localise functions

### Globalise functions