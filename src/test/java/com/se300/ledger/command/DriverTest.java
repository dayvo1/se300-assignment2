package com.se300.ledger.command;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

/**
 * Test Driver Class for testing Blockchain
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-09-25
 */
public class DriverTest {

    /**
     * Main method to run the test
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        DriverTest test = new DriverTest();
        test.testDriver();
    }

    public void testDriver() {
        // Get the input stream for the resource
        InputStream inputStream = getClass().getResourceAsStream("/ledger.script");
        if (inputStream == null) {
            throw new RuntimeException("Could not find ledger.script in classpath");
        }

        // Process the input stream directly
        CommandProcessor processor = new CommandProcessor();
        processor.processCommandInputStream(inputStream);
    }
}
