package tests.tests;

import java.lang.annotation.Annotation;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class ListenerTest implements ITestListener {
	
	protected String[] getJiraTestCases(ITestResult testResult)
	{
		Annotation a = testResult.getMethod().getConstructorOrMethod().getMethod().getAnnotation(Jira.class);
		String val=(String) testResult.getAttribute("JiraIds");
		if(a!=null)
			return ((Jira) a).TC();
		else if(val!=null)
			return val.split(",");
		else
			return null;
	}

	@Override
	public void onFinish(ITestContext Result) {

	}

	@Override
	public void onStart(ITestContext Result) {

	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult Result) {

	}

	// When Test case get failed, this method is called.
	@Override
	public void onTestFailure(ITestResult Result) {
		String[] testCases = getJiraTestCases(Result);
		if(testCases!= null && testCases.length>0)
			ZephyrUtils.updateExecutionStatusOfTests(getJiraTestCases(Result), ZephyrUtils.FAIL);
	}

	// When Test case get Skipped, this method is called.
	@Override
	public void onTestSkipped(ITestResult Result) {
	}

	// When Test case get Started, this method is called.
	@Override
	public void onTestStart(ITestResult Result) {
		
	}

	// When Test case get passed, this method is called.
	@Override
	public void onTestSuccess(ITestResult Result) {
		String[] testCases = getJiraTestCases(Result);
		if(testCases!= null && testCases.length>0)
			ZephyrUtils.updateExecutionStatusOfTests(getJiraTestCases(Result), ZephyrUtils.PASS);
	}

}