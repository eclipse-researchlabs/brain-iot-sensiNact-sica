package org.eclipse.sensinact.gateway.brainiot.sica;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.sensinact.gateway.core.Attribute;
import org.eclipse.sensinact.gateway.core.DataResource;
import org.eclipse.sensinact.gateway.core.ResourceConfig;
import org.eclipse.sensinact.gateway.core.ResourceImpl;
import org.eclipse.sensinact.gateway.core.ResourceProcessableContainer;
import org.eclipse.sensinact.gateway.core.ResourceProcessableData;
import org.eclipse.sensinact.gateway.core.ServiceImpl;
import org.eclipse.sensinact.gateway.generic.ExtModelInstance;
import org.eclipse.sensinact.gateway.generic.ExtResourceConfig;
import org.eclipse.sensinact.gateway.generic.ExtResourceImpl;
import org.eclipse.sensinact.gateway.generic.ExtServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extended {@link ExtResourceImpl} dedicated to the specific 'ALL' SICA resource, in 
 * charge of propagating changes to the resources belonging to the same profile and 
 * with the same parent service
 */
public class SicaAllResource extends SicaResource {

	private static final Logger LOG = LoggerFactory.getLogger(SicaAllResource.class);
	
	/**
	 * Constructor
	 * 
	 * @param modelInstance the instance of the sensiNact service model to which belongs the SicaAllResource to
	 * be instantiated
	 * @param resourceConfig the {@link ResourceConfig} describing the configuration applying on the SicaAllResource 
	 * to be instantiated
	 * @param service the {@link ExtServiceImpl} parent of the SicaAllResource to be instantiated
	 */
	public SicaAllResource(ExtModelInstance<?> modelInstance, ExtResourceConfig resourceConfig, ExtServiceImpl service) {
		super(modelInstance, resourceConfig, service);
	}

	@Override
	protected void updated(Attribute attribute, Object value, boolean hasChanged) {		
		super.updated(attribute, value, hasChanged);
		double[] values = null;
		try {
			if (value.getClass().isArray() && value.getClass().getComponentType() == double.class)
				values = (double[]) value;
		} catch(ClassCastException e) {
			super.modelInstance.mediator().error(e);
		}			
		if (values==null) {
			super.modelInstance.mediator().warn("Null array value");
			return;
		}		
		ServiceImpl service = (ServiceImpl)super.parent;
		List<ResourceImpl> resources = service.getResources();

		if (resources.size() != values.length) {
			super.modelInstance.mediator().error("Invalid array values length [%s][%s]",resources.size(),values.length);
			return;
		}		
		final String serviceName = service.getName();
		
		for(int i=0; i < values.length; i++) {
			int j=0;
			for(;j<resources.size();j++) {
				Attribute attr = resources.get(j).getAttribute("index");
				if(attr == null)
					continue;
				if(i==(int)attr.getValue())
					break;
			}
			if(j==resources.size())
				continue;
			
			final double val = values[i];
			ResourceImpl r = resources.get(j);
			String resourceName = resources.get(j).getName();
			try {
				r.process(new ResourceProcessableContainer<ResourceProcessableData>() {

					@Override
					public String getName() {
						return serviceName;
					}

					@Override
					public Iterator<ResourceProcessableData> iterator() {
						return Arrays.<ResourceProcessableData>asList(new ResourceProcessableData() {

							@Override
							public String getName() {
								return resourceName;
							}

							@Override
							public String getAttributeId() {
								return DataResource.VALUE;
							}

							@Override
							public Object getData() {
								return val;
							}

							@Override
							public String getMetadataId() {
								return null;
							}

							@Override
							public long getTimestamp() {
								return 0;
							}
					
						}).iterator();
					}

					@Override
					public String getResourceId() {
						return resourceName;
					}});
			} catch (NullPointerException e) {
				LOG.error(e.getMessage(), e);
				continue;
			}
		}
	}
}
