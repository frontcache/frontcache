package org.frontcache;

import java.util.logging.Logger;

import org.frontcache.cache.CacheProcessor;

public class FCUtils {

	private FCUtils() {
	}
	
	private static Logger logger = Logger.getLogger(FCUtils.class.getName());

	/**
	 * wrap String to WebComponent.
	 * Check for header - extract caching options.
	 * 
	 * @param content
	 * @return
	 */
	public static final WebComponent parseWebComponent (String content)
	{
		int cacheMaxAgeSec = CacheProcessor.NO_CACHE;
		
		String outStr = null;
		final String START_MARKER = "<fc:component";
		final String END_MARKER = "/>";
		
		int startIdx = content.indexOf(START_MARKER);
		if (-1 < startIdx)
		{
			int endIdx = content.indexOf(END_MARKER, startIdx);
			if (-1 < endIdx)
			{
				String includeTagStr = content.substring(startIdx, endIdx + END_MARKER.length());
				cacheMaxAgeSec = getCacheMaxAge(includeTagStr);
				
				
				// exclude tag from content
				outStr = content.substring(0, startIdx)   +   
						 content.substring(endIdx + END_MARKER.length(), content.length());
				
			} else {
				// can't find closing 
				outStr = content;
			}
			
		} else {
			outStr = content;
		}

		WebComponent component = new WebComponent(outStr, cacheMaxAgeSec);
		
		return component;
	}
	
	/**
	 * 
	 * @param content
	 * @return time to live in cache in seconds
	 */
	private static int getCacheMaxAge(String content)
	{
		logger.info("component tag - " + content);
		final String START_MARKER = "cache-max-age=\"";
		int startIdx = content.indexOf(START_MARKER);
		if (-1 < startIdx)
		{
			int endIdx = content.indexOf("\"", startIdx + START_MARKER.length());
			if (-1 < endIdx)
			{
				String maxAgeStr = content.substring(startIdx + START_MARKER.length(), endIdx);
				logger.info("component cache-max-age - " + maxAgeStr);
				try
				{
					return Integer.parseInt(maxAgeStr);
				} catch (Exception e) {
					logger.warning("can't parse component cache-max-age - " + maxAgeStr + " defalut is used (NO_CACHE)");
					return CacheProcessor.NO_CACHE;
				}
				
			} else {
				// can't find closing 
				return CacheProcessor.NO_CACHE;
			}
			
			
		} else {
			// no cache-max-age attribute
			return CacheProcessor.NO_CACHE;
		}

	}	
	

}
