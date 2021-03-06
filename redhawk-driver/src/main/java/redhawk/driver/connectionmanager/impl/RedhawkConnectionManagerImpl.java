/*
 * This file is protected by Copyright. Please refer to the COPYRIGHT file
 * distributed with this source distribution.
 *
 * This file is part of REDHAWK __REDHAWK_PROJECT__.
 *
 * REDHAWK __REDHAWK_PROJECT__ is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * REDHAWK __REDHAWK_PROJECT__ is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package redhawk.driver.connectionmanager.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import CF.ConnectionManager;
import CF.ConnectionManagerHelper;
import CF.ConnectionStatusIteratorHolder;
import CF.ConnectionManagerPackage.ConnectionStatusSequenceHolder;
import CF.ConnectionManagerPackage.ConnectionStatusType;
import CF.ConnectionManagerPackage.EndpointKind;
import CF.ConnectionManagerPackage.EndpointRequest;
import CF.ConnectionManagerPackage.EndpointResolutionType;
import CF.ConnectionManagerPackage.EndpointStatusType;
import CF.PortPackage.InvalidPort;
import redhawk.driver.base.impl.CorbaBackedObject;
import redhawk.driver.connectionmanager.RedhawkConnectionManager;
import redhawk.driver.domain.RedhawkDomainManager;
import redhawk.driver.exceptions.ConnectionException;
import redhawk.driver.exceptions.EventChannelException;
import redhawk.driver.exceptions.ResourceNotFoundException;
import redhawk.driver.port.RedhawkPort;
import redhawk.driver.port.impl.RedhawkExternalPortImpl;
import redhawk.driver.port.impl.RedhawkPortImpl;
import redhawk.driver.xml.model.sca.scd.Provides;
import redhawk.driver.xml.model.sca.scd.Uses;

public class RedhawkConnectionManagerImpl extends CorbaBackedObject<ConnectionManager> implements RedhawkConnectionManager {

	private ConnectionManager connectionManager;
	
	private RedhawkDomainManager domainManager;
	
	public RedhawkConnectionManagerImpl(RedhawkDomainManager mgr, ConnectionManager connectionManager) {
		super(mgr.getDriver().getOrb().object_to_string(connectionManager), mgr.getDriver().getOrb());
		this.connectionManager = connectionManager;
		this.domainManager = mgr;
	}

	@Override
	public void connect(RedhawkEndpoint usesPort, RedhawkEndpoint providesPort, String requestId, String connectionId) throws ConnectionException{
		//TODO: Move this to a method
		try {
			connectionManager.connect(this.getEndpointRequestFromRedhawkEndpoint(usesPort), 
					this.getEndpointRequestFromRedhawkEndpoint(providesPort), requestId, connectionId);
		} catch (InvalidPort | IOException | EventChannelException e) {
			e.printStackTrace();
			throw new ConnectionException("Issue connecting these two endpoints [provides: "+usesPort+" uses: "+providesPort+"] "+e.getMessage(), e);
		}
	}

	@Override
	public void disconnect(String connectionRecordId) {
		try {
			connectionManager.disconnect(connectionRecordId);
		} catch (InvalidPort e) {
			throw new ConnectionException("Unable do disconnect using this id: "+connectionRecordId);
		}
	}

	@Override
	public List<ConnectionInfo> getConnections() {
		List<ConnectionInfo> connections = new ArrayList<>();
		
		for(ConnectionStatusType connection : connectionManager.connections()){
			ConnectionInfo info = new ConnectionInfo(); 
			info.setConnected(connection.connected);
			info.setConnectionId(connection.connectionId);
			info.setConnectionRecordId(connection.connectionRecordId);
			info.setConnected(connection.connected);
			info.setRequestorId(connection.requesterId);
			EndpointStatusType status = connection.providesEndpoint;
			info.setProvidesEndpointStatus(new RedhawkEndpointStatus(status.portName, status.repositoryId, status.entityId, status.endpointObject));
			
			status = connection.usesEndpoint;
			info.setUsesEndpointStatus(new RedhawkEndpointStatus(status.portName, status.repositoryId, status.entityId, status.endpointObject));

			connections.add(info);
		}
		
		return connections;
	}

	@Override
	protected ConnectionManager locateCorbaObject() throws ResourceNotFoundException {
		return ConnectionManagerHelper.narrow(getOrb().string_to_object(this.getIor()));
	}

	@Override
	public Class<?> getHelperClass() {
		// TODO Auto-generated method stub
		return ConnectionManagerHelper.class;
	}

	@Override
	public ConnectionManager getCorbaObj() {
		// TODO Auto-generated method stub
		return this.getCorbaObject();
	}
	
	private EndpointRequest getEndpointRequestFromRedhawkEndpoint(RedhawkEndpoint point) throws IOException, EventChannelException{
		EndpointResolutionType request = new EndpointResolutionType();
		switch(point.getType()){
		case Application:
			request.applicationId(point.getResourceId());
			
			RedhawkPortEndpoint portEndpoint = (RedhawkPortEndpoint) point;
			//Can't give interface too much information either provide object or provide <id and name>
			//request.objectRef(point.getPort().getCorbaObject());
			if(portEndpoint.getPort() instanceof RedhawkExternalPortImpl){
				return new EndpointRequest(request, ((RedhawkExternalPortImpl)portEndpoint.getPort()).getName());
			}
			throw new IOException("ConnectionManager is expecting an External port for your application ");
		case Device:
			request.deviceId(point.getResourceId());
			
			RedhawkPortEndpoint devicePortEndpoint = (RedhawkPortEndpoint) point;
			//Can't give interface too much information either provide object or provide <id and name>
			//request.objectRef(point.getPort().getCorbaObject());
			return new EndpointRequest(request, devicePortEndpoint.getPort().getName());
		case EventChannel:
			RedhawkEventChannelEndpoint ecEndPoint = (RedhawkEventChannelEndpoint) point;
			request.channelName(ecEndPoint.getResourceId());
			request.objectRef(ecEndPoint.getChannel().getCorbaObj());
			return new EndpointRequest(request, "");
		default:
			throw new IOException("Unhandled Endpoint Type kind");
		}
	}
}
