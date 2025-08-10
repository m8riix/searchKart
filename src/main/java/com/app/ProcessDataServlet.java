package com.app;

import java.io.IOException;
import java.io.PrintWriter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ProcessDataServlet extends HttpServlet {

    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        String jsonData = (String) req.getAttribute("jsonData");

        System.out.println(jsonData);
        // Set the content type to JSON
        res.setContentType("application/json");

        // Send the JSON data to the response
        res.getWriter().write(jsonData);
        
    }
}

