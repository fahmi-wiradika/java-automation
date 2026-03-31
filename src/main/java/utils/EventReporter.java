package utils;

import lombok.Setter;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;
import java.util.logging.Level;

@Setter
public class EventReporter implements WebDriverListener {
    private static final Logger logger = Logger.getLogger(EventReporter.class.getName());
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    // Configuration methods
    // Configuration flags
    private boolean logNavigation = true;
    private boolean logElementInteractions = true;
    private boolean logDriverActions = true;
    private boolean includeTimestamp = true;

    public EventReporter() {
        // Default constructor with all logging enabled
    }

    public EventReporter(boolean logNavigation, boolean logElementInteractions, boolean logDriverActions) {
        this.logNavigation = logNavigation;
        this.logElementInteractions = logElementInteractions;
        this.logDriverActions = logDriverActions;
    }

    private void log(String message) {
        String timestamp = includeTimestamp ? "[" + LocalDateTime.now().format(timeFormatter) + "] " : "";
        String logMessage = timestamp + message;
        System.out.println(logMessage);
        System.out.flush(); // Force immediate output
    }

    private void logWithLevel(String message, Level level) {
        String timestamp = includeTimestamp ? "[" + LocalDateTime.now().format(timeFormatter) + "] " : "";
        String logMessage = timestamp + message;
        logger.log(level, logMessage);
        System.out.println(logMessage);
        System.out.flush(); // Force immediate output
    }

    @Override
    public void beforeGet(WebDriver driver, String url) {
        if (logNavigation) {
            log("🔄 Before navigating to: " + url);
        }
    }

    @Override
    public void afterGet(WebDriver driver, String url) {
        if (logNavigation) {
            log("✅ After navigating to: " + url);
        }
    }

    @Override
    public void beforeFindElement(WebDriver driver, By locator) {
        if (logElementInteractions) {
            log("🔍 Before finding element by: " + locator);
        }
    }

    @Override
    public void afterFindElement(WebDriver driver, By locator, WebElement result) {
        if (logElementInteractions) {
            try {
                String elementInfo = getElementInfo(result);
                log("✅ After finding element by: " + locator + " " + elementInfo);
            } catch (Exception e) {
                log("✅ After finding element by: " + locator + " [Element info unavailable]");
            }
        }
    }

    @Override
    public void beforeClick(WebElement element) {
        if (logElementInteractions) {
            try {
                String elementInfo = getElementInfo(element);
                log("👆 Before clicking element: " + elementInfo);
            } catch (StaleElementReferenceException e) {
                log("👆 Before clicking element: [Stale Element]");
            } catch (Exception e) {
                log("👆 Before clicking element: [Element info unavailable]");
            }
        }
    }

    @Override
    public void afterClick(WebElement element) {
        if (logElementInteractions) {
            try {
                String elementInfo = getElementInfo(element);
                log("✅ After clicking element: " + elementInfo);
            } catch (StaleElementReferenceException e) {
                log("✅ After clicking element: [Element became stale after click - normal behavior]");
            } catch (Exception e) {
                log("✅ After clicking element: [Element info unavailable]");
            }
        }
    }

    @Override
    public void beforeSendKeys(WebElement element, CharSequence... keysToSend) {
        if (logElementInteractions) {
            try {
                String elementInfo = getElementInfo(element);
                String keysPreview = getKeysPreview(keysToSend);
                log("⌨️ Before sending keys to element: " + elementInfo + " | Keys: " + keysPreview);
            } catch (StaleElementReferenceException e) {
                log("⌨️ Before sending keys to element: [Stale Element]");
            } catch (Exception e) {
                log("⌨️ Before sending keys to element: [Element info unavailable]");
            }
        }
    }

    @Override
    public void afterSendKeys(WebElement element, CharSequence... keysToSend) {
        if (logElementInteractions) {
            try {
                String elementInfo = getElementInfo(element);
                String keysPreview = getKeysPreview(keysToSend);
                log("✅ After sending keys to element: " + elementInfo + " | Keys: " + keysPreview);
            } catch (StaleElementReferenceException e) {
                log("✅ After sending keys to element: [Element became stale]");
            } catch (Exception e) {
                log("✅ After sending keys to element: [Element info unavailable]");
            }
        }
    }

    @Override
    public void beforeQuit(WebDriver driver) {
        if (logDriverActions) {
            log("🔴 Before quitting driver");
        }
    }

    @Override
    public void afterQuit(WebDriver driver) {
        if (logDriverActions) {
            log("✅ After quitting driver");
        }
    }

    @Override
    public void beforeClose(WebDriver driver) {
        if (logDriverActions) {
            log("🔴 Before closing driver");
        }
    }

    @Override
    public void afterClose(WebDriver driver) {
        if (logDriverActions) {
            log("✅ After closing driver");
        }
    }

    // Additional method to log test-specific events
    public void logTestEvent(String testName, String event) {
        log("🧪 TEST [" + testName + "]: " + event);
    }

    // Helper method to get element information
    private String getElementInfo(WebElement element) {
        try {
            String tagName = element.getTagName();
            String id = element.getAttribute("id");
            String className = element.getAttribute("class");
            String text = element.getText();

            StringBuilder info = new StringBuilder(tagName);

            if (id != null && !id.isEmpty()) {
                info.append(" [id=").append(id).append("]");
            }

            if (className != null && !className.isEmpty()) {
                info.append(" [class=").append(className, 0, Math.min(className.length(), 30)).append("]");
            }

            if (!text.isEmpty() && text.length() < 30) {
                info.append(" [text=").append(text).append("]");
            }

            return info.toString();
        } catch (Exception e) {
            return "element";
        }
    }

    // Helper method to preview keys being sent (mask passwords)
    private String getKeysPreview(CharSequence... keysToSend) {
        if (keysToSend.length == 0) return "";

        StringBuilder preview = new StringBuilder();
        for (CharSequence keys : keysToSend) {
            if (keys != null) {
                String keyString = keys.toString();
                // Mask potential passwords (simple heuristic)
                if (keyString.matches(".*\\d+.*") && keyString.length() > 4) {
                    preview.append("[MASKED]");
                } else {
                    preview.append(keyString.length() > 20 ? keyString.substring(0, 20) + "..." : keyString);
                }
            }
        }
        return preview.toString();
    }

}