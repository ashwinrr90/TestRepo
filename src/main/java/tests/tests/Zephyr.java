package tests.tests;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * This class handles the report generation and to reset the test cycle in Zephyr. This class uses the following system property variables:
 * <ul>
 * 	<li>testCycleId - The test cycle id to be considered</li>
 *  <li>writeTo - The file name to write the html report to</li>
 *  <li>includeDescription - To include the description provided in jira in the report</li>
 *  <li>includeComponents - To include the components provided in jira in the report</li>
 *  <li>jiraServer - The URL of jira</li>
 *  <li>zephyr - The mode of operation. Valid modes - reset/generate-report</li>
 * </ul>
 * 
 * @author Sgoutham
 *    
 */
public class Zephyr
{
	public static String testCycleId = System.getProperty("testCycleId");

	
	/**
	 * To reset the status of all the test cases in test cycle as UNEXECUTED.
	 * This method will be executed when the zephyr mode is set to reset
	 */
	public static void resetTestCycle()
	{
		ZephyrUtils.initZephyr(testCycleId);
		ZephyrUtils.resetExecutionStatusForCycle();
	}

	/**
	 * This method generates an HTML report of a given test cycle id.
	 * This method will be executed when the zephyr mode is set to generate-report.
	 * <br/>
	 * This method sorts the results with Issue key in asc order 
	 * 
	 */
	public static void generateHtmlReport()
	{
		String writeToFile = System.getProperty("writeTo","jiraresult.html");
		boolean includeDescription = Boolean.valueOf(System.getProperty("includeDescription", "false"));
		boolean includeComponents = Boolean.valueOf(System.getProperty("includeComponents", "false"));//-DincludeComponents=true, to include components in the result generated
		StringBuilder output1 = new StringBuilder();
		output1.append(
				"<style>.PASS{color:green}.FAIL{color:red}.UNEXECUTED{color:orange}.BLOCKED{color:grey}</style><b>Test Case Status:</b><table border=1><tr><th>Jira Key</th><th>Name</th>");
		if (includeDescription)
			output1.append("<th>Description</th>");
		if (includeComponents)
			output1.append("<th>Components</th>");
		output1.append("<th>Result</th></tr>");
		String jiraServer = System.getProperty("jiraServer");

		HashMap<String, Integer> countHash = new HashMap<String, Integer>();

		ZephyrUtils.initZephyr(testCycleId);
		List<HashMap<String, String>> results = ZephyrUtils.getExecutionStatusFromCycle();

		if(includeComponents)
		{
			Collections.sort(results, new Comparator<HashMap<String, String>>()
			{
				
				public int compare(HashMap<String, String> o1, HashMap<String, String> o2)
				{
					String val1 = o1.get("component");
					String val2 = o2.get("component");
					return val1.compareToIgnoreCase(val2);
				}
			});
		}
		else
		{
			Collections.sort(results, new Comparator<HashMap<String, String>>()
			{
				
				public int compare(HashMap<String, String> o1, HashMap<String, String> o2)
				{
					int num1 = Integer.parseInt(o1.get("key").replaceAll("[^0-9]", ""));
					int num2 = Integer.parseInt(o2.get("key").replaceAll("[^0-9]", ""));
					return num1 - num2;
				}
			});
		}

		for (HashMap<String, String> result : results)
		{
			output1.append("<tr><td><a href='" + jiraServer + "browse/" + result.get("key") + "'>" + result.get("key")
					+ "</a></td>");
			output1.append("<td>" + result.get("name") + "</td>");
			if (includeDescription)
				output1.append("<td>" + result.get("description") + "</td>");
			if (includeComponents)
				output1.append("<td>" + result.get("component") + "</td>");
			output1.append("<td class=" + result.get("result") + ">" + result.get("result") + "</td></tr>");
			int count = 0;
			if (countHash.containsKey(result.get("result")))
			{
				count = countHash.get(result.get("result"));
			}
			countHash.put(result.get("result"), ++count);
		}
		output1.append("</table></br></br>");

		// consolidated result:

		StringBuilder output2 = new StringBuilder();
		output2.append("Test Execution summary in - "+ZephyrUtils.getCycleDetails().get("name").toString() +" <br /><br /><b>Execution Summary:</b><br /><table border=1>");
 //<a href='"+ ZephyrUtils.getTestCycleURL()  +"'>Jira</a> - 
		if (countHash.containsKey("PASS"))
		{
			output2.append("<tr class='PASS'><td>PASS</td><td>" + countHash.get("PASS") + "</td>");
		}
		else
		{
			output2.append("<tr class='PASS'><td>PASS</td><td>" + 0 + "</td>");
		}

		if (countHash.containsKey("FAIL"))
		{
			output2.append("<tr class='FAIL'><td>FAIL</td><td>" + countHash.get("FAIL") + "</td>");
		}
		else
		{
			output2.append("<tr class='FAIL'><td>FAIL</td><td>" + 0 + "</td>");
		}

		if (countHash.containsKey("UNEXECUTED"))
		{
			output2.append("<tr class='UNEXECUTED'><td>UNEXECUTED</td><td>" + countHash.get("UNEXECUTED") + "</td>");
		}
		else
		{
			output2.append("<tr class='UNEXECUTED'><td>UNEXECUTED</td><td>" + 0 + "</td>");
		}

		if (countHash.containsKey("BLOCKED"))
		{
			output2.append("<tr class='BLOCKED'><td>BLOCKED</td><td>" + countHash.get("BLOCKED") + "</td>");
		}
		else
		{
			output2.append("<tr class='BLOCKED'><td>BLOCKED</td><td>" + 0 + "</td>");
		}

		output2.append("</table><br />");

		try
		{
			File file=new File(writeToFile);
			// if file doesn't exists, then create it
						if (!file.exists()) {
							file.createNewFile();							
						}
			// true = append file
			
			FileWriter fw = new FileWriter(writeToFile,true);
			fw.write(output2.toString() + output1.toString());
			fw.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		String mode = System.getProperty("zephyr","generate-report");
		if (mode.equals("reset"))
		{
			resetTestCycle();
		}
		else if (mode.equals("generate-report"))
		{
			generateHtmlReport();
		}
	}

}

