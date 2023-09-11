
import java.io.*;
import java.net.*;

public class server {
    public static void main(String[] args) {
        // Define the base directory for serving web content
        String base = "htdocs";

        // Specify the server port
        int port = 2728;

        ServerSocket serverSocket = null;

        try {
            // Create a ServerSocket bound to the specified port
            serverSocket = new ServerSocket(port);
            System.out.println("Server is running on http://127.0.0.1:" + port);

            // Continuously listen for incoming client connections
            while (true) {
                Socket clientSocket = serverSocket.accept(); // Accept incoming connection
                Thread clientThread = new ClientHandler(clientSocket, base); // Create a thread to handle the client
                clientThread.start(); // Start the client handling thread
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (serverSocket != null) {
                    serverSocket.close(); // Close the server socket
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final String baseDirectory;

    public ClientHandler(Socket clientSocket, String baseDirectory) {
        this.clientSocket = clientSocket;
        this.baseDirectory = baseDirectory;
    }

    @Override
    public void run() {
        try {
            // Set up input and output streams for communication with the client
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            OutputStream out = clientSocket.getOutputStream();

            // Read the HTTP request line from the client
            String requestLine = in.readLine();
            String[] requestParts = requestLine.split(" ");
            String requestType = requestParts[0];
            String requestPath = requestParts[1];
            String parameters = "";

            // Parse any query parameters if present in the request
            if (requestPath.contains("?")) {
                String[] pathParts = requestPath.split("\\?");
                requestPath = pathParts[0];
                parameters = pathParts[1];
            }

            // Construct the file location based on the base directory and request path
            String fileLocation = baseDirectory + File.separator + requestPath.substring(1);

            File file = new File(fileLocation);

            // Check if the requested file exists and is within the base directory
            if (file.exists() && fileLocation.startsWith(baseDirectory)) {
                // Handle directory requests with a default index file
                if (file.isDirectory()) {
                    String[] indexFiles = { "index.php", "index.html", "add.html" };
                    boolean found = false;

                    // Try to find an index file in the directory
                    for (String indexFile : indexFiles) {
                        File indexFileLocation = new File(fileLocation, indexFile);

                        if (indexFileLocation.exists()) {
                            file = indexFileLocation;
                            found = true;
                            break;
                        }
                    }

                    // If no index file is found, return a 404 response
                    if (!found) {
                        out.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
                        clientSocket.close();
                        return;
                    }
                }

                // Handle PHP files
                if (!file.isDirectory() && file.getName().endsWith(".php")) {
                    // Handle POST requests to PHP files
                    String phpText = "";
                    String phpCode = "";

                    if (requestType.equals("POST")) {
                        // Read and process POST data
                        StringBuilder postData = new StringBuilder();
                        int contentLength = 0;

                        // Read the content length from the headers
                        String line;
                        while ((line = in.readLine()) != null && !line.isEmpty()) {
                            if (line.startsWith("Content-Length:")) {
                                contentLength = Integer.parseInt(line.substring(16).trim());
                            }
                        }

                        // Read the POST data
                        char[] postDataBuffer = new char[contentLength];
                        in.read(postDataBuffer, 0, contentLength);
                        postData.append(postDataBuffer);

                        // Process the POST data and generate PHP code
                        String[] postDataArray = postData.toString().split("&");
                        phpText = "<?php " + convertToPhpArray(postDataArray) + "\n $_POST = $data; ?> ";
                    }

                    // Read the PHP code from the file
                    try {
                        BufferedReader fileReader = new BufferedReader(new FileReader(file));
                        String line;
                        while ((line = fileReader.readLine()) != null) {
                            phpCode += line + "\n";
                        }
                        fileReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // Create a temporary PHP file with combined POST data and PHP code
                    String tempFilePath = file.getParent() + File.separator + ".temp_" + file.getName();
                    FileWriter tempFileWriter = new FileWriter(tempFilePath);
                    tempFileWriter.write(phpText + phpCode);
                    tempFileWriter.close();

                    // Execute the PHP script using the PHP command-line interpreter
                    ProcessBuilder processBuilder = new ProcessBuilder("php", tempFilePath);
                    Process process = processBuilder.start();
                    BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    StringBuilder phpOutput = new StringBuilder();
                    String line;

                    // Read and capture the PHP script's output
                    while ((line = processOutput.readLine()) != null) {
                        phpOutput.append(line).append("\n");
                    }

                    processOutput.close();

                    // Delete the temporary PHP file
                    File tempFile = new File(tempFilePath);
                    if (tempFile.exists()) {
                        tempFile.delete();
                    }

                    // Send a 200 OK response with the PHP script's output
                    out.write(("HTTP/1.1 200 OK\r\n\r\n" + phpOutput.toString()).getBytes());
                } else {
                    // Serve non-PHP files by reading and sending their content
                    try {
                        FileInputStream fileInputStream = new FileInputStream(file);
                        byte[] buffer = new byte[1024];
                        int bytesRead;

                        out.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());

                        // Read and send the file's content in chunks
                        while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }

                        fileInputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                // Handle requests for files outside the base directory with a 403 response
                out.write("HTTP/1.1 403 Forbidden\r\n\r\n".getBytes());
            }

            // Close input and output streams and the client socket
            in.close();
            out.close();
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper method to convert POST data into PHP array format
    private String convertToPhpArray(String[] data) {
        StringBuilder phpString = new StringBuilder("$data = array(\n");

        for (String item : data) {
            String[] keyValue = item.split("=");
            phpString.append("    '").append(keyValue[0]).append("' => '").append(keyValue[1]).append("',\n");
        }

        phpString.append(");");
        return phpString.toString();
    }
}