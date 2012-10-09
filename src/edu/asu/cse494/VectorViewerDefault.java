package edu.asu.cse494;
import com.lucene.index.*;
import java.io.*;


public class VectorViewerDefault {
	int count=0;
	//display the vector
	public void  showVector()
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
				System.out.println("The Term :" + termval.text() + " Frequency :"+termenum.docFreq() + " contents?: " + termval.field());
				
				
			   //Add following here to retrieve the <docNo,Freq> pair for each term
			   TermDocs termdocs = reader.termDocs(termval);
			   while (termdocs.next())
				   System.out.print(termdocs.doc() + " " + termdocs.freq() + "\n");
			   System.out.println();
			   
			   //to retrieve the <docNo,Freq,<pos1,......posn>> call
			   TermPositions termpositions = reader.termPositions(termval);
			   while (termpositions.next()) {
				   int i = 0;
				   while (i < termpositions.freq()) {
					   System.out.print(termpositions.doc() + " " + termpositions.freq() + " " + termpositions.nextPosition() + "\n");
					   i++;
				   }
			   }
			   
			}
			System.out.println(" Total terms : " + count);
		
		}
		catch(IOException e){
		    System.out.println("IO Error has occured: "+ e);
		    return;
		}
	}


	public static void main(String[] args)
	{
		VectorViewerDefault CSE494Viewer = new VectorViewerDefault();
		CSE494Viewer.showVector();
		System.out.println(" Total terms : " + CSE494Viewer.count);
	}
}
