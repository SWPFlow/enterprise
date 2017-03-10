package redhawk;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import redhawk.driver.RedhawkDriver;
import redhawk.driver.application.RedhawkApplication;
import redhawk.driver.exceptions.ApplicationReleaseException;
import redhawk.driver.exceptions.CORBAException;
import redhawk.driver.exceptions.ConnectionException;
import redhawk.driver.exceptions.MultipleResourceException;
import redhawk.driver.exceptions.ResourceNotFoundException;

public class RedhawkTestBase {
	public static Logger logger = Logger.getLogger(RedhawkTestBase.class.getName());
	
	public static RedhawkDriver driver; 
	
	@BeforeClass
	public static void setupB4Class(){
		logger.info("Jacorb prop is: "+System.getProperty("jacorb"));
		Boolean jacorbTest = Boolean.parseBoolean(System.getProperty("jacorb", "false"));
		
		if(jacorbTest){
			logger.info("Testing with jacorb");
			Properties props = new Properties(); 
			props.put("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
			props.put("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");
			driver = new RedhawkDriver("127.0.0.1", 2809, props);
		}else{
			logger.info("Testing with default orb for JDK");			
			driver = new RedhawkDriver(); 
		}
	}
	
	@AfterClass
	public static void afterClass() throws MultipleResourceException, CORBAException, ApplicationReleaseException{
		if(driver!=null){
			for(RedhawkApplication application : driver.getDomain().getApplications()){
				//Clean up applications
				application.release();
			}
			driver.disconnect();
		}
		//Always make sure you delete waveforms you create
		//TODO: Clean up this logic
		try {
			driver.getDomain("REDHAWK_DEV").getFileManager().removeDirectory("/waveforms/testWaveform");
		} catch (ConnectionException | ResourceNotFoundException | IOException | CORBAException e) {
			logger.info("Unable to delete wavemform likely cause it doesn't exist.");
		}
	}
}
