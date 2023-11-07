package com.evaluator;

public class Result {

    private String docno ;
    private double score ;
    private int rank ;

    private String runName;

    public Result(String docID, double score, int rank, String runName)
    {
        this.docno = docID ;
        this.score = score ;
        this.rank = rank ;
        this.runName = runName ;
    }

    public String getDocno() {
        return docno;
    }

    public double getScore() {
        return score;
    }

    public int getRank() {
        return rank;
    }

    public String getRunName() {
        return runName;
    }

}
