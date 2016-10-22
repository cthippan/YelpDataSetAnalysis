package com.yelpdata.test;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;


public class CategorySimilarityComparer {
	/**
	 * @param : none
	 * @return: grouped hashMap for similar categories 
	 * 			key: Category Name
	 * 			value: Array List of similar categories
	 * 
	 * */
	public  HashMap<String, ArrayList<String>> returnGroupedCollections() throws UnknownHostException {
		HashMap<String,String> categoryFeatureSet = new HashMap<>();
		HashMap<String, String[]> mapWithSplitStrings = new HashMap<>();
		HashMap<String, ArrayList<String>> groupings = new HashMap<>();
		
		// Connecting to mongoDB to read stored feature set for each unique category
		MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
		DB db = mongoClient.getDB( "yelp" );
		DBCollection featureCollection = db.getCollection("feature_set");
		DBObject queryString=new BasicDBObject();
		DBCursor cursor=featureCollection.find(queryString);
		DBObject result;
		while (cursor.hasNext())
		{
			result=cursor.next();
			categoryFeatureSet.put(result.get("category").toString(),result.get("features").toString());
		}
		
		for (String key : categoryFeatureSet.keySet()) {
		    String value = categoryFeatureSet.get(key);
		    String[] words = value.split("\\s+");		    
		    mapWithSplitStrings.put(key, words);
		}

		Integer countCommon;
		for (String keyA : mapWithSplitStrings.keySet()) {
		    String[] valueSetA = mapWithSplitStrings.get(keyA);
		    ArrayList<String> commonSet = new ArrayList<>();
		   
		    for (String keyB : mapWithSplitStrings.keySet()){
			    if (keyB != keyA){
			    	String valueSetB[] = mapWithSplitStrings.get(keyB);
			    	countCommon = findCommon(valueSetA, valueSetB);			    		
			    	if (countCommon > 7){
			    		commonSet.add(keyB);
			    	}
			    }
			}
		    groupings.put(keyA,commonSet);
		}
		return groupings;
	}
	
	/**
	 * @param :String[] listA, String[] listB
	 * @return: Integer: that is intersection count of two lists
	 * 
	 * */
	public  Integer findCommon(String[] listA, String[] listB){
		String arrayToHash[] = listA;
		String arrayToSearch[] = listB;
		
		HashSet<String> intersection = new HashSet<>();
		HashSet<String> hashedArray = new HashSet<>();
		
		for( String entry : arrayToHash){
			hashedArray.add(entry);
		}
		
		for( String entry : arrayToSearch){
			if(hashedArray.contains(entry)){
				intersection.add(entry);
			}
		}
		
		return intersection.size();
	}
}