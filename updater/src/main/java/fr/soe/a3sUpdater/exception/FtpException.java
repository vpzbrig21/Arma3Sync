package fr.soe.a3sUpdater.exception;

public class FtpException extends Exception {
    public FtpException(String message) { super(message); }
    public FtpException(String message, Throwable cause) { super(message, cause); }
}
