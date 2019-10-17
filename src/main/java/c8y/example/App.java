package c8y.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cumulocity.microservice.autoconfigure.MicroserviceApplication;
import com.cumulocity.microservice.settings.service.MicroserviceSettingsService;
import com.cumulocity.microservice.subscription.service.MicroserviceSubscriptionsService;
import com.cumulocity.model.ID;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.measurement.MeasurementApi;

import c8y.IsDevice;
import c8y.OpenWeather;
import net.aksingh.owmjapis.api.APIException;
import net.aksingh.owmjapis.core.OWM;
import net.aksingh.owmjapis.model.CurrentWeather;

@MicroserviceApplication
@RestController
public class App{
		
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @RequestMapping("hello")
    public String greeting(@RequestParam(value = "name", defaultValue = "world") String name) {
        return "hello " + name + "!";
    }

    // You need the inventory API to handle managed objects e.g. creation. You will find this class within the C8Y java client library.
    private final InventoryApi inventoryApi;
    // you need the identity API to handle the external ID e.g. IMEI of a managed object. You will find this class within the C8Y java client library.
    private final IdentityApi identityApi;
    
    // Microservice subscription
    private final MicroserviceSubscriptionsService subscriptionService;
        
    // To access the tenant options
    private final MicroserviceSettingsService microserviceSettingsService;
    
    @Autowired
    public App( InventoryApi inventoryApi, 
    			IdentityApi identityApi, 
    			MicroserviceSubscriptionsService subscriptionService,
    			MeasurementApi measurementApi,
    			MicroserviceSettingsService microserviceSettingsService) {
        this.inventoryApi = inventoryApi;
        this.identityApi = identityApi;
        this.subscriptionService = subscriptionService;
        this.microserviceSettingsService = microserviceSettingsService;
    }
    
    // Check the weather API every 60 seconds for new data
    @Scheduled(initialDelay=10000, fixedDelay=60000)
    public void startThread() {
    	subscriptionService.runForEachTenant(new Runnable() {
			@Override
			public void run() {
		    	ManagedObjectRepresentation managedObjectRepresentation = resolveManagedObject();
		    	try {
					addWeaterDataToManagedObject(managedObjectRepresentation);
				} catch (NumberFormatException | APIException e) {
					e.printStackTrace();
				} 
			}
		});
    }    
    
    private ManagedObjectRepresentation resolveManagedObject() {
   	
    	try {
        	// check if managed object is existing. create a new one if the managed object is not existing
    		ExternalIDRepresentation externalIDRepresentation = identityApi.getExternalId(new ID("c8y_Serial", "Microservice-Part4_externalId"));
			return externalIDRepresentation.getManagedObject();    	    	

    	} catch(SDKException e) {
    		    		
    		// create a new managed object
			ManagedObjectRepresentation newManagedObject = new ManagedObjectRepresentation();
	    	newManagedObject.setName("Microservice-Part4");
	    	newManagedObject.setType("Microservice-Part4");
	    	newManagedObject.set(new IsDevice());	    	
	    	ManagedObjectRepresentation createdManagedObject = inventoryApi.create(newManagedObject);
	    	
	    	// create an external id and add the external id to an existing managed object
	    	ExternalIDRepresentation externalIDRepresentation = new ExternalIDRepresentation();
	    	// Definition of the external id
	    	externalIDRepresentation.setExternalId("Microservice-Part4_externalId");
	    	// Assign the external id to an existing managed object
	    	externalIDRepresentation.setManagedObject(createdManagedObject);
	    	// Definition of the serial
	    	externalIDRepresentation.setType("c8y_Serial");
	    	// Creation of the external id
	    	identityApi.create(externalIDRepresentation);
	    	
	    	return createdManagedObject;
    	}
    }

	public void addWeaterDataToManagedObject(ManagedObjectRepresentation managedObjectRepresentation) throws NumberFormatException, APIException {
		
		// Read API key from tenant options
        OWM owm = new OWM(microserviceSettingsService.getCredential("api_key")); 
        
        // getting current weather by given city id from tenant options
        CurrentWeather currentWeather = owm.currentWeatherByCityId(Integer.parseInt(microserviceSettingsService.get("city")));
    	
    	// Create a new managed object and add the weater data to this object. We will then override the existing managed object with this one.
    	ManagedObjectRepresentation newManagedObjectRepresentation = new ManagedObjectRepresentation();
    	
    	// Add weather data to POJO
    	OpenWeather openWeather = new OpenWeather();
    	openWeather.setCity(currentWeather.getCityName());
    	openWeather.setHumidity(currentWeather.getMainData().getHumidity());
    	openWeather.setPressure(currentWeather.getMainData().getPressure());
    	openWeather.setSpeed(currentWeather.getWindData().getSpeed());
    	openWeather.setTemperatureMin(currentWeather.getMainData().getTempMin());
    	openWeather.setTemperatureMax(currentWeather.getMainData().getTempMax());
    	newManagedObjectRepresentation.set(openWeather);
    	
    	// or as top level property
    	// newManagedObjectRepresentation.setProperty("openWeatherName", currentWeather.getCityName());
    	// newManagedObjectRepresentation.setProperty("openWeatherHumidity", currentWeather.getMainData().getHumidity());
    	// newManagedObjectRepresentation.setProperty("openWeatherPressure", currentWeather.getMainData().getPressure());
    	// newManagedObjectRepresentation.setProperty("openWeatherSpeed", currentWeather.getWindData().getSpeed());
    	// newManagedObjectRepresentation.setProperty("openWeatherTemperatureMin", currentWeather.getMainData().getTempMin());
    	// newManagedObjectRepresentation.setProperty("openWeatherTemperatureMax", currentWeather.getMainData().getTempMax());

    	// Update your existing managed object based on given internal id
    	newManagedObjectRepresentation.setId(managedObjectRepresentation.getId());
    	
    	// Update your existing managed object with the new managed object    	
    	inventoryApi.update(newManagedObjectRepresentation);
	}

}