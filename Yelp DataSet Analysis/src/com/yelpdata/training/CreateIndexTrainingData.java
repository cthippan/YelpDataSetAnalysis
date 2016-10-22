package com.yelpdata.training;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.commons.io.FilenameUtils;

public class CreateIndexTrainingData {
	private String sourceFilePath =null;
	private String indexFilePath= null;
	private IndexWriter writer=null;
	private File indexDirectory=null;


	private CreateIndexTrainingData()throws FileNotFoundException, CorruptIndexException, IOException{

		try{
			Properties configFile = new Properties();
			configFile.load(TrainCategories.class.getClassLoader().getResourceAsStream("config.properties"));
			sourceFilePath = configFile.getProperty("sourcefilepath");
			indexFilePath = configFile.getProperty("indexfilepath");
		}catch(IOException ie){
			ie.printStackTrace();
			System.exit(0);
		}

		try {
			long start=System.currentTimeMillis();
			createIndexWriter();
			checkFileValidity();
			closeIndexWriter();
			
			long end=System.currentTimeMillis();
			System.out.println("Total time Taken :"+(end-start)+" milli seconds");
		}catch (Exception e){
			System.out.println("Exception occured");
		}
	}
	private void createIndexWriter()
	{
		try{
			Analyzer analyzer = new EnglishAnalyzer();
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_4_10_1,analyzer);
			indexWriterConfig.setOpenMode(OpenMode.CREATE);
			File indexDir=new File(indexFilePath);
			Directory fileSystemDirectory = FSDirectory.open(indexDir);
			writer = new IndexWriter(fileSystemDirectory, indexWriterConfig);
				
		}catch(Exception e)
		{
			System.out.println("unable to open direcotry");
		}
	}
	private void checkFileValidity()
	{
		File filestoIndex=new File(sourceFilePath);
		List<String> fileNames = new ArrayList<String>();
		File[] listoffiles=filestoIndex.listFiles();
		for (File file : listoffiles) {
			try{
				if(!file.isDirectory()
						&& !file.isHidden()
						&& file.exists()
						&& file.canRead()
						&& file.isFile() 	)
				{
					fileNames.add(file.getAbsolutePath());
					System.out.println(file.getName());

				}
			}catch(Exception e){
				System.out.println("sorry cannot index"+file.getAbsolutePath());}

		}	
		System.out.println();
		indexFiles(fileNames);


	}
	private void indexFiles(List<String> fileNames)
	{

		System.out.println("Creating Index in the given location");
		for (String filesToBeParsed : fileNames) {
			try{
				String reviewsAndTips = "";
				File filewithext = new File(filesToBeParsed); 
				String fileext =filewithext.getName();
				String category = FilenameUtils.removeExtension(fileext);
				System.out.println(category);
				if (category.equals("Restaurants") || category.equals("Nightlife")){
					//To avoid insufficient heap space as Restaurants is > 500mb
					int lineCount = 0;
					StringBuilder strbld = new StringBuilder("");
					File fileObj = new File(filesToBeParsed);
					try(Scanner scnr = new Scanner(fileObj);)
					{
						//Reading each line of file using Scanner class
						while(scnr.hasNextLine()){
							strbld.append(scnr.nextLine());
							lineCount++;
							if (lineCount>8000){break;}
						}

					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					reviewsAndTips = new String(strbld.toString());
				}else {
					reviewsAndTips = new String(Files.readAllBytes(Paths.get(filesToBeParsed)));
				}
				reviewsAndTips = reviewsAndTips.replace("[", "").replace("]","");
				Document luceneDoc = new Document();
				luceneDoc.add(new StringField("category",category, Store.YES));
				if (reviewsAndTips != "")
				{

					FieldType type = new FieldType();
					type.setIndexed(true);
					type.setStored(true);
					type.setStoreTermVectors(true);
					Field field = new Field("reviewsandtips", reviewsAndTips, type);
					luceneDoc.add(field);								

				}
				writer.addDocument(luceneDoc);
			}
			catch(Exception e)
			{
				e.printStackTrace();  
				System.out.println("Could not add: " + filesToBeParsed);
			}

		}
	}
	private void closeIndexWriter()
	{
		try{
			writer.forceMerge(1);
			writer.commit();
			writer.close();
		}catch(Exception e){
			System.out.println("Indexer cannot be deleted");
		}
	}
	public static void main(String[]args)
	{
		try{
			new CreateIndexTrainingData();

		}catch (Exception ex){
			System.out.println("cannot start");
		}
	}
}
