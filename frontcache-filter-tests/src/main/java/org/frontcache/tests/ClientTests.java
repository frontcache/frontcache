package org.frontcache.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.frontcache.client.FrontCacheClient;
import org.frontcache.client.FrontCacheCluster;
import org.frontcache.core.FCHeaders;
import org.frontcache.io.CachedKeysActionResponse;
import org.frontcache.io.PutToCacheActionResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.util.NameValuePair;

public abstract class ClientTests extends CommonTestsBase {

	private static String FRONTCACHE_CLUSTER_NODE1 = TestConfig.FRONTCACHE_TEST_BASE_URI;
	
	private static String FRONTCACHE_CLUSTER_NODE2 = TestConfig.FRONTCACHE_TEST_BASE_URI;
	
	@Before
	public void setUp() throws Exception {
		super.setUp();

		webClient.addRequestHeader(FCHeaders.ACCEPT, "text/html");
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}
	
	abstract String getCacheKeyBaseURL();

	@Test
	public void getFromCacheClient() throws Exception {
		
		final String TEST_URI_A = "common/fc-agent/a.jsp";

		webClient.addRequestHeader(FCHeaders.X_FRONTCACHE_DEBUG, "true");
		frontcacheClient = new FrontCacheClient(TestConfig.FRONTCACHE_TEST_BASE_URI);
		
		// clean up
		String response = frontcacheClient.removeFromCacheAll();
		Assert.assertNotEquals(-1, response.indexOf("invalidate"));

		// the first request a - response should be cached
		HtmlPage page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI_A);
		assertEquals("a", page.getPage().asText());		

		org.frontcache.core.WebResponse resp = frontcacheClient.getFromCache(getCacheKeyBaseURL() + TEST_URI_A);

		assertEquals("a", new String(resp.getContent()));	
		return;
	}

	@Test
	public void deepInclude() throws Exception {
		
		HtmlPage page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + "common/deep-include-cache/a.jsp");
		assertEquals("abcdef", page.getPage().asText());

		org.frontcache.core.WebResponse resp = frontcacheClient.getFromCache(getCacheKeyBaseURL() + "common/deep-include-cache/a.jsp");
		assertEquals("a<fc:include url=\"/common/deep-include-cache/b.jsp\" />", new String(resp.getContent()));	

		resp = frontcacheClient.getFromCache(getCacheKeyBaseURL() + "common/deep-include-cache/b.jsp");
		assertEquals("b<fc:include url=\"/common/deep-include-cache/c.jsp\" />", new String(resp.getContent()));	
		
		resp = frontcacheClient.getFromCache(getCacheKeyBaseURL() + "common/deep-include-cache/c.jsp");
		assertEquals("c<fc:include url=\"/common/deep-include-cache/d.jsp\" />", new String(resp.getContent()));	
		
		resp = frontcacheClient.getFromCache(getCacheKeyBaseURL() + "common/deep-include-cache/d.jsp");
		assertEquals("d<fc:include url=\"/common/deep-include-cache/e.jsp\" />", new String(resp.getContent()));	
		
		resp = frontcacheClient.getFromCache(getCacheKeyBaseURL() + "common/deep-include-cache/e.jsp");
		assertEquals("e<fc:include url=\"/common/deep-include-cache/f.jsp\" />", new String(resp.getContent()));	
		
		resp = frontcacheClient.getFromCache(getCacheKeyBaseURL() + "common/deep-include-cache/f.jsp");
		assertEquals("f", new String(resp.getContent()));	
		
	}
	
	@Test
	public void getFromCacheClientNull() throws Exception {
		
		final String TEST_URI_A = "common/fc-agent/a.jsp";

		frontcacheClient = new FrontCacheClient(TestConfig.FRONTCACHE_TEST_BASE_URI);
		
		// clean up
		String response = frontcacheClient.removeFromCacheAll();
		Assert.assertNotEquals(-1, response.indexOf("invalidate"));

		org.frontcache.core.WebResponse resp = frontcacheClient.getFromCacheActionResponse(getCacheKeyBaseURL() + TEST_URI_A).getValue();

		assertNull(resp);	
		return;
	}

	@Test
	public void putToCacheClient() throws Exception {
		
		final String TEST_URI_A = "common/fc-agent/a.jsp";

		webClient.addRequestHeader(FCHeaders.X_FRONTCACHE_DEBUG, "true");
		frontcacheClient = new FrontCacheClient(TestConfig.FRONTCACHE_TEST_BASE_URI);
		
		// clean up
		String response = frontcacheClient.removeFromCacheAll();
		Assert.assertNotEquals(-1, response.indexOf("invalidate"));

		// the first request a - response should be cached
		HtmlPage page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI_A);
		assertEquals("a", page.getPage().asText());		
		
		String cacheKey = getCacheKeyBaseURL() + TEST_URI_A; 

		org.frontcache.core.WebResponse resp = frontcacheClient.getFromCacheActionResponse(cacheKey).getValue();

		assertEquals("a", new String(resp.getContent()));
		
		resp.setContent("b".getBytes());
		
		PutToCacheActionResponse actionResponse = frontcacheClient.putToCache(cacheKey, resp);
		
		assertNotNull(actionResponse);
		
		resp = frontcacheClient.getFromCacheActionResponse(cacheKey).getValue();

		assertEquals("b", new String(resp.getContent()));
		
		return;
	}
	
	@Test
	public void getCacheStatusClient() throws Exception {
		
		frontcacheClient = new FrontCacheClient(TestConfig.FRONTCACHE_TEST_BASE_URI);
		
		String response = frontcacheClient.getCacheState();
		Assert.assertNotEquals(-1, response.indexOf("cache status"));
		logger.debug("response " + response);
	}

	@Test
	public void getCacheStatusCluster() throws Exception {
		
		FrontCacheCluster fcCluster = new FrontCacheCluster(FRONTCACHE_CLUSTER_NODE1, FRONTCACHE_CLUSTER_NODE2);
		
		String response = fcCluster.getCacheState().get(FRONTCACHE_CLUSTER_NODE1);
		Assert.assertNotEquals(-1, response.indexOf("cache status"));
		logger.debug("response " + response);
	}

	@Test
	public void getCachedKeysClient() throws Exception {
		
		frontcacheClient = new FrontCacheClient(TestConfig.FRONTCACHE_TEST_BASE_URI);
		
		CachedKeysActionResponse response = frontcacheClient.getCachedKeys();
		Assert.assertNotNull(response);
		Assert.assertEquals("cached keys", response.getAction());
		logger.debug("response " + response);
	}

	@Test
	public void getCachedKeysCluster() throws Exception {
		
		FrontCacheClient frontCacheClient1 = new FrontCacheClient(FRONTCACHE_CLUSTER_NODE1);
		FrontCacheClient frontCacheClient2 = new FrontCacheClient(FRONTCACHE_CLUSTER_NODE2);
		FrontCacheCluster fcCluster = new FrontCacheCluster(frontCacheClient1, frontCacheClient2);
		
		CachedKeysActionResponse response = fcCluster.getCachedKeys().get(frontCacheClient1);
		Assert.assertNotNull(response);
		Assert.assertEquals("cached keys", response.getAction());
		logger.debug("response " + response);
	}
	
	@Test
	public void invalidationByFilterTestClient() throws Exception {
		
		final String TEST_URI = "common/fc-agent/a.jsp";
		
		webClient.addRequestHeader(FCHeaders.X_FRONTCACHE_DEBUG, "true");

		// the first request - response should be cached
		HtmlPage page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI);
		assertEquals("a", page.getPage().asText());
		
		WebResponse webResponse = page.getWebResponse(); 
		String debugCacheable = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHEABLE);
		assertEquals("true", debugCacheable);
		String debugCached = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHED);
		assertEquals("false", debugCached);
		String debugResponseTime = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_RESPONSE_TIME);
		Assert.assertNotNull(debugResponseTime);
		String debugResponseSize = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_RESPONSE_SIZE);
		assertEquals("1", debugResponseSize);

		// second request - the same request - response should be from the cache now
		page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI);
		assertEquals("a", page.getPage().asText());
		webResponse = page.getWebResponse(); 
		debugCached = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHED);
		assertEquals("true", debugCached);
		
		// cache invalidation
		frontcacheClient = new FrontCacheClient(TestConfig.FRONTCACHE_TEST_BASE_URI);
		String response = frontcacheClient.removeFromCache(TEST_URI);
		Assert.assertNotEquals(-1, response.indexOf("invalidate"));
		
		// third request - the same request - response is dynamic
		page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI);
		assertEquals("a", page.getPage().asText());
		webResponse = page.getWebResponse(); 

		debugCached = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHED);
		assertEquals("false", debugCached);
		return;
	}

	@Test
	public void invalidationByFilterTestCluster() throws Exception {
		
		FrontCacheCluster fcCluster = new FrontCacheCluster(FRONTCACHE_CLUSTER_NODE1, FRONTCACHE_CLUSTER_NODE2);
		final String TEST_URI = "common/fc-agent/a.jsp";
		
		webClient.addRequestHeader(FCHeaders.X_FRONTCACHE_DEBUG, "true");

		// the first request - response should be cached
		HtmlPage page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI);
		assertEquals("a", page.getPage().asText());
		
		WebResponse webResponse = page.getWebResponse(); 
		String debugCacheable = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHEABLE);
		assertEquals("true", debugCacheable);
		String debugCached = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHED);
		assertEquals("false", debugCached);
		String debugResponseTime = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_RESPONSE_TIME);
		Assert.assertNotNull(debugResponseTime);
		String debugResponseSize = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_RESPONSE_SIZE);
		assertEquals("1", debugResponseSize);

		// second request - the same request - response should be from the cache now
		page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI);
		assertEquals("a", page.getPage().asText());
		webResponse = page.getWebResponse(); 
		debugCached = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHED);
		assertEquals("true", debugCached);
		
		// cache invalidation
		String response = fcCluster.removeFromCache(TEST_URI).get(FRONTCACHE_CLUSTER_NODE1);
		Assert.assertNotEquals(-1, response.indexOf("invalidate"));
		
		// third request - the same request - response is dynamic
		page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI);
		assertEquals("a", page.getPage().asText());
		webResponse = page.getWebResponse(); 

		debugCached = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHED);
		assertEquals("false", debugCached);
		return;
	}

	@Test
	public void invalidationAllTestClient() throws Exception {
		
		final String TEST_URI_A = "common/fc-agent/a.jsp";
		final String TEST_URI_B = "common/fc-agent/b.jsp";
		webClient.addRequestHeader(FCHeaders.X_FRONTCACHE_DEBUG, "true");
		frontcacheClient = new FrontCacheClient(TestConfig.FRONTCACHE_TEST_BASE_URI);
		
		// clean up
		String response = frontcacheClient.removeFromCacheAll();
		Assert.assertNotEquals(-1, response.indexOf("invalidate"));

		// the first request a - response should be cached
		HtmlPage page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI_A);
		assertEquals("a", page.getPage().asText());		
		WebResponse webResponse = page.getWebResponse(); 
		String debugCached = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHED);
		assertEquals("false", debugCached);

		// the first request b - response should be cached
		page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI_B);
		assertEquals("b", page.getPage().asText());		
		webResponse = page.getWebResponse(); 
		debugCached = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHED);
		assertEquals("false", debugCached);
		
		response = frontcacheClient.getCacheState();
		Assert.assertNotEquals(-1, response.indexOf("\"cached entiries\":\"2\""));
		
		// cache invalidation
		response = frontcacheClient.removeFromCacheAll();
		Assert.assertNotEquals(-1, response.indexOf("invalidate"));
		
		response = frontcacheClient.getCacheState();
		Assert.assertNotEquals(-1, response.indexOf("\"cached entiries\":\"0\""));
		return;
	}
	
	@Test
	public void invalidationAllTestCluster() throws Exception {
		
		final String TEST_URI_A = "common/fc-agent/a.jsp";
		final String TEST_URI_B = "common/fc-agent/b.jsp";
		webClient.addRequestHeader(FCHeaders.X_FRONTCACHE_DEBUG, "true");
		FrontCacheCluster fcCluster = new FrontCacheCluster(FRONTCACHE_CLUSTER_NODE1, FRONTCACHE_CLUSTER_NODE2);
		
		// clean up
		String response = fcCluster.removeFromCacheAll().get(FRONTCACHE_CLUSTER_NODE1);
		Assert.assertNotEquals(-1, response.indexOf("invalidate"));

		// the first request a - response should be cached
		HtmlPage page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI_A);
		assertEquals("a", page.getPage().asText());		
		WebResponse webResponse = page.getWebResponse(); 
		String debugCached = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHED);
		assertEquals("false", debugCached);

		// the first request b - response should be cached
		page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + TEST_URI_B);
		assertEquals("b", page.getPage().asText());		
		webResponse = page.getWebResponse(); 
		debugCached = webResponse.getResponseHeaderValue(FCHeaders.X_FRONTCACHE_DEBUG_CACHED);
		assertEquals("false", debugCached);
		
		response = fcCluster.getCacheState().get(FRONTCACHE_CLUSTER_NODE1);
		Assert.assertNotEquals(-1, response.indexOf("\"cached entiries\":\"2\""));
		
		// cache invalidation
		response = fcCluster.removeFromCacheAll().get(FRONTCACHE_CLUSTER_NODE1);
		Assert.assertNotEquals(-1, response.indexOf("invalidate"));
		
		response = fcCluster.getCacheState().get(FRONTCACHE_CLUSTER_NODE1);
		Assert.assertNotEquals(-1, response.indexOf("\"cached entiries\":\"0\""));
		return;
	}
	
	@Test
	public void httpHeadersDuplication() throws Exception {
		HtmlPage page = webClient.getPage(TestConfig.FRONTCACHE_TEST_BASE_URI + "common/deep-include-cache/e.jsp");
		assertEquals("ef", page.getPage().asText());
		
		List<NameValuePair> headers = page.getWebResponse().getResponseHeaders();
		for (NameValuePair nv : headers)
			System.out.println("HTTP HEADER " + nv.getName() + " -- " + nv.getValue());
		
		Set<String> names = new HashSet<String>();
		for (NameValuePair nv : headers)
		{
			String name = nv.getName();
			if (names.contains(name))
				fail("HTTP header duplicate name - " + name);
			
			names.add(name);
		}
		return;
	}
	
	
}
