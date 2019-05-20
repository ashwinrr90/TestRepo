package tests.tests;

import java.lang.annotation.Annotation;

import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

/**
 * Unit test for simple App.
 */
public class AppTest{
	WebDriver driver;

	public void invokeChrome() {
		System.out.println("Test Started..");
		System.setProperty("webdriver.chrome.driver", "resources/drivers/chromedriver.exe");
		driver = new ChromeDriver();
		driver.manage().window().fullscreen();
	}

}
