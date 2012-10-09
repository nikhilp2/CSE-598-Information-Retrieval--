package edu.asu.cse494;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.index.TermEnum;
import com.lucene.index.TermPositions;

public class ForwardIndex {

	private HashMap<Integer,ArrayList<TermDetail>> documents;
	static int numDocs;
	
	public ForwardIndex() {
		documents = new HashMap<Integer,ArrayList<TermDetail>>();
		
		System.out.println("Building Forward Index");
		try
		{
			IndexReader reader = IndexReader.open("result3index");
			numDocs = reader.numDocs();
			TermEnum termenum = reader.terms();
			// Iterate through all the terms
			while(termenum.next() && termenum.term().field().equals("contents"))
			{
			   Term termval = termenum.term();
			   TermPositions termpositions = reader.termPositions(termval);
			   // Iterate through each term appearance in each doc
			   while (termpositions.next()) {
				   int i = 0;
				   while (i < termpositions.freq()) {
					   
					   // Retrieve the terms list if it exists, otherwise create a new one
					   ArrayList<TermDetail> terms;
					   if (documents.containsKey(termpositions.doc())) 
						   terms = documents.get(termpositions.doc());
					   else
						   terms = new ArrayList<TermDetail>();
					   
					   // Copy values to a temporary variable
					   TermDetail td = new TermDetail();
					   td.term = termval.text();
					   td.position = termpositions.nextPosition();
					   
					   // Store it in documents hashmap
					   terms.add(td);
					   documents.put(termpositions.doc(), terms);
					   i++;
				   }
			   }
			}
			
			// Sort the terms by position in each arraylist of the forward index
			System.out.println("Sorting Forward Index Terms by position");
			for (int i=0; i<=numDocs; i++) {
				if (documents.containsKey(i)) {
					ArrayList temp = documents.get(i);
					Collections.sort(temp, new Comparator(){
						public int compare(Object o1, Object o2) {
							TermDetail t1 = (TermDetail)o1;
							TermDetail t2 = (TermDetail)o2;
							return (t1.position<t2.position ? -1 : (t1.position==t2.position) ? 0 : 1);
						}
					});
					documents.put(i, temp);
				}
			}
		}
		catch(Exception e) {System.out.println(e);}
	}
	
	// Get the list of terms belonging to a document id
	public ArrayList<TermDetail> getTerms(int doc) {
		return documents.get(doc);
	}
	
	// Test method
	public static void main(String args[]) {
		ForwardIndex testFI = new ForwardIndex();
		
		try {
			ArrayList<TermDetail> temp = new ArrayList<TermDetail>();
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			for (int i=0; i<=numDocs; i++) {
	        	System.out.println("DocID: " + i);
	        	temp = testFI.getTerms(i);
        		if (temp.size() > 0)
        		{
        			for (int j=0; j<temp.size(); j++) {
        				System.out.print(temp.get(j).term + " ");
        			}
        		}
        		else
        			System.out.println("Null");
	        	System.out.println();
	        	System.out.print("Press any key for next document");
	        	String line = in.readLine().toLowerCase();
	
			  	if (line.length() == -1)
			  		break;
	        }
		} 
		catch(Exception e) {System.out.println(e);}
	}
}

// Type of Objects that the ArrayList in the documents hashmap is storing
class TermDetail {
	public int position;
	public String term;
	public double tf_idf;
	
	public TermDetail() {
		position = 0;
		term = new String();
		tf_idf = 0d;
	}
}