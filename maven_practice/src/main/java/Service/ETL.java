package Service;

import Bean.CustomerInfo;

import java.io.*;
import java.sql.*;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONObject;
import com.google.gson.*;

import com.couchbase.client.java.*;

public class ETL {

    public static Properties prop ;

    public static Properties readPropertiesFile(String fileName) throws IOException {
        Properties prop = null;
        try(FileInputStream fis = new FileInputStream(fileName)) {

            prop = new Properties();
            prop.load(fis);
        } catch(FileNotFoundException e) {
            e.printStackTrace();
        } catch(IOException e) {
            e.printStackTrace();
        }
        return prop;
    }

    public static Connection getDbConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(prop.getProperty("hostName"),prop.getProperty("userName"),prop.getProperty("password"));
        return conn;
    }

    public static org.json.JSONObject convertObjectToJSON(CustomerInfo cid){

        return new Gson().fromJson(new Gson().toJson(cid),JSONObject.class);

    }

    public static void pushToPOB(JSONArray jsa){
        Cluster cluster = Cluster.connect(prop.getProperty("couchbaseHostName"),
                prop.getProperty("couchbaseUserName"), prop.getProperty("couchbasePassword"));
        Bucket bucket = cluster.bucket(prop.getProperty("bucketName"));
        Collection collection = bucket.defaultCollection();

        for(int i=0;i<jsa.length();i++){
            JSONObject obj = new JSONObject(jsa.get(i));
            collection.upsert(obj.getString("custId")+obj.getString("bookName"),obj);
        }

        System.out.println("Records pushed to couchbase");
    }

    public static void main(String args[]) throws Exception {
        prop = readPropertiesFile("src/main/resources/Application.properties");
        Class.forName("oracle.jdbc.driver.OracleDriver");

        try(Connection conn = getDbConnection()){
            Statement stmt = conn.createStatement();
            //Created statement and execute it
            JSONArray custDetails = new JSONArray();

            ResultSet rs = stmt.executeQuery(prop.getProperty("extractQuery"));
            //Get the values of the record using while loop
                while(rs.next())
                {
                    CustomerInfo cid = new CustomerInfo();
                    cid.setCustId(rs.getString("CustId"));
                    cid.setBookName(rs.getString("BookName"));
                    cid.setPurchasedDate(rs.getDate("PurchasedDate"));
                    cid.setAmount(rs.getInt("Amount"));
                    cid.setLocation(rs.getString("Location"));

                    //store the values which are retrieved using ResultSet and converting into JsonObject then putting into JSON array
                    custDetails.put(convertObjectToJSON(cid));
                }

        }
        catch (SQLException e) {
            //If exception occurs catch it and exit the program
            e.printStackTrace();
        }

    }

}
