package com.yelpdata.test;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.yelpdata.training.TrainCategories;

public class GenerateTestIndex{
	
	//private  File indexDirTest;
	private  MongoClient mongoClient;
	private  DB db;
	
	GenerateTestIndex(MongoClient mongoClient,DB db) throws UnknownHostException
	 {
			this.mongoClient = mongoClient;
			this.db = db;
	 }
	public static void main(String[] args) throws CorruptIndexException,
			LockObtainFailedException, IOException
	 {
			
			//Prepare connection to MongoDB with the YELP database
			MongoClient mongoClient=new MongoClient( );
			DB db=mongoClient.getDB( "yelp" );
			String testindexFilePath = null;
			
			//Get Directory paths from the properties file
			try{
				Properties configFile = new Properties();
				configFile.load(TrainCategories.class.getClassLoader().getResourceAsStream("config.properties"));
				testindexFilePath = configFile.getProperty("testindexpath");
				System.out.println("The test Index Path is: " + testindexFilePath);
			}catch(IOException ie){
				ie.printStackTrace();
				System.exit(0);
			}
			
			//Create index on Training data set
			//Path to create the lucene training index directory
			
			GenerateTestIndex indexGenerator=new GenerateTestIndex(mongoClient,db);
			//indexGenerator.createTrainingIndex(indexDir,new EnglishAnalyzer());
			
			//Create index on Test data set
			//Path to create the lucene training index directory
			//indexDir=new File("R:/IUB/Search/testIndex/");
			File indexDir=new File(testindexFilePath);
			indexGenerator.createTestIndex(indexDir,new EnglishAnalyzer());

			
	}

 
	private  void  createTestIndex(File indexDir,Analyzer analyzer) throws IOException,
			FileNotFoundException {
				
		//Creating and Initializing IndexWriter
		System.out.println("Creating and Initializing Index Writer");
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_4_10_1,analyzer);
		indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		Directory fileSystemDirectory = FSDirectory.open(indexDir);
		IndexWriter writer = new IndexWriter(fileSystemDirectory, indexWriterConfig);
		
		//MongoDB test collection name
		DBCollection collection = db.getCollection("test_Collection");
		
		//Preparing Query to fetch all the documents from the test collectio
		DBObject projectionString=new BasicDBObject("_id",0);
		DBObject queryString=new BasicDBObject();
		DBCursor cursor = collection.find(queryString,projectionString).addOption(Bytes.QUERYOPTION_NOTIMEOUT);
		DBObject result;
		
		//Declaring variables to store field values from the query result
		String businessID;
		List categories;
		String businessName;
		List reviews;
		List reviewStars;
		List tips;
		List tipsLikes;		
		
		try{	
			//Iterating over query results
			System.out.println("Enter the iteration of query");
				while (cursor.hasNext())
				{
					System.out.println("has next");
					result=cursor.next();
					//Creating a lucene document
					Document luceneDoc = new Document();
					
					//Adding fields to the lucene document
					businessID=result.get("business_id").toString();
					luceneDoc.add(new StringField("business_id",businessID, Store.YES));
					
					businessName=result.get("name").toString();
					luceneDoc.add(new StringField("business_name",businessName, Store.YES));
					
					String reviewsAndTips="";
					categories=(List)result.get("categories");
					for (Object category:categories)
					{
						luceneDoc.add(new StringField("categories",category.toString(), Store.YES));
					}
					
					//Preparing a concatenated string of reviews and tips for each business ID
					reviews=(List)result.get("reviews");
					
					for (Object review:reviews)
					{
						reviewsAndTips+=review.toString();
					}
		
					tips=(List)result.get("tips");
					for (Object tip:tips)
					{
						reviewsAndTips+=tip.toString();
					}
					
					luceneDoc.add(new TextField("reviewsandtips",reviewsAndTips, Store.YES));
					// Write the lucene document to the index
					writer.addDocument(luceneDoc);					
				}		
		  }
		finally
			{		cursor.close();
					writer.forceMerge(1);
					writer.commit();
					writer.close();								
			}
		}		
	}
