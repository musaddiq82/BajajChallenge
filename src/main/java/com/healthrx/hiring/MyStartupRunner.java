package com.healthrx.hiring;

import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Component
public class MyStartupRunner implements CommandLineRunner {

    // Use RestTemplate to make web requests
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Application started, beginning the webhook process...");

        try {
            // --- STEP 1: Generate the Webhook ---
            String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            // Create the request body with your details
            // IMPORTANT: Replace these values with your actual details!
            WebhookRequest initialRequest = new WebhookRequest(
                    "John Doe",          // Replace with your name
                    "REG12347",          // Replace with your registration number
                    "john@example.com"   // Replace with your email
            );

            // Set headers for the request
            HttpHeaders initialHeaders = new HttpHeaders();
            initialHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<WebhookRequest> initialEntity = new HttpEntity<>(initialRequest, initialHeaders);

            // Send the POST request and get the response
            System.out.println("Sending registration request to: " + generateUrl);
            ResponseEntity<WebhookResponse> response = restTemplate.exchange(
                    generateUrl,
                    HttpMethod.POST,
                    initialEntity,
                    WebhookResponse.class
            );

            // --- STEP 2: Extract Webhook URL and Access Token from the Response ---
            WebhookResponse responseBody = response.getBody();
            if (responseBody == null) {
                throw new IllegalStateException("Received an empty response from the server.");
            }

            String webhookUrl = responseBody.getWebhook();
            String accessToken = responseBody.getAccessToken();

            System.out.println("Successfully received webhook URL: " + webhookUrl);
            System.out.println("Successfully received access token.");


            // --- STEP 3: Define the SQL Query ---
            String finalQuery = "SELECT p.AMOUNT AS SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, TIMESTAMPDIFF(YEAR, e.DOB, CURDATE()) AS AGE, d.DEPARTMENT_NAME FROM PAYMENTS p JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID WHERE DAY(p.PAYMENT_TIME) <> 1 ORDER BY p.AMOUNT DESC LIMIT 1;";
            System.out.println("Final SQL Query prepared.");


            // --- STEP 4: Submit the Solution to the Webhook URL ---
            String submitUrl = webhookUrl; // The URL we received

            // Create the request body for the solution
            Map<String, String> solutionBody = Collections.singletonMap("finalQuery", finalQuery);

            // Set headers for the submission, including the JWT Authorization token
            HttpHeaders solutionHeaders = new HttpHeaders();
            solutionHeaders.setContentType(MediaType.APPLICATION_JSON);
            solutionHeaders.set("Authorization", accessToken); // This is the JWT token

            HttpEntity<Map<String, String>> solutionEntity = new HttpEntity<>(solutionBody, solutionHeaders);

            // Send the final POST request
            System.out.println("Submitting final query to: " + submitUrl);
            ResponseEntity<String> finalResponse = restTemplate.exchange(
                    submitUrl,
                    HttpMethod.POST,
                    solutionEntity,
                    String.class
            );

            // --- STEP 5: Print the Final Result ---
            System.out.println("Submission successful!");
            System.out.println("Server response: " + finalResponse.getBody());
            System.out.println("Process finished successfully.");

        } catch (Exception e) {
            System.err.println("An error occurred during the process: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

// Helper class for the first request body
class WebhookRequest {
    private String name;
    private String regNo;
    private String email;

    public WebhookRequest(String name, String regNo, String email) {
        this.name = name;
        this.regNo = regNo;
        this.email = email;
    }

    // Getters are needed for JSON serialization
    public String getName() { return name; }
    public String getRegNo() { return regNo; }
    public String getEmail() { return email; }
}

// Helper class to map the response from the first request
class WebhookResponse {
    private String webhook;
    private String accessToken;

    // Getters and setters are needed for JSON deserialization
    public String getWebhook() { return webhook; }
    public void setWebhook(String webhook) { this.webhook = webhook; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}
