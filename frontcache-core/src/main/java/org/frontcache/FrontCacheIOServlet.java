package org.frontcache;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.frontcache.cache.CacheManager;
import org.frontcache.core.WebResponse;
import org.frontcache.io.ActionResponse;
import org.frontcache.io.CacheStatusActionResponse;
import org.frontcache.io.CachedKeysActionResponse;
import org.frontcache.io.DummyActionResponse;
import org.frontcache.io.GetFromCacheActionResponse;
import org.frontcache.io.InvalidateActionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class FrontCacheIOServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected Logger logger = LoggerFactory.getLogger(getClass());
	private ObjectMapper jsonMapper = new ObjectMapper();
	
	FrontCacheEngine fcEngine = null;
	
	public FrontCacheIOServlet() {
	}
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		fcEngine = FrontCacheEngine.getFrontCache();
		
		return;
	}

	@Override
	public void destroy() {
		super.destroy();
		FrontCacheEngine.destroy();
		fcEngine = null;
	}	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		process(req, resp);
		return;
	}


	private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		String action = req.getParameter("action");
		if (null == action)
			action = "";
		
		ActionResponse aResponse = null;
		switch (action)
		{
		case "invalidate":
			aResponse = invalidate(req);
			break;
			
		case "get-cache-state":
			aResponse = getCacheStatus(req);
			break;
			
		case "get-cached-keys":
			aResponse = getCachedKeys(req);
			break;
			
		case "get-from-cache":
			aResponse = getFromCache(req);
			break;
			
			default:
				aResponse = new DummyActionResponse();
			
		}
		resp.getOutputStream().write(jsonMapper.writeValueAsBytes(aResponse));
	}

	/**
	 * 
	 * @param req
	 * @return
	 */
	private ActionResponse invalidate(HttpServletRequest req)
	{
		String filter = req.getParameter("filter");
		ActionResponse aResponse = new InvalidateActionResponse(filter);
		if (null == filter)
		{
			aResponse.setResponseStatus(ActionResponse.RESPONSE_STATUS_ERROR);
			return aResponse;
		}
			
		if ("*".equals(filter))
			CacheManager.getInstance().removeFromCacheAll();
		else
			CacheManager.getInstance().removeFromCache(filter);
			
		logger.info("Invalidation for filter: " + filter);
		return aResponse;
	}

	/**
	 * 
	 * @param req
	 * @return
	 */
	private ActionResponse getCacheStatus(HttpServletRequest req)
	{
		Map<String, String> state = CacheManager.getInstance().getCacheStatus();
		ActionResponse aResponse = new CacheStatusActionResponse(state);
			
		return aResponse;
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	private ActionResponse getCachedKeys(HttpServletRequest req)
	{
		List<String> keys = CacheManager.getInstance().getCachedKeys();
		ActionResponse aResponse = new CachedKeysActionResponse(keys);
			
		return aResponse;
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	private ActionResponse getFromCache(HttpServletRequest req)
	{
		String key = req.getParameter("key");
		if (null == key)
			return new GetFromCacheActionResponse(key);
		
		WebResponse value = CacheManager.getInstance().getFromCache(key);
		GetFromCacheActionResponse out = new GetFromCacheActionResponse(key, value);
		
		try {
			byte[] b = jsonMapper.writeValueAsBytes(out);
			
			GetFromCacheActionResponse r = jsonMapper.readValue(b, GetFromCacheActionResponse.class);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return out;
	}
	
	/**
	 * 
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		process(req, resp);
		return;
	}
	
}
