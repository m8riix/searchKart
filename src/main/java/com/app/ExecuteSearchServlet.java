package com.app;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

@MultipartConfig
public class ExecuteSearchServlet extends HttpServlet {

    private WebDriver driver;
 
    public void service(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        System.out.println("hi");

        Part filePart = req.getPart("fileInput");
        String fileName = getSubmittedFileName(filePart);
 
        // Save the uploaded file to a location (modify the path as needed)
        Path filePath = saveFileToServer(filePart);

        try {
            String jsonResponse = executeSelenium(filePath, req, res);

            // Store JSON data in request attribute
            req.setAttribute("jsonData", jsonResponse);

            //System.out.println(jsonResponse);
            // Forward the request to ProcessDataServlet
            req.getRequestDispatcher("/ProcessDataServlet").forward(req, res);

            
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private Path saveFileToServer(Part part) throws IOException {
        String userDirectory = System.getProperty("user.home");  // Gets the user's home directory

        // Extract the actual file name from the submitted part
        String fileName = getSubmittedFileName(part);

        Path targetPath = Path.of(userDirectory, fileName);

        if (part != null) {
            try (InputStream fileContent = part.getInputStream()) {
                // Save the file to the specified location
                Files.copy(fileContent, targetPath, StandardCopyOption.REPLACE_EXISTING);
                return targetPath;
            }
        } else {
            // Log an error message or handle the situation based on your requirements
            System.err.println("Error: Part object is null. File not saved.");
            return null; // Or throw an exception or handle it as needed
        }
    }

    private String getSubmittedFileName(Part part) {
        if (part != null) {
            for (String cd : part.getHeader("content-disposition").split(";")) {
                if (cd.trim().startsWith("filename")) {
                    String fileName = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                    return fileName.substring(fileName.lastIndexOf('/') + 1).substring(fileName.lastIndexOf('\\') + 1); // MSIE fix.
                }
            }
        }

        // If the part is null or no filename found, return an empty string or a default value
        return "";
    }

    private String executeSelenium(Path filePath, HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException, InterruptedException {
        System.setProperty("webdriver.chrome.driver", "C:\\STS\\chromedriver-win64\\chromedriver.exe");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("disable-extensions");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--disable-extensions");
        options.addArguments("--proxy-server='direct://'");
        options.addArguments("--proxy-bypass-list=*");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--no-sandbox");
        //options.addArguments("window-size=1920x1080");// Add this line to run in headless mode
        driver = new ChromeDriver(options);
        String updatedUrl = "";
        try {
            // Navigate to Google
            driver.get("https://www.google.com/");

            WebDriverWait wait = new WebDriverWait(driver, 2);
            WebElement cameraIcon = wait.until(ExpectedConditions.elementToBeClickable(By.className("nDcEnd")));

            // Find the camera/lens icon using its class name
            cameraIcon = driver.findElement(By.className("nDcEnd"));

            // Click on the camera/lens icon
            cameraIcon.click();

            //Thread.sleep(5000);
            // Find the upload button within the camera/lens icon
            //WebElement uploadButton = driver.findElement(By.xpath("//span[text()='upload a file']"));

            // Click on the upload button
            //uploadButton.click();
            
         // Find the upload button within the camera/lens icon using JavaScript
            JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
            jsExecutor.executeScript("document.querySelector('span[jsname=\"tAPGc\"]').click();");

            // Locate the file input element using JavaScript
            WebElement fileInput = (WebElement) jsExecutor.executeScript("return document.querySelector('input[type=\"file\"]');");
            
            // Handle any additional steps (if needed)
            fileInput.sendKeys(filePath.toAbsolutePath().toString());
            
            //sleep
            Thread.sleep(5000);
            
            // Retrieve the page source using Selenium
            String pageSource = driver.getPageSource();
            
            // Use Jsoup to parse the page source
            Document document = Jsoup.parse(pageSource);

            // Select all div elements with the class "G19kAf ENn9pd"
            Elements productDivs = document.select("div.G19kAf.ENn9pd");

            // Create a JSONArray to store the product details
            JSONArray productsArray = new JSONArray();

            // Iterate through each product div
            for (Element productDiv : productDivs) {
                // Extract product details
                String title = productDiv.select("div[data-item-title]").attr("data-item-title");
                String imageUrl = productDiv.select("div[data-thumbnail-url]").attr("data-thumbnail-url");
                String sourceDomain = productDiv.select("span.fjbPGe").text();
                String price = productDiv.select("span.DdKZJb").text();
                String availability = productDiv.select("span.Bc59rd").text();
                String productLink = productDiv.select("div[data-action-url]").attr("data-action-url");

                // Create a JSONObject for each product
                JSONObject productObject = new JSONObject();
                productObject.put("title", title);
                productObject.put("imageUrl", imageUrl);
                productObject.put("sourceDomain", sourceDomain);
                productObject.put("price", price);
                productObject.put("availability", availability);
                productObject.put("productLink", productLink);

                // Add the product object to the JSONArray
                productsArray.put(productObject);
            }


            // Convert the JSONArray to a JSON string
            return productsArray.toString();
        } finally {
            // Close the browser window
            driver.quit();
        }
    }
}
