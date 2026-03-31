package base;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import utils.ScreenshotHandler;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.logging.Logger;
import java.util.logging.Level;

public class ScreenshotTestWatcher implements TestWatcher {
    private static final Logger logger = Logger.getLogger(ScreenshotTestWatcher.class.getName());

    @Override
    public void testSuccessful(ExtensionContext context) {
        // Clean up driver resources for successful parallel tests too
        try {
            Object testInstance = context.getTestInstance().orElse(null);
            if (testInstance != null) {
                cleanupDriverResourcesIfParallel(testInstance);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to cleanup driver resources after successful test", e);
        }
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        // Use the raw method name (always filesystem-safe) instead of getDisplayName()
        // which includes parameterized test arguments that may contain illegal path characters
        // like ':', '(', ')', '[', ']' etc. on Windows.
        String testMethodName = context.getRequiredTestMethod().getName();
        String testClassName = context.getTestClass()
                .map(Class::getSimpleName)
                .orElse("UnknownTest");

        try {
            // Get the test instance from the context
            Object testInstance = context.getTestInstance().orElse(null);

            if (testInstance != null) {
                ScreenshotHandler screenshotHandler = getScreenshotHandlerFromInstance(testInstance);
                if (screenshotHandler != null) {
                    String screenshotPath = screenshotHandler.takeFailureScreenshot(testMethodName, testClassName);
                    logger.info("Failure screenshot saved at: " + screenshotPath);

                    // Also attach to Allure if available
                    screenshotHandler.attachScreenshotToAllure("FAILURE_" + testMethodName);
                } else {
                    logger.warning("ScreenshotHandler is null, cannot take failure screenshot");
                }

                // Clean up driver resources after taking screenshot (for parallel tests)
                cleanupDriverResourcesIfParallel(testInstance);

            } else {
                logger.warning("Test instance is null, cannot access ScreenshotHandler");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to capture failure screenshot for test: " + testMethodName, e);

            // Still try to cleanup driver resources even if screenshot failed
            try {
                Object testInstance = context.getTestInstance().orElse(null);
                if (testInstance != null) {
                    cleanupDriverResourcesIfParallel(testInstance);
                }
            } catch (Exception cleanupException) {
                logger.log(Level.WARNING, "Failed to cleanup driver resources", cleanupException);
            }
        }
    }

    /**
     * Clean up driver resources if this is a parallel test
     */
    private void cleanupDriverResourcesIfParallel(Object testInstance) {
        try {
            // Check if this extends ParallelBaseTests
            Class<?> testClass = testInstance.getClass();
            Class<?> currentClass = testClass;

            boolean isParallelTest = false;
            while (currentClass != null && !currentClass.equals(Object.class)) {
                if (currentClass.getSimpleName().contains("ParallelBaseTests") ||
                        currentClass.getName().contains("ParallelBaseTests")) {
                    isParallelTest = true;
                    break;
                }
                currentClass = currentClass.getSuperclass();
            }

            if (isParallelTest) {
                // Call the static cleanup method
                Class<?> parallelBaseClass = Class.forName("base.ParallelBaseTests");
                Method cleanupMethod = parallelBaseClass.getDeclaredMethod("cleanupDriverResources");
                cleanupMethod.setAccessible(true);
                cleanupMethod.invoke(null);
                logger.info("Successfully cleaned up driver resources for parallel test");
            }

        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to cleanup driver resources for parallel test", e);
        }
    }

    /**
     * Get ScreenshotHandler from the test instance using reflection
     * Works with both BaseTests (sequential) and ParallelBaseTests (parallel)
     */
    private ScreenshotHandler getScreenshotHandlerFromInstance(Object testInstance) {
        if (testInstance == null) {
            logger.warning("Test instance is null");
            return null;
        }

        Class<?> testClass = testInstance.getClass();
        String className = testClass.getSimpleName();

        logger.info("Attempting to get ScreenshotHandler from test class: " + className);

        // Strategy 1: Try to call getScreenshotHandler() method (ParallelBaseTests approach)
        ScreenshotHandler handler = tryGetScreenshotHandlerViaMethod(testInstance);
        if (handler != null) {
            logger.info("Successfully obtained ScreenshotHandler via getScreenshotHandler() method");
            return handler;
        }

        // Strategy 2: Try to access the public static ThreadLocal in ParallelBaseTests
        handler = tryGetScreenshotHandlerViaParallelBaseStaticField();
        if (handler != null) {
            logger.info("Successfully obtained ScreenshotHandler via ParallelBaseTests static ThreadLocal");
            return handler;
        }

        // Strategy 3: Try to access instance fields (BaseTests approach)
        handler = tryGetScreenshotHandlerViaInstanceField(testInstance);
        if (handler != null) {
            logger.info("Successfully obtained ScreenshotHandler via instance field");
            return handler;
        }

        // Strategy 4: Try to access static fields in base classes
        handler = tryGetScreenshotHandlerViaStaticField(testInstance);
        if (handler != null) {
            logger.info("Successfully obtained ScreenshotHandler via static field");
            return handler;
        }

        // Strategy 5: Try to get from ScreenshotHandler's own static ThreadLocal
        handler = tryGetScreenshotHandlerViaScreenshotHandlerStatic();
        if (handler != null) {
            logger.info("Successfully obtained ScreenshotHandler via ScreenshotHandler static ThreadLocal");
            return handler;
        }

        logger.warning("All strategies failed to obtain ScreenshotHandler");
        return null;
    }

    /**
     * Try to get ScreenshotHandler via getScreenshotHandler() method
     * This works for ParallelBaseTests
     */
    private ScreenshotHandler tryGetScreenshotHandlerViaMethod(Object testInstance) {
        try {
            Method getScreenshotHandlerMethod = findMethodInHierarchy(testInstance.getClass(), "getScreenshotHandler");
            if (getScreenshotHandlerMethod != null) {
                getScreenshotHandlerMethod.setAccessible(true);
                return (ScreenshotHandler) getScreenshotHandlerMethod.invoke(testInstance);
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Failed to get ScreenshotHandler via method", e);
        }
        return null;
    }

    /**
     * Try to access the public static ThreadLocal in ParallelBaseTests directly
     */
    private ScreenshotHandler tryGetScreenshotHandlerViaParallelBaseStaticField() {
        try {
            Class<?> parallelBaseClass = Class.forName("base.ParallelBaseTests");
            Field field = parallelBaseClass.getDeclaredField("screenshotHandlerThreadLocal");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            ThreadLocal<ScreenshotHandler> threadLocal = (ThreadLocal<ScreenshotHandler>) field.get(null);

            if (threadLocal != null) {
                return threadLocal.get();
            }
        } catch (ClassNotFoundException e) {
            // ParallelBaseTests not found, probably using BaseTests
            logger.log(Level.FINE, "ParallelBaseTests class not found", e);
        } catch (Exception e) {
            logger.log(Level.FINE, "Failed to access ParallelBaseTests static field", e);
        }
        return null;
    }

    /**
     * Try to get from ScreenshotHandler's own static ThreadLocal
     */
    private ScreenshotHandler tryGetScreenshotHandlerViaScreenshotHandlerStatic() {
        try {
            Class<?> screenshotHandlerClass = ScreenshotHandler.class;

            // Try to find a static method or field in ScreenshotHandler that can give us the current instance
            Method getDriverMethod = screenshotHandlerClass.getDeclaredMethod("getDriver");
            if (getDriverMethod != null) {
                getDriverMethod.setAccessible(true);
                Object driver = getDriverMethod.invoke(null);
                if (driver != null) {
                    // Create a new ScreenshotHandler with the driver
                    return new ScreenshotHandler((org.openqa.selenium.WebDriver) driver);
                }
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "Failed to get ScreenshotHandler via static method", e);
        }
        return null;
    }

    /**
     * Try to get ScreenshotHandler via instance field
     * This works for BaseTests with instance fields
     */
    private ScreenshotHandler tryGetScreenshotHandlerViaInstanceField(Object testInstance) {
        String[] possibleFieldNames = {
                "screenshotHandler",
                "screenshotHandlerThreadLocal",
                "threadLocalScreenshotHandler"
        };

        return tryGetHandlerFromFields(testInstance, possibleFieldNames, false);
    }

    /**
     * Try to get ScreenshotHandler via static field
     * This works for BaseTests with static fields
     */
    private ScreenshotHandler tryGetScreenshotHandlerViaStaticField(Object testInstance) {
        String[] possibleFieldNames = {
                "screenshotHandler",
                "screenshotHandlerThreadLocal",
                "threadLocalScreenshotHandler"
        };

        return tryGetHandlerFromFields(testInstance, possibleFieldNames, true);
    }

    /**
     * Generic method to try getting handler from fields (static or instance)
     */
    private ScreenshotHandler tryGetHandlerFromFields(Object testInstance, String[] fieldNames, boolean isStatic) {
        Class<?> currentClass = testInstance.getClass();

        // Check the test class and its superclasses
        while (currentClass != null && !currentClass.equals(Object.class)) {
            for (String fieldName : fieldNames) {
                try {
                    Field field = currentClass.getDeclaredField(fieldName);
                    field.setAccessible(true);

                    // For static fields, pass null as instance
                    Object fieldValue = isStatic ? field.get(null) : field.get(testInstance);

                    if (fieldValue instanceof ScreenshotHandler) {
                        return (ScreenshotHandler) fieldValue;
                    } else if (fieldValue instanceof ThreadLocal) {
                        @SuppressWarnings("unchecked")
                        ThreadLocal<ScreenshotHandler> threadLocalHandler = (ThreadLocal<ScreenshotHandler>) fieldValue;
                        ScreenshotHandler handler = threadLocalHandler.get();
                        if (handler != null) {
                            return handler;
                        }
                    }
                } catch (NoSuchFieldException e) {
                    // Field not found in this class, continue to next field or class
                    continue;
                } catch (Exception e) {
                    logger.log(Level.FINE, "Error accessing field " + fieldName + " in class " + currentClass.getSimpleName(), e);
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        return null;
    }

    /**
     * Find a method in the class hierarchy
     */
    private Method findMethodInHierarchy(Class<?> clazz, String methodName) {
        Class<?> currentClass = clazz;
        while (currentClass != null && !currentClass.equals(Object.class)) {
            try {
                return currentClass.getDeclaredMethod(methodName);
            } catch (NoSuchMethodException e) {
                // Method not found in this class, try superclass
                currentClass = currentClass.getSuperclass();
            }
        }
        return null;
    }
}