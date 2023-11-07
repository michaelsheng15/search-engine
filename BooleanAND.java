import java.io.*;
import java.util.*;

import static java.util.Comparator.comparingInt;

public class BooleanAND {
    //will return a set of all documents that contain ALL the query words

    static HashMap<Integer, ArrayList<Integer>> invertedIndex;
    static HashMap<String, Integer> termToIdLexicon;
    static HashMap<Integer, String> idToTermLexicon;
    static HashMap<Integer, String> idToDocnoMap = new HashMap<>();
    static HashMap<String, Integer> docnoToIdMap = new HashMap<>();


    public static void main(String[] args) throws IOException, ClassNotFoundException {

        //check that enough arguments are provided
        if (args.length < 3) {
            System.out.println("Insufficient arguments provided");
            System.out.println("Please provide path to LA Times index, queries text file and results text file");
            System.exit(0);
        }

        String queriesFile = args[1];
        String resultTextFile = args[2];

        deserializeIndexAndLexicons();
        deserializeMappings(docnoToIdMap, idToDocnoMap);

        //cited from https://www.digitalocean.com/community/tutorials/java-read-file-line-by-line
        try {
            File queries = new File(queriesFile);
            BufferedReader reader = new BufferedReader(new FileReader(queries));
            String line = reader.readLine();

            String resultsPath = resultTextFile;
            File resultsFile = new File(resultsPath);
            BufferedWriter resultsWriter = new BufferedWriter(new FileWriter(resultsFile));

            while (line != null) {
                String topicId = line;
                line = reader.readLine();
                String queryText = line;
                System.out.println("TOPIC: " + topicId + ". QUERY: " + queryText);
//                ArrayList<Integer> returnedDocList = searchQuery(queryText);
                ArrayList<Integer> tokenIds = new ArrayList<>();
                ArrayList<ArrayList<Integer>> listOfPostingsLists = new ArrayList<>();

                //tokenize the query
                ArrayList<String> tokenizedQuery = DocumentTools.tokenizer(queryText);

                //get token ids from tokenized query
                for(String token : tokenizedQuery){
                    if(termToIdLexicon.containsKey(token)){
                        tokenIds.add(termToIdLexicon.get(token));
                    }
                }

                //get postings lists from token ids
                for(int id: tokenIds){
                    listOfPostingsLists.add(invertedIndex.get(id));
                }

                //cited from https://stackoverflow.com/questions/3477272/java-how-to-sort-list-of-lists-by-their-size
                //sorts so postings lists are in order of ascending size
                listOfPostingsLists.sort(comparingInt(ArrayList::size));

                if(listOfPostingsLists.isEmpty()){
                    System.out.println("No documents found through BooleanAND for topic " + topicId);
                    line = reader.readLine();
                    continue;
                }

                //intersect postings list
                //code from Andrew Kane
                ArrayList<Integer> intersectedListResult = listOfPostingsLists.get(0);
                for(int i = 0; i < listOfPostingsLists.size(); i++ ){
                    intersectedListResult = intersect(intersectedListResult, listOfPostingsLists.get(i));
                }

                //write the results to results file
                writeToTRECFile(intersectedListResult, topicId, resultsWriter);
                line = reader.readLine();
            }
            resultsWriter.close();
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<Integer> intersect(ArrayList<Integer> postingList1, ArrayList<Integer> postingList2){
        //assume p1 <= p2
        ArrayList<Integer> answer = new ArrayList<>();

        int i = 0;
        int j = 0;

        while(i != postingList1.size() && j != postingList2.size()){
            if(postingList1.get(i).equals(postingList2.get(j))){
                answer.add(postingList1.get(i));
                answer.add(0);
                i += 2;
                j += 2;
            } else if (postingList1.get(i) < postingList2.get(j)) {
                i += 2;
            } else {
                j += 2;
            }
        }
        return answer;
    }

    //Naive BooleanAND approach
    private static ArrayList<Integer> getEqualDocs(String query){

        ArrayList<Integer> tokenIds = new ArrayList<>();

        ArrayList<String> tokenizedQuery = DocumentTools.tokenizer(query);
        for(String token : tokenizedQuery){
            if(termToIdLexicon.containsKey(token)){
                tokenIds.add(termToIdLexicon.get(token));
            }
        }


        HashMap<Integer, Integer> docCount = new HashMap<>();
        ArrayList<Integer> answer = new ArrayList<>();

        for(int id : tokenIds){
            ArrayList<Integer> postings = invertedIndex.get(id);
            for(int docId : postings){
                if(docCount.containsKey(docId)){
                    docCount.put(docId, docCount.get(docId) + 1);
                }else{
                    docCount.put(docId, 1);
                }
            }
        }

        for(int docId : docCount.keySet()){
            if(docCount.get(docId) == tokenIds.size()){
                answer.add(docId);
            }
        }
        return answer;
    }


    private static void writeToTRECFile(ArrayList<Integer> returnedDocList, String topicID, BufferedWriter resultsWriter) throws IOException {
        //topicID Q0 docno rank score runTag
        String runTag = "m3shengAND";
        try {
            int rank = 1;
            for(int i = 0; i < returnedDocList.size(); i+=2){
                int score = returnedDocList.size() / 2 - rank;
                String docno = idToDocnoMap.get(returnedDocList.get(i));
                String resultString = topicID + " " + "Q0" + " " + docno  + " " + rank + " " + score + " " + runTag;
                resultsWriter.write(resultString);
                resultsWriter.newLine();
                rank++;
            }
             resultsWriter.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deserializeIndexAndLexicons () throws IOException, ClassNotFoundException {

//        FileInputStream fis = new FileInputStream("/Users/michaelsheng/Desktop/msci541-test-storage/invertedIndex.ser");
        FileInputStream fis = new FileInputStream("/Users/michaelsheng/Desktop/msci541-storage/invertedIndex.ser");
        ObjectInputStream ois = new ObjectInputStream(fis);

        System.out.println("Deserializing inverted index...");
        invertedIndex = (HashMap) ois.readObject();
        ois.close();
        fis.close();
        //        FileInputStream fis = new FileInputStream("/Users/michaelsheng/Desktop/msci541-test-storage/termToIdLexicon.ser");
        FileInputStream fis2 = new FileInputStream("/Users/michaelsheng/Desktop/msci541-storage/termToIdLexicon.ser");
        ObjectInputStream ois2 = new ObjectInputStream(fis2);

        System.out.println("Deserializing term to id lexicon...");
        termToIdLexicon = (HashMap) ois2.readObject();
        ois2.close();
        fis2.close();

//        FileInputStream fis3 = new FileInputStream("/Users/michaelsheng/Desktop/msci541-test-storage/idToTermLexicon.ser");
        FileInputStream fis3 = new FileInputStream("/Users/michaelsheng/Desktop/msci541-storage/idToTermLexicon.ser");
        ObjectInputStream ois3 = new ObjectInputStream(fis3);

        System.out.println("Deserializing id to term lexicon...");
        idToTermLexicon = (HashMap) ois3.readObject();

        fis3.close();
        ois3.close();
    }

    private static void deserializeMappings(HashMap<String, Integer> docnoToIdMap, HashMap<Integer, String> idToDocnoMap){

        System.out.println("Deserializing id to docno mappings");

        //cited from https://www.geeksforgeeks.org/how-to-serialize-hashmap-in-java/

        try {
//            File mapTextFile = new File("/Users/michaelsheng/Desktop/msci541-test-storage/DocnoToIdMapping.txt");
            File mapTextFile = new File("/Users/michaelsheng/Desktop/msci541-storage/DocnoToIdMapping.txt");

            BufferedReader mapReader = new BufferedReader(new FileReader(mapTextFile));
            String mappingsLine = mapReader.readLine();

            while (mappingsLine != null) {
                String[] mappings = mappingsLine.split(":");
                String docno = mappings[0];
                int internalId = Integer.parseInt(mappings[1]);

                idToDocnoMap.put(internalId, docno);
                docnoToIdMap.put(docno, internalId);

                mappingsLine = mapReader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
