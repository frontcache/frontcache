package org.frontcache.cache;

import java.util.Properties;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.http.client.HttpClient;
import org.frontcache.core.FCUtils;
import org.frontcache.core.FrontCacheException;
import org.frontcache.core.WebResponse;
import org.frontcache.reqlog.RequestLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoCacheProcessor implements CacheProcessor {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public WebResponse processRequest(String originUrlStr, MultiValuedMap<String, String> requestHeaders, HttpClient client) throws FrontCacheException {

		long start = System.currentTimeMillis();
		boolean isRequestCacheable = true;
		boolean isRequestDynamic = true;
		long lengthBytes = -1;
		
		WebResponse cachedWebResponse = FCUtils.dynamicCall(originUrlStr, requestHeaders, client);

		lengthBytes = cachedWebResponse.getContentLenth();

		RequestLogger.logRequest(originUrlStr, isRequestCacheable, isRequestDynamic, System.currentTimeMillis() - start, lengthBytes);

		return cachedWebResponse;
	}

	@Override
	public void init(Properties properties) {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub

	}

	@Override
	public void putToCache(String url, WebResponse component) {
		// TODO Auto-generated method stub

	}

	@Override
	public WebResponse getFromCache(String url) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeFromCache(String filter) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeFromCacheAll() {
		// TODO Auto-generated method stub

	}

}


