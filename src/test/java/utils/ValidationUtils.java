package utils;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;


import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for handling test validations with enhanced reporting
 * Allows tests to continue execution even when some assertions fail
 */
public class ValidationUtils {
    public List<String> validationErrors;
    private final ScreenshotHandler screenshotHandler;
    public List<String> validationSuccess;
    private final String testClassName;


    /**
     * Constructor to initialize ValidationUtils for a specific test class
     * @param testClassName Name of the test class using this utility
     */
    public ValidationUtils(WebDriver driver, String testClassName) {
        this.screenshotHandler = new ScreenshotHandler(driver);
        this.testClassName = testClassName;
        this.validationErrors = new ArrayList<>();
        this.validationSuccess = new ArrayList<>();

    }

    public ValidationUtils(String testClassName) {
        this.screenshotHandler = null;
        this.testClassName = testClassName;
        this.validationErrors = new ArrayList<>();
        this.validationSuccess = new ArrayList<>();
    }


    /**
     * Enhanced validation method that supports multiple assertion types
     * @param testName Name of the test being validated
     * @param expected Expected value (can be null for some validation types)
     * @param actual Actual value from UI/API
     * @param validationType Type of validation: "EQUALS", "NOT_NULL", "TRUE", "FALSE"
     */
    public void validationCheck(String testName, Object expected, Object actual, String validationType) {
        try {
            performAssertion(expected, actual, validationType);

            // If assertion passes, create a successful step
            Allure.step(testName + " ✅", () -> {
                String successMsg = formatSuccessMessage(testName, validationType, expected, actual);
                validationSuccess.add(successMsg);
                //Allure.addAttachment("✅ Validation Status : ", successMsg);
            });

        } catch (AssertionError e) {
            String errorMessage = formatErrorMessage(validationType, expected, actual);
            validationErrors.add(testName + " - " + errorMessage);

            // Create a failed step in Allure but DON'T let it stop the test
            try {
                Allure.step(testName + " ❌", () -> {
                    System.err.println("❌ Validation Status : " + testName + " verification failed: " + errorMessage);
                    Allure.addAttachment("Validation Failure Details", errorMessage);
                    if(screenshotHandler != null){
                        screenshotHandler.attachScreenshotToAllure(testName);
                    }
                    // Don't throw exception here - we want test to continue
                });
            } catch (Exception stepException) {
                // Catch any exception from the Allure step to prevent it from stopping the test
                // The step is now marked as failed in Allure, but the test continues
                System.err.println("⚠️ Warning: Error in Allure step, but continuing test execution");
            }
        }
    }

    /**
     * Keep original method for backward compatibility (defaults to EQUALS)
     */
    public void validationCheck(String testName, Object expected, Object actual) {
        validationCheck(testName, expected, actual, "EQUALS");
    }

    // Convenience methods for each assertion type:

    /**
     * Validate that two values are equal
     */
    public void assertEquals(String testName, Object expected, Object actual) {
        validationCheck(testName, expected, actual, "EQUALS");
    }

    /**
     * Validate that two values are equal
     */
    public void assertNotEquals(String testName, Object expected, Object actual) {
        validationCheck(testName, expected, actual, "NOT_EQUAL");
    }

    /**
     * Validate that a value is not null
     */
    public void assertNotNull(String testName, Object actual) {
        validationCheck(testName, null, actual, "NOT_NULL");
    }

    /**
     * Validate that a value is null
     */
    public void assertNull(String testName, Object actual) {
        validationCheck(testName, null, actual, "NULL");
    }

    /**
     * Validate that a value is true
     */
    public void assertTrue(String testName, Object actual) {
        validationCheck(testName, true, actual, "TRUE");
    }

    /**
     * Validate that a value is false
     */
    public void assertFalse(String testName, Object actual) {
        validationCheck(testName, false, actual, "FALSE");
    }

    // Helper methods (add these to your class):

    /**
     * Performs the actual assertion based on validation type
     */
    private void performAssertion(Object expected, Object actual, String validationType) {
        switch (validationType.toUpperCase()) {
            case "NOT_EQUAL":
                Assertions.assertNotEquals(expected, actual);
                break;
            case "EQUALS":
                Assertions.assertEquals(expected, actual);
                break;
            case "NOT_NULL":
                Assertions.assertNotNull(actual);
                break;
            case "NULL":
                Assertions.assertNull(actual);
                break;
            case "TRUE":
                Assertions.assertTrue(isTrue(actual));
                break;
            case "FALSE":
                Assertions.assertFalse(isTrue(actual));
                break;
            default:
                throw new IllegalArgumentException("Unsupported validation type: " + validationType);
        }
    }

    /**
     * Helper method to check if value represents true
     */
    private boolean isTrue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return "true".equalsIgnoreCase((String) value);
        if (value instanceof Number) return ((Number) value).doubleValue() != 0.0;
        return false;
    }

    /**
     * Formats success message based on validation type
     */
    private String formatSuccessMessage(String testName, String validationType, Object expected, Object actual) {
        return switch (validationType.toUpperCase()) {
            case "EQUALS","NOT_EQUAL" -> testName + " - verification success \nExpected: " + expected + "\nActual: " + actual;
            case "NOT_NULL", "TRUE", "FALSE", "NULL"-> testName + " - verification success \nValue : " + actual;
            default -> testName + " - verification success";
        };
    }

    /**
     * Formats error message based on validation type
     */
    private String formatErrorMessage(String validationType, Object expected, Object actual) {
        return switch (validationType.toUpperCase()) {
            case "EQUALS" -> "\nExpected: " + expected + "\nbut got: " + actual;
            case "NOT_NULL" -> "Expected value to be "+ expected+",\n but got: null";
            case "NULL" -> "Expected value to be null, but got: \n"+ actual;
            case "TRUE" -> "Expected value to be true, but got: \n" + actual;
            case "FALSE" -> "Expected value to be false, but got: \n" + actual;
            default -> "Validation failed";
        };
    }

    /**
     * Method to check if there were any validation failures and fail the test if needed
     * This should be called at the end of your test method
     * Now also creates a summary step in Allure to show overall test status
     */
    public void checkValidationResults() {
        if (!validationErrors.isEmpty()) {
            StringBuilder errorReport = new StringBuilder("\n=== VALIDATION FAILURES SUMMARY for " + testClassName + " ===\n");
            for (int i = 0; i < validationErrors.size(); i++) {
                errorReport.append((i + 1)).append(". ").append(validationErrors.get(i)).append("\n");
            }
            errorReport.append("Total failures: ").append(validationErrors.size());

            // Create a failed summary step in Allure
            try {
                Allure.step("❌ VALIDATION SUMMARY - " + validationErrors.size() + " FAILED", () -> {
                    System.err.println(errorReport.toString());

                    // Add detailed failure report as attachment
                    Allure.addAttachment("Validation Failures Summary", "text/plain", errorReport.toString());

                    // Add summary stats
                    String summaryStats = String.format(
                            "Total Validations: %d\nSuccessful: %d\nFailed: %d\nSuccess Rate: %.1f%%",
                            getTotalValidationCount(),
                            getValidationSuccessCount(),
                            getValidationErrorCount(),
                            (getValidationSuccessCount() * 100.0 / getTotalValidationCount())
                    );
                    Allure.addAttachment("Test Statistics", "text/plain", summaryStats);

                    // Throw exception to mark this summary step as failed
                    throw new AssertionError("Test failed with " + validationErrors.size() + " validation errors");
                });
            } catch (AssertionError summaryException) {
                // Don't catch this one - we want it to propagate and fail the test
                // Clear the list for next test run
                clearValidationErrors();
                throw summaryException;
            }
        } else {
            // Create a successful summary step in Allure
            Allure.step("✅ ALL VALIDATIONS PASSED - " + validationSuccess.size() + " SUCCESSFUL", () -> {
                System.out.println("\n🎉 All validations passed successfully for " + testClassName + "!");

                // Add success summary as attachment
                String successSummary = String.format(
                        "All %d validations passed successfully!\n\nSuccessful validations:\n%s",
                        validationSuccess.size(),
                        String.join("\n", validationSuccess)
                );
                Allure.addAttachment("Success Summary", "text/plain", successSummary);
            });

            // Clear the list for next test run
            clearValidationErrors();
        }
    }

    /**
     * Method to get current validation error count without failing the test
     * @return Number of validation errors accumulated
     */
    public int getValidationErrorCount() {
        return validationErrors.size();
    }

    /**
     * Method to check if there are any validation errors
     * @return true if there are validation errors, false otherwise
     */
    public boolean hasValidationErrors() {
        return !validationErrors.isEmpty();
    }

    /**
     * Method to get all validation errors as a list
     * @return List of validation error messages
     */
    public List<String> getValidationErrors() {
        return new ArrayList<>(validationErrors);
    }

    /**
     * Method to manually clear validation errors and success messages
     * Useful when you want to reset between test methods in the same class
     */
    public void clearValidationErrors() {
        validationErrors.clear();
        validationSuccess.clear();
    }

    /**
     * Method to print validation summary without failing the test
     * Shows both successful and failed validations with Allure attachments
     * Useful for logging purposes
     */
    public void printValidationSummary() {
        try{
            StringBuilder summaryReport = new StringBuilder();

            // Build the summary report
            // summaryReport.append("=== VALIDATION SUMMARY for ").append(testClassName).append(" ===\n\n");

            // Add successful validations to report
            if (!validationSuccess.isEmpty()) {
                summaryReport.append("✅ SUCCESSFUL VALIDATIONS (").append(validationSuccess.size()).append("):\n\n");
                for (int i = 0; i < validationSuccess.size(); i++) {
                    summaryReport.append(i + 1).append(". ").append(validationSuccess.get(i)).append("\n\n");
                }
                summaryReport.append("----------------------------------------------------------------------------------\n\n");
            }

            // Add failed validations to report
            if (!validationErrors.isEmpty()) {
                summaryReport.append("❌ FAILED VALIDATIONS (").append(validationErrors.size()).append("):\n\n");
                for (int i = 0; i < validationErrors.size(); i++) {
                    summaryReport.append(i + 1).append(". ").append(validationErrors.get(i)).append("\n\n");
                }
                summaryReport.append("----------------------------------------------------------------------------------\n\n");
            }

            // Add overall summary
            int totalValidations = validationSuccess.size() + validationErrors.size();
            if (totalValidations > 0) {
                summaryReport.append("📊 OVERALL SUMMARY:\n");
                summaryReport.append("   Total validations: ").append(totalValidations).append("\n");
                summaryReport.append("   Successful: ").append(validationSuccess.size()).append("\n");
                summaryReport.append("   Failed: ").append(validationErrors.size()).append("\n");
                summaryReport.append("   Success rate: ").append(String.format("%.1f%%",
                        (validationSuccess.size() * 100.0 / totalValidations))).append("\n");
            } else {
                summaryReport.append("No validations were performed.\n");
            }

            // Print to console (keep original behavior)
            System.out.println(summaryReport.toString());

            // Create Allure step with attachments
            Allure.step("📋 Validation Summary Report", () -> {
                // Add main summary as attachment
                Allure.addAttachment("Complete Validation Summary", "text/plain", summaryReport.toString());

                if(!validationErrors.isEmpty()){
                    throw new AssertionError("Test failed with " + validationErrors.size() + " validation errors");
                }
            });
        }
        catch (AssertionError summaryException){
            clearValidationErrors();
            throw summaryException;
        }

    }

    /**
     * Method to add a custom validation error manually
     * @param testName Name of the test
     * @param errorMessage Error message to add
     */
    public void addValidationError(String testName, String errorMessage) {
        validationErrors.add(testName + " - " + errorMessage);
    }

    /**
     * Method to get current validation success count
     * @return Number of successful validations accumulated
     */
    public int getValidationSuccessCount() {
        return validationSuccess.size();
    }

    /**
     * Method to get all validation success messages as a list
     * @return List of validation success messages
     */
    public List<String> getValidationSuccesses() {
        return new ArrayList<>(validationSuccess);
    }

    /**
     * Method to get total number of validations performed
     * @return Total number of validations (success + errors)
     */
    public int getTotalValidationCount() {
        return validationSuccess.size() + validationErrors.size();
    }
}