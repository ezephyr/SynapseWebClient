package org.sagebionetworks.repo;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * WikiGenerator is used to auto-generate the wiki for the Platform Repository
 * Service.
 * 
 * All the log output goes to stdout. See generateRepositoryServiceWiki.sh for
 * how the log output is cleaned and turned into an actual wiki page. The reason
 * why this writes log output file instead of a normal output to stdout is
 * because I want to include the response headers logged by HttpClient and this
 * was a quick way to make that happen.
 * 
 * Also note that I originally wrote this against HtmlUnit so that I could
 * include a bit of testing to make sure the responses coming back were sane,
 * but HtmlUnit does not support PUT or DELETE.
 * 
 * {code} svn checkout
 * https://sagebionetworks.jira.com/svn/PLFM/trunk/tools/wikiutil cd wikiutil
 * ~/platform/trunk/tools/wikiutil>mvn clean compile
 * ~/platform/trunk/tools/wikiutil>./generateRepositoryServiceWiki.sh
 * http://localhost:8080 > wiki.txt {code}
 * 
 */
public class ReadOnlyWikiGenerator {

	private static final Logger log = Logger.getLogger(WikiGenerator.class
			.getName());

	private static String serviceEndpoint = "http://localhost:8080";
	
	private static final String QUERY_SYNTAX = 
	"{code}SELECT * FROM <data type> WHERE <expression> (AND <expression>)* [LIMIT <#>] [OFFSET #]\n\n"
	+ "<expresssion> := <field name> <operator> <value>{code}\n"
	+ "{{<value>}} should be in quotes for strings, but not numbers (i.e. {{name == \"Smith\" AND size > 10}})\n\n" 
	+ "Curently supported {{<operators>}} with their required URL escape codes:\n"
	+ "||Operator ||Value|| URL Escape Code \n"
	+ "| Equal| == | %3D%3D|\n"
	+ "| Does Not equal| != | !%3D|\n"
	+ "| Greater Than | > | %3E |\n"
	+ "| Less than | < | %3C |\n"
	+ "| Greater than or equals | >= | %3E%3D |\n"
	+ "| Less than or equals | <= | %3C%3D |\n";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		if (1 <= args.length) {
			serviceEndpoint = args[0];
		}

		WikiGenerator wiki = new WikiGenerator(serviceEndpoint);
		
		log.info("h1. Query API");
		log.info("The Query API is loosely modeled after Facebook's [Query Language|https://developers.facebook.com/docs/reference/fql/].");
		log.info("h2. Examples");
		wiki.doGet(
				"/repo/v1/query?query=select+*+from+dataset+limit+3+offset+1",
				"h3. 'Select *' Query",
				"These queries are generally of the form:\n"
						+ "{code}SELECT * FROM <data type> [LIMIT <#>] [OFFSET <#>]{code}");
		wiki.doGet(
				"/repo/v1/query?query=select+*+from+dataset+order+by+Number_of_Samples+DESC+limit+3+offset+1",
				"h3. 'Order By' Query",
				"These queries are generally of the form:\n"
						+ "{code}SELECT * FROM <data type> ORDER BY <field name> [ASC|DESC] [LIMIT <#>] [OFFSET #]{code}");

		JSONObject results = wiki.doGet(
				"/repo/v1/query?query=select+*+from+dataset+where+name+==+%22MSKCC+Prostate+Cancer%22",
				"h3. Single clause 'Where' Query",
				"These queries are generally of the form:\n" + QUERY_SYNTAX);
		JSONArray datasets = results.getJSONArray("results");
		JSONObject dataset = null;
		for (int i = 0; i < datasets.length(); i++) {
			dataset = datasets.getJSONObject(i);
			if (dataset.getString("dataset.name").equals(
					"MSKCC Prostate Cancer")) {
				break;
			}
		}

		wiki.doGet(
				"/repo/v1/query?query=select+*+from+dataset+where+dataset.Species+==+%22Human%22+and+dataset.Number_of_Samples+%3E+100+limit+3+offset+1",
				"h3. Multiple clause 'Where' Query",
				"These queries are generally of the form:\n" + QUERY_SYNTAX);
		
		wiki.doGet(
				"/repo/v1/query?query=select+*+from+layer+where+dataset.id+==+%22"
						+ dataset.getString("dataset.id") + "%22",
				"h3. 'Select *' Query for the Layers of a Dataset",
				"These queries are generally of the form:\n"
						+ "{code}SELECT * FROM layer WHERE dataset.id == <datasetId> [LIMIT <#>] [OFFSET <#>]{code}");

		wiki.doGet(
				"/repo/v1/query?query=select+*+from+layer+where+dataset.id+==+%22"
						+ dataset.getString("dataset.id") + "%22+ORDER+BY+type",
				"h3. 'Order By' Query for the Layers of a Dataset",
				"These queries are generally of the form:\n"
						+ "{code}SELECT * FROM layer WHERE dataset.id == <datasetId> ORDER BY <field name> [ASC|DESC] [LIMIT <#>] [OFFSET <#>]{code}");

		log.info("h2. Schema");
		wiki.doGet(
				"/repo/v1/query/schema",
				"h3. Query Response Schema",
				"The [JsonSchema|http://json-schema.org/] is an emerging standard similar to DTDs for XML.");

		log.info("h1. REST API");
		log.info("The REST API took inspiration from several successful REST APIs:");
		log.info("* Facebook's [Graph API|https://developers.facebook.com/docs/reference/api/]");
		log.info("* Google's [Data Protocol|http://code.google.com/apis/gdata/docs/2.0/basics.html]");
		log.info("* The [Open Data Protocol|http://www.odata.org/developers/protocols/overview]\n");
		
		log.info("h2. Read-Only Examples");
		wiki.doGet(
				"/repo/v1/dataset/test",
				"h3. Sanity check request",
				"This is just a 'hello world' type request that you can make to ensure that the service is running.");
		wiki.doGet(
				"/repo/v1/dataset?sort=name&limit=3",
				"h3. Get All Datasets",
				"Optional Parameters\n"
						+ "* offset - _integer_ - 1-based pagination offset\n"
						+ "* limit - _integer_ - maximum number of results to return\n"
						+ "* sort - _string_ - the name of the field upon which to sort\n"
						+ "* ascending - _boolean_ - whether or not to sort ascending");
		dataset = wiki.doGet(
				"/repo/v1/dataset/" + dataset.getString("dataset.id"),
				"h3. Get a Dataset",
				"This returns the primary fields of a dataset and links to get additional info.");
		wiki.doGet(dataset.getString("annotations"),
				"h3. Get Annotations for a Dataset",
				"This returns the annotations for a dataset.");
		results = wiki.doGet(dataset.getString("layer"),
				"h3. Get all the Layers for a Dataset",
				"This returns the primary fields for all the layers of a dataset.");
		JSONArray layers = results.getJSONArray("results");
		JSONObject layer = null;
		for (int i = 0; i < layers.length(); i++) {
			layer = layers.getJSONObject(i);
			String type = layer.getString("type");
			if (type.equals("G")) {
				type = "Genetic";
			} else if (type.equals("E")) {
				type = "Expression";
			} else if (type.equals("C")) {
				type = "Clinical";
			}
			wiki.doGet(layer.getString("uri"), "h3. Get a " + type
					+ " Dataset Layer",
					"This returns the metadata for a dataset layer.");
			wiki.doGet(layer.getString("annotations"), "h4. Get Annotations for a "
					+ type + " Dataset Layer",
					"This returns the annotations for a dataset layer.");
			wiki.doGet(layer.getString("preview"), "h4. Get preview data for a "
					+ type + " Dataset Layer",
					"This returns the preview data for a dataset layer.");
			wiki.doGet(layer.getString("uri") + "/previewAsMap",
					"h4. Get preview data as a map for a " + type
							+ " Dataset Layer",
					"This returns the preview data for a dataset layer.");
			wiki.doGet(layer.getString("uri") + "/locations",
					"h4. Get the locations for a " + type + " Dataset Layer",
					"This returns all the locations metadata for a dataset layer.");
			JSONArray locations = layer.getJSONArray("locations");
			for (int j = 0; j < locations.length(); j++) {
				String location = locations.getString(j);
				if (location.endsWith("Location")) {
					int slash = location.lastIndexOf("/");
					String locationType = location.substring(slash + 1);
					wiki.doGet(location, "h4. Get the " + locationType + " for a "
							+ type + " Dataset Layer",
							"This returns the location data for a dataset layer.");
				}
			}
		}

		log.info("h2. Schemas");
		wiki.doGet(
				"/repo/v1/dataset/schema",
				"h3. Datasets List Schema",
				"The [JsonSchema|http://json-schema.org/] is an emerging standard similar to DTDs for XML.");
		wiki.doGet(
				dataset.getString("uri") + "/schema",
				"h3. Dataset Schema",
				"The [JsonSchema|http://json-schema.org/] is an emerging standard similar to DTDs for XML.");
		wiki.doGet(
				dataset.getString("annotations") + "/schema",
				"h3. Dataset Annotations Schema",
				"The [JsonSchema|http://json-schema.org/] is an emerging standard similar to DTDs for XML.");
		wiki.doGet(
				dataset.getString("layer") + "/schema",
				"h3. Dataset Layers List Schema",
				"The [JsonSchema|http://json-schema.org/] is an emerging standard similar to DTDs for XML.");
		wiki.doGet(
				layer.getString("uri") + "/schema",
				"h3. Layer Schema",
				"The [JsonSchema|http://json-schema.org/] is an emerging standard similar to DTDs for XML.");
		wiki.doGet(
				layer.getString("preview") + "/schema",
				"h3. Layer Preview Schema",
				"The [JsonSchema|http://json-schema.org/] is an emerging standard similar to DTDs for XML.");
		wiki.doGet(
				layer.getString("uri") + "/previewAsMap/schema",
				"h3. Layer Preview as Map Schema",
				"The [JsonSchema|http://json-schema.org/] is an emerging standard similar to DTDs for XML.");
		JSONArray locations = layer.getJSONArray("locations");
		for (int j = 0; j < locations.length(); j++) {
			String location = locations.getString(j);
			if (!location.endsWith("Location")) {
				wiki.doGet(
						location + "/schema",
						"h3. Layer Locations Schema",
						"The [JsonSchema|http://json-schema.org/] is an emerging standard similar to DTDs for XML.");

			}
		}
		for (int j = 0; j < locations.length(); j++) {
			String location = locations.getString(j);
			if (location.endsWith("Location")) {
				int slash = location.lastIndexOf("/");
				String locationType = location.substring(slash + 1);
				wiki.doGet(
						location + "/schema",
						"h4. Get the " + locationType + " for a "
								+ " Dataset Layer",
						"The [JsonSchema|http://json-schema.org/] is an emerging standard similar to DTDs for XML.");
			}
		}
	}
}