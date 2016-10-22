package com.yelpdata.training;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;


public class FutureExtractor {

	/**
	 * @param args
	 * @throws IOException 
	 * Main function: 	Calls indexReaderFunctoin
	 *
	 * 
	 * INDEX path: 
	 * CHANGE indexPath = "<PATH ON YOUR MACHINE WHERE YOU WANT INDEX FILES TO BE STORED>"
	 * 
	 *
	 */
	public static void main(String[] args) throws IOException {
		
		String indexPath = null;		
		//read Lucene index
		try{
			Properties configFile = new Properties();
			configFile.load(TrainCategories.class.getClassLoader().getResourceAsStream("config.properties"));
			indexPath = configFile.getProperty("indexfilepath");
			System.out.println(indexPath);
		}catch(IOException ie){
			ie.printStackTrace();
			System.exit(0);
		}
		indexReaderFunction(indexPath);	
		
	}
	
	/**
	 * @param String indexPath: path to lucene index on local machine
	 * @throws IOException
	 * 
	 * Reads Indexed directory
	 * Do feature set pre-processing
	 * Prepares MongoDB object to add to feature_set collection
	 * Dumps it to yelp database
	 * 
	 * Result: Feature set dictionary is created in yelp database as a collection
	 * 
	 */
	private static void indexReaderFunction(String indexPath) throws IOException{
		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
		
		//Print the total number of documents in the corpus
		System.out.println("Total number of documents in the corpus: "+reader.maxDoc());		
		
		Terms vocabulary = MultiFields.getTerms(reader, "reviewtips");
		
		Double TfIdfScore;
		IndexSearcher searcher = new IndexSearcher(reader);
		
		// connecting to mongoDB on local port
		MongoClient mongoClient = new MongoClient( );
		DB db = mongoClient.getDB( "yelp" );
		DBCollection collection = db.getCollection("feature_set");
    	DBObject insertString;
    	
    	// iterating over index to get feature set for each unique category
		Integer totalDocs = reader.maxDoc();
		for(int i=0; i < totalDocs ; i++){
			HashMap<String,Double> termTfIdfScore = new HashMap<>();
			final Terms terms = reader.getTermVector(i, "reviewsandtips");
			if (terms != null && terms.size() > 0) {
			    TermsEnum termsEnum = terms.iterator(null); // access the terms for this field
			    BytesRef term = null;
			    while ((term = termsEnum.next()) != null) {// explore the terms for this field
			        DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one
			        int docIdEnum;
			        // filter1: searches for numbers
			        // filter2: searches for website names
			        // filter3: searches for words ending with 's
			        // filter4: searches for words with dot
			        // filter5: searches for one or two letter words specifically
			        // filter6: searches for words like aaaaaaa lolololo mapmapmapmap which has repeating sequences in it
			        while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS) {
				          TfIdfScore = ((docsEnum.freq()^2))*(Math.log10(totalDocs/reader.docFreq(new Term("reviewsandtips", term.utf8ToString()))));
				          //check if numbers exist in features
				          if ((term.utf8ToString().matches(".*\\d.*")) || 							
				        		  (term.utf8ToString().matches(".*.*\\b(www.|.com)\\b.*.*") ||
				        		  (term.utf8ToString().matches(".*'s.*")) ||
				        		  (term.utf8ToString().matches(".*\\..*")) ||
				        		  (term.utf8ToString().matches("^[a-zA-Z]{1,2}$")) ||
				        		  filterRepeatingChars(term.utf8ToString()))){
				        	  
				          }
				          else{
				        	  termTfIdfScore.put(term.utf8ToString(), TfIdfScore);
				          }		          
				        }
			    }
			}
					
			Map<String, Double> sortedTfIdfScore = sortByComparator(termTfIdfScore);
			Integer count = 0;
			String featureSet = "";
			for (Map.Entry<String, Double> entity: sortedTfIdfScore.entrySet()){
				if (count != 10){
					featureSet = featureSet + " " + entity.getKey();
					count = count + 1;
				}
				else{
					break;
				}					
			}
			Document indexDoc = searcher.doc(i);
			insertString=new BasicDBObject("category",indexDoc.get("category")).append("features",featureSet);
			collection.insert(insertString);
			System.out.println("Feature Set generated for :" + indexDoc.get("category"));
			System.out.println("Features: " + featureSet);
		}		
		reader.close();
	}
	
	/**
	 * @param : Map<String, Double> unsortMap : a Map which has to be sorted on keys
	 * @return: Map<String, Double> : sortedMap 
	 * 
	 * Function sorts a map on keys and returns the sorted map
	 * 
	 */
	private static Map<String, Double> sortByComparator(Map<String, Double> unsortMap) {
		 
		// Convert Map to List
		List<Map.Entry<String, Double>> list = 
			new LinkedList<Map.Entry<String, Double>>(unsortMap.entrySet());
 
		// Sort list with comparator, to compare the Map values
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
 
		// Convert sorted map back to a Map
		Map<String, Double> sortedMap = new LinkedHashMap<String, Double>();
		for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();) {
			Map.Entry<String, Double> entry = it.next();
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	/**
	 * @param : String stringToMatch: checkString 
	 * @return: boolean
	 * 
	 * Function checks if string has words like: aaaaaa, bobobobobo 
	 * which have 2 or more repeating characters or bunch of characters
	 * 
	 */
	private static boolean filterRepeatingChars(String stringToMatch){
		Pattern p = Pattern.compile("(\\w\\w)\\1+");
		Matcher m = p.matcher(stringToMatch);
		if (m.find())
		{
		    return true;
		}
		else {
			return false;		
		}
	}
}
