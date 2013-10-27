import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

/**
 * JAVA HTTP Server
 * This simple HTTP server supports GET and HEAD requests.
 * @author Bhumit Patel - Student ID - 12015646
 */
public class HttpServer implements Runnable
{
	//instance variables
	Socket 			csocket;
	Scanner 		scanin;
	OutputStream 	sendToClient;
    InputStream 	inputFromClient;
    
    //constructor
	HttpServer(Socket csocket) {
		this.csocket = csocket;
	}	
	/**
	 * main method creates a new HttpServer instance for each
	 * request and starts it running in a separate thread.
	 */
	public static void main(String args[]) throws Exception	  {
			if( args.length != 1 ) {
		      System.out.println("Usage: HTTPServer port");
		      System.exit(1); // exit if port is not provided.
			}
			try {
			    ServerSocket serverSock = new ServerSocket(Integer.parseInt(args[0])); //create a socket on the port
			    //listen until user halts execution
			    while(true) {
			      Socket sockConn = serverSock.accept();
			      new Thread(new HttpServer(sockConn)).start(); //instantiate HttpServer and start thread
			    }
			}catch(IOException e)	{
			      System.err.println("HTTPServer: Error on socket");
			      System.exit(1);
			}		    
	}
	
	/* @override
	 * run method services each request in separate thread
	 */
	public void run() {
		try	{
				sendToClient = csocket.getOutputStream(); // Output stream to client
				inputFromClient = csocket.getInputStream(); // Input stream from client
				//variables to log in log.txt file
			    String hostAddress,command,resource,protocol,statusCode,contentType,fileName;
			    long sizeOfFile = 0;
		        hostAddress = csocket.getInetAddress().getHostAddress(); //Host address which is connected to socket.
		        String line=null;
		        int nLines=0;
		        scanin = new Scanner(inputFromClient); // scan the input stream
		        String[] clientReqLines = new String[32]; // Store the client request lines
		        //Scan until there is no more input from client
		        while (true) {
			         line = scanin.nextLine(); 
			         if(line.length()==0) break;
			         clientReqLines[nLines] = line; 
			         nLines = nLines + 1;       
		        }			      
			      Scanner reqScan = new Scanner(clientReqLines[0]); // get first line of request from the client
				  command = reqScan.next(); // extract the command from the input
				  resource = reqScan.next(); // extract the resource request 
				  protocol = reqScan.next(); // extract the protocol of the request.
				  contentType = getMimeType(resource); // get the MIME type of requested file

			      fileName ="";		      
			      if(resource.startsWith("/")) { // see if requested resource is valid
			    	  if(command.equals("PUT") || command.equals("DELETE") || command.equals("TRACE")) { //methods other than GET and HEAD are not implemented
			    		    statusCode = "405";
			    		    String reply="HTTP/1.0 405 Method Not Allowed\r\n" +
			    		    	  "Connection: close\r\n" +
			                      "Allow: GET, HEAD\r\n" +
			                      "Date: " + getServerTime() + "\r\n" +
			                      "Content-Type: "+ contentType + "\r\n" +
			                      "\r\n" +
			                      "<h1>Method Not Allowed</h1>\r\n";
			    		    sendToClient.write(reply.getBytes()); // send reply to the client
			    	  }
			    	  else if(command.equals("GET") || command.equals("HEAD")) { // if the command is valid
			    	      fileName = "www" + resource; // Concatenate resource to the default www directory
			    	      File ifile=new File(fileName); // create a new file object with that file name
			    	      if(resource.endsWith("/")) { // if the resource is a directory
			    	    	  String fName = "www"+resource+"index.html"; // temporary file object to check the directory for index.html 
			    	    	  String fName2 = "www"+resource+"index.htm";
			    	    	  File tempFile = new File(fName);
			    	    	  File tempFile2 = new File(fName2);
			    	    	  if(ifile.isDirectory()) { // if the requested resource is a directory 
			    	    		  if(tempFile.exists()) { // and it has index.html 
			    	    			  ifile = new File(fName); // send index.html to the client
			    	    		  }
			    	    		  else if(tempFile2.exists()) { // if it has index.htm
			    	    			  ifile = new File(fName2); // send index.htm to the client
			    	    		  }
			    	    	  }		    	    	  
			    	      }		    	      
			    		  if(!ifile.exists()) {	// if the file is not in the directory 	    			  
			    			  statusCode = "404"; // send this for HEAD request with out the body
			    			  String reply="HTTP/1.0 404 Not Found\r\n" +
			      	                   "Connection: close\r\n" +
			      	                   "Date: " + getServerTime() + "\r\n" +
			      	                   "Content-Type: "+ contentType + "\r\n" +
			      	                   "\r\n";
			      	                   
			    	          if(command.equals("GET")) { // send the headers with the body if the command is GET
			    	        	  	reply = reply + "<h1>File Not Found</h1>\r\n"; 
			    	          }
			    	          sendToClient.write(reply.getBytes());    			  
			    	        }    		  
			    		  else{
			    			  InputStream fins=new FileInputStream(ifile); // open file input stream
			    	          byte fileContents[] = new byte[512];
			    	          sizeOfFile = ifile.length(); // size of file
			    	          statusCode = "200";
			    			  String reply="HTTP/1.0 200 OK\r\n" + //HEADERS
			    	                   "Connection: close\r\n" +
			    	                   "Date: " + getServerTime() + "\r\n" +
			    	                   "Content-Length: " + ifile.length() + "\r\n" +
			    	                   "Content-Type: "+ contentType + "\r\n" +
			    	                   "\r\n";
			    			     sendToClient.write(reply.getBytes()); // Send Headers
			    			     if(command.equals("GET")) { // if command is get send the contents of the file
			    			    	 while(true) {
				    			             int rc = fins.read(fileContents,0,512);
				    			             if (rc <= 0) break;
				    			             sendToClient.write(fileContents,0,rc);		    			             
			    			    	 }
			    			    	 fins.close(); //close file input 
			    			     }
			    		  }
			    	  }
			    	  else {
			    		    statusCode = "501";
			    		    String reply="HTTP/1.0 501 Method Not Implemented\r\n" + // Send method not implemented headers 
			                      "Connection: close\r\n" +
			                      "Date: " + getServerTime() + "\r\n" +
			                      "Allow: GET, HEAD\r\n" + "\r\n" +
			                      "Content-Type: "+ contentType + "\r\n" +
			                      "<h1>Method Not Implemented</h1>\r\n";
			    		    sendToClient.write(reply.getBytes());
			    	  	   }
			      }
			      else {
			    	  	 statusCode = "400"; 
			    	     String reply="HTTP/1.0 400 Bad Request\r\n" + //send bad request headers
			                   "Connection: close\r\n" +
			                   "Date: " + getServerTime() + "\r\n" +
			                   "Content-Type: "+ contentType + "\r\n" +
			                   "\r\n" +
			                   "<h1>Bad Request</h1>\r\n";
			    	     sendToClient.write(reply.getBytes());
			    	     
			           }		      
					 SimpleDateFormat df = new SimpleDateFormat("dd/MMM/yyyy:HH:mm:ss Z"); //date format for the log file (Apache server format)
				     String dateStr = df.format(new Date());

				     File newlogfile = new File("log.txt"); // create log file
				     if(!newlogfile.exists()) { // if the log file doesn't exist 
				          newlogfile.createNewFile(); // create new file
				     }
				     try {
				    	 	FileOutputStream file = new FileOutputStream(newlogfile,true); // open file output stream for the log file
				    	 	PrintStream writeTo = new PrintStream(file); // printstream to write to the file
				    	 	if(statusCode.equals("200")) { // if the response is 200 - add details with size of file
				    	 		writeTo.println(hostAddress+ " - " + " - " + "["+dateStr+"] " + '"'+ command +" " + 
				            					fileName + " "+ protocol + '"' + " "+ statusCode + " " + sizeOfFile);
				    	 	}
				       else {
				    	   		writeTo.println(hostAddress+ " - " + " - " + "["+dateStr+"] " + '"'+ command +" " + 
				    	   						fileName + " "+ protocol + '"' + " " + statusCode + " " + "-");
				        }
				        writeTo.flush(); // flush the contents to file
				        writeTo.close(); // close the printstream
				     	}catch(IOException e) {
				        e.printStackTrace();
				    }
				sendToClient.close(); // close the output stream to the host
				inputFromClient.close(); // close the input stream from the host
				csocket.close(); // close the socket
			}
			catch(IOException e) {
				e.printStackTrace();	
			    System.err.println("HTTPServer: error reading socket");
			    System.exit(1);
			}
		}
	/**
	 * @param resource the file whose MIME type is to be detected
	 * @return contentType returns the extension of file
	 */
	private String getMimeType(String resource) {
		String contentType;
		String extension = resource.substring(resource.lastIndexOf(".") + 1, resource.length());
		  if(extension.equals("jpeg") || extension.equals("jpg")) {
		      contentType = "image/jpeg";
		  }
		  else if(extension.equals("html") || extension.equals("htm") || resource.endsWith("/")) {
		      contentType = "text/html";
		  }
		  else {
		      contentType = "text/plain";
		  }
		return contentType;
	}
	/**
	 * @return String representation of the time for the header DATE
	 */
	public static String getServerTime() {
	    Calendar calendar = Calendar.getInstance();
	    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
	    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	    return dateFormat.format(calendar.getTime());
    }
}
