package org.eclipse.sensinact.gateway.brainiot.sica;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;

import org.eclipse.sensinact.gateway.brainiot.sica.components.SicaProxyComponent;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.GetCommand;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.GetResultInfos;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.HistoryCommand;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.HistoryResultInfos;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.SetCommand;
import org.eclipse.sensinact.gateway.brainiot.sica.proxy.SetResultInfos;
import org.eclipse.sensinact.gateway.generic.Task.CommandType;
import org.eclipse.sensinact.gateway.generic.annotation.TaskCommand;
import org.eclipse.sensinact.gateway.generic.annotation.TaskExecution;
import org.eclipse.sensinact.gateway.generic.annotation.TaskInject;
import org.eclipse.sensinact.gateway.util.UriUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link TaskExecution} annotated POJO, in charge of translating sensiNact access method invocation into 
 * SicaProxy call
 */
@TaskExecution
public class SicaTaskEndpoint {
	
	@TaskInject
	private SicaProxyComponent proxy;
	
	private static final Logger LOG = LoggerFactory.getLogger(SicaTaskEndpoint.class);

	private static class SicaAttribute {
		String profile;
		int index;
	}
	
	private static SicaAttribute parseSicaId(String uri) {
		String[] pathElements = UriUtils.getUriElements(uri);
		String[] tokens = pathElements[0].split("#");
		int index = -1;
		String profile = tokens[0];
		if(tokens.length > 2)
			index = Integer.parseInt(tokens[1]);
		
		SicaAttribute sicaAttribute =  new SicaAttribute();
		sicaAttribute.profile = profile;
		sicaAttribute.index = index;					
		return sicaAttribute;		
	}
	
	private static String parseField(String uri) {
		String[] pathElements = UriUtils.getUriElements(uri);
		return pathElements[2];
	}
	
	/**
	 * Constructor
	 */
	public SicaTaskEndpoint() {}
	
	/**
	 * Propagates a SET access method invocation to the SicaProxy
	 * 
	 * @param uri the String path of the resource whose SET access method invocation is propagated in here 
	 * @param attributeName the String name of the attribute targeted by the SET access method
	 * @param value the Object value to be set
	 * 
	 * @return the propagated and translated SET access method invocation result
	 */
	@TaskCommand(target="/*/values/all", synchronization=TaskCommand.SynchronizationPolicy.SYNCHRONOUS, method=CommandType.SET)
	public Object setAll(String uri, String attributeName, Object value) {
		
		SicaAttribute sicaAttribute = parseSicaId(uri);
		String field = parseField(uri);
		double[] values = null;
		if(value!=null 
			&& value.getClass().isArray() 
			&& value.getClass().getComponentType() == double.class) {
			values = (double[]) value;
		}
		if(values == null)
			return null;
		long date = ZonedDateTime.now().toEpochSecond() * 1000;
		SetResultInfos result = proxy.set(new SetCommand(sicaAttribute.profile, field, sicaAttribute.index, date, values));
		if (result.result.isSuccess) {
			LOG.info("SET: success for " + result.command);
		} else {
			String msg = "SET: failure for " + result.command;
			if (result.result.message != null && result.result.message.length() != 0)
				msg += " message = " + result.result.message; 
			LOG.error(msg);
		}
		return value;
	}
	
	/**
	 * Propagates a GET access method invocation to the SicaProxy
	 * 
	 * @param uri the String path of the resource whose GET access method invocation is propagated in here 
	 * @param attributeName the String name of the attribute targeted by the GET access method
	 * 
	 * @return the propagated and translated GET access method invocation result
	 */
	@TaskCommand(target="/*/values/*", synchronization=TaskCommand.SynchronizationPolicy.SYNCHRONOUS, method=CommandType.GET)
	public Object get(String uri, String attributeName) {		

		SicaAttribute sicaAttribute = parseSicaId(uri);
		String field = parseField(uri);
		GetCommand command = new GetCommand(sicaAttribute.profile, field,sicaAttribute.index);

		GetResultInfos result = this.proxy.get(command);
		if (result.result.isSuccess)
			return parseResult(result.result.value);
		 
		LOG.error(result.result.error);
		if (result.result.message != null && result.result.message.length() != 0)
			LOG.error(result.result.message);
		throw new RuntimeException(result.result.error);
	}

	/**
	 * Propagates a GET access method invocation on history resource to the SicaProxy
	 * 
	 * @param uri the String path of the history resource whose GET access method invocation is propagated in here 
	 * @param attributeName the String name of the attribute targeted by the GET access method
	 * @param from long unix epoch date defining the history start datetime
	 * @param to long unix epoch date defining the history stop datetime
	 * 
	 * @return the propagated and translated history GET access method invocation result
	 */
	@TaskCommand(target="/*/values/history", synchronization=TaskCommand.SynchronizationPolicy.SYNCHRONOUS, method=CommandType.GET)
	public Object getHistory(String uri, String attributeName, long from, long to, String[] fields) {		

		SicaAttribute sicaAttribute = parseSicaId(uri);

		HistoryCommand command = new HistoryCommand(sicaAttribute.profile, Instant.ofEpochMilli(from), Instant.ofEpochMilli(to), fields);
		HistoryResultInfos infos = this.proxy.getHistory(command);
		
		if (infos.result.isSuccess)
			return Arrays.toString(infos.result.measures);
		
		if (infos.result.message != null && infos.result.message.length() != 0)
			LOG.error(infos.result.message);
		
		throw new RuntimeException(infos.result.message);	
	}

	private Object parseResult(String value) {
		// Is the object a double ?
		try {
			return Double.valueOf(value);
		} catch (Exception e) {
			// do nothing
		}
		
		// Is the object embedded inside a JSON array ?
		try {
			JSONObject json = new JSONObject(value);
			JSONArray values = json.getJSONArray("Values");
			double[] tab = new double[values.length()];
			for(int i=0;i<values.length();i++) {
				try {
					tab[i] = values.getDouble(i);
				} catch (Exception e) {
					tab[i] = 0;
				}
			}			
			return tab;
		} catch (Exception e) {
			// do nothing
		}
		
		// Oups...
		String message = "SET : can't parse value " + value;
		LOG.error(message);
		throw new RuntimeException(message);
	}
}
