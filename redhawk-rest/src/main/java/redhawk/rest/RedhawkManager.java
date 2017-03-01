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
package redhawk.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import redhawk.driver.Redhawk;
import redhawk.driver.RedhawkDriver;
import redhawk.driver.application.RedhawkApplication;
import redhawk.driver.base.QueryableResource;
import redhawk.driver.component.RedhawkComponent;
import redhawk.driver.device.RedhawkDevice;
import redhawk.driver.devicemanager.RedhawkDeviceManager;
import redhawk.driver.domain.RedhawkDomainManager;
import redhawk.driver.exceptions.*;
import redhawk.driver.port.RedhawkPort;
import redhawk.driver.port.RedhawkPortStatistics;
import redhawk.driver.properties.*;
import redhawk.driver.xml.model.sca.prf.Properties;
import redhawk.driver.xml.model.sca.sad.Softwareassembly;
import redhawk.rest.model.*;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class RedhawkManager {

    private static Log logger = LogFactory.getLog(RedhawkManager.class);

    private List<ServiceReference<Redhawk>> redhawkDriverServices;
    private Map<String, Redhawk> redhawkDrivers = new ConcurrentHashMap<String, Redhawk>();

    private DomainConverter converter = new DomainConverter();
    private BundleContext context;

    private Redhawk getDriverInstance(String nameServer) throws ResourceNotFoundException {
        Redhawk redhawk = null;
        if (redhawkDrivers.get(nameServer) != null) {
            return redhawkDrivers.get(nameServer);
        } else {
            if (nameServer.contains(":")) {
                String[] hostAndPort = nameServer.split(":");
                redhawk = new RedhawkDriver(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
                redhawkDrivers.put(nameServer, redhawk);
                return redhawk;
            } else {
                throw new ResourceNotFoundException("You did not specify a valid host and port to the REDHAWK name server. An example of a valid url is: localhost:2809");
            }
        }
    }


    public <T> T get(String nameServer, String type, String location) throws ResourceNotFoundException, Exception {
        Redhawk redhawk = getDriverInstance(nameServer);
        return (T) converter.convert(type, internalGet(redhawk, type, location));
    }

    public <T> PortStatisticsContainer getRhPortStatistics(String nameServer, String type, String location) throws Exception {
        Redhawk redhawk = getDriverInstance(nameServer);
        T port = internalGet(redhawk, type, location);
        List<RedhawkPortStatistics> stats = new ArrayList<>();

        if (port instanceof RedhawkPort) {
            RedhawkPort rhPort = (RedhawkPort) port;
            stats.addAll(rhPort.getPortStatistics());
        }
        return new PortStatisticsContainer(stats);
    }

    public void createApplication(String nameServer, String domainName, String instanceName, WaveformInfo info) throws ResourceNotFoundException, ApplicationCreationException {
        Redhawk redhawk = null;
        boolean createdNewInstance = false;

        try {
            if (redhawkDrivers.get(nameServer) != null) {
                redhawk = redhawkDrivers.get(nameServer);
            } else {
                if (nameServer.contains(":")) {
                    String[] hostAndPort = nameServer.split(":");
                    redhawk = new RedhawkDriver(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
                    createdNewInstance = true;
                } else {
                    throw new ResourceNotFoundException("You did not specify a valid host and port to the REDHAWK name server. An example of a valid url is: localhost:2809");
                }
            }

            logger.info("Trying to create this application: "+instanceName+" sadLocation: "+info.getSadLocation());
            RedhawkApplication application = redhawk.getDomain(domainName).createApplication(instanceName, info.getSadLocation());
            try {
                application.start();
            } catch (ApplicationStartException e) {
                throw new ApplicationCreationException("Unable to start the application", e);
            }

        } catch (ConnectionException e1) {
            e1.printStackTrace();
        } catch (CORBAException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } finally {
            if (redhawk != null && createdNewInstance) {
                redhawk.disconnect();
            }
        }
    }
    
    //TODO: Release/Create/Contol have a lot of similar code make this simpler 
    public void controlApplication(String nameServer, String domainName, String appId, String control) throws Exception {
        Redhawk redhawk = null;
        boolean createdNewInstance = false;

        try {
            if (redhawkDrivers.get(nameServer) != null) {
                redhawk = redhawkDrivers.get(nameServer);
            } else {
                if (nameServer.contains(":")) {
                    String[] hostAndPort = nameServer.split(":");
                    redhawk = new RedhawkDriver(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
                    createdNewInstance = true;
                } else {
                    throw new ResourceNotFoundException("You did not specify a valid host and port to the REDHAWK name server. An example of a valid url is: localhost:2809");
                }
            }

            RedhawkApplication application;
            try{
            	application = redhawk.getDomain(domainName).getApplicationByIdentifier(appId);
            }catch(ResourceNotFoundException ex){
            	logger.debug("Unable to application by Identifier: "+appId+" trying by Name");
            	application = redhawk.getDomain(domainName).getApplicationByName(appId);
            }
         
            if (application == null) {
                throw new ResourceNotFoundException("Could not find application with an Identifier of: " + appId);
            }

            if(control.equalsIgnoreCase("stop")){
            	application.stop();
            }else if(control.equalsIgnoreCase("start")){
            	application.start();
            }else{
            	throw new Exception("Unknown control string "+control+" appropriate commands are 'start' or 'stop'");
            }
        } finally {
            if (redhawk != null && createdNewInstance) {
                redhawk.disconnect();
            }
        }
    }

    public void releaseApplication(String nameServer, String domainName, String appId) throws Exception {
        Redhawk redhawk = null;
        boolean createdNewInstance = false;

        try {
            if (redhawkDrivers.get(nameServer) != null) {
                redhawk = redhawkDrivers.get(nameServer);
            } else {
                if (nameServer.contains(":")) {
                    String[] hostAndPort = nameServer.split(":");
                    redhawk = new RedhawkDriver(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
                    createdNewInstance = true;
                } else {
                    throw new ResourceNotFoundException("You did not specify a valid host and port to the REDHAWK name server. An example of a valid url is: localhost:2809");
                }
            }

            RedhawkApplication application;
            try{
            	application = redhawk.getDomain(domainName).getApplicationByIdentifier(appId);
            }catch(ResourceNotFoundException ex){
            	logger.debug("Unable to application by Identifier: "+appId+" trying by Name");
            	application = redhawk.getDomain(domainName).getApplicationByName(appId);
            }
         
            if (application == null) {
                throw new ResourceNotFoundException("Could not find application with an Identifier of: " + appId);
            }

            application.release();

        } finally {
            if (redhawk != null && createdNewInstance) {
                redhawk.disconnect();
            }
        }
    }


    public Map<String, Softwareassembly> getWaveforms(String nameServer, String domainName) throws Exception {
        Redhawk redhawk = null;
        boolean createdNewInstance = false;

        try {
            if (redhawkDrivers.get(nameServer) != null) {
                redhawk = redhawkDrivers.get(nameServer);
            } else {
                if (nameServer.contains(":")) {
                    String[] hostAndPort = nameServer.split(":");
                    redhawk = new RedhawkDriver(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
                    createdNewInstance = true;
                } else {
                    throw new ResourceNotFoundException("You did not specify a valid host and port to the REDHAWK name server. An example of a valid url is: localhost:2809");
                }
            }

            return redhawk.getDomain(domainName).getFileManager().getWaveforms();

        } finally {
            if (redhawk != null && createdNewInstance) {
                redhawk.disconnect();
            }
        }
    }

    /**
     * Returns the specified type object from the nameserver at the specified
     * location
     *
     * @param nameServer Location of the omniName Server
     * @param type       Type of object to bring back (domain, application, component, port, devicemanager, device, waveform)
     * @param location   Name of the DOMAIN you're connecting too(i.e REDHAWK_DEV)
     * @param fetchMode  Whether or not to return everything in the tree.  Defaults to FetchMode.EAGER
     * @return Returns the model for the object type specified (i.e. redhawk.rest.model.Domain)
     * @throws ResourceNotFoundException
     * @throws Exception
     */
    public <T> List<T> getAll(String nameServer, String type, String location, FetchMode fetchMode) throws ResourceNotFoundException, Exception {
        Redhawk redhawk = null;
        boolean createdNewInstance = false;

        try {
            if (redhawkDrivers.get(nameServer) != null) {
                redhawk = redhawkDrivers.get(nameServer);
            } else {
                if (nameServer.contains(":")) {
                    String[] hostAndPort = nameServer.split(":");
                    redhawk = new RedhawkDriver(hostAndPort[0], Integer.parseInt(hostAndPort[1]));
                    createdNewInstance = true;
                } else {
                    throw new ResourceNotFoundException("You did not specify a valid host and port to the REDHAWK name server. An example of a valid url is: localhost:2809");
                }
            }

            return (List<T>) converter.convertAll(type, internalGetAll(redhawk, type, location), fetchMode);

        } finally {
            if (redhawk != null && createdNewInstance) {
                redhawk.disconnect();
            }
        }
    }

    /**
     * Returns a PropertyContainer for the specified type at a specific location
     *
     * @param nameServer
     * @param type
     * @param location
     * @return
     * @throws Exception
     */
    public PropertyContainer getProperties(String nameServer, String type, String... location) throws Exception {
        Redhawk redhawk = getDriverInstance(nameServer);

//	  QueryableResource resource = internalGet(redhawk, type, location);
//	  Object object = converter.convert(type, resource); 

        Map<String, RedhawkProperty> properties = null;
        Properties propConfig = null;

        switch (type) {
            case "domain": {
                RedhawkDomainManager domain = internalGet(redhawk, type, location);
                properties = domain.getProperties();
                propConfig = domain.getPropertyConfiguration();
                break;
            }
            case "application": {
                RedhawkApplication application = internalGet(redhawk, type, location);
                properties = application.getProperties();
                break;
            }
            case "devicemanager": {
                RedhawkDeviceManager devMgr = internalGet(redhawk, type, location);
                properties = devMgr.getProperties();
                break;
            }
            case "component": {
                RedhawkComponent comp = internalGet(redhawk, type, location);
                properties = comp.getProperties();
                propConfig = comp.getPropertyConfiguration();
                break;
            }
            case "device": {
                RedhawkDevice device = internalGet(redhawk, type, location);
                properties = device.getProperties();
                propConfig = device.getPropertyConfiguration();
                break;
            }
        }

        List<Property> propertyList = converter.convertProperties(properties, propConfig);
        return new PropertyContainer(propertyList);
    }

    public Property getProperty(String propertyId, String nameServer, String type, String... location) throws Exception {
        List<Property> properties = getProperties(nameServer, type, location).getProperties();
        List<Property> filteredProps = properties.parallelStream().filter(p -> {
            return p.getId().equalsIgnoreCase(propertyId);
        }).collect(Collectors.toList());

        if (filteredProps.size() > 0) {
            return filteredProps.get(0);
        } else {
            return null;
        }
    }

    public void setProperty(FullProperty prop, String nameServer, String type, String... location) throws Exception {
        Redhawk redhawk = getDriverInstance(nameServer);
        QueryableResource resource = internalGet(redhawk, type, location);
        internalSetProperty(resource, prop);
    }

    public void setProperties(List<FullProperty> properties, String nameServer, String type, String... location) throws Exception {
        Redhawk redhawk = getDriverInstance(nameServer);
        QueryableResource resource = internalGet(redhawk, type, location);
        properties.stream().forEach(property -> {
            try {
                internalSetProperty(resource, property);
            } catch (Exception e) {
                logger.error("Problem setting property: " + property.getId(), e);
            }
        });
    }


    private <T> T internalGet(Redhawk redhawk, String type, String... location) throws ResourceNotFoundException, Exception {
        switch (type) {
            case "domain":
                return (T) redhawk.getDomain(location[0]);
            case "application":
                return (T) redhawk.getApplication(location[0]);
            case "devicemanager":
                return (T) redhawk.getDeviceManager(location[0]);
            case "component":
                return (T) redhawk.getComponent(Arrays.stream(location).collect(Collectors.joining("/")));
            case "port":
                return (T) redhawk.getPort(Arrays.stream(location).collect(Collectors.joining("/")));
            case "device":
                return (T) redhawk.getDevice(Arrays.stream(location).collect(Collectors.joining("/")));
            case "softwarecomponent":
                return (T) redhawk.getComponent(Arrays.stream(location).collect(Collectors.joining("/"))).getSoftwareComponent();
        }

        throw new ResourceNotFoundException("Could not locate object of type: " + type + " at: " + location);
    }

    private <T> List<T> internalGetAll(Redhawk redhawk, String type, String... location) throws ResourceNotFoundException, Exception {
      /*
	   * Convert Location to array list and run parallel stream on it 
	   */
        switch (type) {
            case "domain":
                return (List<T>) redhawk.getDomains().values().stream().collect(Collectors.toList());
            case "application":
                return (List<T>) redhawk.getDomain(location[0]).getApplications();
            case "devicemanager":
                return (List<T>) redhawk.getDomain(location[0]).getDeviceManagers();
            case "component":
                return (List<T>) redhawk.getApplication(Arrays.stream(location).collect(Collectors.joining("/"))).getComponents();
            case "port":
                return (List<T>) redhawk.getComponent(Arrays.stream(location).collect(Collectors.joining("/"))).getPorts();
            case "device":
                return (List<T>) redhawk.getDeviceManager(Arrays.stream(location).collect(Collectors.joining("/"))).getDevices();
            //case "softwarecomponent": return (List<T>) redhawk.getComponent(Arrays.stream(location).collect(Collectors.joining("/"))).getSoftwareComponent();
        }

        throw new ResourceNotFoundException("Could not locate object of type: " + type + " at: " + location);
    }


    private Object convertSimple(String type, String value) throws Exception {

        if (type.equals("java.lang.Integer")) {
            return Integer.parseInt(value);
        } else if (type.equals("java.math.BigInteger")) {
            return new BigInteger(value);
        } else if (type.equals("java.lang.Byte")) {
            return Byte.parseByte(value);
        } else if (type.equals("java.lang.Double")) {
            return Double.parseDouble(value);
        } else if (type.equals("java.lang.Float")) {
            return Float.parseFloat(value);
        } else if (type.equals("java.lang.String")) {
            return value;
        } else if (type.equals("java.lang.Long")) {
            return Long.parseLong(value);
        } else if (type.equals("java.lang.Character")) {
            return value.toCharArray()[0];
        } else if (type.equals("java.lang.Short")) {
            return Short.parseShort(value);
        } else if (type.equals("java.lang.Boolean")) {
            return Boolean.parseBoolean(value);
        }

        throw new IllegalArgumentException("Could not determine simple type for: " + type + " with value: " + value);
    }


    private void internalSetProperty(QueryableResource resource, FullProperty prop) throws Exception {

        switch (prop.getType()) {
            case "simple": {
                RedhawkSimple simple = resource.getProperty(prop.getId());

                if (simple != null) {
                    simple.setValue(convertSimple(prop.getDataType(), prop.getValue()));
                }
                break;
            }
            case "simplesequence": {
                RedhawkSimpleSequence simpSeq = resource.getProperty(prop.getId());
                simpSeq.clearAllValues();
                simpSeq.addValues(prop.getValues().toArray());
                break;
            }
            case "struct": {
                RedhawkStruct struct = resource.getProperty(prop.getId());
                Map<String, Object> values = new HashMap<String, Object>();
                prop.getAttributes().forEach(p -> {
                    if (p.getType().equals("simple")) {
                        try {
                            values.put(p.getId(), convertSimple(p.getDataType(), p.getValue()));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (p.getType().equals("simplesequence")) {
                        values.put(p.getId(), prop.getValues().toArray());
                    }
                });
                struct.setValues(values);
                break;
            }
            case "structsequence": {
                RedhawkStructSequence structSequence = resource.getProperty(prop.getId());
                structSequence.removeAllStructs();

                List<Map<String, Object>> elements = prop.getStructs().stream().map(struct -> {
                    Map<String, Object> values = new HashMap<String, Object>();
                    struct.getAttributes().stream().forEach(v -> {
                        try {
                            if (v.getType().equals("simple")) {
                                values.put(v.getId(), convertSimple(v.getDataType(), v.getValue()));
                            } else if (v.getType().equals("simplesequence")) {
                                values.put(v.getId(), prop.getValues().toArray());
                            }
                        } catch (Exception e) {
                            logger.error("EXCEPTION IN INTERNAL SET PROPERTY: ", e);
                        }
                    });
                    return values;
                }).collect(Collectors.toList());
                structSequence.addStructsToSequence(elements);
                break;
            }
        }
    }

    public BundleContext getContext() {
        return context;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public void bindRedhawk(ServiceReference<Redhawk> reference) {
        String connectionName = (String) reference.getProperty("connectionName");
        logger.debug("BINDING REDHAWK SERVICE: " + connectionName);
        if (connectionName != null) {
        	try{
        		Redhawk rh = context.getService(reference);
            	redhawkDrivers.put(connectionName, rh);
        	}catch(Exception ex){
        		logger.error("Error trying to get reference from the context ", ex);
        	}
        }
    }

    public void unbindRedhawk(ServiceReference<Redhawk> reference) {
        if(reference!=null && reference.getProperty("connectionName")!=null){
        	String connectionName = (String) reference.getProperty("connectionName");
            logger.debug("UNBINDING REDHAWK SERVICE: " + connectionName);
            if (connectionName != null) {
                redhawkDrivers.remove(connectionName);
            }
        }else{
        	logger.debug("Unable to unbind Null reference passed in or no connectionName present");
        }
    }

    public List<ServiceReference<Redhawk>> getRedhawkDriverServices() {
        return redhawkDriverServices;
    }

    public void setRedhawkDriverServices(List<ServiceReference<Redhawk>> redhawkDriverServices) {
        this.redhawkDriverServices = redhawkDriverServices;
    }

}