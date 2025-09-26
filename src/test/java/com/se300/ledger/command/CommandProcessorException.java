package com.se300.ledger.command;

/**
 * CommandProcessorException class implementation designed display errors to the user while
 * processing commands
 *
 * @author  Sergey L. Sundukovskiy
 * @version 1.0
 * @since   2025-09-25
 */
public class CommandProcessorException extends Exception {

    private String command;
    private String reason;
    private int lineNumber;

    /**
     * Exception Constructor
     * @param command
     * @param reason
     */
    public CommandProcessorException(String command, String reason) {
        this.command = command;
        this.reason = reason;
    }

    /**
     * Getter method for command
     * @return
     */
    public String getCommand() {
        return command;
    }

    /**
     * Setter method for command
     * @return
     */
    public String getReason() {
        return reason;
    }

    /**
     * Getter method for line number
     * @return
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Setter method for command
     * @param command
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * Setter method for reason
     * @param reason
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Setter method for line number
     * @param lineNumber
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
