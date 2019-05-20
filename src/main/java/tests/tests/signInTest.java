package tests.tests;

import java.lang.annotation.Annotation;

import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestNG;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.testng.internal.IResultListener2;

import pageobject.pageobject.loginPage;

@Listeners(ListenerTest.class)
public class signInTest extends AppTest {

	loginPage loginpage;

	@Jira(TC = { "AUC-18770" })
	@Test(description = "Login and assert an asset")
	public void signIn() {
		invokeChrome();
		driver.get("https://cascadeqa.np.xome.com");

		loginpage = new loginPage(driver);
		loginpage.signIn();
		loginpage.quickSearchClick();
		String assetMgmtPropId = "191068-1";
		loginpage.enterTextAndSearch(assetMgmtPropId);
		Assert.assertEquals(assetMgmtPropId, loginpage.getAssetId());

		driver.close();
	}

	@AfterTest
	public void updateJiraAfterTest() {
		ZephyrUtils.initZephyr("1584");
		ZephyrUtils.updateExecutionStatusInJIRA();
	}

}
