import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.regex.*;  

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageClass;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Solution_2 {

    public static void main(String []args) throws IOException {

        /**
         * The url is the endpoint where the data for the recorded files is
         */
        String url = "https://vcc-naX.8x8.com/api/recordings/files/";
        String charset = "UTF-8";

        // The Austhorisation header
        String userpass = "tenant_name" + ":" + "authentication_token";
        String basicAuth = "Basic :" + new String(Base64.getEncoder().encode(userpass.getBytes())); 

        // Establish the connection
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("Accept-Charset", charset);
        connection.setRequestProperty ("Authorization", basicAuth);  
        InputStream response = connection.getInputStream();

        Scanner sc = new Scanner(response);

        //Reading line by line from scanner to StringBuffer
        StringBuffer sb = new StringBuffer();
        while(sc.hasNext()){
            sb.append(sc.nextLine());
        }

        // This gives us a string form of the xml output
        String str = sb.toString(); 

        // Now we are going to use regex to extract the filename and the phone number from
        // the XML response
        Pattern phone = Pattern.compile("phonenumber");

        List<String> phonenumbers = new ArrayList<String>();
        Matcher m = phone.matcher(str);

        while (m.find()) {
            phonenumbers.add(m.group());
        }

        List<String> filenames = new ArrayList<String>();
        Matcher m2 = Pattern.compile("filename").matcher(str);
        
        while (m2.find()) {
            filenames.add(m2.group());
        }

        /**
         * If we use a google cloud bucket then we can create a folder for each phone number
         * in the bucket (technically it won't be a folder because that is not how data is structured in a bucket).
         * Essentially a prefix will be attached to the filename which will represent the phonenumber
         * 
         * To do this we can use a loop to rename files to this format - 
         * 
         * phone_number/filename.wav
         * 
         * and then add them to db
         * 
         * 
         * Now the files can also be added to the cloud database using the shell and gsutil
         * but in code - 
         */

        // Creating a bucket

        Storage storage = StorageOptions.newBuilder().setProjectId("projectId").build().getService();
        StorageClass storageClass = StorageClass.COLDLINE;  
        String location = "ASIA";
    
        Bucket bucket =
            storage.create(
                BucketInfo.newBuilder("bucketName")
                    .setStorageClass(storageClass)
                    .setLocation(location)
                    .build());
    
        System.out.println("Created bucket " + bucket.getName() + " in " + bucket.getLocation() + " with storage class " + bucket.getStorageClass());


    

        // Do this for each file and the files will be saved with phonenumber as folder and the recordings inside

        for (int i = 0; i < filenames.size(); i++) {
            
            // Convert input stream to file
            File targetFile = new File("somefolder/" + phonenumbers.get(i) + "/" + filenames.get(i) + ".wav");

            OutputStream outStream = new FileOutputStream(targetFile);

            byte[] buffer = new byte[8 * 1024];
            int bytesRead;
            while ((bytesRead = response.read(buffer)) != -1) {
                outStream.write(buffer, 0, bytesRead);
            }

            outStream.close();

            // Add file to bucket with phonenumber as prefix
            BlobId blobId = BlobId.of("bucketName", phonenumbers.get(i) + "/" + filenames.get(i) + ".wav");
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
            storage.create(blobInfo, Files.readAllBytes(targetFile.toPath()));
    
            System.out.println(
                "File " + targetFile + " uploaded to bucket " + "bucketName" + " as " + objectName);
    
            }
            
        }




}
