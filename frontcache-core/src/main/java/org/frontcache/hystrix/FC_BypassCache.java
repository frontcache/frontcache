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
package org.frontcache.hystrix;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpRequest;
import org.frontcache.core.FCHeaders;
import org.frontcache.core.FCUtils;
import org.frontcache.core.RequestContext;
import org.frontcache.core.WebResponse;
import org.frontcache.hystrix.fr.FallbackResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

public class FC_BypassCache extends HystrixCommand<Object> {

	private final HttpClient client;
	private final RequestContext context;
	private Logger logger = LoggerFactory.getLogger(FC_BypassCache.class);

    public FC_BypassCache(HttpClient client, RequestContext context) {
        
        super(Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(context.getDomainContext().getDomain()))
                .andCommandKey(HystrixCommandKey.Factory.asKey("Origin-Hits"))
        		);
        
        this.client = client;
        this.context = context;
    }

    @Override
    protected Object run() throws Exception {

    	forwardToOrigin();
    	
    	return null;
    }
    
    @Override
    protected Object getFallback() {
    	
		try {
			HttpServletRequest httpRequest = context.getRequest();
			String url = FCUtils.getRequestURL(httpRequest);
			HttpServletResponse httpResponse = context.getResponse();
			
			context.setHystrixFallback();
			
			String failedExceptionMessage = "";
			if (null != getFailedExecutionException())
				failedExceptionMessage += getFailedExecutionException().getMessage();
			
			logger.error("FC - ORIGIN ERROR - " + url + " " + failedExceptionMessage + ", Events " + getExecutionEvents());
			
			WebResponse webResponse = FallbackResolverFactory.getInstance().getFallback(context.getDomainContext(), this.getClass().getName(), url);
			
			httpResponse.getOutputStream().write(webResponse.getContent());
			httpResponse.setContentType(webResponse.getHeader(FCHeaders.CONTENT_TYPE));
		} catch (Exception e) {
			e.printStackTrace();
		}
        return null;
    }
    
    
	private void forwardToOrigin() throws IOException, ServletException
	{
		HttpServletRequest request = context.getRequest();
		
		if (context.isFilterMode())
		{
			HttpServletResponse response = context.getResponse();
			FilterChain chain = context.getFilterChain();
			chain.doFilter(request, response);
		} else {
			
			// stand alone mode
			
			Map<String, List<String>> headers = FCUtils.buildRequestHeaders(request);
			headers.put(FCHeaders.X_FRONTCACHE_CLIENT_IP, Arrays.asList(new String[]{FCUtils.getClientIP(context.getRequest())}));
			
			String verb = request.getMethod();
			InputStream requestEntity = getRequestBody(request);
			String uri = context.getRequestURI();

			try {
				HttpResponse response = forward(client, verb, uri, request, headers, requestEntity);
				
				// response 2 context
				setResponse(response);
				
			}
			catch (Exception ex) {
				ex.printStackTrace();
				context.set("error.status_code", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				context.set("error.exception", ex);
				
				context.setHystrixFallback();
				logger.error("FC - ORIGIN ERROR - " + uri);
				
			}
		}
		
		return;
	}
    
	
	private void setResponse(HttpResponse response) throws IOException {
		
		context.setHttpClientResponse((CloseableHttpResponse) response);
		
		setResponse(response.getStatusLine().getStatusCode(),
				response.getEntity() == null ? null : response.getEntity().getContent(),
				FCUtils.revertHeaders(response.getAllHeaders()));
	}

	
	private void setResponse(int status, InputStream entity, Map<String, List<String>> headers) throws IOException {
		
		context.setResponseStatusCode(status);
		
		if (entity != null) {
			context.setResponseDataStream(entity);
		}
		
		for (String key : headers.keySet()) {
			for (String value : headers.get(key)) {
				context.addOriginResponseHeader(key, value);
			}
		}

	}	
	

	private InputStream getRequestBody(HttpServletRequest request) {
		InputStream requestEntity = null;
		try {
			requestEntity = request.getInputStream();
		}
		catch (IOException ex) {
			// no requestBody is ok.
		}
		return requestEntity;
	}	
	
	/**
	 * forward all kind of requests (GET, POST, PUT, ...)
	 * 
	 * @param httpclient
	 * @param verb
	 * @param uri
	 * @param request
	 * @param headers
	 * @param params
	 * @param requestEntity
	 * @return
	 * @throws Exception
	 */
	private HttpResponse forward(HttpClient httpclient, String verb, String uri, HttpServletRequest request,
			Map<String, List<String>> headers, InputStream requestEntity)
					throws Exception {

		URL host = context.getOriginURL();
		HttpHost httpHost = FCUtils.getHttpHost(host);
		uri = (host.getPath() + uri).replaceAll("/{2,}", "/");
		
		HttpRequest httpRequest;
		switch (verb.toUpperCase()) {
		case "POST":
			HttpPost httpPost = new HttpPost(uri + context.getRequestQueryString());
			httpRequest = httpPost;
			httpPost.setEntity(new InputStreamEntity(requestEntity, request.getContentLength()));
			break;
		case "PUT":
			HttpPut httpPut = new HttpPut(uri + context.getRequestQueryString());
			httpRequest = httpPut;
			httpPut.setEntity(new InputStreamEntity(requestEntity, request.getContentLength()));
			break;
		case "PATCH":
			HttpPatch httpPatch = new HttpPatch(uri + context.getRequestQueryString());
			httpRequest = httpPatch;
			httpPatch.setEntity(new InputStreamEntity(requestEntity, request.getContentLength()));
			break;
		default:
			httpRequest = new BasicHttpRequest(verb, uri + context.getRequestQueryString());
		}
		
		
		try {
			httpRequest.setHeaders(FCUtils.convertHeaders(headers));
			Header acceptEncoding = httpRequest.getFirstHeader("accept-encoding");
			if (acceptEncoding != null && acceptEncoding.getValue().contains("gzip"))
			{
				httpRequest.setHeader("accept-encoding", "gzip");
			}
			HttpResponse originResponse = httpclient.execute(httpHost, httpRequest);
			return originResponse;
		} finally {
			// When HttpClient instance is no longer needed,
			// shut down the connection manager to ensure
			// immediate deallocation of all system resources
			// httpclient.getConnectionManager().shutdown();
		}
	}	
    
}