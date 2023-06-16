# SDK for Roam Reasearch API

This clj library allows to use the [Roam API](https://roamresearch.com/#/app/developer-documentation/page/tIaOPdXCj) outside of the Roam Research webapp.

## Usage
### 1. Create a token

You can create and edit roam-graph-tokens from a new section "API tokens" in the "Graph" tab in the Settings:

![https://firebasestorage.googleapis.com/v0/b/firescript-577a2.appspot.com/o/imgs%2Fapp%2Froamteam%2FkqaM1ePPbV.png?alt=media&token=e113f2b5-4fbe-4b75-8d30-a114a6aa0f8d]

For more details, see [https://roamresearch.com/#/app/developer-documentation/page/bmYYKQ4vf].

### 2. Import the library

```clojure
{:paths ["src"]
 :deps {com.roamresearch/backend-sdk {:mvn/version "0.0.4"}}}
```

### 3. Call the API functions

These "other functions" are: `q`, `pull`, `createBlock`, `moveBlock`, `updateBlock`, `deleteBlock`, `createPage`, `updatePage` and `deletePage`.

For example you can search for all blocks containing the string "sdk" in your graph:

```clojure
(b/q {:token ""
      :graph "Clojuredart"}
  "[:find ?block-uid ?block-str :in $ ?search-string :where [?b :block/uid ?block-uid] [?b :block/string ?block-str] [(clojure.string/includes? ?block-str ?search-string)]]"
  "apple")
```

Refer to the [Roam API documentation](https://roamresearch.com/#/app/developer-documentation/page/tIaOPdXCj) for an exhaustive reference of the provided functions.
