package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class ITBasicRepository {
	
	private static Logger log = Logger.getLogger(ITBasicRepository.class.getName());
	public static List<String> urlsToTest = new ArrayList<String>();
	public static String repoBaseUrl = null;
	public static RestTemplate template;
	
	@BeforeClass
	public static void beforeClass(){
		// Load the required system properties
		String propName = "org.sagebionetworks.repository.service.base.url";
		repoBaseUrl = System.getProperty(propName);
		assertNotNull("Failed to find the system property: " + propName,
				repoBaseUrl);
		log.info("Loaded system property: " + propName + " = " + repoBaseUrl);
		urlsToTest.add("dataset?sort=name&limit=3");
		template = new RestTemplate();
	}
	
	
	@Ignore // This does not work if no data has been pushed into the repository because the tables do not exist yet.
	@Test
	public void testAllURLs(){
		System.out.println("Starting the test...");
		// run each url on the list
		for(String suffix: urlsToTest){
			String url = repoBaseUrl + suffix;
			System.out.println("Testing url: "+url);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = new HttpEntity<String>("", headers);
			ResponseEntity<Object> response = template.exchange(url, HttpMethod.GET, entity, Object.class);
			assertNotNull(response);
			response.getStatusCode();
			System.out.println(response.getBody());
			assertEquals(HttpStatus.OK, response.getStatusCode());
		}		
	}

}