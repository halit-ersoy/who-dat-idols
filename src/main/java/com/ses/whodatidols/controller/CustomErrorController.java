package com.ses.whodatidols.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public Object handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        try {
            if (status != null) {
                Integer statusCode = Integer.valueOf(status.toString());
                if (statusCode == 404) {
                    return "redirect:/";
                }
            }

            Resource htmlPage = new ClassPathResource("static/error/html/error.html");
            if (!htmlPage.exists()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
            if (status != null) {
                httpStatus = HttpStatus.valueOf(Integer.parseInt(status.toString()));
            }

            return ResponseEntity.status(httpStatus)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                    .body(htmlPage);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
