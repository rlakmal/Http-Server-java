Java Web Server
This is a simple Java web server that serves web content from a specified directory and can handle PHP files and POST requests.

Prerequisites
Before running the server, ensure you have the following installed:

Java Development Kit (JDK)
PHP (for handling PHP files)
Usage
Clone this repository to your local machine.

Navigate to the project directory.

Compile the Java code:

bash
Copy code
javac server.java
Start the server:

bash
Copy code
java server
The server will start and listen for incoming connections on http://127.0.0.1:2728 by default. You can change the base directory and port in the server.java file.

Place your web content files in the htdocs directory. The server will serve files from this directory.

Access the web content in your browser by navigating to http://127.0.0.1:2728 or the appropriate URL based on your configuration.

Features
Handles static files (HTML, CSS, JS, etc.) and serves them directly.
Supports PHP files (.php) and executes them using the PHP command-line interpreter.
Handles POST requests to PHP files, parsing and processing the data.
Handles directory requests with default index files (e.g., index.php, index.html, add.html).
Returns appropriate HTTP status codes (e.g., 404 Not Found, 403 Forbidden).
Configuration
You can configure the server by modifying the server.java file:

Set the base variable to specify the base directory for serving web content.
Change the port variable to specify the server port.
