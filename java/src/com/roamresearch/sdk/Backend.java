package com.roamresearch.sdk;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Backend {
	final String token;
	final String graph;
	final HttpClient client;
	final ObjectMapper om;
	static final Map EMPTY_MAP = Map.of();

	private final static String frontDeskBaseUrl = "https://api.roamresearch.com";
	// private final static String frontDeskBaseUrl = "http://localhost:8080";
	private final Map<String, String> graphBaseUrls = new HashMap<String, String>();

	public Backend(String token, String graph) {
		this.token = token;
		this.graph = graph;
		client = HttpClient.newBuilder().followRedirects(Redirect.NORMAL).build();
		om = new ObjectMapper();
	}

	public Map<String, Object> api(String path, String method, Map<String, Object> body) {
		try {
			String baseUrl = graphBaseUrls.get(graph);
			URI uri = new URI((baseUrl == null ? frontDeskBaseUrl : baseUrl) + path);
			HttpResponse<String> response = client.send(
					HttpRequest.newBuilder(uri)
							.method(method, HttpRequest.BodyPublishers.ofString(om.writeValueAsString(body))) // TODO ?
																												// make
																												// something
																												// better
																												// than
																												// plain
																												// cast
							.headers("Content-Type", "application/json; charset=utf-8", "Authorization",
									"Bearer " + token, "x-authorization", "Bearer " + token)
							.build(),
					HttpResponse.BodyHandlers.ofString());
			Optional<HttpResponse<String>> previousResponse = response.previousResponse();
			if (previousResponse.isPresent()) {
				Optional<String> location = previousResponse.get().headers().firstValue("Location");
				if (location.isPresent()) {
					URI locuri = new URI(location.get());
					graphBaseUrls.put(graph, locuri.getScheme() + "://" + locuri.getHost() + ":" + locuri.getPort());
				}
			}
			switch (response.statusCode()) {
			case 200:
				break;
			case 400:
				throw new RuntimeException("Error: " + (String) ((Map) om.readValue(response.body(), Map.class)).getOrDefault("message",
						"HTTP 400"));
			case 401:
				throw new RuntimeException("Invalid token or token doesn't have enough privileges.");
			case 500:
				throw new RuntimeException("Server Error: " + ((Map) om.readValue(response.body(), Map.class)).getOrDefault("message",
						"HTTP 500"));
                        case 503:
                                throw new RuntimeException("HTTP Status: 503. Your graph is not ready yet for a request, please retry in a few seconds.");
			default:
				throw new RuntimeException("HTTP Status: " + response.statusCode());
			}

			if (response.body().isBlank())
				return EMPTY_MAP;
			return om.readValue(response.body(), Map.class);
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void doCommand(Map cmd) {
		api("/api/graph/" + graph + "/write", "POST", cmd);
	}

	public Object q(String query, String... args) {
		return api("/api/graph/" + graph + "/q", "POST", Map.of("query", query, "args", args)).get("result");
	}

	public Object pull(String pattern, String eid) {
		return api("/api/graph/" + graph + "/pull", "POST", Map.of("eid", eid, "selector", pattern)).get("result");
	}

	public void deleteBlock(String uid) {
		doCommand(Map.of("action", "delete-block", "block", Map.of("uid", uid)));
	}

	static Object ensure(Object m, String prop, String parent) {
		Object v = prop != null ? ((Map) m).get(prop) : null;
		if (v != null)
			return v;
		throw new IllegalArgumentException("No " + prop + " found under " + parent + ".");
	}

	static Map selectKeys(Map m, String... keys) {
		HashMap r = new HashMap();
		for (String key : keys) {
			final Object v = m.get(key);
			if (v != null)
				r.put(key, v);
		}
		return r;
	}

	public void deleteBlock(Map cmd) {
		deleteBlock((String) ensure(cmd.get("block"), "uid", "block"));
	}

	public static Object location(String parentUid, String order) {
		return Map.of("parent-uid", parentUid, "order", order);
	}

	public static Object location(String parentUid, long order) {
		return Map.of("parent-uid", parentUid, "order", order);
	}

	public void moveBlock(String uid, Object location) {
		doCommand(Map.of("action", "move-block", "location", location, "block", Map.of("uid", uid)));
	}

	static Map<String, Object> ensureLoc(Map cmd) {
		Object loc = cmd.get("location");
		return Map.of("parent-uid", ensure(loc, "parent-uid", "location"), "order", ensure(loc, "order", "location"));
	}

	public void moveBlock(Map cmd) {
		String uid = (String) ensure(cmd.get("block"), "uid", "block");
		Map<String, Object> loc2 = ensureLoc(cmd);
		moveBlock(uid, loc2);
	}

	public void createBlock(Object location, String string, Map block) {
		block = selectKeys(block, "uid", "open", "heading", "text-align", "children-view-type");
		block.put("string", string);
		doCommand(Map.of("action", "create-block", "location", location, "block", block));
	}

	public void createBlock(Map cmd) {
		Map block = (Map) cmd.get("block");
		createBlock(ensureLoc(cmd), (String) ensure(block, "string", "block"), block);
	}

	public void updateBlock(String uid, Map block) {
		block = selectKeys(block, "string", "open", "heading", "text-align", "children-view-type");
		block.put("uid", uid);
		doCommand(Map.of("action", "update-block", "block", block));
	}

	public void updateBlock(Map cmd) {
		Map block = (Map) cmd.get("block");
		updateBlock((String) ensure(block, "uid", "block"), block);
	}

	public void createPage(String title, Map page) {
		page = selectKeys(page, "uid", "children-view-type");
		page.put("title", title);
		doCommand(Map.of("action", "create-page", "page", page));
	}

	public void createPage(Map cmd) {
		Map page = (Map) cmd.get("page");
		createPage((String) ensure(page, "title", "page"), page);
	}

	public void updatePage(String uid, Map page) {
		page = selectKeys(page, "title", "children-view-type");
		page.put("uid", uid);
		doCommand(Map.of("action", "update-page", "page", page));
	}

	public void updatePage(Map cmd) {
		Map page = (Map) cmd.get("page");
		updatePage((String) ensure(page, "uid", "page"), page);
	}

	public void deletePage(String uid) {
		doCommand(Map.of("action", "delete-page", "page", Map.of("uid", uid)));
	}

	public void deletePage(Map cmd) {
		Map page = (Map) cmd.get("page");
		deletePage((String) ensure(page, "uid", "page"));
	}

	public void batch(Map... cmds) {
		doCommand(Map.of("action", "batch-actions", "actions", List.of(cmds)));
	}

	public void batch(Map cmd) {
		List actions = (List) cmd.get("actions");
		batch((Map[]) actions.toArray(new Map[0]));
	}

	public static void main(String[] args) throws JsonProcessingException {
		new Backend("", "Clojuredart").createBlock(Map.of("block",
				Map.of("string", "hi from java sdk"), "location", Map.of("parent-uid", "01-11-2023", "order", "last")));
		System.err.println(new Backend("", "Clojuredart").q(
				"[:find ?block-uid ?block-str :in $ ?search-string :where [?b :block/uid ?block-uid] [?b :block/string ?block-str] [(clojure.string/includes? ?block-str ?search-string)]]",
				"apple"));
		System.err.println(new Backend("", "Clojuredart").pull(
				"[:block/uid :node/title :block/string {:block/children [:block/uid :block/string]} {:block/refs [:node/title :block/string :block/uid]}]",
				"[:block/uid \"08-30-2022\"]"));
	}
}
