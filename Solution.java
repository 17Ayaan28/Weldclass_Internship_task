import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;
import java.util.regex.*;  

/**
 * This is some code to show how I would implement the first part
 * of the integration problem of extracting data from 8x8 and 
 * then adding it to a cloud database.
 * 
 * My understanding of the 8x8 endpoint provided in the document is 
 * that it doesn't return the actual contents of the audio file, but it gives the filename and 
 * other details such as start time, agentID etc (I am not too sure about this, I would have easily
 * confirmed if I could actually use the API). Either way if this is the case then
 * after we get the filename we just need to make another API call (to some other endpoint) to get the actual file.
 * If I am wrong then the contents of the audio file can be extracted using regex from the response itself.
 * 
 * https://8x8gateway-8x8apis.apigee.io/docs/css/1/overview -- on this page I found an endpoint that 
 * allows to query the file itself - 
 * 
 * https://api.8x8.com/storage/ap/v3/callrecording - this endpoint can be used to get the actual recording,
 * by giving the filename
 * 
 */

public class Solution {

    public static void main(String []args) throws IOException {

        /**
         * The url is the endpoint where the data for the recorded files is
         */
        String url = "https://vcc-naX.8x8.com/api/recordings/files/";
        String charset = "UTF-8";

        // The Austhorisation header, the tenant name and auth token are required to make the call
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

        ArrayList<String> filenames = new ArrayList<String>();
        Matcher m2 = Pattern.compile("filename").matcher(str);
        
        while (m2.find()) {
            filenames.add(m2.group());
        }

        /**
         * Now, we have 2 lists
         * One contains all the phone numbers
         * the other contains the corressponding call recordings
         * using a trivial regex replace we can also remove the tags <phonenumber> and <filename>
         * from these strings
         */

        
        //This loop can be used to make the API call to the storage and get the actual recorded file, we can use the fiilename
        // as a query parameter

        List<byte[]> r_file_data = new ArrayList<byte[]>();

        for (int i = 0; i < filenames.size(); i++) {
            
            URLConnection con = new URL("https://api.8x8.com/storage/ap/v3/callrecording?relativePath="+ filenames.get(i)).openConnection();
            con.setRequestProperty("Accept-Charset", charset);
            InputStream res = connection.getInputStream();

            byte[] bytes = new byte[1000];
            res.read(bytes);
            r_file_data.add(bytes);
        }
        
        // Once this loop is done we have all the recorded files in byte arrays and the associated phone numbers

        // If we use a google firebase database(a JSON based no SQL db), then we can do it as shown below
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("server/saving-data/fireblog");

        DatabaseReference recordref = ref.child("callrecords");

        for (int i = 0; i < filenames.size(); i++) {
            recordref.setValueAsync(phonenumbers.get(i), r_file_data.get(i));
        }

    }


}

