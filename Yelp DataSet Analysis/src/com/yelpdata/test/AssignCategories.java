package com.yelpdata.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.yelpdata.training.TrainCategories;
	public class AssignCategories {
		static String pathToIndex;
		static String outputDirectoryPath;
		static String queryFilePath;
		static HashMap<String, ArrayList<String>> groupedCategories;
		public static void main(String[] args) throws ParseException, IOException
		{
			// Taking Index Directory path as input from the user
			// Scanner input=new Scanner(System.in); 
			// System.out.println("Enter Index Directory Path \n");
			pathToIndex=null;			
			outputDirectoryPath=null;
			try{
				Properties configFile = new Properties();
				configFile.load(TrainCategories.class.getClassLoader().getResourceAsStream("config.properties"));
				pathToIndex= configFile.getProperty("testindexpath");
				outputDirectoryPath= configFile.getProperty("outputpath");
			}catch(IOException ie){
				ie.printStackTrace();
				System.exit(0);
			}
			
			//Comparing various retrieval and ranking algorithms
			System.out.println("********Comparing BM25,Dirichlet Smoothing*******\n ");
			
			AssignCategories compareAlgorithms=new AssignCategories();
			
			CategorySimilarityComparer groupCategories=new CategorySimilarityComparer();
			groupedCategories=groupCategories.returnGroupedCollections();
			compareAlgorithms.compareSearchAlgorithms("BM25",new BM25Similarity());
			System.out.println("output file created for BM25\n");			
// 			Uncomment code below to run for DirichletSmoothing			
//			compareAlgorithms.compareSearchAlgorithms("DirichletSmoothing",new LMDirichletSimilarity());
//			System.out.println("output file created for Dirichlet Smoothing\n");			
			
			System.out.println("Categories Assigned!!!!");
			
		}

	/**
	 * This method is used to create output files for short and long queries based on the ranking and retrieval algorithm
	 * @param algoName : Retrieval and Ranking algorithm name
	 * @param rankingAlgorithm : Object of the retrieval and ranking algorithm
	 * @throws IOException
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 * @throws ParseException
	 */
		private  void compareSearchAlgorithms(String algoName,Similarity rankingAlgorithm)
				throws IOException, FileNotFoundException,
				UnsupportedEncodingException, ParseException {
			
			//Initializing the IndexReader and Searcher based on the Retrieval and Rankign algorithm used
			IndexReader reader = DirectoryReader .open(FSDirectory.open(new File(pathToIndex)));
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(rankingAlgorithm); //You need to explicitly specify the ranking algorithm using the respective Similarity class
			
			Analyzer analyzer = new EnglishAnalyzer();
			QueryParser parser = new QueryParser("reviewsandtips", analyzer);
				
			
			String queryString;
			MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
			DB db = mongoClient.getDB( "yelp" );
			DBCollection collection = db.getCollection("feature_set");
			DBCollection outputCollection = db.getCollection("categories_assigned_from_code");
			DBCursor outputCursor = null;
			DBObject outputResult=null;
			DBObject outputQueryString=null;
			DBObject queryString1=new BasicDBObject();
			DBCursor cursor=collection.find(queryString1);
			DBObject result;
			String category;
			String query;
			while (cursor.hasNext())
			{
				result=cursor.next();
				category=(String) result.get("category");
				
				query=(String) result.get("features");
				searchAndRetrieveDocs(searcher, parser, query,outputCollection,outputCursor,outputResult,outputQueryString,category,algoName);
			
				}			
			
			//Closing the IndexReader and PrintWriters
			reader.close();
//			QueryFile.flush();
//			QueryFile.close();
			
		}
		/**
		 * @param : String s
		 * 
		 * */
		public static String removeDuplicates(String s) {
		    return new LinkedHashSet<String>(Arrays.asList(s.split(","))).toString().replaceAll("(^\\[|\\]$)", "");
		}
		
		/**
		 * This method outputs the doc ID and its relevance score for the input query in the form of a map {docID: relevance score}
		 * @param searcher
		 * @param parser
		 * @param queryString
		 * @param searchResults
		 * @return
		 * @throws ParseException
		 * @throws IOException
		 */
		private  void searchAndRetrieveDocs(IndexSearcher searcher,
				QueryParser parser, String queryString,DBCollection outputCollection,DBCursor outputCursor,DBObject outputResult,DBObject outputQueryString,String category,String algoName) throws ParseException,
				IOException {
			
			DBObject insertString;
			DBObject updateString;
			DBObject searchString;
			DBObject updateCategory;
			String categories;
			if ((!queryString.equals("")) && (null!=queryString))
			{Query query = parser.parse(queryString);
			TopScoreDocCollector collector = TopScoreDocCollector.create(25, true);
			searcher.search(query, collector);
			ScoreDoc[] docs = collector.topDocs().scoreDocs;
			if (groupedCategories.keySet().contains(category) && groupedCategories.get(category).size()>0 )
				{
				
				category+=","+ (groupedCategories.get(category).toString().replace("[","").replace("]", "").replace(", ",","));
				//System.out.println(category);
				}
			for (int i = 0; i < docs.length; i++) 
			{
				Document doc = searcher.doc(docs[i].doc);
				outputQueryString=new BasicDBObject("business_id",doc.get("business_id"));
				outputCursor=outputCollection.find(outputQueryString);
				if (!outputCursor.hasNext())
				{
					
					insertString=new BasicDBObject("business_id",doc.get("business_id")).append("categories",category);
					outputCollection.insert(insertString);
				//writer.println(doc.get("business_id")+" \t\t"+category+" \t\t"+(i+1)+" \t\t"+docs[i].score+" \t\t"+algoName);
				}
				else
				{
					outputResult=outputCursor.next();
					categories=(String) outputResult.get("categories")+","+category;
					if (doc.get("business_id").equals("a3F3y4qOUOyPS0Qc-HM5Mw"))
						System.out.println(categories);
					categories=removeDuplicates(categories);
					if (doc.get("business_id").equals("a3F3y4qOUOyPS0Qc-HM5Mw"))
						System.out.println(categories);
					searchString=new BasicDBObject("business_id",doc.get("business_id"));
					
					updateCategory=new BasicDBObject("categories",categories);
					updateString=new BasicDBObject("$set",updateCategory);
					outputCollection.update(searchString, updateString);
				}
				
			}
			
		}
		}
	}
