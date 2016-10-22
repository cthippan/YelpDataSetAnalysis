package com.yelpdata.training;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.LockObtainFailedException;

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;

public class TrainCategories {
	private  MongoClient mongoClient;
	private  DB db;
	
	TrainCategories(MongoClient mongoClient,DB db) throws UnknownHostException
	 {
			this.mongoClient = mongoClient;
			this.db = db;
	 }
	public static void main(String[] args) throws CorruptIndexException,
	LockObtainFailedException, IOException{
	
	//Prepare connection to MongoDB with the YELP database
	MongoClient mongoClient=new MongoClient( "localhost" , 27017 );
	DB db=mongoClient.getDB( "yelp" );
	
	TrainCategories indexGenerator=new TrainCategories(mongoClient,db);
	indexGenerator.createTrainingIndex(new EnglishAnalyzer());

}
	void  createTrainingIndex(Analyzer analyzer) throws IOException,
	FileNotFoundException {
		String outputFile; 
		String dirIndexPath = null;
		try{
			Properties configFile = new Properties();
			configFile.load(TrainCategories.class.getClassLoader().getResourceAsStream("config.properties"));
			dirIndexPath = configFile.getProperty("sourcefilepath");
		}catch(IOException ie){
			ie.printStackTrace();
			System.exit(0);
		}
		File dir = new File(dirIndexPath);
		DBCollection collection = db.getCollection("training_collection");
		DBObject projectionString=new BasicDBObject("_id",0).append("reviews",1).append("tips",1);
		DBObject fetchCategoriesQueryString;
		DBCursor cursor = null;
		DBObject result;
		PrintWriter writer = null;
		
		//Query to fetch all the distinct categories from the training data set and eliminating all null categories
		List<String> categoriesList = (List<String>)collection.distinct("categories");
		categoriesList.removeAll(Collections.singleton(null));		
		String  reviewsAndTips= null;
			
				for (String category:categoriesList)
				{
		
					outputFile = category +".txt";
					outputFile = outputFile.replace("/", "");
					File out = new File (dir, outputFile);
					System.out.println("Output File Name:" +outputFile);
					out.createNewFile();
					writer = new PrintWriter(out, "ASCII");
					fetchCategoriesQueryString=new BasicDBObject("categories",category);
					cursor=collection.find(fetchCategoriesQueryString,projectionString).addOption(Bytes.QUERYOPTION_NOTIMEOUT);
					while(cursor.hasNext())
					{
						result=cursor.next();
						writer.println(result.get("reviews"));
						writer.println(result.get("tips"));
					}
				}
				
		writer.close();
	}

}