import 'dart:convert' as convert;
import 'dart:io' as io;

class RoamClient {
  final String graph;
  final String token;
  final Map<String, String> _graphBaseUrls = {};
  late final io.HttpClient _client;

  static final String _frontDeskBaseUrl = "https://api.roamresearch.com";
  static final Map<String, Object> _empty = {};

  void dispose() {
    _client.close();
  }

  RoamClient(this.graph, this.token) {
    _client = io.HttpClient();
  }

  Future<Map<String, dynamic>> api(
      String path, String method, Map<String, Object> body) async {
    final baseUrl = _graphBaseUrls[graph] ?? _frontDeskBaseUrl;
    final Uri uri = Uri.parse(baseUrl + path);
    final io.HttpClientRequest request =
        _configureRequest(await _client.openUrl(method, uri), body);
    return _readResponse(await request.close(), path, method, body);
  }

  io.HttpClientRequest _configureRequest(
      io.HttpClientRequest request, Map<String, Object> body) {
    request.headers
      ..add("Content-Type", "application/json; charset=utf-8")
      ..add("Authorization", "Bearer $token")
      ..add("x-authorization", "Bearer $token");
    request.followRedirects = false;
    request.write(convert.json.encode(body));
    return request;
  }

  Future<Map<String, dynamic>> _decodeResponse(
      io.HttpClientResponse response) async {
    final String textBody =
        await response.transform(convert.utf8.decoder).join();
    if (textBody.isEmpty) return _empty;
    return convert.json.decode(textBody);
  }

  Future<Map<String, dynamic>> _readResponse(io.HttpClientResponse response,
      String path, String method, Map<String, Object> body) async {
    switch (response.statusCode) {
      case 308:
        final String location =
            response.headers.value(io.HttpHeaders.locationHeader)!;
        Uri newUri = Uri.parse(location);
        _graphBaseUrls[graph] =
            "${newUri.scheme}://${newUri.host}:${newUri.port}";
        await response.drain();
        return api(path, method, body);

      case 400:
        var decodedResp = await _decodeResponse(response);
        throw Exception("Error: " + (decodedResp["message"] ?? "HTTP 400"));

      case 401:
        throw Exception(
            "HTTP Status: 401. Invalid token or token doesn't have enough privileges.");

      case 500:
        var decodedResp = await _decodeResponse(response);
        throw Exception("HTTP Status: 500. " + (decodedResp["message"] ?? ""));

      case 503:
        throw Exception(
            "HTTP Status: 503. Your graph is not ready yet for a request, please retry in a few seconds.");

      case 200:
        return _decodeResponse(response);

      default:
        throw Exception("HTTP Status: ${response.statusCode}");
    }
  }

  Future<Map<String, dynamic>> _doCommand(Map<String, Object> cmd) {
    return api("/api/graph/$graph/write", "POST", cmd);
  }

  Future<Object> q(String query, List<String> args) async {
    final result = await api(
        "/api/graph/$graph/q", "POST", {"query": query, "args": args});
    return result["result"] ?? _empty;
  }

  Future<Object> pull(String pattern, String eid) async {
    final result = await api(
        "/api/graph/$graph/pull", "POST", {"eid": eid, "selector": pattern});
    return result["result"] ?? _empty;
  }

  Future<bool> deleteBlock(String uid) async {
    final result = await _doCommand({
      "action": "delete-block",
      "block": {"uid": uid}
    });
    return result == _empty;
  }

  Future<bool> moveBlock(String uid, Map<String, dynamic> location) async {
    final result = await _doCommand({
      "action": "move-block",
      "block": {"uid": uid},
      "location": location
    });
    return result == _empty;
  }

  Future<bool> createBlock(
      Map<String, dynamic> location, String string, Map block) async {
    final result = await _doCommand({
      "action": "create-block",
      "block": block..addAll({"string": string}),
      "location": location
    });
    return result == _empty;
  }

  Future<bool> updateBlock(String uid, Map block) async {
    final result = await _doCommand({
      "action": "update-block",
      "block": block..addAll({"uid": uid})
    });
    return result == _empty;
  }

  Future<bool> createPage(String title, Map page) async {
    final result = await _doCommand({
      "action": "create-page",
      "page": page..addAll({"title": title})
    });
    return result == _empty;
  }

  Future<bool> updatePage(String uid, Map page) async {
    final result = await _doCommand({
      "action": "update-page",
      "page": page..addAll({"uid": uid})
    });
    return result == _empty;
  }

  Future<bool> deletePage(String uid) async {
    final result = await _doCommand({
      "action": "delete-page",
      "page": {"uid": uid}
    });
    return result == _empty;
  }
}

void main(List<String> args) async {
  var client = RoamClient(
      "Clojuredart", "roam-graph-token-ir949E3YGdKCRSaXk29jD1cW5mi4W");

  var r1 = client.pull(
      "[{:block/children [:block/string]}]", "[:block/uid \"06-25-2023\"]");

// "[{:block/children [:block/string]}]" "[:block/uid \"06-30-2023\"]"
  var r = client.q(
      r'[:find (pull ?b [:block/uid :block/string]) :in $ ?search-string :where [?b :block/string ?block-str] [(clojure.string/includes? ?block-str ?search-string)]]',
      ["apple"]);
  print("ddd");

  var r3 = await client.createBlock({"parent-uid": "06-25-2023", "order": "first"}, "Test", {});
  print(r3);

  //print(await r);
}
