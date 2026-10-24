package com.ses.whodatidols.config;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({ ClientAbortException.class, AsyncRequestNotUsableException.class })
    public void handleClientAbortException(Exception ex) {
        // Suppress stack trace and don't write to response to avoid "start async" errors
        logger.warn("Client aborted connection (stream ended): {}", ex.getMessage());
    }

    @ExceptionHandler(IOException.class)
    public Object handleIOException(IOException ex, HttpServletRequest request) {
        if (isClientAbort(ex)) {
            logger.warn("Client connection broken (Broken pipe/Abort): {}", ex.getMessage());
            return null;
        }
        return handleAllExceptions(ex, request);
    }

    @ExceptionHandler(Exception.class)
    public Object handleAllExceptions(Exception ex, HttpServletRequest request) {
        // Check if wrapped exception is a client abort
        if (isClientAbort(ex)) {
            logger.warn("Client connection broken (nested): {}", ex.getMessage());
            return null;
        }

        // Determine real HTTP status code
        HttpStatusCode status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex instanceof NoResourceFoundException || ex instanceof NoHandlerFoundException) {
            status = HttpStatus.NOT_FOUND;
        } else if (ex instanceof ResponseStatusException rse) {
            status = rse.getStatusCode();
        } else if (ex instanceof ErrorResponse er) {
            status = er.getStatusCode();
        }

        String uri = request != null ? request.getRequestURI() : "unknown";

        // Log appropriately: 404 as debug/info, 5xx as error with stack trace
        if (status.value() == 404) {
            logger.debug("Resource not found (404) for URI: {}", uri);
        } else if (status.is4xxClientError()) {
            logger.info("Client error ({}) for URI: {}: {}", status.value(), uri, ex.getMessage());
        } else {
            logger.error("Internal server error (500) caught for URI: {}", uri, ex);
        }

        String accept = request != null ? request.getHeader(HttpHeaders.ACCEPT) : null;
        boolean isHtmlRequest = (accept != null && accept.contains(MediaType.TEXT_HTML_VALUE))
                || (!uri.startsWith("/api/"));

        // For browser/crawler requests, return the branded error HTML page with proper status code
        if (isHtmlRequest) {
            try {
                Resource htmlPage = new ClassPathResource("static/error/html/error.html");
                if (htmlPage.exists()) {
                    return ResponseEntity.status(status)
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                            .body(htmlPage);
                }
            } catch (Exception ignored) {
            }
            return ResponseEntity.status(status).build();
        }

        // For API requests, return a clean JSON response without leaking stack traces or internal details
        String errorName = status.value() == 404 ? "Not Found" :
                (status.is5xxServerError() ? "Internal Server Error" : "Bad Request");

        return ResponseEntity
                .status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("status", status.value(), "error", errorName));
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
