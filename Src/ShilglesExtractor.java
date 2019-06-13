import lucene4ir.Lucene4IRConstants;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

public class ShilglesExtractor {

    public IndexReader reader;
    public HashMap<String, Integer> bimap;
    public HashMap<String, Integer> unimap;

    public ShilglesExtractor (String indexName)
    {
        if (!indexName.isEmpty())
        {
            unimap = new HashMap<String, Integer>();
            bimap = new HashMap<String, Integer>();
            openReader(indexName);
            try {
                extractGramsFromText();
            }
            catch (Exception ex)
            {
                System.out.println("Error in Shingles Extraction");
            }
        }
    }


    private boolean unigramQuery (String query)
    {
        // Check whether the input query unigram or bigram
        // Unigram returns true , Bigram returns false
        return query.split(" ").length < 2;
    }
    private void openReader(String indexName) {
        try {
            reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexName)));

        } catch (IOException e) {
            System.out.println(" caught a " + e.getClass() +
                    "\n with message: " + e.getMessage());
        }

    }
    public void extractGramsFromText() throws IOException {
        Document doc;
        CharTermAttribute ca;
        Analyzer an = new StandardAnalyzer();

        TokenStream ts = an.tokenStream(null,"");
        ShingleFilter filter;
        String currentGram , all;

        for (int i = 0 ; i < reader.maxDoc() ; i++)
        {
            CharArraySet stopWords = EnglishAnalyzer.getDefaultStopSet();
            doc = reader.document(i);
            all = doc.get(Lucene4IRConstants.FIELD_ALL);
            ts = an.tokenStream(null,all);
            ts = new StopFilter(ts,stopWords);
            filter = new ShingleFilter(ts);
            filter.reset();
            ca = filter.addAttribute(CharTermAttribute.class);

            while(filter.incrementToken())
            {
                currentGram = ca.toString().trim();
                if (currentGram.contains("_"))
                    continue;
                else if (unigramQuery(currentGram))
                // Unigram
                {
                    if (unimap.containsKey(currentGram))
                        unimap.put(currentGram , unimap.get(currentGram)+1);
                    else
                        unimap.put(currentGram,1);
                    System.out.println("Added Unigram : " + currentGram);
                }
                else
                // BiGram
                {
                    if (bimap.containsKey(currentGram))
                        bimap.put(currentGram , bimap.get(currentGram)+1);
                    else
                        bimap.put(currentGram,1);
                    System.out.println("Added Bigram : " + currentGram);
                }
            } // End while(filter.incrementToken())
            ts.close();
        } // End  for (int i = 0 ; i < reader.maxDoc() ; i++)
    }

}
