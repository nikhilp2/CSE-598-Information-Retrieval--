package edu.asu.cse494;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Map;

import com.lucene.index.IndexReader;

public class PageRank {

	static float threshold = .000001f;
	static double c = 0.85;
	static LinkAnalysis analysis = new LinkAnalysis();
	static int N;
	static double lastpart_M_star;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		VectorViewerIDF idf = new VectorViewerIDF(); //Preprocess idf
		Vector[] pr = getPageRank2(); //Preprocess general pagerank
		try {
	    	IndexReader reader = IndexReader.open("result3index");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	        while (true) {
	        	System.out.print("Query: ");
	        	String line = in.readLine().toLowerCase();
	
			  	if (line.length() == -1)
			  		break;
			  	
			  	System.out.print("Weight: ");
			  	String w = in.readLine();
			  	
			  	if (w.length() == -1)
			  		break;
			  	
			  	double weight = Double.parseDouble(w);
			  	
			  	//Vector[] results = getPageRank2();
			  	
			  	Vector[] results = getTop(line, weight, idf, pr);
			  	
			  	System.out.println("Pagerank:");
			  	for (int i=0; i<100; i++) {
			  		System.out.println(results[i].docId + " " + results[i].value + " " + reader.document(results[i].docId).getField("url").stringValue());
			  	}
	        }
        }
        catch(IOException e)
        {
        	System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }

	}

	public static Vector[] getTop(String query, double weight, VectorViewerIDF idf, Vector[] pr)
	{
		try {
			N = IndexReader.open("result3index").numDocs();
		}
		catch (Exception e){ System.out.println(e); }
		
		String[] q = query.split("\\s+");
		
		// Retrieve TF-IDF of query related documents  
		Map<Integer,Double> idfValues = idf.getAllDetail(q, N+1);
		
		// Initialize return vector
		Vector[] retVal = new Vector[N];
		for (int i=0; i<retVal.length; i++) {
			retVal[i] = new Vector();
			retVal[i].value = 0;
			retVal[i].docId = i;
		}
		
		// Combine pagerank and vector space similarity
		for (int doc : idfValues.keySet()) {
			retVal[doc].value = weight * pr[doc].value + (1-weight)* idfValues.get(doc);
		}
		
		// Sort
		Arrays.sort(retVal, new VectorComparator());
		
		return retVal;
	}
	
	public static Vector[] getPageRank2() {
		try {
			N = IndexReader.open("result3index").numDocs();
		}
		catch (Exception e){ System.out.println(e); }
		
		lastpart_M_star = (double)(1-c)/N; //constant
		Vector[] pr = new Vector[N];
		double[] pr_old = new double[N];
		for (int i=0; i<N; i++) {
			pr_old[i] = (double)1/N;
			pr[i] = new Vector();
			pr[i].docId = i;
			pr[i].value = (double)1/N;
		}
		
		// M*= c (M + Z) * pr_i-1 + (1 – c) * K * pr_i-1
		
		int loop=1;
		while (true) {
			System.out.println("Loop#" + loop++);
			// Iterate through each doc
			for (int i=0; i<N; i++) {
				if (analysis.getLinks(i).length == 0) // Sink nodes
				{
					pr[i].value += (double)pr_old[i]/N; // 1/N
				}
				else // Not sink node
				{
					int[] outLinks = analysis.getLinks(i);
					for (int j=0; j<outLinks.length; j++) {
						pr[j].value += (double)pr[i].value/outLinks.length; // 1/(number of outlinks)
					}
				}
			}
			
			// Find the maximum pagerank to use in normalization
			double maximumPagerank = 0;
			for (int i=0; i<N; i++) {
				pr[i].value = c * pr[i].value + (1-c)/N;
				maximumPagerank = maximumPagerank < pr[i].value ? pr[i].value : maximumPagerank;
			}
			
			// Normalize
			for (int i=0; i<N; i++) {
				pr[i].value = (double)pr[i].value/maximumPagerank;
			}
			
			// Check for conversion
			if (hasConverged(pr, pr_old)) {
				break;
			}
			else
				for (int i=0; i<pr_old.length; i++) 
					pr_old[i] = pr[i].value;
		}
		
		return pr;
	}
	
	public static Vector[] getPageRank()
	{
		// M*= c (M + Z) + (1 – c) * K
		try {
			N = IndexReader.open("result3index").numDocs();
		}
		catch (Exception e){ System.out.println(e); }
		
		lastpart_M_star = (double)(1-c)/N;
		Vector[] pr = new Vector[N];
		double[] pr_old = new double[N];
		for (int i=0; i<N; i++) {
			pr_old[i] = (double)1/N;
			pr[i] = new Vector();
			pr[i].docId = i;
			pr[i].value = (double)1/N;
		}
		
		int loop = 1;
		boolean loopFlag;
		do {
			System.out.println("Loop #:" + loop);
			double maxPagerank = 0;
			for (int i=0; i<N; i++) {
				if (i%5000 == 0)
					System.out.println("PageRank docId:" + i);
				for (int j=0; j<N; j++) {
					pr[i].value += (double)M_star(i,j) * (double)pr_old[j];
				}
				maxPagerank = maxPagerank < pr[i].value ? pr[i].value : maxPagerank;
			}
			
			// Normalize
			for (int i=0; i<N; i++)
				pr[i].value /= (double)maxPagerank;
			
			// check for convergence
			loopFlag = hasConverged(pr, pr_old);
			if (!loopFlag) {
				for (int i=0; i<pr_old.length; i++) {
					pr_old[i] = pr[i].value;
				}
			}
			loop++;
		} while(!loopFlag && loop<50);
		
		return pr;
	}
	
	private static double M_star(int row, int column) {
		// M*= c (M + Z) + (1 – c) * K
		return (c * (mPlusZ(row,column))) + lastpart_M_star;
	}
	
	private static double mPlusZ (int docId, int linkedDocId) {
		// (M + Z)
		int[] outLinks = analysis.getLinks(docId);
		
		// Column is all zeros or sink node
		if (outLinks.length == 0) {
			return (double)1/N;
		}
		
		boolean doesContainLink = false;
		for (int i=0; i<outLinks.length; i++) {
			if (outLinks[i] == linkedDocId) {
				doesContainLink = true;
				break;
			}
		}
		
		// if it contains linked doc
		if (doesContainLink)
			return (double)1/outLinks.length;
		else // it doesn't contain linked doc
			return 0;
	}
	
	// Helper method to check if results have converged or not
	private static boolean hasConverged(Vector[] pr, double[] oldPr)
	{
		double difference = 0;
		for (int i=0; i<oldPr.length; i++)
		{
			difference += Math.pow(pr[i].value - oldPr[i], 2);
		}
		if (Math.sqrt(difference) < threshold) 
			return true;
		else
			return false;
	}
}
