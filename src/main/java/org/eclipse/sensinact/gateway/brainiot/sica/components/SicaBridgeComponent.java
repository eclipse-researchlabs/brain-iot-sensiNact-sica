package org.eclipse.sensinact.gateway.brainiot.sica.components;

import java.util.Collections;

import org.eclipse.sensinact.gateway.common.bundle.Mediator;
import org.eclipse.sensinact.gateway.core.SensiNactResourceModelConfiguration.BuildPolicy;
import org.eclipse.sensinact.gateway.generic.ExtModelConfiguration;
import org.eclipse.sensinact.gateway.generic.ExtModelConfigurationBuilder;
import org.eclipse.sensinact.gateway.generic.InvalidProtocolStackException;
import org.eclipse.sensinact.gateway.generic.local.LocalProtocolStackEndpoint;
import org.eclipse.sensinact.gateway.generic.packet.Packet;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = SicaBridgeComponent.class, immediate = true) 
public class SicaBridgeComponent {

	private static final Logger LOG = LoggerFactory.getLogger(SicaBridgeComponent.class);
	private LocalProtocolStackEndpoint<Packet> endpoint;
	
	@Reference
	private SicaProxyComponent sicaProxy;
	
	@Activate
	protected void activate(ComponentContext context) {
		Mediator mediator = new Mediator(context.getBundleContext());
		
		ExtModelConfiguration<Packet> configuration = ExtModelConfigurationBuilder.instance(mediator
	        ).withStartAtInitializationTime(true
	        ).withServiceBuildPolicy(BuildPolicy.BUILD_COMPLETE_ON_DESCRIPTION.getPolicy()
	        ).withResourceBuildPolicy(BuildPolicy.BUILD_COMPLETE_ON_DESCRIPTION.getPolicy()
	        ).build("resources.xml", Collections.<String, String>emptyMap());
		
		this.endpoint = new LocalProtocolStackEndpoint<Packet>(mediator);
		this.endpoint.addInjectableInstance(SicaProxyComponent.class, sicaProxy);
		
		try {
			endpoint.connect(configuration);
		} catch (InvalidProtocolStackException e) {
			LOG.error(e.getMessage(), e);
		}		
		LOG.info("sensiNact SICA bridge Component ACTIVATED");
	}
	
	@Deactivate
	protected void deactivate() {
		if (this.endpoint != null)
			this.endpoint.stop();
		LOG.info("sensiNact SICA bridge Component DEACTIVATED");
	}
}
