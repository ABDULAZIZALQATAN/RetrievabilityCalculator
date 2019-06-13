import lucene4ir.Lucene4IRConstants;
import lucene4ir.RetrievalApp;
import javax.xml.bind.JAXB;
import java.io.*;
import java.util.*;


/**
 * Created by colin on 21/12/16.
 */

public class RetrievabilityCalculator {

    public enum gramTypes {
        uniGramOnly , biGramOnly , allGrams
    }

    // Public Variables
    public RetrievabilityCalculatorParams p;
    public ShilglesExtractor shilglesExtractor;
    public gramTypes targetGramType;
    public ArrayList<queryInfo> qryList;


    // docID (Base Key) -- > QryID (2nd Key) ---> rank (value)
    private HashMap<Integer,HashMap<Integer,Integer>> docMap;
    private String sourceParameterFile;

    /*
    Private Sub Functions
     */
    // Constructor Method
    public RetrievabilityCalculator(String inputParameters ) {
        System.out.println("Bigram&UnigramGenerator");
        qryList = new ArrayList<queryInfo>();
        targetGramType = gramTypes.allGrams;
        if (inputParameters.isEmpty())
            sourceParameterFile = "params/RetrievabilityCalculator.xml";
        else
            sourceParameterFile = inputParameters;
    }

    private void displayMsg(String msg)
    {
        System.out.println(msg);
        System.exit(0);
    }
    /*
    Generating Queries Methods
     */
    private boolean equalString (String str1 , String str2)
    {
        return str1.toLowerCase().compareTo(str2.toLowerCase()) == 0;
    }

    public void readBigramGeneratorParamsFromFile() {
        System.out.println("Reading Param File");
        try {

            p = JAXB.unmarshal(new File(sourceParameterFile), RetrievabilityCalculatorParams.class);

            if (p.indexName.toString().isEmpty())
                displayMsg ("IndexName Parameter is Missing");
            System.out.println("Index: " + p.indexName);
            if (p.qryShort.toString().isEmpty())
                displayMsg ("Qry Short File Parameter is Missing");
            System.out.println("Query Short File : " + p.qryShort);
            if (p.qryLong.isEmpty())
                displayMsg ("Qry Long File Parameter is Missing");
            System.out.println("Query Long File : " + p.qryLong);

            if(equalString(p.gramTypes.toLowerCase() , "all"))
                targetGramType = gramTypes.allGrams;
            else if(equalString(p.gramTypes.toLowerCase() , "bigram"))
                targetGramType = gramTypes.biGramOnly;
            else if(equalString(p.gramTypes.toLowerCase() , "unigram"))
                targetGramType = gramTypes.uniGramOnly;
            else
                displayMsg("Please Specify gramsTypes - unigram - bigram or all");

            if (p.biCutoff < 1) {
                p.biCutoff = 0;
            }
            System.out.println("biGram Cutoff: " + p.biCutoff);

            if (p.uniCutoff < 1) {
                p.uniCutoff = 0;
            }
            System.out.println("uniGram Cutoff: " + p.uniCutoff);

            if (p.field == null) {
                p.field = Lucene4IRConstants.FIELD_ALL;
            }
            System.out.println("Field: " + p.field);
        } catch (Exception e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
            System.exit(1);
        }
    }

    public void pruneGrams(boolean unigram){

        int cutoff;
        Set set;
        if (unigram)
        {
            set = shilglesExtractor.unimap.entrySet();
            cutoff = p.uniCutoff;
        }
           else
        {
            set = shilglesExtractor.bimap.entrySet();
            cutoff = p.biCutoff;
        }
        Iterator iterator = set.iterator();
        while(iterator.hasNext()) {
            Map.Entry me = (Map.Entry)iterator.next();
            if ((int)me.getValue() <= cutoff) {
                System.out.println("Remove Gram : " + me.getKey() + " , Whose Ctr : " + me.getValue());
                iterator.remove();
            }
        }
    }
    public static Comparator<queryInfo> weightComparator = new Comparator<queryInfo>() {
        /*
        This is an array sorting method based on the following url
        https://dzone.com/articles/sorting-java-arraylist
         */
        @Override
        public int compare(queryInfo o1, queryInfo o2) {
            return (o2.weight < o1.weight ? -1 : o2.weight == o1.weight ? 0 : 1) ;
        }
    };

    private void calculateScores(boolean uniGram)
    {
       /* This Function is Used To do the following :
           1- accept the input boolean (uniGram) to identify which query map to work on (unigram or bigram)
           2- calculate the weight for each query based on its type (unigram or bigram)
           3- Add the resultant query data to the final Query List
           4- Sort the Query List Descendingly based on Query Weight

          then it sorts the list b

        */
        Iterator it = shilglesExtractor.unimap.entrySet().iterator();
        Map.Entry currentMap;
        double pi , pj , pij ;
        long v1 , v2  ;

        queryInfo currentQry ;

        // Identify main Parameters for current gram according to its input type (Single , Bi)

        if (uniGram)
            it = shilglesExtractor.unimap.entrySet().iterator();
        else
            it = shilglesExtractor.bimap.entrySet().iterator();

        while (it.hasNext()) {
            currentMap = (Map.Entry) it.next();
            currentQry = new queryInfo();
            currentQry.qryCtr = (int) currentMap.getValue();
            currentQry.query = currentMap.getKey().toString() ;
            if (uniGram)
                // Unigram Score
                currentQry.weight = currentQry.qryCtr;
            else
            {
                // Bigram Score
                pij = (double) ((currentQry.qryCtr + 1.0) / (shilglesExtractor.bimap.size() + 1.0));
                String[] terms = currentQry.query.split(" ");
                v1 = 1;
                v2 = 1;
                if (shilglesExtractor.unimap.containsKey(terms[0]))
                    v1 =   shilglesExtractor.unimap.get(terms[0]);
                if (shilglesExtractor.unimap.containsKey(terms[1]))
                    v2 =  shilglesExtractor.unimap.get(terms[1]);
                pi = (double) ((v1 + 1.0) / (shilglesExtractor.unimap.size() + 1.0));
                pj = (double) ((v2 + 1.0) / (shilglesExtractor.unimap.size() + 1.0));
                currentQry.weight = Math.log(pij / (pi * pj));
            } // End Else
            qryList.add(currentQry);
            System.out.println("Added Query " + currentQry.query + " to QueryList");
        } // End while

    }
    private void outputGrams()
    {
        /*
        This Function is used to
          1-  print the filtered grams on the screen and in an output file
         */
        int qryID;
        String outLine;
            // Sort Query List Based on Score
            // after gathering all queries
            // Then Assign Query ID after Sorting
            Collections.sort(qryList,weightComparator);
            qryID = 1;
            try {
            PrintWriter prShort = new PrintWriter(p.qryShort);
            PrintWriter prLong = new PrintWriter(p.qryLong);

            for (queryInfo qry : qryList)
            {
                qry.qryID = qryID;
                prShort.write (String.format("%d %s\n",
                        qryID , qry.query));

                outLine = String.format("%d %s / %d %f\n",
                        qryID++ , qry.query, qry.qryCtr , qry.weight );
                prLong.write(outLine);
                System.out.print(outLine);
            } // End For
            prShort.close();
            prLong.close();
            } // End Try
            catch (Exception ex)
            {
                System.out.println("Error While Printing Query List - Line around 278");
            } // End Catch
    } // End Function

    public void finalizeQueries() {
        long totalGrams , uniGrams , biGrams;
        uniGrams = shilglesExtractor.unimap.size();
        biGrams = shilglesExtractor.bimap.size();
        totalGrams = uniGrams + biGrams;
        System.out.println("Total Bigrams: " + biGrams + System.lineSeparator() +
                           "Total Unigrams: " + uniGrams + System.lineSeparator() +
                            "Total grams : " + totalGrams
        );

        if (targetGramType == gramTypes.uniGramOnly)
            // Remove Uni-Grams Repetition < 5
            calculateScores(true);

        else if (targetGramType == gramTypes.biGramOnly)
            // Remove Bi-Grams Repetition < 20
            calculateScores(false);
        else
        {
            // Remove Uni-Grams Repetition < 5
            calculateScores(true);
            // Remove Bi-Grams Repetition < 20
            calculateScores(false);
        }
        outputGrams();
    }
    private void generateQueries  () throws IOException
    {
        // Reading Parameters from Retrievability Counter XML File
        readBigramGeneratorParamsFromFile();
        shilglesExtractor = new ShilglesExtractor(p.indexName);

        if (targetGramType == gramTypes.uniGramOnly)
            // Prune Unigram
            pruneGrams(true);
        else if (targetGramType == gramTypes.biGramOnly)
            // Prune bigram
            pruneGrams(false);
        else
        {
            // Prune Unigram
            pruneGrams(true);
            // Prune bigram
            pruneGrams(false);
        }

        finalizeQueries();
    }

    // Retrievability Methods
private  ArrayList<Integer> getDocumentVector () throws Exception
{

    ArrayList<Integer> result = new ArrayList<Integer>();

    for (int i =0 ; i < shilglesExtractor.reader.maxDoc() ; i++)
        result.add(Integer.parseInt(shilglesExtractor.reader.document(i).get("docnum")));
    return result;
}
private void createDocMap () throws Exception
{
    /*
    This Function is Used to Create Document Map From Retrieval File Which is
    MainHashmap <docID , SubHashMap>
    SubHashMap <QryID , Rank>
     */
    docMap = new HashMap<Integer, HashMap<Integer, Integer>>();
    Integer qryID , rank;
    String retrievalOutPath = "out/CommonCoreResults.res",
            retrievalParamsPath = sourceParameterFile,
            line,
            parts[];
    Integer docID;

    // Run Retrievbility Over Generated Query and Index
    RetrievalApp re = new RetrievalApp(retrievalParamsPath);
    re.processQueryFile();


    BufferedReader br = new BufferedReader(new FileReader(retrievalOutPath));

    line = br.readLine();

    while (line != null)
    {
        parts = line.split(" ",5);
        qryID = Integer.parseInt(parts[0]);
        docID = Integer.parseInt(parts[2]);
        rank = Integer.parseInt(parts[3]);
        if (docMap.containsKey(docID))
            docMap.get(docID).put(qryID,rank);
        else
           docMap.put(docID,new HashMap<Integer, Integer>(qryID,rank));
        line = br.readLine();
    };
    br.close();
}

private double getQryWeight (int qryID)
{
    /*
    This Function is used to get a Query Weight From qryList
    based on input qryID
     */
    double result = 0;
    for (queryInfo qry : qryList)
    {
        if (qry.qryID == qryID)
        {
            result = qry.weight;
            break;
        } // End if
    } // End For
    return result;
}
private double getRetrievability(Integer targetDocID) throws Exception
{
    final int maxResults = 100;
    double result = 0 , weight = 0 , currentR;
    int qryID , rank ;
    Iterator subHashIterator;

    if (docMap.containsKey(targetDocID))
    {
        // if The document is not exist in docMap retirievability = 0
        // Else Calculate Retrievability
        subHashIterator = docMap.get(targetDocID).entrySet().iterator();
       while (subHashIterator.hasNext())
       {
           Map.Entry currentHash = (Map.Entry) subHashIterator.next();
           qryID = (int) currentHash.getKey();
           rank = (int) currentHash.getValue();
           weight = getQryWeight(qryID);
           currentR = weight * rank;
           result += currentR;
       } // End While
    } // End if
    return result;
}
    public void start ()   {
        ArrayList<Integer> sourceDocIds;
        double r , gNumerator = 0 , gDenominator = 0;
        int N , i = 0;
        String outputLine;
        try
        {
            PrintWriter pr = new PrintWriter("out/DocRetrievability.txt");
            generateQueries();
            sourceDocIds = getDocumentVector();
            N = sourceDocIds.size();
            createDocMap();
            for (Integer docID : sourceDocIds)
            {
                r = getRetrievability (docID);
                outputLine = "Doc ID = " + docID + " And R = " + r + "\n";
                System.out.print(outputLine);
                pr.write(outputLine);
                gNumerator += (2 * ++i - N - 1) * r;
                gDenominator += r;
            }
            gDenominator *= N;
            System.out.println("G Coefficient = " + gNumerator / gDenominator);
            pr.close();
        }
       catch (Exception ex)
       {
           System.out.println(ex.getMessage());
       }
    }

    public static void main(String args[])
    {
        RetrievabilityCalculator ret = new RetrievabilityCalculator("");
        ret.start();
    }

};

// SubClasses
class queryInfo
{
    int qryCtr , qryID;
    String query;
    double weight;
}
class RetrievabilityCalculatorParams {
    public String indexName , qryShort , field , qryLong , gramTypes ;
    public int uniCutoff , biCutoff , maxResults;
    public float b , k;
}/**/