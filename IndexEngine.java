import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;


public class IndexEngine{

    public static final String HEADLINE_START = "<HEADLINE>";
    public static final String HEADLINE_END = "</HEADLINE>";

    public static HashMap<Integer, String> idToDocnoMap = new HashMap<>();
    public static HashMap<String, Integer> docnoToIdMap = new HashMap<>();
    public static HashMap<Integer, String> idToMetadataMap = new HashMap<>();



    public static void main(String[] args) throws IOException, ClassNotFoundException {

        //check that enough arguments are provided
        if (args.length < 2) {
            System.out.println("Insufficient arguments provided");
            System.out.println("Please provide path to latimes.gz and storage directory");
            System.exit(0);
        }

        //get latimes file path and storage directory path
        final String DATA_PATH = args[0];
        final String STORAGE_PATH = args[1];

        File storageDirectory = new File(STORAGE_PATH);

        //check given dir exists, if not then create dir
        if (!storageDirectory.exists()) {
            boolean created = storageDirectory.mkdirs();
            if (created) {
                System.out.println("Directory created successfully.");
            } else {
                System.out.println("Failed to create directory.");
                return;
            }
        }

        System.out.println("latimes.gz file path: " + DATA_PATH);
        System.out.println("storage directory path: " + storageDirectory);


        ArrayList<Integer> docLengths = new ArrayList<>();
        HashMap<String, Integer> termToIdLexicon = new HashMap<>();
        HashMap<Integer, String> idToTermLexicon = new HashMap<>();
        HashMap<Integer, ArrayList<Integer>> invertedIndex = new HashMap<>();


        //unzip .gz file
        try {

            FileInputStream fileStream = new FileInputStream(DATA_PATH);

            GZIPInputStream decompressedStream = new GZIPInputStream(fileStream);

            //citing https://stackoverflow.com/questions/1080381/gzipinputstream-reading-line-by-line
            Reader readStream = new InputStreamReader(decompressedStream);
            BufferedReader reader = new BufferedReader(readStream);

            String docLine = reader.readLine();

            StringBuffer strBuffer = new StringBuffer();

            int interalId = 0;
            String fileDOCNO = "";

            boolean parsingHeadline = false;
            boolean parsingTarget = false;
            boolean foundPTag = false;


            StringBuilder fileHeadline = new StringBuilder();
            StringBuilder targetSections = new StringBuilder();


            while(docLine != null) {
                //loop through and find <DOC> and </DOC> tags
                //store document in seperate file and store in STORAGE_PATH
                strBuffer.append(docLine);

                if (docLine.equals(HEADLINE_END)) {
                    parsingHeadline = false;
                } else if (docLine.equals("</TEXT>") || docLine.equals("</GRAPHIC>")) {
                    parsingTarget = false;
                }

                if (docLine.equals("<P>") || docLine.equals("</P>")) {
                    foundPTag = true;
                }else{
                    foundPTag = false;
                }

                if (parsingHeadline && !foundPTag) {
                    fileHeadline.append(docLine);
                    targetSections.append(docLine);
                } else if (parsingTarget && !foundPTag) {
                    targetSections.append(docLine + " ");
                }

                if (docLine.contains("<DOCNO>")) {
                    //save docno for file storing purposes
                    fileDOCNO = docLine;
                } else if (docLine.equals(HEADLINE_START)) {
                    parsingHeadline = true;
                } else if (docLine.equals("<TEXT>") || docLine.equals("<GRAPHIC>")) {
                    parsingTarget = true;
                }

                strBuffer.append(System.lineSeparator());

                //store readDocument into STORAGE_PATH
                if (docLine.equals("</DOC>")) {
                    String target = targetSections.toString();
//                    allTargetSections.add(target.toLowerCase());
                    String finalDocument = strBuffer.toString();
                    String[] docnoArray = DocumentTools.getDateFromDocno(fileDOCNO.substring(10, 21));

                    String docno = docnoArray[4];

                    String metadata = buildMetadata(interalId, docno, formatHeadline(fileHeadline), formatDate(docnoArray[0], docnoArray[1], docnoArray[2]));

                    initMappings(interalId, docno, metadata);
                    writeFilesToStorage(finalDocument, metadata, STORAGE_PATH, docnoArray);

                    ArrayList<String> tokens = DocumentTools.tokenizer(target);
                    docLengths.add(tokens.size()); //add to doc lengths list for processing

//                    System.out.println(tokens);

                    ArrayList<Integer> tokenIds = convertTokensToIds(tokens, termToIdLexicon, idToTermLexicon);

                    HashMap<Integer, Integer> wordCount = countWords(tokenIds);

                    addToPostingsList(wordCount, interalId, invertedIndex);

                    interalId++;
                    strBuffer.setLength(0);
                    fileHeadline.setLength(0);
                    targetSections.setLength(0);

                    System.out.println(metadata);
                    System.out.println(docno + "Successfully stored");
//                    break;
                }
                docLine = reader.readLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        serializeMappings(docnoToIdMap, idToDocnoMap);
        saveDocumentLengths(docLengths);

        System.out.println("Serializing inverted index...");
        saveToStorage(invertedIndex, "invertedIndex.ser");

        System.out.println("Serializing term to ID lexicon...");
        saveToStorage(termToIdLexicon, "termToIdLexicon.ser");

        System.out.println("Serializing ID to term lexicon...");
        saveToStorage(idToTermLexicon, "idToTermLexicon.ser");

//        System.out.println("Deserializing");
//        HashMap<Integer, ArrayList<Integer>> map = deserializeIndex();
    }

    private static void writeFilesToStorage(String finalDocument, String metadata, String STORAGE_PATH, String[] docnoArray) throws IOException {
        String docDirectoryPath = STORAGE_PATH + "/19" + docnoArray[0] + "/" + docnoArray[1] + "/" + docnoArray[2] + "/" + docnoArray[3] + ".txt";
        String metadataDirectoryPath = STORAGE_PATH + "/19" + docnoArray[0] + "/" + docnoArray[1] + "/" + docnoArray[2] + "/" + docnoArray[3] + "-metadata.txt";

        File documentFile = new File(docDirectoryPath);
        File metadataFile = new File(metadataDirectoryPath);

        documentFile.getParentFile().mkdirs();
        metadataFile.getParentFile().mkdirs();
        documentFile.createNewFile();
        metadataFile.createNewFile();

        FileWriter documentWriter = new FileWriter(docDirectoryPath);
        FileWriter metadataWriter = new FileWriter(metadataDirectoryPath);

        documentWriter.write(finalDocument);
        metadataWriter.write(metadata);

        documentWriter.close();
        metadataWriter.close();
    }

    private static String buildMetadata (int internalId, String docno, String headline, String date){
        StringBuffer metadataBuffer = new StringBuffer();
        metadataBuffer.append("Internal Id: ").append(internalId);
        metadataBuffer.append(System.lineSeparator());
        metadataBuffer.append("DOCNO: ").append(docno);
        metadataBuffer.append(System.lineSeparator());
        metadataBuffer.append("Headline: ").append(headline);
        metadataBuffer.append(System.lineSeparator());
        metadataBuffer.append("Date: ").append(date);

        return metadataBuffer.toString();
    }

    private static void initMappings(int internalId, String docno, String metadata){
        idToDocnoMap.put(internalId, docno);
        docnoToIdMap.put(docno, internalId);
        idToMetadataMap.put(internalId, metadata);
    }


    private static String formatHeadline( StringBuilder unformattedHeadline ){
        if(unformattedHeadline.length() == 0){
            System.out.println(unformattedHeadline);
            return "NO HEADLINE EXISTS";
        }

        //cited from https://stackoverflow.com/questions/240546/remove-html-tags-from-a-string
        return unformattedHeadline.toString().replaceAll("\\<[^>]*>","");
    }


    private static String formatDate (String year, String month, String day){
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

        String newMonth = months[Integer.parseInt(month)   - 1];
        String newDay = Integer.toString(Integer.parseInt(day));
        String newYear = "19" + year;

        return newMonth + " " + newDay + ", " + newYear;
    }

    //cited from https://www.geeksforgeeks.org/write-hashmap-to-a-text-file-in-java/
    private static void serializeMappings(HashMap<String, Integer> docnoToIdMap, HashMap<Integer, String> idToDocnoMap){
//        String docnoToIdFilePath =  "/Users/michaelsheng/Desktop/msci541-test-storage/DocnoToIdMapping.txt";
//        String idToDocnoFilePath =  "/Users/michaelsheng/Desktop/msci541-test-storage/IdToDocnoMapping.txt";

        String docnoToIdFilePath =  "/Users/michaelsheng/Desktop/msci541-storage/DocnoToIdMapping.txt";
        String idToDocnoFilePath =  "/Users/michaelsheng/Desktop/msci541-storage/IdToDocnoMapping.txt";

        File docnoToIdFile = new File(docnoToIdFilePath);
        File idToDocnoFile = new File(idToDocnoFilePath);

        BufferedWriter docnoToIdWriter = null;
        BufferedWriter idToDocnoWriter = null;


        try {
            docnoToIdWriter = new BufferedWriter(new FileWriter(docnoToIdFile));
            idToDocnoWriter = new BufferedWriter(new FileWriter(idToDocnoFile));

            for (Map.Entry<String, Integer> entry : docnoToIdMap.entrySet()) {
                docnoToIdWriter.write(entry.getKey() + ":" + entry.getValue());
                docnoToIdWriter.newLine();
            }
            for (Map.Entry<Integer, String> entry : idToDocnoMap.entrySet()) {
                idToDocnoWriter.write(entry.getKey() + ":" + entry.getValue());
                idToDocnoWriter.newLine();
            }

            docnoToIdWriter.flush();
            idToDocnoWriter.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            try {
                docnoToIdWriter.close();
                idToDocnoWriter.close();
            }
            catch (Exception e) {
            }
        }
    }

    public static void saveDocumentLengths(ArrayList<Integer> docLengths) throws IOException {
        //line number in document correponds to docId indexed at 0
//        String path = "/Users/michaelsheng/Desktop/msci541-test-storage/doc-lengths.txt";
        String path = "/Users/michaelsheng/Desktop/msci541-storage/doc-lengths.txt";

        File docLengthFile = new File(path);
        docLengthFile.getParentFile().mkdirs();
        docLengthFile.createNewFile();

        BufferedWriter docLengthWriter = new BufferedWriter(new FileWriter(path));

        for (int docLength : docLengths) {
//            System.out.println(docLength);
            docLengthWriter.write(String.valueOf(docLength));
            docLengthWriter.newLine();
        }
        docLengthWriter.flush();
        docLengthWriter.close();
    }

    public static ArrayList<Integer> convertTokensToIds (ArrayList<String> tokens, HashMap<String, Integer> termToIdLexicon, HashMap<Integer, String> idToTermLexicon){
        ArrayList<Integer> tokenIds = new ArrayList<>();

        for(String token : tokens){
            if(!termToIdLexicon.containsKey(token)){
                int currentId = termToIdLexicon.size();
                termToIdLexicon.put(token, currentId);
                idToTermLexicon.put(currentId, token);
            }
            tokenIds.add(termToIdLexicon.get(token));
        }

        return tokenIds;
    }

    public static HashMap<Integer, Integer> countWords(ArrayList<Integer> tokenIds){
        HashMap<Integer, Integer> wordCountMap = new HashMap<>(); //Key: tokenId, Value: wordCount

        for(int id : tokenIds){
            if(wordCountMap.containsKey(id)){
                wordCountMap.put(id, wordCountMap.get(id) + 1);
            }else{
                wordCountMap.put(id, 1);
            }
        }

        return wordCountMap;
    }

    public static void addToPostingsList(HashMap<Integer, Integer> wordCounts, int internalId, HashMap<Integer, ArrayList<Integer>> invertedIndex){
        for(int termId : wordCounts.keySet()){
            int count = wordCounts.get(termId);

            //no postings lists exists for current term so we create a mapping
            if(!invertedIndex.containsKey(termId)){
                invertedIndex.put(termId, new ArrayList<>());
            }

            //we get the postings list and add internal id and count
            ArrayList<Integer> postings = invertedIndex.get(termId);
            postings.add(internalId);
            postings.add(count);

        }
    }

    private static void saveToStorage(Object save, String fileName) throws IOException {
//        String pathToSave =  "/Users/michaelsheng/Desktop/msci541-test-storage/" + fileName;
        String pathToSave =  "/Users/michaelsheng/Desktop/msci541-storage/" + fileName;

        //cited from https://stackoverflow.com/questions/3347504/how-to-read-and-write-a-hashmap-to-a-file
            FileOutputStream f = new FileOutputStream(pathToSave);
            ObjectOutputStream s = new ObjectOutputStream(f);
            s.writeObject(save);

            s.close();
            f.close();
    }
}