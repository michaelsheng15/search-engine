package com.evaluator;

import java.util.ArrayList;
import java.util.HashMap;

public class Results {

    //maps topicId to list of results
    public HashMap<String, ArrayList<Result>> queryToResults;

    public Results() {
        this.queryToResults = new HashMap<>();
    }

    public void addToResults(String topicId, Result result){
        if(!queryToResults.containsKey(topicId)){
            queryToResults.put(topicId, new ArrayList<>());
        }
        queryToResults.get(topicId).add(result);
    }
}
