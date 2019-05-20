package tests.tests;



import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;


import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;


/**
 * Utils class for updating the Zephyr
 * 
 * @author Sgoutham
 *
 */
@SuppressWarnings("unchecked")
public class ZephyrUtils
{
	public final static String PASS = "1";
	public final static String FAIL = "2";
	public final static String UNEXECUTED = "-1";
	public final static String BLOCKED = "4";
	public final static String TESTCYCLE_URL = "https://jira.solutionstarit.com/rest/zapi/1.0/cycle/";

	static String jiraServer, jiraAuth, cycleId;

	public static HashMap<String, String> testCaseStatus = new HashMap<String, String>();

	
	public static Map<String,Object> convertJSONToMap(String jsonString)
	{
		Map<String, Object> retMap = new Gson().fromJson(jsonString, new TypeToken<HashMap<String, Object>>() {}.getType());
		return retMap;
	}
	
	public static String objToJson(Object obj) {
		Gson gson = new Gson();
		String json = gson.toJson(obj);
		return json;
	}
	
	public static Object constructObjfromJson(String json, Class<?> classObj) {
		Gson gson = new Gson();
		Object obj = gson.fromJson(json, classObj);
		return obj;
	}
	

	/**
	 * Initializes the zephyr utils by setting the jira server, auth token from the system parameter values. The system parameters used are:
	 * <ul>
	 *  <li>jiraServer - The URL of jira server</li>
	 *  <li>jiraAuth - The auth token, the username and password of jira encoded using base 64</li>
	 * </ul>
	 * @param testCycleId The test cycle which has to be updated
	 */
	public static void initZephyr(String testCycleId)
	{
		if (cycleId == null)
		{
			cycleId = testCycleId;
			jiraServer = System.getProperty("jiraServer","https://jira.solutionstarit.com/");
			jiraAuth = System.getProperty("jiraAuth","YXJhbWVzaDpBc2h3aW4xMDk5MCE=");
			
		}
	}
	
	/**
	 * This method is used to get the cycle details. The following hashmap will be returned
	 * <pre>
	 * {
	 *	"versionId": -1,
		"environment": "",
  		"build": "",
  		"createdBy": "gouthamraj",
  		"name": "ShortSale - Regression Phase2",
  		"description": "",
  		"modifiedBy": "gouthamraj",
  		"id": 89,
  		"projectId": 10909
		}
	 * </pre>
	 * 
	 * 
	 * @return Map of Sting, Object which represent the cycle details
	 * 
	 */
	public static Map<String, Object> getCycleDetails()
	{
		Client client = ClientBuilder.newClient();
		Response response = client.target(jiraServer + "rest/zapi/latest/cycle/" + cycleId)
				.request(MediaType.APPLICATION_JSON_TYPE).header(HttpHeaders.AUTHORIZATION, "Basic " + jiraAuth).get();
		
		Map<String, Object> responseMap = convertJSONToMap(response.readEntity(String.class));
		return responseMap;
	}
	
	/**
	 * Returns the test cycle url based on the testcycleid passed.
	 * @return the url for test cycle
	 */
	public static String getTestCycleURL()
	{
		String url = jiraServer + "/secure/enav/#?query=cycleName%20in%20(\"" + getCycleDetails().get("name").toString() + "\")";
		return url;
	}

	/**
	 * This method is used to add the test cases which are not as part of the current test cycle
	 * @param testCases The array of test case ids to be added to the cycle
	 * @return HashMap of the keys and their execution ids, which has to be used to mark them pass/fail
	 */
	public static HashMap<String, String> createNewTestExecution(String[] testCases)
	{
		HashMap<String, String> testExectionIdMap = new HashMap<String, String>();
		Client client = ClientBuilder.newClient();
		Map<String, Object> responseMap = getCycleDetails();
		Object projectId = responseMap.get("projectId");

		/*
		 * Add test to cycle - request will return appropriate message if test
		 * is already added to the cycle
		 */
		HashMap<String, Object> hm = new HashMap<String, Object>();
		hm.put("issues", testCases);
		hm.put("cycleId", cycleId);
		hm.put("projectId", String.valueOf(Double.valueOf(projectId.toString()).intValue()));
		hm.put("methodId", String.valueOf(1));

		Response response = client.target(jiraServer + "rest/zapi/latest/execution/addTestsToCycle")
				.request(MediaType.APPLICATION_JSON_TYPE).header(HttpHeaders.AUTHORIZATION, "Basic " + jiraAuth)
				.post(Entity.json(objToJson(hm)));

		

		/*
		 * Create & perform execution for all test cases
		 */

		for (String s : testCases)
		{
			/*
			 * Get JIRA issue id
			 */
			response = client.target(jiraServer + "rest/api/latest/search")
					.queryParam("jql", "%20id%20in%20(" + s + ")").request(MediaType.APPLICATION_JSON_TYPE)
					.header(HttpHeaders.AUTHORIZATION, "Basic " + jiraAuth).get();

			

			responseMap = (HashMap<String, Object>) constructObjfromJson(response.readEntity(String.class),
					responseMap.getClass());
			ArrayList<?> issues = (ArrayList<?>) responseMap.get("issues");
			LinkedTreeMap<String, Object> ltm = (LinkedTreeMap<String, Object>) issues.get(0);

			/*
			 * Create execution
			 */
			hm.clear();
			hm.put("issueId", ltm.get("id").toString());
			hm.put("cycleId", cycleId);
			hm.put("projectId", String.valueOf(Double.valueOf(projectId.toString()).intValue()));

			response = client.target(jiraServer + "rest/zapi/latest/execution").request(MediaType.APPLICATION_JSON_TYPE)
					.header(HttpHeaders.AUTHORIZATION, "Basic " + jiraAuth).post(Entity.json(objToJson(hm)));

			

			responseMap = (HashMap<String, Object>) constructObjfromJson(response.readEntity(String.class),
					responseMap.getClass());

			testExectionIdMap.put(s, responseMap.keySet().toArray()[0].toString());
		}
		client.close();

		return testExectionIdMap;

	}

	/**
	 * Takes the latest execution details for the given test cycle id, this is used initially to get the test cases in the test cycle
	 * @return HashMap of the latest execution details
	 */
	public static Map<String, Object> getInfoFromTestCycle()
	{
		Client client = ClientBuilder.newClient();

		Response response = client.target(jiraServer + "rest/zapi/latest/execution").queryParam("cycleId", cycleId)
				.request(MediaType.APPLICATION_JSON_TYPE).header(HttpHeaders.AUTHORIZATION, "Basic " + jiraAuth).get();

		

		Map<String, Object> responseMap = convertJSONToMap(response.readEntity(String.class));

		client.close();

		return responseMap;
	}
	
	/**
	 * Returns HashMap of Issue Key and the execution number for the same
	 * @return
	 */
	public static HashMap<String, String> getExecutionIdFromTestCycle()
	{
		HashMap<String, String> testExectionIdMap = new HashMap<String, String>();

		Map<String, Object> responseMap = getInfoFromTestCycle();

		List<LinkedTreeMap<String, Object>> executions = (List<LinkedTreeMap<String, Object>>) responseMap
				.get("executions");

		for (LinkedTreeMap<String, Object> execution : executions)
		{
			testExectionIdMap.put(execution.get("issueKey").toString(),
					"" + ((Double) execution.get("id")).longValue());
		}

		List<String> testCasesToBeAddded = new ArrayList<String>();

		for (String key : testCaseStatus.keySet())
		{
			if (!testExectionIdMap.containsKey(key))
			{
				testCasesToBeAddded.add(key);
			}
		}

		if (testCasesToBeAddded.size() > 0)
		{
			testExectionIdMap.putAll(
					createNewTestExecution(testCasesToBeAddded.toArray(new String[testCasesToBeAddded.size()])));
		}

		return testExectionIdMap;
	}

	/**
	 * Bulk updates the all passed in execution ids with the passed in status
	 * @param executionIds The list of exection ids that needs to be updated
	 * @param status The status that needs to be set
	 */
	public static void bulkUpdateStatus(Collection<String> executionIds, String status)
	{
		Client client = ClientBuilder.newClient();
		
		

		HashMap<String, Object> hm = new HashMap<String, Object>();
		hm.put("status", status);
		hm.put("stepStatus", "1");
		hm.put("executions", executionIds.toArray(new String[executionIds.size()]));

		String json = objToJson(hm);

		Response response = client.target(jiraServer + "rest/zapi/latest/execution/updateBulkStatus")
				.request(MediaType.APPLICATION_JSON_TYPE).header(HttpHeaders.AUTHORIZATION, "Basic " + jiraAuth)
				.put(Entity.json(json));

		

		client.close();
	}

	/**
	 * This method is to reset i.e. set execution status as unexecuted for all the test cases in the test cycle
	 */
	public static void resetExecutionStatusForCycle()
	{
		HashMap<String, String> testCaseExecutionMap = getExecutionIdFromTestCycle();
		bulkUpdateStatus(testCaseExecutionMap.values(), UNEXECUTED);
	}
	
	/**
	 * This method is used to get the execution status of a cycle 
	 * @return List of HahMap which indicating the execution status of the test cases
	 */
	public static List<HashMap<String, String>> getExecutionStatusFromCycle()
	{

		List<HashMap<String, String>> output = new ArrayList<HashMap<String, String>>();

		Map<String, Object> responseMap = getInfoFromTestCycle();

		List<LinkedTreeMap<String, Object>> executions = (List<LinkedTreeMap<String, Object>>) responseMap
				.get("executions");

		for (LinkedTreeMap<String, Object> execution : executions)
		{
			String mode = System.getProperty("zephyr");
			if((mode!=null && mode.equals("generate-report") )|| testCaseStatus.containsKey(execution.get("issueKey")))
			{
				HashMap<String, String> tmp = new HashMap<String, String>();
				tmp.put("key", execution.get("issueKey").toString());
				if (execution.containsKey("summary"))
				{
					tmp.put("name", execution.get("summary").toString());
				}
				else
				{
					tmp.put("name", "");
				}
				if (execution.containsKey("issueDescription"))
				{
					tmp.put("description", execution.get("issueDescription").toString());
				}
				else
				{
					tmp.put("description", "");
				}
				if (execution.containsKey("component"))
				{
					tmp.put("component", execution.get("component").toString());
				}
				else
				{
					tmp.put("component", "");
				}
				switch (execution.get("executionStatus").toString().toUpperCase())
				{
				case "UNEXECUTED":
					tmp.put("result", "UNEXECUTED");
					break;
				case "PASS":
					tmp.put("result", "PASS");
					break;
				case "FAIL":
					tmp.put("result", "FAIL");
					break;
				case "BLOCKED":
					tmp.put("result", "BLOCKED");
					break;
				default:
					tmp.put("result", "");
				}
				output.add(tmp);
			}
		}

		return output;
	}

	/**
	 * Updates the execution status in bulk in jira
	 */
	public static void updateExecutionStatusInJIRA()
	{
		try
		{
			// group test cases as per status
			HashMap<String, String> testCaseExecutionMap = getExecutionIdFromTestCycle();
			
			HashMap<String, Collection<String>> groupingMap = new HashMap<String, Collection<String>>();
			for (String testCase : testCaseStatus.keySet())
			{
				if (groupingMap.containsKey(testCaseStatus.get(testCase)))
				{
					groupingMap.get(testCaseStatus.get(testCase)).add(testCaseExecutionMap.get(testCase));
				}
				else
				{
					Collection<String> testCases = new ArrayList<String>();
					testCases.add(testCaseExecutionMap.get(testCase));
					groupingMap.put(testCaseStatus.get(testCase), testCases);
				}
			}
			for (String status : groupingMap.keySet())
			{
				bulkUpdateStatus(groupingMap.get(status), status);
			}

		}
		catch (Exception e)
		{
			e.printStackTrace();
			
		}
	}

	/**
	 * Updates the test case status in a static variable
	 * 
	 * @param testCases
	 *            - JIRA test cases whose execution statuses are to be updated
	 * @param status
	 *            - Execution status
	 * 
	 */
	public static void updateExecutionStatusOfTests(String[] testCases, String status)
	{
		for (String testCase : testCases)
		{
			if (testCaseStatus.containsKey(testCase))
			{
				if (status.equals(FAIL))
				{
					testCaseStatus.put(testCase, status);
				}
				else if (testCaseStatus.get(testCase).equals(FAIL))
				{
					continue;
				}
				else if (testCaseStatus.get(testCase).equals(PASS) && !testCaseStatus.get(testCase).equals(status))
				{
					continue;
				}
				else if (testCaseStatus.get(testCase).equals(BLOCKED) && !testCaseStatus.get(testCase).equals(status))
				{
					testCaseStatus.put(testCase, status);
				}
			}
			else
			{
				testCaseStatus.put(testCase, status);
			}
		}

	}
	
	

	

}

