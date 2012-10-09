package edu.asu.cse494;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.index.TermDocs;
import com.lucene.index.TermEnum;
import com.lucene.index.TermPositions;

public class ForwardIndexWithFreq {

	private HashMap<Integer, HashMap<String,Double>> documents;
	
	public ForwardIndexWithFreq() {
		documents = new HashMap<Integer, HashMap<String,Double>>();
		
		System.out.println("Building Forward Index");
		try
		{
			IndexReader reader = IndexReader.open("result3index");
			TermEnum termenum = reader.terms();
			// Iterate through all the terms
			while(termenum.next() && termenum.term().field().equals("contents"))
			{
			   Term termval = termenum.term();
			   TermDocs termdocs = reader.termDocs(termval);
			   // Iterate through each term appearance in each doc
			   while (termdocs.next()) {
				   
				   // Retrieve the terms list if it exists, otherwise create a new one
				   HashMap<String,Double> terms;
				   if (documents.containsKey(termdocs.doc())) 
					   terms = documents.get(termdocs.doc());
				   else
					   terms = new HashMap<String,Double>();
				   
				   // Store it in documents hashmap
				   terms.put(termval.text(), (double)termdocs.freq());
				   documents.put(termdocs.doc(), terms);
			   }
			   
			   //System.out.println(termval.text());
			}
		}
		catch(Exception e) {System.out.println(e);}
	}
	
	// Get the list of terms belonging to a document id
	public HashMap<String,Double> getTerms(int doc) {
		return documents.get(doc);
	}
	
	// Get the frequency for a specific doc and term
	public double getFrequency(int doc, String term) {
		if (documents.get(doc).containsKey(term))
			return documents.get(doc).get(term);
		return 0;
	}
	
	// Test method
	public static void main(String args[]) {
		ForwardIndexWithFreq testFI = new ForwardIndexWithFreq();
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			while(true) {
				System.out.println("Input Doc Id: ");
				int docId = Integer.parseInt(in.readLine());
				System.out.println("Input term: ");
				String term = in.readLine().toLowerCase();
	        	
	    		System.out.println("Frequency: " + testFI.getFrequency(docId, term) + "\n\n");
			}
		} 
		catch(Exception e) {System.out.println(e);}
	}
}

