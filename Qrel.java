package com.evaluator;

import java.util.ArrayList;
import java.util.HashMap;

public class Qrel {
    //code cited from RelevanceJudgements_cs.txt by Professor Mark Smucker
    HashMap<String, QrelTuple> qrelTuplesMap;
    HashMap<String, ArrayList<String>> topicToRelevantDocs;

    public Qrel(){
        this.qrelTuplesMap = new HashMap<>() ;
        this.topicToRelevantDocs = new HashMap<>() ;
    }

    public void addJudgement(QrelTuple qrelTuple) {
        String qrelKey = qrelTuple.getKey();
        if(qrelTuplesMap.containsKey(qrelKey)){
            System.out.println("Key already exists in qrel map - each mapping should have a unique key");
            System.exit(1);
        }

        //storing unique key with qrel row data
        qrelTuplesMap.put(qrelKey, qrelTuple);

        if(qrelTuple.isRelevant()){
            if (!topicToRelevantDocs.containsKey(qrelTuple.getTopicId())) {
                ArrayList<String> docnoList = new ArrayList<>();
                topicToRelevantDocs.put(qrelTuple.getTopicId(), docnoList);
            }
            topicToRelevantDocs.get(qrelTuple.getTopicId()).add(qrelTuple.getDocno());
        }
    }

    public boolean isRelevant(String topicId, String docno){
        String key = topicId + "-" + docno;

        if(!qrelTuplesMap.containsKey(key)){
//            System.out.println("qrelTuplesMap does not contain key: " + key);
            return false;
        }
        return qrelTuplesMap.get(key).isRelevant();
    }

}
