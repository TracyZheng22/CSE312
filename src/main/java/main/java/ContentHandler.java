package main.java;

import com.mongodb.client.MongoClient;
 import com.mongodb.client.MongoClients;
 import com.mongodb.client.MongoCollection;
 import com.mongodb.client.MongoCursor;
 import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;

import static com.mongodb.client.model.Filters.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;

/**
 * Accesses and handles our database.
 */
public class ContentHandler {
	MongoDatabase csDatabase;
	MongoCollection<Document> col;
	MongoCollection<Document> sec;
	//GridFSBucket gridFSBucket;
	
    public ContentHandler(){
		MongoClient mongoClient = MongoClients.create("mongodb+srv://cse312g20:2020sucks@team20-fb7o8.azure.mongodb.net/test?retryWrites=true&w=majority");

        //MongoDatabase csDatabase = mongoClient.getDatabase("CSE312");
        csDatabase = mongoClient.getDatabase("Team20");
        col = csDatabase.getCollection("CSE312 Group");
        sec = csDatabase.getCollection("Secure");
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
    
    /**
     * Writes salted hash and username to database
     * @param username
     * @param salted_hash
     */
    public void secureWrite(String username, byte[] salted_hash, byte[] salt) {
    	System.out.println("Write to Secure! Contents Hidden For Safety");
    	Document document = new Document("username", username)
    			.append("password", salted_hash)
    			.append("salt", salt);
    	sec.insertOne(document);
    }
    
    /**
     * Gets credentials from secure collection
     * @param username
     * @return
     */
    public Document getCredentials(String username) {
    	Document document = sec.find(eq("username", username)).first();
    	return document;
    }
    
    public ArrayList<String> getFriends(String username){
    	ArrayList<String> fl = (ArrayList<String>) sec.find(eq("username", username)).first().get("friends");
    	return fl;
    }
    
    /**
     * Writes document to base collection
     * 
     * @param name
     * @param type
     * @param msg
     * @param likes
     * @param file
     * @param line2
     * @param filename
     * @return
     */
    public Document write(String name, int type, String msg, int likes, byte[] file, byte[] line2, String filename) {
    	Document document = null;
    	if(type == 0) {
    		//Post Message
    		System.out.println("Write to Database: " + name + " " + msg);
    		
    		document = new Document("type", type)
            		.append("name", name)
            		.append("message", msg)
            		.append("likes", likes)
    				.append("line2", line2);
    	}else if(type == 1) {
    		//Post File
    		System.out.println("Write to Database: " + name);
    		
    		document = new Document("type", type)
            		.append("name", name)
            		.append("filename", filename)
            		.append("file", file)
            		.append("likes", likes)
    				.append("line2", line2);
    	}else if(type == 3) {
    		System.out.println("Write to Database: " + name + " " + msg);
    		
    		document = new Document("type", type)
    		.append("name", name)
    		.append("message", msg)
    		.append("likes", likes)
    		.append("file", file)
			.append("line2", line2);
    	}
    	col.insertOne(document);
    	return document;
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
     * Likes a specific post
     * @param _id
     */
    public int like(byte[] _id) {
    	ObjectId objid = new ObjectId(_id);
    	Document doc = col.find(eq("_id", objid)).first();
    	int likes = doc.getInteger("likes");
    	
    	col.updateOne(eq("_id", objid), new Document("$set", new Document("likes", likes+1)));
    	
    	doc = col.find(eq("_id", objid)).first();
    	return doc.getInteger("likes");
    }
    
    /**
     * Finds a list of all documents for all posts made by a given user and number
     * @param n username
     * @param start
     * @param end
     * @return 
     */
    public ArrayList<Document> getPosts(String name, int start, int end){
    	ArrayList<Document> docs = new ArrayList<Document>();   //end-start);
    	MongoCursor<Document> cur = col.find().iterator();
    	int counter = 0;
    	while (cur.hasNext()) {
    		Document doc = cur.tryNext();
    		String n = doc.getString("name");
    		
    		if(n.equals(name)) {
    			//Number temporarily removed due to deadline
		        //if(start <= counter && counter < end) {  
	        	System.out.println(doc.getInteger("type"));
	            docs.add(doc);
		        //}
		        counter++;
    		}
        }
    	return docs;
    }
    
    /**
     * Check if a given username exists on the system
     * @param username
     * @return
     */
    public boolean userExists(String username){
		MongoCursor<Document> cur = sec.find().iterator();
		while(cur.hasNext()) {
			Document doc = cur.tryNext();
			String n = doc.getString("username");
			if(username.equals(n)) {
				return true;
			}
		}
		return false;
    }
    
    /**
     * Finds a list of all documents for all posts given an interval
     * 
     * @param start
     * @param end
     * @return
     */
    public ArrayList<Document> getPosts(int start, int end){
    	ArrayList<Document> docs = new ArrayList<Document>(end-start);
    	MongoCursor<Document> cur = col.find().iterator();
    	int counter = 0;
    	while (cur.hasNext()) {
    		Document doc = cur.tryNext();
    		String n = doc.getString("name");
    		Binary x = (Binary) doc.get("file");
    		byte[] _id = null;
    		if(x!=null) {
    			_id = x.getData();
    		}
    		int type = doc.getInteger("type");
    		
	        if((type == 3 && arrayId(docs, _id)) || ((type == 0 || type == 1))){ //&& start <= counter && counter < end)) {  
	        	System.out.println(doc.getInteger("type"));
	            docs.add(doc);
	        }
	        if(type==0 || type==1) {
	        	counter++;
	        }
        }
    	return docs;
    }
    
    public ArrayList<Document> addFriend(String username, String friend) {
    	if(sec.find(eq("username", username)).limit(1).first() == null || sec.find(eq("username", friend)).limit(1).first() == null) {
    		return null;
    	}
    	sec.updateOne(eq("username", username), Updates.addToSet("friends", friend));
    	sec.updateOne(eq("username", friend), Updates.addToSet("friends", username));
    	Document document1 = sec.find(eq("username", username)).limit(1).first();
    	Document document2 = sec.find(eq("username", friend)).limit(1).first();
    	ArrayList<Document> ret = new ArrayList<Document>();
    	ret.add(document1);
    	ret.add(document2);
    	return ret;
    }
    
    public ArrayList<Document> removeFriend(String username, String friend) {
    	if(sec.find(eq("username", username)).limit(1).first() == null || sec.find(eq("username", friend)).limit(1).first() == null) {
    		return null;
    	}
    	sec.updateOne(eq("username", username), Updates.pull("friends", friend));
    	sec.updateOne(eq("username", friend), Updates.pull("friends", username));
    	Document document1 = sec.find(eq("username", username)).limit(1).first();
    	Document document2 = sec.find(eq("username", friend)).limit(1).first();
    	ArrayList<Document> ret = new ArrayList<Document>();
    	ret.add(document1);
    	ret.add(document2);
    	return ret;
    }

    /**
     * Finds if it a document is a comment to a post. This works because there cannot be a post after a comment.
     * 
     * @param docs
     * @param _id
     * @return
     */
	private boolean arrayId(ArrayList<Document> docs, byte[] _id) {
		for(Document doc : docs) {
			byte[] od = ((ObjectId) doc.getObjectId("_id")).toByteArray();
			if(Arrays.equals(od, _id)) {
				return true;
			}
		}
		return false;
	}

	public Document addDM(String id, String friend, String dm) {
		//Sort alphabetically for ease of search
		String colname = sortAlphaId(id,friend);
		MongoCollection<Document> dms = csDatabase.getCollection(colname);
		Document document = new Document("name", id)
				.append("dm", dm);
		dms.insertOne(document);
		return document;
	}
	
	public String sortAlphaId(String id1, String id2) {
		if(id1.compareTo(id2)>0) {
			return id1 + "x" + id2;
		}else if(id1.compareTo(id2)<0) {
			return id2 + "x" + id1;
		}
		return "ERROR! This should never be the case since usernames must be unique";
	}

	public ArrayList<Document> getDMs(String id, String friend) {
		ArrayList<Document> docs = new ArrayList<Document>();
		String colname = sortAlphaId(id,friend);
		MongoCollection<Document> dms = csDatabase.getCollection(colname);
    	MongoCursor<Document> cur = dms.find().iterator();
    	while (cur.hasNext()) {
    		Document doc = cur.tryNext();
	        docs.add(doc);
        }
    	return docs;
	}
	
	public boolean isFriend(String username, String friend) {
		ArrayList<String> friends = getFriends(username);
		return friends.contains(friend);
	}
}
