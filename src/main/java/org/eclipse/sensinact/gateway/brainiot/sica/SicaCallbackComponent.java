/**
 * 
 */
package org.eclipse.sensinact.gateway.brainiot.sica;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.eclipse.sensinact.gateway.nthbnd.http.callback.HttpRequestWrapper;
import org.eclipse.sensinact.gateway.nthbnd.http.callback.CallbackContext;
import org.eclipse.sensinact.gateway.nthbnd.http.callback.CallbackService;
import org.eclipse.sensinact.gateway.util.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@Component(immediate = true, service = CallbackService.class )
public class SicaCallbackComponent implements CallbackService {

	private static final Logger LOG = LoggerFactory.getLogger(SicaCallbackComponent.class);
	
	private CloseableHttpClient client;
	private static final String CALLBACK_ENDPOINT="/sica-history";
	
	@Activate
	public void activate() {
		 this.client = HttpClients.createDefault();
	}
	
	@Deactivate
	public void deactivate() {
		try {
			this.client.close();
		} catch (IOException e) {
			LOG.debug(e.getMessage());
		} finally {
			this.client = null;
		}
	}
	
	@Override	
	public String getPattern() {
		return CALLBACK_ENDPOINT;
	}

	@Override	
	public int getCallbackType() {
		return CallbackService.CALLBACK_SERVLET;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	@Override
	public Dictionary getProperties() {
		return new Hashtable() {{
			this.put("pattern",CALLBACK_ENDPOINT);
		}};
	}

	@Override
	public void process(CallbackContext context) {
		String baseUrl = new StringBuilder(
			).append("http://"
			).append(((HttpRequestWrapper)context.getRequest()).getServerName()
			).append(":"
			).append(((HttpRequestWrapper)context.getRequest()).getServerPort()
			).append("/sensinact/").toString();

		String query = ((HttpRequestWrapper)context.getRequest()).getQueryString();
		Map<String,String> queryMap = processRequestQuery(query);
		String sica = queryMap.get("sicaId");
		String fromStr = queryMap.get("from");
		String toStr = queryMap.get("to");
		String fieldsStr = queryMap.get("fields");
		int wait = 5000;
		while(getLockState(baseUrl, sica)) {
			try {
				Thread.sleep(150);
				wait-=150;
				if(wait < 0) {
					setError(context, 520, new StringBuilder().append("Internal server error :"
						).append("locked status timeout").toString());
					return;
				}
			} catch (InterruptedException e) {
				Thread.interrupted();
				
			}
		}
		setLockState(baseUrl,sica,true);
		long from = fromStr==null?0:Long.parseLong(fromStr);
		long to = toStr==null?0:Long.parseLong(toStr);
		String[] fields = fieldsStr==null?null:fieldsStr.split(",");
		setFrom(baseUrl,sica,from);
		setTo(baseUrl,sica,to);
		setFields(baseUrl,sica,fields);
		String history = this.get(baseUrl,sica,"value");
		setLockState(baseUrl,sica,false);
		
		try {
			context.getResponse().setContent(history.getBytes());
			context.getResponse().setResponseStatus(200);			
		} catch (Exception e) {
			setError(context, 520, new StringBuilder().append("Internal server error :\n").append(e.getClass().getName()
					).append("\n").append(e.getMessage()).toString());
		}
	}
	
	private void setError(CallbackContext context, int status, String message) {
		try {
			context.getResponse().setError(status, message);
		} catch (Exception ex) {
			LOG.error(ex.getMessage(),ex);
		}
	}
	
	private boolean getLockState(String baseUrl, String sica) {
		Boolean lock = this.<Boolean>get(baseUrl, sica, "lock");	
		return lock==null?false:lock.booleanValue();
	}
	
	private void setLockState(String baseUrl, String sica, boolean locked) {
		this.post(baseUrl, sica, "lock", "boolean", locked);
	}

	private void setFrom(String baseUrl, String sica, long from) {
		this.post(baseUrl, sica, "from", "long", from);
	}

	private void setTo(String baseUrl, String sica, long to) {
		if(to == 0) {
			to = System.currentTimeMillis();
		}
		this.post(baseUrl, sica, "to", "long", to);
	}

	private void setFields(String baseUrl, String sica, String[] fields) {
		this.post(baseUrl, sica, "fields", "Array of java.lang.String", fields==null?"null":Arrays.toString(fields));
	}
	
    private Map<String, String> processRequestQuery(String queryString) {
        if (queryString == null) {
            return Collections.<String, String>emptyMap();
        }
        Map<String, String> queryMap = new HashMap<String,String>();

        char[] characters = queryString.toCharArray();
        int index = 0;
        int length = characters.length;

        boolean escape = false;
        String name = null;
        String value = null;
        StringBuilder element = new StringBuilder();

        for (; index < length; index++) {
            char c = characters[index];
            if (escape) {
                escape = false;
                element.append(c);
                continue;
            }
            switch (c) {
                case '\\':
                    escape = true;
                    break;
                case '=':
                    if (name == null) {
                        name = element.toString();
                        element = new StringBuilder();

                    } else {
                        element.append(c);
                    }
                    break;
                case '&':
                	if(name == null && element.length()>0) {
                    	name = element.toString();
                        queryMap.put(name, Boolean.TRUE.toString());
                	} else {
	                    value = element.toString();
	                    queryMap.put(name, value);
                	}
                    name = null;
                    value = null;
                    element = new StringBuilder();
                    break;
                default:
                    element.append(c);
            }
        }
        if(name == null && element.length()>0) {
        	name = element.toString();
            queryMap.put(name, Boolean.TRUE.toString());
            return queryMap;
        }
        value = element.toString();
        queryMap.put(name, value);
        return queryMap;
    }

	private <R> R post(String baseUrl, String sica, String attribute, String type, Object value) {
		String url = new StringBuilder().append(baseUrl).append(sica).append("/values/history/SET").toString();
		HttpPost httppost = new HttpPost(url);

		try {
			JSONArray array = new JSONArray();
			array.put(new JSONObject().put("name", "attributeName").put("type","string").put("value",attribute));
			array.put(new JSONObject().put("name", "value").put("type", type).put("value",value));
			
			StringEntity requestEntity = new StringEntity(array.toString(), ContentType.APPLICATION_JSON);
			httppost.setEntity(requestEntity);
			httppost.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());

			try (CloseableHttpResponse response = client.execute(httppost)) {
				int code = response.getStatusLine().getStatusCode();
				LOG.debug("Status code " + code);
				byte[] content = IOUtils.read(response.getEntity().getContent());
				JSONObject r = new JSONObject(new String(content));
				return (R)r.getJSONObject("response").get("value");
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
		}
		return (R) null;
	}
	
	private <R> R get(String baseUrl, String sica, String attribute) {
		String url = new StringBuilder().append(baseUrl).append(sica).append("/values/history/GET?attributeName=").append(attribute).toString();
		HttpGet httpget = new HttpGet(url);
		try {
			httpget.addHeader("Accept", ContentType.APPLICATION_JSON.getMimeType());

			try (CloseableHttpResponse response = client.execute(httpget)) {
				int code = response.getStatusLine().getStatusCode();
				LOG.debug("Status code " + code);
				byte[] content = IOUtils.read(response.getEntity().getContent());
				JSONObject r = new JSONObject(new String(content));
				R result = (R)r.getJSONObject("response").get("value");
				if(!JSONObject.NULL.equals(result)) {
					return result;
				}
			}
		} catch (Exception e) {
			LOG.error(e.getMessage(),e);
		}
		return (R) null;
	}

}
