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
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;


public class VectorViewerIDFSnippets {
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
	
	public double[] getTwoNorm_di(Map<String,Double> IDF) {
		System.out.println("\nCalculating |di|");
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
					    twoNorm_di[termdocs.doc()] += Math.pow(termdocs.freq()*IDF.get(termval.text()), 2);
				    	//twoNorm_di[termdocs.doc()] += Math.pow(termdocs.freq()*getIDF(termval.text()), 2);
				    
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

	// Compute all the IDF for all words
	public Map<String,Double> getIDF() {
		System.out.println("Calculating IDF");
		Map<String,Double> IDF = new HashMap<String,Double>();
		
		try {
			IndexReader reader = IndexReader.open("result3index");
			IDF = new HashMap<String,Double>((int)(167000/.75));
			TermEnum termenum = reader.terms();
			
			int termCount = 0;
			while(termenum.next()) { // Per term
				Term termval = termenum.term();
				if (termval.field() == "contents") { // check the term.field( ) property (and only consider those terms that return "contents")
				    IDF.put(termval.text(), Math.log(reader.numDocs()/termenum.docFreq()));
				    
				    if (termCount%1000 == 0)
				    	System.out.println("-Reference: " + termCount);
				    termCount++;
				}
			}
		}
		catch (Exception e) {
			System.out.println("Exception: " + e);
		}
		
		return IDF;
	}
	
	// Get IDF on individual word. This method is slower than having a bunch of them ready,
	// maybe it's because we keep opening the index many times
	public Double getIDF(String q) {
		try {
			IndexReader reader = IndexReader.open("result3index");
			//IDF = new HashMap<String,Double>((int)(167000/.75));
			TermEnum termenum = reader.terms(new Term("contents", q));
		    return Math.log(reader.numDocs()/termenum.docFreq());
		}
		catch (Exception e) {
			System.out.println("Exception: " + e);
		}
		
		return 0d;
	}
	
	private String getSnippet(ArrayList<TermDetail> docTerms, String[] q) {
		final int WIDTH = 10;
		int matchIndex = -1;
		outerloop:
		for (int i=0; i<docTerms.size(); i++) {
			// Look for a match
			for (int j=0; j<q.length; j++) {
				if (q[j].equalsIgnoreCase(docTerms.get(i).term)) {
					matchIndex = i;
					break outerloop;
				}
			}
		}
		
		String retVal = "";
		if (matchIndex != -1)
		{
			int start = matchIndex - WIDTH < 0 ? 0 : matchIndex - WIDTH;
			int end = matchIndex + WIDTH > docTerms.size()-1 ? docTerms.size()-1 : matchIndex + WIDTH;
			for (int i=start; i<end; i++) {
				retVal += docTerms.get(i).term + " ";
			}
		}
		else
			retVal = "Not Found";
		
		return retVal;
	}

	public static void main(String[] args)
	{
	    try {
			IndexReader reader = IndexReader.open("result3index");
			VectorViewerIDFSnippets CSE494Viewer = new VectorViewerIDFSnippets();
			long startForwardIndexTime = new Date().getTime();
			ForwardIndex forwardIndex = new ForwardIndex();
			long endForwardIndexTime = new Date().getTime();
			System.out.println("ForwardIndex time taken:" + (endForwardIndexTime-startForwardIndexTime));
			System.out.println();
			
			// Preprocessing
			Map<String,Double> IDF = CSE494Viewer.IDF;
			double[] twoNorm_di = CSE494Viewer.twoNorm_di;
			//Map<String,Double> IDF = CSE494Viewer.getIDF();
			//double[] twoNorm_di = CSE494Viewer.getTwoNorm_di(IDF);
	    	
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
			  	long numeratorTime = new Date().getTime();
			  	ArrayList<int[]> doc_tf;
			  	Map<Integer,Double> dotProduct_qDi = new HashMap<Integer,Double>();
			  	for(int i=0; i<query.length; i++) {
			  		doc_tf = CSE494Viewer.getDoc_tf(query[i]);
			  		for (int j=0; j<doc_tf.size(); j++) {
			  			int docId = doc_tf.get(j)[0];
			  			int freq = doc_tf.get(j)[1];
			  			if (!dotProduct_qDi.containsKey(docId))
			  				dotProduct_qDi.put(docId, (double)freq*IDF.get(query[i]));
			  				//dotProduct_qDi.put(docId, (double)freq*CSE494Viewer.getIDF(query[i]));
			  			else
			  				//dotProduct_qDi.put(docId, dotProduct_qDi.get(docId) + (freq*CSE494Viewer.getIDF(query[i])) );
			  				dotProduct_qDi.put(docId, dotProduct_qDi.get(docId) + (freq*IDF.get(query[i])) );
			  		}
			  	}
			  	long endNumeratorTime = new Date().getTime();
			  	System.out.println("Numerator time: " + (endNumeratorTime-numeratorTime));
			  	
			  	// Calculate the similarity, (dividing the dotProduct by the denominator)
			  	System.out.println("Calculating similarity");
			  	long similarityTime = new Date().getTime();
			  	Map<Integer,Double> similarity = new HashMap<Integer,Double>();
			  	Iterator<Integer> itr = dotProduct_qDi.keySet().iterator();
			  	while (itr.hasNext()) {
			  		int docId = itr.next();
			  		similarity.put(docId, dotProduct_qDi.get(docId)/(twoNorm_di[docId]*twoNorm_q));
			  	}
			  	long endSimilarityTime = new Date().getTime();
			  	System.out.println("Denominator time: " + (endSimilarityTime-similarityTime));
			  	
			  	// Sorting results
			  	System.out.println("Sorting documents");
			  	long sortingTime = new Date().getTime();
			  	FrequencyComparator2 fc = new FrequencyComparator2(similarity);
			  	TreeMap<Integer,Double> sortedSimilarity = new TreeMap(fc);
			  	sortedSimilarity.putAll(similarity);
			  	long endSortingTime = new Date().getTime();
			  	System.out.println("Sorting time: " + (endSortingTime-sortingTime));
			  	long endTime = new Date().getTime();
			  	System.out.println("Time spent: " + (endTime-startTime) + " milliseconds");
			  	
			  	System.out.println("IDF based results: (" + sortedSimilarity.size() + "hits)");
			  	int k = 1;
			  	for (int doc : sortedSimilarity.keySet()) {
			  		System.out.println(k + ". DocumentId:" + doc + "\tSimilarity:" + sortedSimilarity.get(doc) +   
			  							"\tURL:" + reader.document(doc).getField("url").stringValue());
			  		
			  		// Snippet
			  		long startSnippetTime = new Date().getTime();
			  		System.out.println("Snippet: " + CSE494Viewer.getSnippet(forwardIndex.getTerms(doc), query));
			  		long endSnippetTime = new Date().getTime();
			  		System.out.println("Snippet time: " + (endSnippetTime-startSnippetTime));
			  		
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
	
	
	Map<String,Double> IDF;
	double[] twoNorm_di;
	public VectorViewerIDFSnippets()
	{
		IDF = getIDF();
		twoNorm_di = getTwoNorm_di(IDF);
	}
	
	// Same purpose as main method, but this is a helper used for external classes
	public int[] getTop(String[] query, int topK) {
		try {
			//VectorViewerIDF CSE494Viewer = new VectorViewerIDF();
			double twoNorm_q = Math.sqrt(query.length);
			
			// Preprocessing
			//Map<String,Double> IDF = CSE494Viewer.getIDF();
			//double[] twoNorm_di = CSE494Viewer.getTwoNorm_di(IDF);
			
			long startTime = new Date().getTime();
			
		  	// Calculate the numerator (dotProduct of q*di)
		  	System.out.println("Calculating numerator");
		  	long numeratorTime = new Date().getTime();
		  	ArrayList<int[]> doc_tf;
		  	Map<Integer,Double> dotProduct_qDi = new HashMap<Integer,Double>();
		  	for(int i=0; i<query.length; i++) {
		  		doc_tf = getDoc_tf(query[i]);
		  		for (int j=0; j<doc_tf.size(); j++) {
		  			int docId = doc_tf.get(j)[0];
		  			int freq = doc_tf.get(j)[1];
		  			if (!dotProduct_qDi.containsKey(docId))
		  				dotProduct_qDi.put(docId, (double)freq*IDF.get(query[i]));
		  				//dotProduct_qDi.put(docId, (double)freq*CSE494Viewer.getIDF(query[i]));
		  			else
		  				//dotProduct_qDi.put(docId, dotProduct_qDi.get(docId) + (freq*CSE494Viewer.getIDF(query[i])) );
		  				dotProduct_qDi.put(docId, dotProduct_qDi.get(docId) + (freq*IDF.get(query[i])) );
		  		}
		  	}
		  	long endNumeratorTime = new Date().getTime();
		  	System.out.println("Numerator time: " + (endNumeratorTime-numeratorTime));
		  	
		  	// Calculate the similarity, (dividing the dotProduct by the denominator)
		  	System.out.println("Calculating similarity");
		  	long similarityTime = new Date().getTime();
		  	Map<Integer,Double> similarity = new HashMap<Integer,Double>();
		  	Iterator<Integer> itr = dotProduct_qDi.keySet().iterator();
		  	while (itr.hasNext()) {
		  		int docId = itr.next();
		  		similarity.put(docId, dotProduct_qDi.get(docId)/(twoNorm_di[docId]*twoNorm_q));
		  	}
		  	long endSimilarityTime = new Date().getTime();
		  	System.out.println("Denominator time: " + (endSimilarityTime-similarityTime));
		  	
		  	// Sorting results
		  	System.out.println("Sorting documents");
		  	long sortingTime = new Date().getTime();
		  	FrequencyComparator2 fc = new FrequencyComparator2(similarity);
		  	TreeMap<Integer,Double> sortedSimilarity = new TreeMap(fc);
		  	sortedSimilarity.putAll(similarity);
		  	long endSortingTime = new Date().getTime();
		  	System.out.println("Sorting time: " + (endSortingTime-sortingTime));
		  	long endTime = new Date().getTime();
		  	System.out.println("Time spent: " + (endTime-startTime) + " milliseconds");
		  	
		  	// Return topK results
		  	int retValSize = Math.min(sortedSimilarity.keySet().size(), topK);
		  	int[] retVal = new int[retValSize];
		  	int k = 0;
		  	for (int doc : sortedSimilarity.keySet()) {
		  		if (k>=retValSize) {
		  			break;
	  		    }
		  		retVal[k] = doc;
		  		k++;
		  	}
		  	return retVal;
		}
	  	catch(Exception e) {
	  		System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
	  		return new int[0];
	  	}
	}

	// Returns 2 values
	public Vector[] getTopDetail(String[] query, int topK) {
		try {
			//VectorViewerIDF CSE494Viewer = new VectorViewerIDF();
			double twoNorm_q = Math.sqrt(query.length);
			
			// Preprocessing
			//Map<String,Double> IDF = CSE494Viewer.getIDF();
			//double[] twoNorm_di = CSE494Viewer.getTwoNorm_di(IDF);
			
			long startTime = new Date().getTime();
			
		  	// Calculate the numerator (dotProduct of q*di)
		  	System.out.println("Calculating numerator");
		  	long numeratorTime = new Date().getTime();
		  	ArrayList<int[]> doc_tf;
		  	Map<Integer,Double> dotProduct_qDi = new HashMap<Integer,Double>();
		  	for(int i=0; i<query.length; i++) {
		  		doc_tf = getDoc_tf(query[i]);
		  		for (int j=0; j<doc_tf.size(); j++) {
		  			int docId = doc_tf.get(j)[0];
		  			int freq = doc_tf.get(j)[1];
		  			if (!dotProduct_qDi.containsKey(docId))
		  				dotProduct_qDi.put(docId, (double)freq*IDF.get(query[i]));
		  				//dotProduct_qDi.put(docId, (double)freq*CSE494Viewer.getIDF(query[i]));
		  			else
		  				//dotProduct_qDi.put(docId, dotProduct_qDi.get(docId) + (freq*CSE494Viewer.getIDF(query[i])) );
		  				dotProduct_qDi.put(docId, dotProduct_qDi.get(docId) + (freq*IDF.get(query[i])) );
		  		}
		  	}
		  	long endNumeratorTime = new Date().getTime();
		  	System.out.println("Numerator time: " + (endNumeratorTime-numeratorTime));
		  	
		  	// Calculate the similarity, (dividing the dotProduct by the denominator)
		  	System.out.println("Calculating similarity");
		  	long similarityTime = new Date().getTime();
		  	Map<Integer,Double> similarity = new HashMap<Integer,Double>();
		  	Iterator<Integer> itr = dotProduct_qDi.keySet().iterator();
		  	while (itr.hasNext()) {
		  		int docId = itr.next();
		  		similarity.put(docId, dotProduct_qDi.get(docId)/(twoNorm_di[docId]*twoNorm_q));
		  	}
		  	long endSimilarityTime = new Date().getTime();
		  	System.out.println("Denominator time: " + (endSimilarityTime-similarityTime));
		  	
		  	// Sorting results
		  	System.out.println("Sorting documents");
		  	long sortingTime = new Date().getTime();
		  	FrequencyComparator2 fc = new FrequencyComparator2(similarity);
		  	TreeMap<Integer,Double> sortedSimilarity = new TreeMap(fc);
		  	sortedSimilarity.putAll(similarity);
		  	long endSortingTime = new Date().getTime();
		  	System.out.println("Sorting time: " + (endSortingTime-sortingTime));
		  	long endTime = new Date().getTime();
		  	System.out.println("Time spent: " + (endTime-startTime) + " milliseconds");
		  	
		  	// Return topK results
		  	int retValSize = Math.min(sortedSimilarity.keySet().size(), topK);
		  	Vector[] retVal = new Vector[retValSize];
		  	int k = 0;
		  	for (int doc : sortedSimilarity.keySet()) {
		  		if (k>=retValSize) {
		  			break;
	  		    }
		  		retVal[k] = new Vector();
		  		retVal[k].docId = doc;
		  		retVal[k].value = sortedSimilarity.get(doc);
		  		k++;
		  	}
		  	return retVal;
		}
	  	catch(Exception e) {
	  		System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
	  		return new Vector[0];
	  	}
	}

	// Unsorted
	public Map<Integer,Double> getAllDetail(String[] query, int topK) {
		try {
			//VectorViewerIDF CSE494Viewer = new VectorViewerIDF();
			double twoNorm_q = Math.sqrt(query.length);
			
			// Preprocessing
			//Map<String,Double> IDF = CSE494Viewer.getIDF();
			//double[] twoNorm_di = CSE494Viewer.getTwoNorm_di(IDF);
			
			long startTime = new Date().getTime();
			
		  	// Calculate the numerator (dotProduct of q*di)
		  	System.out.println("Calculating numerator");
		  	long numeratorTime = new Date().getTime();
		  	ArrayList<int[]> doc_tf;
		  	Map<Integer,Double> dotProduct_qDi = new HashMap<Integer,Double>();
		  	for(int i=0; i<query.length; i++) {
		  		doc_tf = getDoc_tf(query[i]);
		  		for (int j=0; j<doc_tf.size(); j++) {
		  			int docId = doc_tf.get(j)[0];
		  			int freq = doc_tf.get(j)[1];
		  			if (!dotProduct_qDi.containsKey(docId))
		  				dotProduct_qDi.put(docId, (double)freq*IDF.get(query[i]));
		  				//dotProduct_qDi.put(docId, (double)freq*CSE494Viewer.getIDF(query[i]));
		  			else
		  				//dotProduct_qDi.put(docId, dotProduct_qDi.get(docId) + (freq*CSE494Viewer.getIDF(query[i])) );
		  				dotProduct_qDi.put(docId, dotProduct_qDi.get(docId) + (freq*IDF.get(query[i])) );
		  		}
		  	}
		  	long endNumeratorTime = new Date().getTime();
		  	System.out.println("Numerator time: " + (endNumeratorTime-numeratorTime));
		  	
		  	// Calculate the similarity, (dividing the dotProduct by the denominator)
		  	System.out.println("Calculating similarity");
		  	long similarityTime = new Date().getTime();
		  	Map<Integer,Double> similarity = new HashMap<Integer,Double>();
		  	Iterator<Integer> itr = dotProduct_qDi.keySet().iterator();
		  	while (itr.hasNext()) {
		  		int docId = itr.next();
		  		similarity.put(docId, dotProduct_qDi.get(docId)/(twoNorm_di[docId]*twoNorm_q));
		  	}
		  	long endSimilarityTime = new Date().getTime();
		  	System.out.println("Denominator time: " + (endSimilarityTime-similarityTime));
		  	
		  	return similarity;
		}
	  	catch(Exception e) {
	  		System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
	  		return new HashMap<Integer,Double>();
	  	}
	}
}


