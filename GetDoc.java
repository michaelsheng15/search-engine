import java.io.*;
import java.util.HashMap;

public class GetDoc {
    public static HashMap<Integer, String> idToDocnoMap = new HashMap<>();
    public static HashMap<String, Integer> docnoToIdMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.out.println("Insufficient arguments provided");
            System.out.println("Please provide path to latimes.gz, an identifier, and indentifier value");
            System.exit(0);
        }

        final String PATH = args[0];
        final String IDENTIFIER = args[1];
        String value = args[2];

        deserializeMappings(docnoToIdMap, idToDocnoMap);

        //if the identifier is id then we find the docno, else the value provided will already be the docno
        if(IDENTIFIER.equals("id")){
            value = idToDocnoMap.get(Integer.parseInt(value));
            System.out.println("DOCNO from ID: " + value);
        }

        String[] docnoArray = DocumentTools.getDateFromDocno(value);

        outputMetadata(docnoArray, PATH);
        outputDocument(docnoArray, PATH);
    }

    private static void deserializeMappings(HashMap<String, Integer> docnoToIdMap, HashMap<Integer, String> idToDocnoMap){
        try {
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

    private static void outputMetadata(String[] docnoArray, String path) {
        try {
            String metadataDirectoryPath = path + "/19" + docnoArray[0] + "/" + docnoArray[1] + "/" + docnoArray[2] + "/" + docnoArray[3] + "-metadata.txt";

            File metadataFile = new File(metadataDirectoryPath);
            BufferedReader metadataReader = new BufferedReader(new FileReader(metadataFile));
            String metadataLine = metadataReader.readLine();

            System.out.println("METADATA");
            while (metadataLine != null) {
                System.out.println(metadataLine);
                metadataLine = metadataReader.readLine();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void outputDocument(String[] docnoArray, String path) {
        try {
            String docDirectoryPath = path + "/19" + docnoArray[0] + "/" + docnoArray[1] + "/" + docnoArray[2] + "/" + docnoArray[3] + ".txt";

            File documentPath = new File(docDirectoryPath);
            BufferedReader docReader = new BufferedReader(new FileReader(documentPath));
            String docLine = docReader.readLine();

            System.out.println("\nRAW DOCUMENT");
            while (docLine != null) {
                System.out.println(docLine);
                docLine = docReader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}