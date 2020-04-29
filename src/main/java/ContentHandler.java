import com.mongodb.client.MongoClient;
 import com.mongodb.client.MongoClients;
 import com.mongodb.client.MongoCollection;
 import com.mongodb.client.MongoCursor;
 import com.mongodb.client.MongoDatabase;

import java.util.ArrayList;

import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * Accesses and handles our database.
 */
public class ContentHandler {
	
	MongoCollection<Document> col;
	//GridFSBucket gridFSBucket;
	
    public ContentHandler(){
		MongoClient mongoClient = MongoClients.create("mongodb+srv://cse312g20:2020sucks@team20-fb7o8.azure.mongodb.net/test?retryWrites=true&w=majority");

        //MongoDatabase csDatabase = mongoClient.getDatabase("CSE312");
        MongoDatabase csDatabase = mongoClient.getDatabase("Team20");
        col = csDatabase.getCollection("CSE312 Group");
        //gridFSBucket = GridFSBuckets.create(csDatabase);

        /*Document document = new Document("name","Vin")
        		.append("Sex","male")
        		.append("Age","20");

        col.insertOne(document);*/
        
        /*MongoCursor<Document> cur = col.find().iterator();
        while (cur.hasNext()) {
            Document doc = cur.tryNext();
            
            ObjectId id = doc.getObjectId("_id");
            String name = doc.getString("name");

            System.out.println("id: " + id);
            System.out.println("name: " + name);
        }*/
    }
    
    public void write(String name, int type, String msg, int likes, byte[] file, byte[] line2, String filename) {
    	if(type == 0) {
    		//Post Message
    		System.out.println("Write to Database: " + name + " " + msg);
    		
    		Document document = new Document("type", type)
            		.append("name", name)
            		.append("message", msg)
    				.append("line2", line2);
    		
    		col.insertOne(document);
    	}else if(type == 1) {
    		//Post File
    		System.out.println("Write to Database: " + name);
    		
    		Document document = new Document("type", type)
            		.append("name", name)
            		.append("filename", filename)
            		.append("file", file)
    				.append("line2", line2);
    		
    		col.insertOne(document);
    	}
    }
    
    /**
     * Finds a list of all unique ids for all posts and comments made by a given user
     * @param n susername
     * @return 
     */
    public ArrayList<ObjectId> getIds(String n) {
    	ArrayList<ObjectId> ids = new ArrayList<ObjectId>();
    	MongoCursor<Document> cur = col.find().iterator();
        while (cur.hasNext()) {
            Document doc = cur.tryNext();
            
            ObjectId id = doc.getObjectId("_id");
            String name = doc.getString("name");
            
            if(name.equals(n)) {
            	System.out.println("id: " + id);
                System.out.println("name: " + name);
                
                ids.add(id);
            }
        }
        return ids;
    }
    
    /**
     * Finds a list of all documents for all posts made by a given user and number
     * @param n username
     * @return 
     */
    public ArrayList<Document> getPosts(String name, int start, int end){
    	ArrayList<Document> docs = new ArrayList<Document>(end-start);
    	MongoCursor<Document> cur = col.find().iterator();
    	int counter = 0;
    	while (cur.hasNext()) {
    		Document doc = cur.tryNext();
    		String n = doc.getString("name");
    		
    		if(n.equals(name)) {
		        if(start <= counter && counter < end) {  
		            docs.add(doc);
		        }
		        counter++;
    		}
        }
    	return docs;
    }
}
