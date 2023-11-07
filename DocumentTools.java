import java.util.ArrayList;

public class DocumentTools {

    public static String[] getDateFromDocno(String docno){
        //0 = year
        //1 = month
        //2 = day
        //3 = docNumber
        //4 = docno

        String day = docno.substring(2,4);
        String month = docno.substring(0, 2);
        String year = docno.substring(4, 6);
        String docNumber = docno.substring(docno.length()-4);

        return new String[]{year, month, day, docNumber, docno};
    }

    public static ArrayList<String> tokenizer(String documentText){
        documentText = documentText.toLowerCase();

        ArrayList<String> tokens = new ArrayList<>();

        int start = 0;
        int i;
        for(i = 0; i < documentText.length(); i++){
            char c = documentText.charAt(i);
            if(!Character.isLetterOrDigit(c)){
                if(start != i){
                    String token = documentText.substring(start, i);
                    tokens.add(token);
                }
                start = i+1;
            }
        }
        if(start != i) tokens.add(documentText.substring(start, i));

//        System.out.println(tokens);
        return tokens;
    }




}
