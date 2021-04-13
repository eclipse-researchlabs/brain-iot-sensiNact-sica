package org.eclipse.sensinact.gateway.brainiot.sica;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.sensinact.gateway.core.Attribute;
import org.eclipse.sensinact.gateway.core.DataResource;
import org.eclipse.sensinact.gateway.core.ResourceConfig;
import org.eclipse.sensinact.gateway.core.ResourceImpl;
import org.eclipse.sensinact.gateway.core.ServiceImpl;
import org.eclipse.sensinact.gateway.core.method.AccessMethod;
import org.eclipse.sensinact.gateway.generic.ExtModelInstance;
import org.eclipse.sensinact.gateway.generic.ExtResourceConfig;
import org.eclipse.sensinact.gateway.generic.ExtResourceImpl;
import org.eclipse.sensinact.gateway.generic.ExtServiceImpl;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended {@link ExtResourceImpl} dedicated to the specific 'ALL' SICA resource, in 
 * charge of propagating changes to the resources belonging to the same profile and 
 * with the same parent service
 */
public class SicaHistoryResource extends SicaResource {

	private static final Logger LOG = LoggerFactory.getLogger(SicaHistoryResource.class);
	
	/**
	 * Constructor
	 * 
	 * @param modelInstance the instance of the sensiNact service model to which belongs the SicaAllResource to
	 * be instantiated
	 * @param resourceConfig the {@link ResourceConfig} describing the configuration applying on the SicaAllResource 
	 * to be instantiated
	 * @param service the {@link ExtServiceImpl} parent of the SicaAllResource to be instantiated
	 */
	public SicaHistoryResource(ExtModelInstance<?> modelInstance, ExtResourceConfig resourceConfig, ExtServiceImpl service) {
		super(modelInstance, resourceConfig, service);
	}

	 /**
     * @throws Exception
     * @inheritDoc
     * @see ResourceImpl#
     * passOn(AccessMethod.Type, java.lang.Object[])
     */
    @Override
    protected JSONObject passOn(String type, String uri, Object[] parameters) throws Exception {
    	String attribute = (parameters==null||parameters.length==0)?DataResource.VALUE:String.valueOf(parameters[0]);
    	if("GET".equals(type) && DataResource.VALUE.equals(attribute)) {
    		
    		Object fromObj = super.getAttribute("from").getValue();
    		long from = fromObj==null?0:(long)fromObj;
    		
    		Object toObj = super.getAttribute("to").getValue();
    		long to = toObj==null?System.currentTimeMillis():(long)toObj;
    		
    		Object fieldsObj = super.getAttribute("fields").getValue();
    		String[] fields = null;
    		if(fieldsObj == null) {
    			ServiceImpl service = (ServiceImpl)super.parent;
    			List<ResourceImpl> resources = service.getResources();
    			List<String> fieldsLst = new ArrayList<>();
    			for(int j=0;j<resources.size();j++) {
    				Attribute attr = resources.get(j).getAttribute("index");
    				if(attr == null)
    					continue;
    				String name = (String)resources.get(j).getAttribute("sica").getValue();
    				if(name == null)
    					name = resources.get(j).getName();
    				fieldsLst.add((int)attr.getValue(), name);    				
    			}
    			fields = fieldsLst.toArray(new String[0]);
    		}
    		else 
    			fields = (String[])fieldsObj;
    		
    		return super.passOn(type, uri, new Object[] {DataResource.VALUE, from,to,fields});
    	}
    	return null;
    }
	
}
