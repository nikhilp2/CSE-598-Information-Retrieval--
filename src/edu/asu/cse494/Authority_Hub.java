package edu.asu.cse494;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.lucene.index.IndexReader;

public class Authority_Hub {

	static VectorViewerIDF viewer;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		viewer = new VectorViewerIDF();
		
        try {
	    	IndexReader reader = IndexReader.open("result3index");
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	        while (true) {
	        	// Read input
	        	System.out.print("Query: ");
	        	String line = in.readLine().toLowerCase();
	
			  	if (line.length() == -1)
			  		break;
			  	
			  	// Get Results
			  	AuthHub results = getTop(line);
			  	
			  	// Print Results
			  	System.out.println("Top 10 Hubs:");
			  	for (int i=0; i<10; i++) {
			  		System.out.println(results.hub[i].docId + " " + results.hub[i].value + " " + reader.document(results.hub[i].docId).getField("url").stringValue());
			  	}
			  	
			  	System.out.println("Top 10 Auth:");
			  	for (int i=0; i<10; i++) {
			  		System.out.println(results.auth[i].docId + " " + results.auth[i].value + " " + reader.document(results.auth[i].docId).getField("url").stringValue());
			  	}
	        }
        }
        catch(IOException e)
        {
        	System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
        }
	}

	public static AuthHub getTop(String line) {
		int N = 0;
		try {
			N = IndexReader.open("result3index").numDocs();
		}
		catch (Exception e){ System.out.println(e); }
		
		String[] query = line.split("\\s+");
  		
	  	// Print query
	  	for(int i=0; i<query.length; i++)
	  		System.out.println(i+1 + ") " + query[i]);		
		
	  	// Obtain rootset
	  	long startRootSet = new Date().getTime();
		int[] rootSet = viewer.getTop(query, 10);
		long endRootSet = new Date().getTime();
		System.out.println("Obtaining rootset: " + (endRootSet-startRootSet));
		
		// Top-K
		System.out.println("Top-K");
		for (int i : rootSet)
			System.out.println(i);
	
		// Adjacency Matrix is computed from the link analysis on the fly
		LinkAnalysis linkAnalysis = new LinkAnalysis();
		HashMap<Integer, Integer> baseSet = new HashMap<Integer,Integer>();
		
		// Create the baseset by adding all the document ids from root set, the documents the root set is pointing to, and the documents that are pointing to the root set.
		long startBaseSet = new Date().getTime();
		for (int i=0; i<rootSet.length; i++) {
			if (!baseSet.containsKey(rootSet[i])) {
				baseSet.put(rootSet[i], 1);
				int[] outLinks = linkAnalysis.getLinks(rootSet[i]);
				int[] inLinks = linkAnalysis.getCitations(rootSet[i]);
				for (int j=0; j < outLinks.length; j++)
					if (!baseSet.containsKey(outLinks[j])) {
						baseSet.put(outLinks[j], 1);
					}
				for (int j=0; j < inLinks.length; j++)
					if (!baseSet.containsKey(inLinks[j]))
						baseSet.put(inLinks[j], 1);
			}
		}
		long endBaseSet = new Date().getTime();
		System.out.println("Create baseset: " + (endBaseSet-startBaseSet));
		
		// Create the Authority and Hub vectors and initialize them
		double[] authVector_old = new double[N];
		double[] hubVector_old = new double[N];
		Vector[] authVector = new Vector[N];
		Vector[] hubVector = new Vector[N];
		for (int i=0; i<N; i++) {
			authVector[i] = new Vector();
			authVector[i].docId = i;
			//authVector[i].value = 1;
			hubVector[i] = new Vector();
			hubVector[i].value = 1;
			hubVector[i].docId = i;
		}
		
		long startLoop = new Date().getTime();
		long authTime = 0;
		long hubTime = 0;
		long normTime = 0;
		long convergenceTime = 0;
		int loop = 1;
		float threshold = .00001f;
		Iterator<Integer> baseSetIterator = baseSet.keySet().iterator();
		while (loop < 50) { // Iterate through 50 loops or until it converges
			System.out.println("Loop #:" + loop);
			long startSingleLoop = new Date().getTime();
			
			// Iterate through each page in the baseSet to compute the authority vector
			long startAuthority = new Date().getTime();
			while (baseSetIterator.hasNext()) {
				int docId = baseSetIterator.next();
				int[] outLinks = linkAnalysis.getLinks(docId);
				for (int outL : outLinks)  // a_i = A' * h_i-1 (for each outLinks, it's the same as for each 1 in A' row)
					authVector[docId].value += hubVector[outL].value; 
			}
			long endAuthority = new Date().getTime();
			System.out.println("Authority Computation: " + (endAuthority-startAuthority));
			
			// Iterate through each page in the baseSet to compute the hub vector
			long startHub = new Date().getTime();
			baseSetIterator = baseSet.keySet().iterator();
			while (baseSetIterator.hasNext()) {
				int docId = baseSetIterator.next();
				int[] inLinks = linkAnalysis.getCitations(docId);
				for (int inL : inLinks) // h_i = A * a_i (for each inLinks, it's the same as for each 1 in A row)
					hubVector[docId].value += authVector[inL].value;
			}
			long endHub = new Date().getTime();
			System.out.println("Hub Computation: " + (endHub-startHub));
			
			// Normalize
			long startNormalize = new Date().getTime();
			double auth_denominator = 0;
			double hub_denominator = 0;
			for (int i=0; i<N; i++) {
				auth_denominator += Math.pow(authVector[i].value, 2);
				hub_denominator += Math.pow(hubVector[i].value, 2);
			}
			for (int i=0; i<N; i++) {
				authVector[i].value /= Math.sqrt(auth_denominator);
				hubVector[i].value /= Math.sqrt(hub_denominator);
			}
			long endNormalize = new Date().getTime();
			System.out.println("Normalization: " + (endNormalize-startNormalize));
			
			// Compute differences to check for convergence
			long startConvergence = new Date().getTime();
			double authDiff = 0;
			double hubDiff = 0;
			for (int i=0; i<N; i++) {
				authDiff += Math.pow(authVector[i].value - authVector_old[i],2);
				hubDiff += Math.pow(hubVector[i].value - hubVector_old[i], 2);
			}
			long endConvergence = new Date().getTime();
			System.out.println("Convergence: " + (endConvergence-startConvergence));

			// Check for convergence
			if (Math.sqrt(authDiff) < threshold && Math.sqrt(hubDiff) < threshold)
				break;
			else
				// Copy new values to old values
				for (int i=0; i<N; i++) {
					authVector_old[i] = authVector[i].value;
					hubVector_old[i] = hubVector[i].value;
				}
			
			long endSingleLoop = new Date().getTime();
			System.out.println("Single Loop: " + (endSingleLoop-startSingleLoop));
			authTime += (endAuthority-startAuthority);
			hubTime += endHub-startHub;
			normTime += endNormalize-startNormalize;
			convergenceTime += endConvergence-startConvergence;
			loop++;
		}
		long endLoop = new Date().getTime();
		System.out.println("Total Looping time: " + (endLoop-startLoop));
		System.out.println("Total Auth time: " + authTime);
		System.out.println("Total Hub time: " + hubTime);
		System.out.println("Total Norm time: " + normTime);
		System.out.println("Total Convergence time: " + convergenceTime);
		
		// sort auth & hubs
		long startSort = new Date().getTime();
		Arrays.sort(authVector, new VectorComparator());
		Arrays.sort(hubVector, new VectorComparator());
		long endSort = new Date().getTime();
		System.out.println("Sorting: " + (endSort-startSort));
		
		return new AuthHub(authVector, hubVector);
	}
}

// Class to return the results
class Vector
{
	int docId;
	double value;
}

// Class to compare the values and help sorting
class VectorComparator implements Comparator<Vector>
{
	float threshold = 0.00001f;
	
	public int compare(Vector a, Vector b) {
		if(a.value < b.value) 
			return 1;
		 else if(a.value == b.value) 
			return 0;
		 else 
			return -1;
	}
}

// Class that has all the authority and hub results in one object
class AuthHub
{
	public Vector[] auth;
	public Vector[] hub;
	
	public AuthHub(Vector[] auth, Vector[] hub)
	{
		this.auth = auth;
		this.hub = hub;
	}
}