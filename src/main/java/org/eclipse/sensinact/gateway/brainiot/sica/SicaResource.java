package org.eclipse.sensinact.gateway.brainiot.sica;

import org.eclipse.sensinact.gateway.core.Attribute;
import org.eclipse.sensinact.gateway.core.DataResource;
import org.eclipse.sensinact.gateway.core.ResourceConfig;
import org.eclipse.sensinact.gateway.generic.ExtModelInstance;
import org.eclipse.sensinact.gateway.generic.ExtResourceConfig;
import org.eclipse.sensinact.gateway.generic.ExtResourceImpl;
import org.eclipse.sensinact.gateway.generic.ExtServiceImpl;
import org.eclipse.sensinact.gateway.util.UriUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended {@link ExtResourceImpl} dedicated to SICA resource
 */
public class SicaResource extends ExtResourceImpl {

	private static final Logger LOG = LoggerFactory.getLogger(SicaResource.class);
	
	/**
	 * Constructor
	 * 
	 * @param modelInstance the instance of the sensiNact service model to which belongs the SicaAllResource to
	 * be instantiated
	 * @param resourceConfig the {@link ResourceConfig} describing the configuration applying on the SicaAllResource 
	 * to be instantiated
	 * @param service the {@link ExtServiceImpl} parent of the SicaAllResource to be instantiated
	 */
	public SicaResource(ExtModelInstance<?> modelInstance, ExtResourceConfig resourceConfig, ExtServiceImpl service) {
		super(modelInstance, resourceConfig, service);
	}
	
	@Override
	protected JSONObject passOn(String type, String uri, Object[] parameters) throws Exception {
    	String attribute = (parameters==null||parameters.length==0)?DataResource.VALUE:String.valueOf(parameters[0]);
		if(!DataResource.VALUE.equals(attribute))
			return null;
		String index = "";
		String profile = "";
		Attribute attr = super.getAttribute("index");
		if(attr != null) 
			index = String.valueOf(attr.getValue()).concat("#");
		profile = super.getModelInstance().getProfile().concat("#");
		String[] els = UriUtils.getUriElements(uri);
		els[0] = String.format("%s%s%s",profile,index,els[0]);
		String newUri = UriUtils.getUri(els);
		System.out.println("URI : " + newUri);
		return super.passOn(type, newUri, parameters);
	}
}
