package com.evaluator;

public class QrelTuple {

    //code cited from RelevanceJudgements_cs.txt by Mark Smucker
    private String topicId;
    private String docno;
    private int judgement;

    public QrelTuple(String queryId, String docId, int judgement) {
        this.topicId = queryId;
        this.docno = docId;
        this.judgement = judgement;
    }

    public String getTopicId() {
        return topicId;
    }
    public String getDocno() {
        return docno;
    }

    public int getBinaryRelevance() {
        return judgement;
    }

    public boolean isRelevant(){
        return judgement == 1;
    }

    public String getKey()
    {
        return topicId + "-" + docno ;
    }

    public String generateKey(String topicId, String docno)
    {
        return topicId + "-" + docno ;
    }




}
