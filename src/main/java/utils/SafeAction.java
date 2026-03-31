package utils;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class SafeAction {
    private final WebDriver driver;
    private final WebDriverWait wait;
    private final WebDriverWait longWait;

    // Highlight configuration
    private static final String HIGHLIGHT_STYLE = "border: 3px solid red; background-color: yellow; opacity: 0.8;";
    private static final int HIGHLIGHT_DURATION = 100; // 2 seconds

    public SafeAction(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        this.longWait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    public SafeAction(WebDriver driver, int defaultTimeout, int longTimeout) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(defaultTimeout));
        this.longWait = new WebDriverWait(driver, Duration.ofSeconds(longTimeout));
    }

    /**
     * Highlights an element by changing its style temporarily
     * @param element The WebElement to highlight
     */
    private void highlightElement(WebElement element) {
        try {
            // Store original style
            String originalStyle = element.getAttribute("style");

            // Apply highlight style
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].setAttribute('style', arguments[1]);",
                    element, HIGHLIGHT_STYLE
            );

            // Wait for highlight duration
            Thread.sleep(HIGHLIGHT_DURATION);

            // Restore original style
            if (originalStyle != null && !originalStyle.isEmpty()) {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].setAttribute('style', arguments[1]);",
                        element, originalStyle
                );
            } else {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].removeAttribute('style');",
                        element
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("Warning: Could not highlight element: " + e.getMessage());
        }
    }

    /**
     * Enhanced safeFindingScrolling with element highlighting
     * @param pivot The By locator for the scrollable container
     * @param locator The By locator of the element to find and highlight
     */
    public void safeFindingScrolling(By pivot, By locator) {
        WebElement foundElement = null;

        try {
            // Try to find the element first
            foundElement = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            System.out.println("Element found without scrolling - highlighting...");
            highlightElement(foundElement);

        } catch (TimeoutException e) {
            System.out.println("Element not visible, attempting to scroll...");

            // Scrolling Side Bar Menu
            WebElement sidebar = driver.findElement(pivot);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTop += 500;", sidebar);

            // Wait a bit for scroll to complete
            try {
                Thread.sleep(300);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // Try again with original wait timeout after scrolling
            try {
                foundElement = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                System.out.println("Element found after scrolling - highlighting...");
                highlightElement(foundElement);

            } catch (TimeoutException te) {
                System.out.println("Element still not found after scrolling");
                throw te;
            }
        }
    }

    /**
     * Enhanced safeFindingScrolling with WebElement parameter and highlighting
     * @param pivot The By locator for the scrollable container
     * @param element The WebElement to find and highlight
     */
    public void safeFindingScrolling(By pivot, WebElement element) {
        try {
            // Try to wait for element visibility first
            wait.until(ExpectedConditions.visibilityOf(element));
            System.out.println("Element found without scrolling - highlighting...");
            highlightElement(element);

        } catch (TimeoutException e) {
            System.out.println("Element not visible, attempting to scroll...");

            // Scrolling Side Bar Menu
            WebElement sidebar = driver.findElement(pivot);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTop += 500;", sidebar);

            // Wait a bit for scroll to complete
            try {
                Thread.sleep(300);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // Try again with original wait timeout after scrolling
            try {
                wait.until(ExpectedConditions.visibilityOf(element));
                System.out.println("Element found after scrolling - highlighting...");
                highlightElement(element);

            } catch (TimeoutException te) {
                System.out.println("Element still not found after scrolling");
                throw te;
            }
        }
    }

    /**
     * Alternative method with custom highlight style and duration
     * @param pivot The By locator for the scrollable container
     * @param locator The By locator of the element to find
     * @param highlightStyle Custom CSS style for highlighting
     * @param highlightDurationMs Duration to keep highlight in milliseconds
     */
    public void safeFindingScrollingWithCustomHighlight(By pivot, By locator,
                                                        String highlightStyle,
                                                        int highlightDurationMs) {
        WebElement foundElement = null;

        try {
            foundElement = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            System.out.println("Element found without scrolling - highlighting...");
            highlightElementWithCustomStyle(foundElement, highlightStyle, highlightDurationMs);

        } catch (TimeoutException e) {
            System.out.println("Element not visible, attempting to scroll...");

            WebElement sidebar = driver.findElement(pivot);
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTop += 500;", sidebar);

            try {
                Thread.sleep(300);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            try {
                foundElement = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
                System.out.println("Element found after scrolling - highlighting...");
                highlightElementWithCustomStyle(foundElement, highlightStyle, highlightDurationMs);

            } catch (TimeoutException te) {
                System.out.println("Element still not found after scrolling");
                throw te;
            }
        }
    }

    /**
     * Highlights an element with custom style and duration
     */
    private void highlightElementWithCustomStyle(WebElement element, String style, int duration) {
        try {
            String originalStyle = element.getAttribute("style");

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].setAttribute('style', arguments[1]);",
                    element, style
            );

            Thread.sleep(duration);

            if (originalStyle != null && !originalStyle.isEmpty()) {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].setAttribute('style', arguments[1]);",
                        element, originalStyle
                );
            } else {
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].removeAttribute('style');",
                        element
                );
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.out.println("Warning: Could not highlight element: " + e.getMessage());
        }
    }

    /**
     * Method to just highlight an element without scrolling
     * @param locator The By locator of the element to highlight
     */
    public void highlightElement(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
            highlightElement(element);
        } catch (Exception e) {
            System.out.println("Could not highlight element: " + e.getMessage());
        }
    }

    /**
     * Safely clicks on an element with scrolling on web element
     * @param locator The By locator of the element to click
     * @param pivot The By pivot of element to be scrolled
     */
    public void safeClickingScrolling(By pivot, By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            highlightElement(element); // Highlight before clicking
            element.click();

        } catch (TimeoutException e) {
            // Scrolling Side Bar Menu
            WebElement sidebar = driver.findElement(pivot);  // Scrolling Side Bar Menu
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTop += 500;", sidebar);

            // Wait a bit for scroll to complete
            try {
                Thread.sleep(300);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }

            // Try again with original wait timeout
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            highlightElement(element); // Highlight before clicking
            element.click();
        }
    }

    /**
     * Safely clicks on an element with fallback to longer wait
     * @param locator The By locator of the element to click
     */
    public void safeClick(By locator) {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            highlightElement(element); // Highlight before clicking
            element.click();
        } catch (Exception e) {
            // Fallback: try waiting a bit longer
            WebElement element = longWait.until(ExpectedConditions.elementToBeClickable(locator));
            highlightElement(element); // Highlight before clicking
            element.click();
        }
    }

    /**
     * Safely inputs text into an element with value verification
     * @param locator The By locator of the input element
     * @param text The text to input
     */
    public void safeInput(By locator, String text) {
        try {
            WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
            highlightElement(element); // Highlight before input

            // element.clear();
            element.sendKeys(Keys.CONTROL+"a");
            element.sendKeys(Keys.DELETE);

            element.sendKeys(text);
            // waitForElementToHaveValue(locator, text);
        } catch (Exception e) {
            WebElement element = longWait.until(ExpectedConditions.elementToBeClickable(locator));
            highlightElement(element); // Highlight before input

            // element.clear();
            element.sendKeys(Keys.CONTROL+"a");
            element.sendKeys(Keys.DELETE);

            element.sendKeys(text);
            // waitForElementToHaveValue(locator, text);
        }
    }

    /**
     * Finds and returns a Select dropdown element
     * @param locator The By locator of the dropdown element
     * @return Select object for dropdown operations
     */
    public Select findDropdownElement(By locator) {
        WebElement dropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        highlightElement(dropdown); // Highlight the dropdown
        return new Select(dropdown);
    }

    /**
     * Waits for an element to be visible and returns it
     * @param locator The By locator of the element
     * @return The visible WebElement
     */
    public WebElement waitForElementToBeVisible(By locator) {
        highlightElement(locator);
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    /**
     * Waits for an element to be clickable
     * @param locator The By locator of the element
     */
    public void waitForElementToBeClickable(By locator) {
        highlightElement(locator);
        wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public Boolean waitForButtonToBeEnabled(By locator){
        highlightElement(locator);
        return driver.findElement(locator).isEnabled();
    }

    /**
     * Waits for an element to disappear from the DOM
     * @param locator The By locator of the element
     */
    public void waitForElementToDisappear(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    /**
     * Waits for an element to have any value in its value attribute
     * @param locator The By locator of the element
     */
    public void waitForElementToHaveValue(By locator) {
        wait.until(ExpectedConditions.attributeToBeNotEmpty(
                wait.until(ExpectedConditions.presenceOfElementLocated(locator)), "value"));
    }

    /**
     * Waits for an element to have a specific value in its value attribute
     * @param locator The By locator of the element
     * @param expectedValue The expected value
     */
    public void waitForElementToHaveValue(By locator, String expectedValue) {
        //highlightElement(locator);
        wait.until(ExpectedConditions.attributeToBe(locator, "value", expectedValue));
    }

    /**
     * Waits for specific text to be present in an element
     * @param locator The By locator of the element
     * @param text The text to wait for
     */
    public void waitForTextToBePresent(By locator, String text) {
        highlightElement(locator);
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    /**
     * Waits for an element's text content to stabilize (useful for dynamic content)
     * @param locator The By locator of the element
     */
    public void waitForElementToBeStable(By locator) {
        WebElement element = waitForElementToBeVisible(locator);
        String initialText = element.getText();
        try {
            Thread.sleep(500);
            String finalText = element.getText();
            if (!initialText.equals(finalText)) {
                waitForElementToBeStable(locator);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Selects an option from a dropdown by visible text
     * @param locator The By locator of the dropdown element
     * @param option The visible text of the option to select
     */
    public void selectFromDropdown(By locator, String option) {
        findDropdownElement(locator).selectByVisibleText(option);
    }

    /**
     * Selects an option from a dropdown by value
     * @param locator The By locator of the dropdown element
     * @param value The value attribute of the option to select
     */
    public void selectFromDropdownByValue(By locator, String value) {
        findDropdownElement(locator).selectByValue(value);
    }

    /**
     * Selects an option from a dropdown by index
     * @param locator The By locator of the dropdown element
     * @param index The index of the option to select (0-based)
     */
    public void selectFromDropdownByIndex(By locator, int index) {
        findDropdownElement(locator).selectByIndex(index);
    }

    /**
     * Clears the content of an input field
     * @param locator The By locator of the input element
     */
    public void clearInput(By locator) {
        WebElement element = waitForElementToBeVisible(locator);
        highlightElement(element); // Highlight before clearing

        // clearing web element
        // element.clear();

        // clearing web element with user behavior
        element.sendKeys(Keys.CONTROL + "a");
        element.sendKeys(Keys.DELETE);
    }

    /**
     * Configuration methods for highlight customization
     */
    public void setHighlightDuration(int duration) {
        // You can add this as an instance variable if needed
    }

    /**
     * Gets the WebDriver instance
     * @return The WebDriver instance
     */
    public WebDriver getDriver() {
        return driver;
    }

    /**
     * Gets the default WebDriverWait instance
     * @return The WebDriverWait instance
     */
    public WebDriverWait getWait() {
        return wait;
    }

    /**
     * Gets the long WebDriverWait instance
     * @return The long WebDriverWait instance
     */
    public WebDriverWait getLongWait() {
        return longWait;
    }
}