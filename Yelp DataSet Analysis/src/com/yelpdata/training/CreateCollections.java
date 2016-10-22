package com.yelpdata.training;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class CreateCollections {

	private MongoClient mongoClient;
	private DB db;
	private DBCollection traingingCollections;
	private DBCollection testCollections;
	CreateCollections (MongoClient mongoClient,DB db,DBCollection trainingCollection,DBCollection testCollection){
		this.mongoClient=mongoClient;
		this.db=db;
		this.traingingCollections=trainingCollection;
		this.testCollections = testCollection;

	}

	void createCollections(DB db,DBCollection trainingCollection,DBCollection testCollection){

		DBCollection businessCollection=db.getCollection("business");
		DBCollection reviewCollection=db.getCollection("review");
		DBCollection tipsCollection=db.getCollection("tip");

		DBObject query = new BasicDBObject("_id",0).append("business_id", 1).append("categories", 1).append("name", 1);
		DBCursor buCursor=businessCollection.find(new BasicDBObject(),query);
		int numberOfRecords = 0;
		while(buCursor.hasNext()){
			DBObject bu = buCursor.next();
			DBObject document = new BasicDBObject(bu.toMap());
			String businessID=(String)bu.get("business_id");

			DBObject reviewQuery=new BasicDBObject("business_id",businessID);
			DBCursor reviewCursor=reviewCollection.find(reviewQuery);
			List reviewlist = new ArrayList();
			List reviewStars = new ArrayList();

			while(reviewCursor.hasNext())
			{
				DBObject reviewObject=reviewCursor.next();
				reviewlist.add((String) reviewObject.get("text"));
				String textdata = (String)reviewObject.get("text");

				if (reviewObject.get("stars")==null)
					reviewStars.add(0);
				else
					reviewStars.add((int)reviewObject.get("stars"));
			}

			document.put("reviews",reviewlist);
			document.put("review stars",reviewStars );

			DBObject tipsQuery = new BasicDBObject("business_id",businessID);
			DBCursor tipsCursor = tipsCollection.find(tipsQuery);
			List tipslist = new ArrayList();
			List tipslikes = new ArrayList();

			while (tipsCursor.hasNext()){
				DBObject tipsObject = tipsCursor.next();
				tipslist.add((String) tipsObject.get("text"));

				if(tipsObject.get("likes") == null)
					tipslikes.add(0);
				else
					tipslikes.add((int)tipsObject.get("likes"));
			}
			document.put("tips", tipslist);
			document.put("tips likes", tipslikes);
		
			if(numberOfRecords <= 40000){
				
				trainingCollection.insert(document);
				numberOfRecords++;
			}
			else {
				
				testCollection.insert(document);
				if (numberOfRecords == 40001)
					System.out.println("Creating Training completed\n starting test collection");
				numberOfRecords++;
			}

		}		
	
	}
	public static void main(String[] args) throws UnknownHostException{
		MongoClient mongoClient =  new MongoClient();
		DB db = mongoClient.getDB("yelp");
		DBCollection trainingCollection = db.getCollection("training_collection");
		DBCollection testCollection = db.getCollection("test_Collection");
		CreateCollections collect = new CreateCollections(mongoClient, db, trainingCollection, testCollection);
		collect.createCollections(db, trainingCollection, testCollection);
	}
}
