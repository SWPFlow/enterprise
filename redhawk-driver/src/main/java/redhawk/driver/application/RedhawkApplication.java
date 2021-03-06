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
package redhawk.driver.application;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import CF.Application;
import redhawk.driver.base.PortBackedObject;
import redhawk.driver.base.QueryableResource;
import redhawk.driver.component.RedhawkComponent;
import redhawk.driver.device.RedhawkDevice;
import redhawk.driver.domain.RedhawkDomainManager;
import redhawk.driver.exceptions.ApplicationException;
import redhawk.driver.exceptions.ApplicationReleaseException;
import redhawk.driver.exceptions.ApplicationStartException;
import redhawk.driver.exceptions.ApplicationStopException;
import redhawk.driver.exceptions.MultipleResourceException;
import redhawk.driver.exceptions.ResourceNotFoundException;
import redhawk.driver.port.RedhawkPort;
import redhawk.driver.properties.RedhawkProperty;
import redhawk.driver.xml.model.sca.sad.Softwareassembly;

/**
 * POJO representing a Redhawk Application
 */
public interface RedhawkApplication extends PortBackedObject {
    /**
     * @return A {@link java.util.List} with {@link RedhawkComponent} objects. 
     */
	List<RedhawkComponent> getComponents();
	
	/**
	 * Get a list of components by a Regex
	 * @param name
	 * 	Regex for a component name. 
	 * @return
	 * 	A {@link java.util.List} with {@link RedhawkComponent} objects.
	 */
    List<RedhawkComponent> getComponentsByName(String name);
    
    /**
     * Get a specific {@link RedhawkComponent} by name. 
     * @param name
     * 	Name of the RedhawkComponent. 
     * @return
     * @throws MultipleResourceException
     * @throws ResourceNotFoundException
     */
    RedhawkComponent getComponentByName(String name) throws MultipleResourceException, ResourceNotFoundException;
    
    /** 
     * @return Name of the Redhawk Application
     */
    String getName();
    
    /**
     * 
     * @return Identifier for the Redhawk Application
     */
    String getIdentifier();
    
    /**
     * @return The Redhawk Domain Manager that this application is hosted by. 
     */
    RedhawkDomainManager getRedhawkDomainManager();
    
    /** 
     * @return The CORBA object representing this RedhawkApplication
     */
    Application getCorbaObj();
    
    /**
     * @return {@link Softwareassembly} for this object. 
     * @throws IOException
     */
	Softwareassembly getAssembly() throws IOException;
	
	/**
	 * Use this method to start your Redhawk Application. 
	 * 
	 * @throws ApplicationStartException
	 */
    void start() throws ApplicationStartException;
    
    /**
     * Use this method to stop your Redhawk Application. 
     * @throws ApplicationStopException
     */
    void stop() throws ApplicationStopException;
    
    /**
     * Use this method to release your Redhawk Application
     * @throws ApplicationReleaseException
     */
    void release() throws ApplicationReleaseException;
    
    /**
     * Utility method to figure out if an application is started or not. 
     * @return
     */
    boolean isStarted();
    
    /**
     * Helper method for getting a map of External Properties from an application 
     * 
     */
    Map<String, RedhawkProperty> getExternalProperties();
    
    
    /** 
     * This boolean attribute contains the aware state of Application. 
     * This attribute shows whether the Components in the Application are given a pointer to the Application and Domain Manager. 
     **/
    boolean isAware();
    
    /**
     * Return a map of Components in your application to the Device they're launched on. 
     * @return
     */
    Map<String, RedhawkDevice> getComponentDevices(); 
    
    /**
     * Return a map of Component->ProcessId
     * @return
     */
    Map<String, Integer> getComponentProcessIds();
    
    /**
     * Map of each Component -> Implementation type(i.e. cpp, java, python)
     * @return
     */
    Map<String, String> getComponentImplementations();
    
    /**
     * Returns a list containing the metrics for an application
     * @return
     * @throws ApplicationException 
     */
    Map<String, Map<String, Object>> getMetrics() throws ApplicationException;
    
    /**
     * Returns a list containing the metrics for an application
     * 
     * @param components
     * 	Component Names/Component Ids to get metrics for
     * @param attributes
     * 	Attributes to include in response. Possible attributes:
     * 	- valid
     * 	- shared(component only)
     * 	- processes
     * 	- cores
     * 	- memory
     * 	- threads
     * 	- files
     * 	- componenthosts(component only)
     * @return
     * @throws ApplicationException 
     */
    Map<String, Map<String, Object>> getMetrics(String[] components, String[] attributes) throws ApplicationException;
}