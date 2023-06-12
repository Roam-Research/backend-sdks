# SDK for Roam Reasearch API

This JS module allows to use the [Roam API](https://roamresearch.com/#/app/developer-documentation/page/tIaOPdXCj) outside of the Roam Research webapp.

## Usage
### 1. Create a token

You can create and edit roam-graph-tokens from a new section "API tokens" in the "Graph" tab in the Settings:

![https://firebasestorage.googleapis.com/v0/b/firescript-577a2.appspot.com/o/imgs%2Fapp%2Froamteam%2FkqaM1ePPbV.png?alt=media&token=e113f2b5-4fbe-4b75-8d30-a114a6aa0f8d]

For more details, see [https://roamresearch.com/#/app/developer-documentation/page/bmYYKQ4vf].

### 2. Create the graph object

The graph object bundles a graph and its token, it will be passed to all other functions.

```js
const graph = initializeGraph({
  token: "roam-graph-token-XYZ",
  graph: "YourGraphName",
});
```

### 3. Call the API functions

These "other functions" are: `q`, `pull`, `createBlock`, `moveBlock`, `updateBlock`, `deleteBlock`, `createPage`, `updatePage` and `deletePage`.

For example you can search for all blocks containing the string "sdk" in your graph:

```js
q(graph,
  "[:find ?block-uid ?block-str :in $ ?search-string :where [?b :block/uid ?block-uid] [?b :block/string ?block-str] [(clojure.string/includes? ?block-str ?search-string)]]",
  ["sdk"])
.then((r) => {
  console.log(r);
});
```

Refer to the [Roam API documentation](https://roamresearch.com/#/app/developer-documentation/page/tIaOPdXCj) for an exhaustive reference of the provided functions.

## Using into the browser
```html
<script type="module">
import {initializeGraph, q} from 'https://unpkg.com/@roam-research/roam-api-sdk@0.9.0/dist/roamapisdk.js';
const graph = initializeGraph({
  token: "roam-graph-token-XYZ",
  graph: "YourGraphName",
});
q(graph,
  "[:find ?block-uid ?block-str :in $ ?search-string :where [?b :block/uid ?block-uid] [?b :block/string ?block-str] [(clojure.string/includes? ?block-str ?search-string)]]",
  ["sdk"])
.then((r) => {
  console.log(r);
});
</script>
```


## Using from Node.js
Depending on the `type` set in your `package.json`

Common JS (the default):
```js
// this works also in ES module mode
import('@roam-research/roam-api-sdk')
.then(({initializeGraph, q}) => {
  const graph = initializeGraph({
    token: "roam-graph-token-XYZ",
    graph: "YourGraphName",
  });
  q(graph,
    "[:find ?block-uid ?block-str :in $ ?search-string :where [?b :block/uid ?block-uid] [?b :block/string ?block-str] [(clojure.string/includes? ?block-str ?search-string)]]",
    ["sdk"])
  .then((r) => {
    console.log(r);
  });
});
```

ES Module (for `type: 'module'`)
```js
import {initializeGraph, q} from 'https://unpkg.com/@roam-research/roam-api-sdk@0.0.5/dist/roamapisdk.js';
const graph = initializeGraph({
  token: "roam-graph-token-XYZ",
  graph: "YourGraphName",
});
q(graph,
  "[:find ?block-uid ?block-str :in $ ?search-string :where [?b :block/uid ?block-uid] [?b :block/string ?block-str] [(clojure.string/includes? ?block-str ?search-string)]]",
  ["sdk"])
.then((r) => {
  console.log(r);
});
```
