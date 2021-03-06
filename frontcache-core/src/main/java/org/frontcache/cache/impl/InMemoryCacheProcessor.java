/**
 *        Copyright 2017 Eternita LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.frontcache.cache.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.frontcache.cache.CacheProcessor;
import org.frontcache.cache.CacheProcessorBase;
import org.frontcache.core.WebResponse;

public class InMemoryCacheProcessor extends CacheProcessorBase implements CacheProcessor {


	private Map<String, WebResponse> cache = new ConcurrentHashMap<String, WebResponse>();

	private int currentSize = 0;
	private int maxSize = 0;
	
	@Override
	public void init(Properties properties) {
		Objects.requireNonNull(properties, "Properties should not be null");
		super.init(properties);
		
		try
		{
			String maxSizeStr = properties.getProperty("front-cache.cache-processor.impl.in-memory.maxsize");
			String countStr = maxSizeStr;
			
			if (null == maxSizeStr)
			{
				logger.info("front-cache.cache-processor.impl.in-memory.maxsize is not defined. Please define");
				logger.info("max cache size is " + maxSize);
				return;
			}
			
			if (maxSizeStr.endsWith("G"))
			{
				countStr = maxSizeStr.substring(0, maxSizeStr.indexOf("G"));
				maxSize = Integer.parseInt(countStr)  * 1024 * 1024 * 1024; 
			} else if (maxSizeStr.endsWith("M")) {
				countStr = maxSizeStr.substring(0, maxSizeStr.indexOf("M"));
				maxSize = Integer.parseInt(countStr)  * 1024 * 1024; 
			} else if (maxSizeStr.endsWith("K")) {
				countStr = maxSizeStr.substring(0, maxSizeStr.indexOf("K"));
				maxSize = Integer.parseInt(countStr)  * 1024; 
			} else {
				maxSize = Integer.parseInt(countStr); 
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		logger.info("max cache size is " + maxSize);
	}	

	@Override
	public void putToCache(String domain, String url, WebResponse component) {
		
		int newSize = currentSize + component.getContent().length;
		if (newSize < maxSize)
		{
			cache.put(url, component);
			currentSize = newSize;
		} else {
			logger.info("web component is not cached - max cache size " + maxSize + " is reached");
		}
		
		return;
	}

	@Override
	public WebResponse getFromCacheImpl(String url) {
		
		WebResponse comp = cache.get(url);
		if (null == comp)
			return null;
		
		return comp;
	}

	@Override
	public void destroy() {
		super.destroy();
		cache.clear();
		
		return;
	}

	@Override
	public void removeFromCache(String domain, String filter) {

		List<String> removeList = new ArrayList<String>();
		
		for(String key : cache.keySet())
		{
			if (-1 < key.indexOf(filter))
				removeList.add(key);	
		}
		
		for(String key : removeList)
			cache.remove(key);

		return;
	}

	@Override
	public void removeFromCacheAll(String domain) {
		cache.clear();
	}
	
	@Override
	public Map<String, String> getCacheStatus() {
		Map<String, String> status = super.getCacheStatus();
		
		status.put("impl", this.getClass().getName());

		status.put(CacheProcessor.CACHED_ENTRIES, "" + cache.keySet().size());
		
		status.put("current size", "" + currentSize);
		
		status.put("max size", "" + maxSize);
		
		return status;
	}

	@Override
	public List<String> getCachedKeys() {
		List<String> keys = new ArrayList<String>();
		keys.addAll(cache.keySet());
		return keys;
	}

	@Override
	public void patch() {
		// TODO Auto-generated method stub
		
	}
	
}



