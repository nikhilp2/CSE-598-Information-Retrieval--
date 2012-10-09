package edu.asu.cse494;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import com.lucene.index.IndexReader;
import com.lucene.index.Term;
import com.lucene.index.TermDocs;
import com.lucene.index.TermEnum;

public class Clustering {

	final int N = 50;
	ForwardIndexWithFreq forwardIndex;
	VectorViewerIDF vectorViewer;
	IndexReader reader; 
	
	public Clustering() {
		forwardIndex = new ForwardIndexWithFreq();
		vectorViewer = new VectorViewerIDF();
		try{
			reader = IndexReader.open("result3index");
		}
		catch (IOException e) {System.out.println(e);}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Clustering c = new Clustering();
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	        while (true) {
	        	System.out.print("Query: ");
	        	String line = in.readLine().toLowerCase();

			  	if (line.length() == -1)
			  		break;
			  	
			  	long startTime = new Date().getTime();
			  	String[] query = line.split("\\s+");
			  		
			  	// Print query
			  	for(int i=0; i<query.length; i++)
			  		System.out.println(i+1 + ") " + query[i]);
			  	
			  	System.out.print("k: ");
	        	int k = Integer.parseInt(in.readLine());
	        	
	        	// Get top N results from tf-idf
	        	Vector[] topN = c.vectorViewer.getTopDetail(query, c.N);
	        	//HashMap<Integer,Double> simValues = new HashMap<Integer,Double>();
	        	for (int i=0; i<topN.length; i++) {
	        		System.out.println(topN[i].docId + " " + topN[i].value);
	        		//simValues.put(topN[i].docId, topN[i].value);
	        	}
	        	
	        	// Retrieve clusters
	        	long startClustersTime = new Date().getTime();
	        	HashMap<Integer,Vector[]> results = c.GetClusters(k, query, topN);
	        	long endClustersTime = new Date().getTime();
	        	System.out.println("Complete Clusters Calculation Time: " + (endClustersTime-startClustersTime));
	        	
	        	// Print clusters 
	        	System.out.println("Final Clusters:");
	        	Iterator<Integer> itr = results.keySet().iterator();
	        	while (itr.hasNext()) {
	        		int clusterNum = itr.next();
	        		Vector[] cluster = results.get(clusterNum);
	        		System.out.println("Cluster #" + clusterNum);
	        		for (int i=0; i<cluster.length; i++) {
	        			System.out.println("\t" + cluster[i].docId + "\t" + cluster[i].value + "\t" + c.reader.document(cluster[i].docId).getField("url").stringValue());
	        		}
	        	}
	        }
			
		}
		catch (IOException e) {System.out.println(e);}
	}

	public HashMap<Integer,Vector[]> GetClusters(int k, String[] query, Vector[] topN) throws IOException{
		ArrayList<HashMap<String,Double>> centroids = new ArrayList<HashMap<String,Double>>();
		HashMap<Integer,ArrayList<Integer>> clusters = new HashMap<Integer,ArrayList<Integer>>();
		HashMap<Integer,ArrayList<Integer>> oldClusters = new HashMap<Integer,ArrayList<Integer>>();

		// Obtain random centroids
		System.out.println("Obtain random centroids");
		Random randomGen = new Random();
		ArrayList<Integer> randomNumbers = new ArrayList<Integer>();
		for (int i=0; i<k; i++) {
			int randTemp = randomGen.nextInt(N);
			if (!randomNumbers.contains(randTemp))
				randomNumbers.add(randTemp);
			else
				i--;
		}
		System.out.println("Random Numbers:");
		for (int i=0; i<randomNumbers.size(); i++)
			System.out.println(randomNumbers.get(i));
		
		// Initialize centroids
		System.out.println("Initialize centroids");
		for (int i=0; i<k; i++) {
			HashMap<String,Double> centroid = new HashMap<String,Double>();
			centroid = forwardIndex.getTerms(randomNumbers.get(i));
			centroids.add(centroid);
		}
		System.out.println("# of centroids: " + centroids.size());
		
		int iteration = 1;
		while (true) {
			
			// Compute similarity of each document with each centroid
			long startSimTime = new Date().getTime();
			System.out.println("Compute similarity of each document with each centroid");
			HashMap<Integer,ArrayList<Double>> simWithCentroids = new HashMap<Integer,ArrayList<Double>>();
			for (int i=0; i<topN.length; i++) {
				ArrayList<Double> oneTermSimWithCentroids = new ArrayList<Double>();
				for (int j=0; j<k; j++){
					oneTermSimWithCentroids.add(similarity(topN[i].docId, centroids.get(j)));
				}
				simWithCentroids.put(topN[i].docId,oneTermSimWithCentroids); 
			}
			long endSimTime = new Date().getTime();
			System.out.println("Similarity of each document with each centroid:");
			for (int i=0; i<topN.length; i++) {
				ArrayList<Double> oneTermSimWithCentroids = simWithCentroids.get(topN[i].docId);
				System.out.println(topN[i].docId);
				for (int j=0; j<oneTermSimWithCentroids.size(); j++)
					System.out.println("\t" + oneTermSimWithCentroids.get(j));
			}
			System.out.println("Similarity computation time: " + (endSimTime-startSimTime));
			
			// Assign current clusters to old Clusters
			oldClusters = clusters;
			
			// Assign/Distribute docs to clusters
			System.out.println("Assign to clusters"); 
			clusters = new HashMap<Integer,ArrayList<Integer>>();
			for (int i=0; i<topN.length; i++) {
				ArrayList<Integer> tempCluster;
				int clusterNum = getIdxOfHighestValue(simWithCentroids.get(topN[i].docId));
				if (clusters.containsKey(clusterNum))
					tempCluster = clusters.get(clusterNum);
				else
					tempCluster = new ArrayList<Integer>();
				tempCluster.add(topN[i].docId);
				clusters.put(clusterNum, tempCluster);
			}
			
			// Print current iteration clusters
			System.out.println("Iteration #" + iteration);
        	Iterator<Integer> itr = clusters.keySet().iterator();
        	while (itr.hasNext()) {
        		int clusterNum = itr.next();
        		ArrayList<Integer> cluster = clusters.get(clusterNum);
        		System.out.println("Cluster #" + clusterNum);
        		for (int i=0; i<cluster.size(); i++) {
        			System.out.print(cluster.get(i) + " ");
        		}
        		System.out.println();
        	}
			
			// Check for convergence, if it converges then break/end
			boolean converged = true;
			itr = clusters.keySet().iterator(); 
			while(itr.hasNext()) {
				int clusterNum = itr.next();
				if (!oldClusters.containsKey(clusterNum)) {
					converged = false;
					break;
				}
				
				ArrayList docsInCluster = clusters.get(clusterNum);
				ArrayList docsInOldCluster = oldClusters.get(clusterNum);
				if (docsInCluster.size() != docsInOldCluster.size()) {
					converged = false;
					break;
				}
				
				for (int i=0; i<docsInCluster.size(); i++) {
					if (!docsInOldCluster.contains(docsInCluster.get(i))) {
						converged = false;
						break;
					}
				}
			}
			
			if (converged)
				break;
			
			// Compute new centroid, average of all the documents in the cluster. It's a vector.
			System.out.println("Computing new centroids");
			long startCentroidTime = new Date().getTime();
			itr = clusters.keySet().iterator(); 
			while(itr.hasNext()) { 
				int clusterNum = itr.next(); 
				ArrayList<Integer> docsInCluster = clusters.get(clusterNum);
				centroids.set(clusterNum, getAverageInCluster(docsInCluster));
			}
			long endCentroidTime = new Date().getTime();
			System.out.println("Computing Centroids time: " + (endCentroidTime-startCentroidTime));
			
			iteration++;
		}
		
		// Get the most predominant keywords from each centroid and apply it as a label to each belonging cluster
		//<String,Double>
		ArrayList<TreeMap<String,Double>> sortedCentroids = new ArrayList<TreeMap<String,Double>>();
		for (int i=0; i<centroids.size(); i++) {
			ValueComparator bvc =  new ValueComparator(sortedCentroids.get(i));
			TreeMap<String,Double> sortedSingleCentroid = new TreeMap<String,Double>(bvc);
			sortedSingleCentroid.putAll(sortedCentroids.get(i));
			sortedCentroids.add(sortedSingleCentroid);
		}
		
		// Mapping of doc Id with query similarity value
    	HashMap<Integer,Double> simValues = new HashMap<Integer,Double>();
    	for (int i=0; i<topN.length; i++) {
    		simValues.put(topN[i].docId, topN[i].value);
    	}
		
		// Sort the documents in the clusters by similarity values (already present from arguments)
		HashMap<Integer,Vector[]> retVal = new HashMap<Integer,Vector[]>();
		Iterator<Integer> itr = clusters.keySet().iterator();
		while(itr.hasNext()) { 
			int clusterNum = itr.next();
			ArrayList<Integer> docsInCluster = clusters.get(clusterNum);
			Vector[] docsInClusterCopy = new Vector[docsInCluster.size()];
			for (int i=0; i<docsInCluster.size(); i++) {
				Vector v = new Vector();
				v.docId = docsInCluster.get(i);
				v.value = simValues.get(v.docId);
				docsInClusterCopy[i] = v;
			}
			Arrays.sort(docsInClusterCopy, new VectorComparator());
			retVal.put(clusterNum, docsInClusterCopy);
		}
		
		return retVal;
	}	
	
	// Calculate the similarity between a document and a centroid
	private double similarity(int docId, HashMap<String,Double> centroid) {
		try {
			TermEnum termenum = reader.terms();
			double sum_dotProduct_tf_idf = 0;
			double twoNorm_centroid = 0;
			double twoNorm_doc = 0;
			
			// Compute numerator <doc_tf_idf>*<centroid_tf_idf>
			while(termenum.next() && termenum.term().field().equals("contents")) {
				Term termval = termenum.term();
				
				// Compute the tf_idf and add it up to the total
				double docTerm_tf_idf = forwardIndex.getFrequency(docId, termval.text()) * vectorViewer.IDF.get(termval.text());
				double centroidTerm_tf = centroid.get(termval.text()) != null ? centroid.get(termval.text()) : 0;
				double centroidTerm_tf_idf = centroidTerm_tf * vectorViewer.IDF.get(termval.text());
				sum_dotProduct_tf_idf += centroidTerm_tf_idf * docTerm_tf_idf;
				
				// Start calculating the two norm for the centroid
				twoNorm_centroid += Math.pow(centroidTerm_tf_idf, 2);
				twoNorm_doc += Math.pow(docTerm_tf_idf, 2);
			}
			
			//double denominator = vectorViewer.twoNorm_di[docId] * Math.sqrt(twoNorm_centroid);
			double denominator = Math.sqrt(twoNorm_doc) * Math.sqrt(twoNorm_centroid);
			return sum_dotProduct_tf_idf/denominator;
		} catch (IOException e) {System.out.println(e);}
		return 0;
	}
	
	// Return the index of the highest value in the arraylist
	private int getIdxOfHighestValue(ArrayList<Double> list) {
		double highValue = -1;
		int index = 0;
		for (int i=0; i<list.size(); i++) {
			if (list.get(i) > highValue) {
				highValue = list.get(i);
				index = i;
			}
		}
		return index;
	}

	// Helper method to compute the new centroids, which are the average of the cluster
	private HashMap<String,Double> getAverageInCluster(ArrayList<Integer> cluster) throws IOException {
			TermEnum termenum = reader.terms();
			HashMap<String,Double> retVal = new HashMap<String,Double>();
			
			// Compute average for each term and store it in hashmap of retVal
			while(termenum.next() && termenum.term().field().equals("contents")) {
				Term termval = termenum.term();
				
				double termAverage = 0;
				for (int i=0; i<cluster.size(); i++) {
					termAverage += forwardIndex.getFrequency(cluster.get(i), termval.text());
				}
				termAverage /= cluster.size();
				
				retVal.put(termval.text(), termAverage);
			}
			
			return retVal;
	}
}

class ValueComparator implements Comparator {

	  Map base;
	  public ValueComparator(Map base) {
	      this.base = base;
	  }

	  public int compare(Object a, Object b) {

	    if((Double)base.get(a) < (Double)base.get(b)) {
	      return 1;
	    } else if((Double)base.get(a) == (Double)base.get(b)) {
	      return 0;
	    } else {
	      return -1;
	    }
	  }
	}