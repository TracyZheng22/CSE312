import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;

import org.bson.Document;
import java.util.Arrays;
import com.mongodb.Block;

import com.mongodb.client.MongoCursor;
import static com.mongodb.client.model.Filters.*;
import com.mongodb.client.result.DeleteResult;
import static com.mongodb.client.model.Updates.*;
import com.mongodb.client.result.UpdateResult;
import java.util.ArrayList;
import java.util.List;

public class ContentHandler {

    public ContentHandler(){

        String URI = "mongodb+srv://cse312g20:2020sucks@cse312-g20-ynmcc.mongodb.net/test?retryWrites=true&w=majority";
        MongoClientURI clientURI = new MongoClientURI(URI);
        MongoClient mongoClient = new MongoClient(clientURI);

        MongoDatabase csDatabase = mongoClient.getDatabase("CSE312");
        MongoCollection<Document> col = csDatabase.getCollection("contents");

        Document document = new Document("name","Vin");

        document.append("Sex","male");
        document.append("Age","20");

        col.insertOne(document);

    }


}
