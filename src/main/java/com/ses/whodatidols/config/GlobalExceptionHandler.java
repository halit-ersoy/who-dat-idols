package com.ses.whodatidols.config;

import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ ClientAbortException.class, AsyncRequestNotUsableException.class })
    public void handleClientAbortException(Exception ex) {
        // Suppress stack trace and don't write to response to avoid "start async"
        // errors
        logger.warn("Client aborted connection (stream ended): {}", ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, String>> handleIOException(IOException ex) {
        if (isClientAbort(ex)) {
            logger.warn("Client connection broken (Broken pipe/Abort): {}", ex.getMessage());
            return null;
        }
        return handleAllExceptions(ex);
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleAllExceptions(Exception ex) {
        // Check if wrapped exception is a client abort
        if (isClientAbort(ex)) {
            logger.warn("Client connection broken (nested): {}", ex.getMessage());
            return null;
        }

        logger.error("Global exception caught", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("error", "GLOBAL_HANDLER_CAUGHT: " + ex.getMessage()));
    }

    private boolean isClientAbort(Throwable ex) {
        if (ex == null)
            return false;
        if (ex instanceof ClientAbortException || ex instanceof AsyncRequestNotUsableException) {
            return true;
        }
        if (ex instanceof IOException && ex.getMessage() != null &&
                (ex.getMessage().contains("Broken pipe") || ex.getMessage().contains("Connection reset by peer"))) {
            return true;
        }
        return isClientAbort(ex.getCause());
    }
}
