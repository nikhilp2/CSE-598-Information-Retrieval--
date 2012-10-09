package edu.asu.cse494;
import com.lucene.analysis.Analyzer;
import com.lucene.analysis.StopAnalyzer;
import com.lucene.index.*;
import com.lucene.queryParser.QueryParser;
import com.lucene.search.Hits;
import com.lucene.search.IndexSearcher;
import com.lucene.search.Query;
import com.lucene.search.Searcher;

import java.io.*;
import java.sql.Time;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


public class VectorViewer {
	int count=0;
	//display the vector
	public void showVector()
	{
		// lists the vector
		try{
			IndexReader reader = IndexReader.open("result3index");
			System.out.println(" Number of Docs in Index :" + reader.numDocs());
			
			// use the TermEnum object to iterate through all the terms in the index
			TermEnum termenum = reader.terms();
			System.out.println("Printing the Terms and the Frequency \n");
			while(termenum.next())
			{
				count++;
				Term termval = termenum.term();
				System.out.println("The Term :" + termval.text() + " Frequency :"+termenum.docFreq());
				
				/*
				   //Add following here to retrieve the <docNo,Freq> pair for each term
				   TermDocs termdocs = reader.termDocs(termval);
			
				   //to retrieve the <docNo,Freq,<pos1,......posn>> call
				   TermPositions termpositions = termval.termPositions(termval)
				*/
			
			}
			System.out.println(" Total terms : " + count);
		
		}
		catch(IOException e){
		    System.out.println("IO Error has occured: "+ e);
		    return;
		}
	}
	
	public ArrayList<int[]> getDoc_tf(String q) {
		ArrayList<int[]> retVal = new ArrayList<int[]>();
		try {
			IndexReader reader = IndexReader.open("result3index");
			TermDocs termdocs = reader.termDocs(new Term("contents", q));
			
			if (termdocs != null) {
				while (termdocs.next())
				    retVal.add(new int[]{termdocs.doc(), termdocs.freq()});
				return retVal;
			}
		}
		catch (Exception e) {
			System.out.println("Exception: " + e);
		}
		
		return retVal;
	}
	
	public double[] getTwoNorm_di() {
		System.out.println("Calculating |di|");
		double[] twoNorm_di = new double[0];
		
		try {
			IndexReader reader = IndexReader.open("result3index");
			twoNorm_di = new double[reader.numDocs()];
			TermEnum termenum = reader.terms();
			
			int termCount = 0;
			while(termenum.next()) { // Per term
				Term termval = termenum.term();
				if (termval.field() == "contents") { // check the term.field( ) property (and only consider those terms that return "contents")
				    TermDocs termdocs = reader.termDocs(termval);
				    while (termdocs.next()) // Per doc
					    twoNorm_di[termdocs.doc()] += Math.pow(termdocs.freq(), 2);
				    
				    if (termCount%1000 == 0)
				    	System.out.println("-Reference: " + termCount);
				    termCount++;
				}
			}
		}
		catch (Exception e) {
			System.out.println("Exception: " + e);
		}
		
		System.out.println("-computing square roots");
		for (int i=0; i< twoNorm_di.length; i++)
			twoNorm_di[i] = Math.sqrt(twoNorm_di[i]); 
		
		System.out.println("-complete\n");
		return twoNorm_di;
	}

	public static void main(String[] args)
	{
		VectorViewer CSE494Viewer = new VectorViewer();
//		CSE494Viewer.showVector();
//		System.out.println(" Total terms : " + CSE494Viewer.count);
		
		// Preprocess
		double[] twoNorm_di = CSE494Viewer.getTwoNorm_di();
		
	    try {
	    	IndexReader reader = IndexReader.open("result3index");
	        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	        while (true) {
	        	System.out.print("Query: ");
	        	String line = in.readLine().toLowerCase();

			  	if (line.length() == -1)
			  		break;
			  	
			  	long startTime = new Date().getTime();
			  	String[] query = line.split("\\s+");
			  	double twoNorm_q = Math.sqrt(query.length);
			  		
			  	// Print query
			  	for(int i=0; i<query.length; i++)
			  		System.out.println(i+1 + ") " + query[i]);
			  	
			  	// Calculate the numerator (dotProduct of q*di)
			  	System.out.println("Calculating numerator");
			  	ArrayList<int[]> doc_tf;
			  	Map<Integer,Double> dotProduct_qDi = new HashMap<Integer,Double>();
			  	for(int i=0; i<query.length; i++) {
			  		doc_tf = CSE494Viewer.getDoc_tf(query[i]);
			  		for (int j=0; j<doc_tf.size(); j++) {
			  			int docId = doc_tf.get(j)[0];
			  			int freq = doc_tf.get(j)[1];
			  			if (!dotProduct_qDi.containsKey(docId))
			  				dotProduct_qDi.put(docId, (double)freq);
			  			else
			  				dotProduct_qDi.put(docId, dotProduct_qDi.get(docId) + freq);
			  		}
			  	}
			  	
			  	// Calculate the similarity, (dividing the dotProduct by the denominator)
			  	System.out.println("Calculating similarity");
			  	Map<Integer,Double> similarity = new HashMap<Integer,Double>();
			  	Iterator<Integer> itr = dotProduct_qDi.keySet().iterator();
			  	while (itr.hasNext()) {
			  		int docId = itr.next();
			  		similarity.put(docId, dotProduct_qDi.get(docId)/(twoNorm_di[docId]*twoNorm_q));
			  	}
			  	
			  	// Sorting results
			  	System.out.println("Sorting documents");
			  	FrequencyComparator fc = new FrequencyComparator(similarity);
			  	TreeMap<Integer,Double> sortedSimilarity = new TreeMap(fc);
			  	sortedSimilarity.putAll(similarity);
			  	long endTime = new Date().getTime();
			  	System.out.println("Time spent: " + (endTime-startTime) + " milliseconds");
			  	
			  	System.out.println("Results: (" + sortedSimilarity.size() + "hits)");
			  	int k = 1;
			  	for (int doc : sortedSimilarity.keySet()) {
			  		System.out.println(k + ". DocumentId:" + doc + "\tSimilarity:" + sortedSimilarity.get(doc) +   
  							"\tURL:" + reader.document(doc).getField("url").stringValue());
			  		if (k%10==0) {
			  			System.out.print("more (y/n) ? ");
			  		    line = in.readLine();
			  		    if (line.length() == 0 || line.charAt(0) == 'n')
			  		    	break;
		  		    }
			  		k++;
			  	}
	        }
	        
        } catch (Exception e) {
		    System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
	}
}

class FrequencyComparator implements Comparator {

	Map base;
	
	public FrequencyComparator(Map base) {
		this.base = base;
	}

	public int compare(Object a, Object b) {

		if((Double)base.get(a) < (Double)base.get(b)) 
			return 1;
		 else if((Double)base.get(a) == (Double)base.get(b)) 
			return 0;
		 else 
			return -1;
	}
}
