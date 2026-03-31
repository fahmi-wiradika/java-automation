package utils;

import io.qameta.allure.Allure;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * POM class to handle screenshot operations in Selenium WebDriver
 * Provides methods for full page, element-specific, and viewport screenshots
 */
public class ScreenshotHandler {
    private static final ThreadLocal<WebDriver> threadDriver = new ThreadLocal<>();
    private final WebDriver driver;
    private final WebDriverWait wait;
    /**
     * -- GETTER --
     *  Gets the current test class name
     *
     */
    @Getter
    private String testClassName;
    private static final Logger logger = Logger.getLogger(ScreenshotHandler.class.getName());
    private static final String BASE_SCREENSHOT_DIR = "evidence/screenshot";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static void setDriver(WebDriver driver) {
        threadDriver.set(driver);
    }
    public static WebDriver getDriver() {
        return threadDriver.get();
    }
    /**
     * Constructor to initialize ScreenshotHandler
     *
     * @param driver WebDriver instance
     */
    public ScreenshotHandler(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
        this.testClassName = getCurrentTestClassName();
        createScreenshotDirectory();
    }

    /**
     * Constructor to initialize ScreenshotHandler with specific test class name
     *
     * @param driver WebDriver instance
     * @param testClassName Name of the test class
     */
    public ScreenshotHandler(WebDriver driver, String testClassName) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, java.time.Duration.ofSeconds(10));
        this.testClassName = testClassName;
        createScreenshotDirectory();
    }

    /**
     * Attaches a screenshot directly into Allure report
     *
     * @param name Name of the screenshot in the report
     */
    public void attachScreenshotToAllure(String name) {
        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Allure.getLifecycle().addAttachment(
                    name, "image/png", "png", screenshot
            );
            logger.info("Screenshot attached to Allure report: " + name);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to attach screenshot to Allure report", e);
        }
    }

    public void attachHighlightScreenshotToAllure(By locator, String fileName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));

            // Store original style
            String originalStyle = element.getAttribute("style");

            // Highlight the element
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.border='3px solid red'", element);

            // Take screenshot
            attachScreenshotToAllure(fileName);

            // Restore original style
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].setAttribute('style', arguments[1])", element, originalStyle);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to take highlighted element screenshot", e);
            throw new RuntimeException("Highlighted element screenshot capture failed", e);
        }
    }

    /**
     * Takes a full page screenshot
     *
     * @param fileName Name of the screenshot file (without extension)
     * @return File path of the saved screenshot
     */
    public String takeFullPageScreenshot(String fileName) throws InterruptedException {
        Thread.sleep(500);
        try {
            TakesScreenshot screenshot = (TakesScreenshot) driver;
            File sourceFile = screenshot.getScreenshotAs(OutputType.FILE);
            String filePath = generateFilePath(fileName);
            File destFile = new File(filePath);

            FileUtils.moveFile(sourceFile, destFile);
            logger.info("Full page screenshot saved: " + filePath);
            return filePath;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to take full page screenshot", e);
            throw new RuntimeException("Screenshot capture failed", e);
        }
    }

    /**
     * Takes a screenshot of a specific element
     *
     * @param element  WebElement to capture
     * @param fileName Name of the screenshot file (without extension)
     * @return File path of the saved screenshot
     */
    public String takeElementScreenshot(WebElement element, String fileName) {
        try {
            // Wait for element to be visible
            wait.until(ExpectedConditions.visibilityOf(element));

            File sourceFile = element.getScreenshotAs(OutputType.FILE);
            String filePath = generateFilePath(fileName);
            File destFile = new File(filePath);

            FileUtils.moveFile(sourceFile, destFile);
            logger.info("Element screenshot saved: " + filePath);
            return filePath;

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to take element screenshot", e);
            throw new RuntimeException("Element screenshot capture failed", e);
        }
    }

    /**
     * Takes a screenshot of element found by locator
     *
     * @param locator  By locator to find the element
     * @param fileName Name of the screenshot file (without extension)
     * @return File path of the saved screenshot
     */
    public String takeElementScreenshot(By locator, String fileName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            return takeElementScreenshot(element, fileName);
        } catch (TimeoutException e) {
            logger.log(Level.SEVERE, "Element not found for screenshot: " + locator, e);
            throw new RuntimeException("Element not found for screenshot", e);
        }
    }

    /**
     * Takes a screenshot on test failure
     *
     * @param testName  Name of the failed test
     * @param testClass Class name of the test
     * @return File path of the saved screenshot
     */
    public String takeFailureScreenshot(String testName, String testClass) throws InterruptedException {
        String fileName = "FAILURE_" + testClass + "_" + testName + "_" +
                LocalDateTime.now().format(DATE_FORMAT);
        return takeFullPageScreenshot(fileName);
    }

    /**
     * Highlights an element and takes a screenshot
     *
     * @param element  WebElement to highlight
     * @param fileName Name of the screenshot file
     */
    public void takeHighlightedElementScreenshot(WebElement element, String fileName) {
        try {
            // Store original style
            String originalStyle = element.getAttribute("style");

            // Highlight the element
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.border='3px solid red'", element);

            // Take screenshot
            String filePath = takeFullPageScreenshot(fileName);

            // Restore original style
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].setAttribute('style', arguments[1])", element, originalStyle);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to take highlighted element screenshot", e);
            throw new RuntimeException("Highlighted element screenshot capture failed", e);
        }
    }

    public void takeHighlightedElementScreenshot(By locator, String fileName) {
        try {
            WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));

            // Store original style
            String originalStyle = element.getAttribute("style");

            // Highlight the element
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].style.border='3px solid red'", element);

            // Take screenshot
            String filePath = takeFullPageScreenshot(fileName);

            // Restore original style
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].setAttribute('style', arguments[1])", element, originalStyle);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to take highlighted element screenshot", e);
            throw new RuntimeException("Highlighted element screenshot capture failed", e);
        }
    }

    /**
     * Sanitizes a string to be safe for use as a file or directory name on all OS platforms.
     * Replaces characters illegal on Windows (\ / : * ? " < > | and brackets/special chars),
     * collapses whitespace to underscores, and trims leading/trailing underscores.
     *
     * @param input Raw string (e.g. test display name or class name)
     * @return Sanitized, filesystem-safe string
     */
    private static String sanitize(String input) {
        if (input == null || input.isBlank()) return "Unknown";
        return input
                .replaceAll("[\\\\/:*?\"<>|()\\[\\]{}#%&@!]", "_")  // illegal path chars → _
                .replaceAll("\\s+", "_")                               // whitespace → _
                .replaceAll("_+", "_")                                 // collapse repeated underscores
                .replaceAll("^_|_$", "");                              // trim leading/trailing underscores
    }

    /**
     * Generates file path with timestamp and proper extension.
     * The fileName is sanitized to remove characters illegal on Windows/Linux.
     *
     * @param fileName Base filename without extension
     * @return Complete file path
     */
    private String generateFilePath(String fileName) {
        String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        String safeFileName = sanitize(fileName);
        String safeClassName = sanitize(testClassName);
        String fullFileName = timestamp + "-" + safeFileName + ".png";
        return BASE_SCREENSHOT_DIR + File.separator + safeClassName + File.separator + fullFileName;
    }

    /**
     * Creates screenshot directory if it doesn't exist
     */
    private void createScreenshotDirectory() {
        String safeClassName = sanitize(testClassName);
        File directory = new File(BASE_SCREENSHOT_DIR + File.separator + safeClassName);
        if (!directory.exists()) {
            directory.mkdirs();
            logger.info("Created screenshot directory: " + directory.getAbsolutePath());
        }
    }

    /**
     * Gets current test class name from stack trace
     *
     * @return Test class name
     */
    private String getCurrentTestClassName() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            if (className.contains("Test") && !className.contains("ScreenshotHandler")) {
                return className.substring(className.lastIndexOf('.') + 1);
            }
        }
        return "UnknownTest";
    }

    /**
     * Clears all screenshots from the test class directory
     */
    public void clearScreenshotDirectory() {
        try {
            File directory = new File(BASE_SCREENSHOT_DIR + File.separator + testClassName);
            if (directory.exists()) {
                FileUtils.cleanDirectory(directory);
                logger.info("Cleared screenshot directory for: " + testClassName);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to clear screenshot directory", e);
        }
    }

    /**
     * Gets the count of screenshots in the test class directory
     *
     * @return Number of screenshot files
     */
    public int getScreenshotCount() {
        File directory = new File(BASE_SCREENSHOT_DIR + File.separator + testClassName);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));
            return files != null ? files.length : 0;
        }
        return 0;
    }

    /**
     * Gets the full directory path for screenshots
     *
     * @return Directory path
     */
    public String getScreenshotDirectory() {
        return BASE_SCREENSHOT_DIR + File.separator + testClassName;
    }
}