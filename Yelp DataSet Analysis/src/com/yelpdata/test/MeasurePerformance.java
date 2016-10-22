package com.yelpdata.test;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.UnknownHostException;

import java.util.List;
import java.util.Properties;

import org.apache.lucene.queryparser.classic.ParseException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.yelpdata.training.TrainCategories;

public class MeasurePerformance {

	private MongoClient mongoClient;
	private DB db;
	private DBCollection testCollection;
	private DBCollection outputCollection;
	MeasurePerformance(MongoClient mongoClient,DB db,DBCollection testCollection,DBCollection outputCollection)
	{
		this.mongoClient=mongoClient;
		this.db=db;
		this.testCollection=testCollection;
		this.outputCollection=outputCollection;
	}
	public static void main(String[] args) throws UnknownHostException, IOException, ParseException
	{
		//Defining required parameters to create an object of the "MeasurePerformance" class
		//Prepare connection to MongoDB with the YELP database
		MongoClient mongoClient = new MongoClient();
		DB db = mongoClient.getDB( "yelp" );
		
		//"test_set" is the collection name of the test data
		DBCollection testCollection = db.getCollection("test_Collection");
		//"categories_assigned_from_code" stores the categories assigned programmatically for each business ID in the test collection
		DBCollection outputCollection = db.getCollection("categories_assigned_from_code");
		
		MeasurePerformance evaluator=new MeasurePerformance(mongoClient,db,testCollection,outputCollection);
		evaluator.generateEvaluationMetrics(testCollection, outputCollection);
	}

	/**
	 * This method is used to prepare the evaluation metrics (Precision,Recall and F2-measure for businesses in the test data
	 * @param testCollection
	 * @param outputCollection
	 */
	private  void generateEvaluationMetrics(DBCollection testCollection,
			DBCollection outputCollection) throws ParseException, IOException{
		
		//Declaring variable for MongoDB query operations
		DBObject queryString=new BasicDBObject();
		DBCursor outputCursor=outputCollection.find(queryString);
		DBObject result;
		DBObject result1;
		DBCursor testCursor;
		DBObject queryString1;
		String businessID;
		List testCategories;
		String outputCategories;
		String[] generatedCategories;
		String file = null;
		try{
			Properties configFile = new Properties();
			configFile.load(TrainCategories.class.getClassLoader().getResourceAsStream("config.properties"));
			file = configFile.getProperty("outputpath");
		}catch(IOException ie){
			ie.printStackTrace();
			System.exit(0);
		}
		PrintWriter writer = new PrintWriter(file, "UTF-8");
		float count = 0 ;
		float sum =0;
		float prec = 0;
		float f1Score = 0;
		//Iterating over each document in the "categories_assigned_from_code" collection that stores the programmatically assigned categories for each business in test data
		while(outputCursor.hasNext())
		{
			result=outputCursor.next();
			businessID=(String) result.get("business_id");
			outputCategories=(String) result.get("categories"); 
			
			// Categories assigned by our code
			generatedCategories=outputCategories.split(","); 
			
			queryString1=new BasicDBObject("business_id",businessID);
			testCursor=testCollection.find(queryString1);
			if(testCursor.hasNext())
			{
				result1=testCursor.next();
				//Ground truth categories
				testCategories=(List) result1.get("categories"); 
				
				//Compute the number of matched categories between the programmatically assigned and ground truth categories
				int matched=0;
				float precision=0;
				float recall=0;
				float fMeasure=0;
				if (generatedCategories.length > testCategories.size())
				{	
					for(String generatedCategory:generatedCategories)
					{
						for(Object testCategory:testCategories )
						{
							if ((generatedCategory.trim()).equals(testCategory.toString().trim()))
							{
								matched++;
							}
									
						}
					}
				}//if
				else
			
				{
					for(Object testCategory:testCategories)
					{
						for(String generatedCategory:generatedCategories)
						{
							if ((generatedCategory.trim()).equals(testCategory.toString().trim()))
							{
								matched++;
							}
									
						}
					}
					
				}//else
				if (testCategories.size()>0) //i.e. The business id has some ground truth to measure precision and recall
				{
					//Compute Precision,Recall and F2 measure
					precision=((float)matched/(generatedCategories.length));
					recall=((float)matched/(testCategories.size()));
					if(precision!=0 && recall!=0)
						fMeasure=((float) 2*precision*recall)/(precision+recall)*(5/4);
					else
						fMeasure=0;
					writer.println("Business ID: "+businessID+"\t | Ground Truth Categories :"+testCategories.toString()+ "\t\t\t\t | Programatically assigned Categoires :" +outputCategories +"\t\t | precision : "+precision+" \t\t | Recall : "+recall+"\t\t | F-Measure : "+fMeasure);
					//System.out.println("Business ID : "+businessID+" | Ground Truth Categories : "+testCategories.toString()+" | Programatically Assigned Categories : "+outputCategories+" | Precision : "+precision+" | Recall : "+recall+" | F-Measure: "+fMeasure);
					
					
					sum += recall;
					prec += precision;
					f1Score += fMeasure;
					count ++;
					
						
						
				} //if test categoires
				
			}
			
			
		}
		float total_prec = prec/count ;
		float total_recall = sum/count;
		float total_f1Score = f1Score/count;
		System.out.println("total precison is "+ total_prec + "total recall is " + total_recall + "total f1score is" +total_f1Score+ "sum value is" + sum + " precision is " + prec + "count is " +count );
		writer.close();
	}
}