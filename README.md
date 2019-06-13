# RetrievabilityCalculator

General Description :
---------------------
It is used to calculate retrievability (**retrieval possibility**) of a set of documents From Lucene4IR Index
This Retrievability Calculator is based on the attached paper 
[Retrievability An Evaluation Measure for Higher Order Information Access Tasks.pdf](https://github.com/ABDULAZIZALQATAN/RetrievabilityCalculator/blob/master/Retrievability%20An%20Evaluation%20Measure%20for%20Higher%20Order%20Information%20Access%20Tasks.pdf)
to calculate the document retievability  of a range of documents from a [Lucene4IR](https://github.com/lucene4ir/lucene4ir/blob/master/README.md) index File 

It is created using :
---
![IntellJ Version 2019.1.3](https://github.com/ABDULAZIZALQATAN/RetrievabilityCalculator/blob/master/Explanations/1-%20IntelliJ%20Version.jpg)

Running Steps :
-------------------------
### 1. Download [Lucene4IR](https://github.com/lucene4ir/lucene4ir/blob/master/README.md)
### 2. Place Src files and parameter files in a new project 
![Place files in a new project](https://github.com/ABDULAZIZALQATAN/RetrievabilityCalculator/blob/master/Explanations/2-%20Placing%20Files.jpg)
### 3. Create a configuration for Retrievability Calculator 
![Create Configuration](https://github.com/ABDULAZIZALQATAN/RetrievabilityCalculator/blob/master/Explanations/3-%20Configuration.jpg)
### 4. Index Some data using [IndexerApp of Lucene4IR](https://github.com/lucene4ir/lucene4ir/blob/master/README.md) 
![Indexing Data](https://github.com/ABDULAZIZALQATAN/RetrievabilityCalculator/blob/master/Explanations/4-%20Index%20Data.jpg)
### 5. Fill Retrievability input XML parameters file as your needs then run the code 
![Fill Parameters](https://github.com/ABDULAZIZALQATAN/RetrievabilityCalculator/blob/master/Explanations/6-%20Run%20Code.jpg)
### 6. if the run succceed you get a results like this 
![Output Results](https://github.com/ABDULAZIZALQATAN/RetrievabilityCalculator/blob/master/Explanations/7-%20Output.jpg)
 
Output Files 
---
1. CommonCoreResults.res >> Retrieval Results
2. DocRetrievability.txt >> List of all documents with their r values 
3. longQueryGrams.txt >> Detailed list of grams (QryID , QryName / Counter , weight)
4. queryGram.txt >> Detailed list of grams (QryID , QryName ) used for Retrieval process
