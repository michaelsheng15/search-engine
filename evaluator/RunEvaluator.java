package com.evaluator;


import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class RunEvaluator {

    static HashMap<String, ArrayList<String>> topicIdToEvalMeasure = new HashMap<>();
    static HashMap<String, ArrayList<Double>> topicIdToAveragePrecision = new HashMap<>();
    static HashMap<String, ArrayList<Double>> topicIdToPrecisionAt10 = new HashMap<>();
    static HashMap<String, ArrayList<Double>> topicIdToNDCGAt10 = new HashMap<>();
    static HashMap<String, ArrayList<Double>> topicIdToNDCGAt1000 = new HashMap<>();


    public static void main(String[] args) throws IOException {

        if (args.length < 2) {
            System.out.println("Insufficient arguments provided");
            System.out.println("Please provide path to latimes.gz and storage directory");
            System.exit(0);
        }

        String qrelsFilePath = args[0];
        String resultsFilePath = args[1];

        Qrel qrel = new Qrel();
        Results results = new Results();

        String resultsPath = "/Users/michaelsheng/Desktop/msci-541-f23-hw3-michaelsheng15/com/evaluator/evaluator-results.txt";
        File resultsFile = new File(resultsPath);
        BufferedWriter resultsWriter = new BufferedWriter(new FileWriter(resultsFile));

        //parsing data from input files
        parsingQrel(qrelsFilePath, qrel);
        parsingResults(resultsFilePath, results);

        calculateAveragePrecision(qrel, results);
        calculatePrecisionAt10(qrel, results);
        calculateNDCGAt10(qrel, results);
        calculateNDCGAt1000(qrel, results);

        String meanAveragePrecision = calculateMean(topicIdToAveragePrecision);
        String meanPrecisionAt10 = calculateMean(topicIdToPrecisionAt10);
        String meanNDCGAt10 = calculateMean(topicIdToNDCGAt10);
        String meanNDCGAt1000 = calculateMean(topicIdToNDCGAt1000);

//        System.out.println(meanAveragePrecision + " " + meanPrecisionAt10 + " " + meanNDCGAt10 + " " + meanNDCGAt1000);

        writeToFile(topicIdToEvalMeasure, resultsWriter);

        resultsWriter.write("Mean Average Precision" + " = " + meanAveragePrecision);
        resultsWriter.newLine();
        resultsWriter.write("Mean Precision@10" + " = " + meanPrecisionAt10);
        resultsWriter.newLine();
        resultsWriter.write("Mean NDCG@10" + " = " + meanNDCGAt10);
        resultsWriter.newLine();
        resultsWriter.write("Mean NDCG@1000" + " = " + meanNDCGAt1000);
        resultsWriter.newLine();

        resultsWriter.flush();

        outputResults(topicIdToEvalMeasure);
        System.out.println("Mean AP = " + meanAveragePrecision);
        System.out.println("Mean P@10 = " + meanPrecisionAt10);
        System.out.println("Mean NDCG@10 = " + meanNDCGAt10);
        System.out.println("Mean NDCG@1000 = " + meanNDCGAt1000);
    }

    private static String calculateMean(HashMap<String, ArrayList<Double>> evalMeasureMap){
        DecimalFormat df = new DecimalFormat("0.000");
        double mean;
        double sum = 0;

        for(String topicID : evalMeasureMap.keySet()){
            ArrayList<Double> valueList = evalMeasureMap.get(topicID);
            for(Double value : valueList){
                sum += value;
            }
        }
        mean = sum/45.0;
        return df.format(mean);
    }

    private static void writeToFile(HashMap<String, ArrayList<String>> evalMeasureMap, BufferedWriter resultsWriter) throws IOException {
        ArrayList<String> topicsList = new ArrayList<>(evalMeasureMap.keySet());
        Collections.sort(topicsList);

        for(String topicID : topicsList){
            ArrayList<String> valueList = evalMeasureMap.get(topicID);
            String result = topicID + ": " + valueList;
            resultsWriter.write(result);
            resultsWriter.newLine();
        }
    }

    private static void outputResults(HashMap<String, ArrayList<String>> evalMeasureMap){
        ArrayList<String> topicsList = new ArrayList<>(evalMeasureMap.keySet());
        Collections.sort(topicsList);
        System.out.printf("-------------------------------------------------------------------------------------------%n");
        System.out.printf(" Evaluation Measure %n");
        System.out.printf("-------------------------------------------------------------------------------------------%n");
        System.out.printf("| %-15s | %-15s | %-15s | %-15s | %-15s |%n", "TOPIC ID", "AVG PRECISION", "PRECISION@10", "NDCG@10", "NDCG1000");
        System.out.printf("-------------------------------------------------------------------------------------------%n");

        for(String topicID : topicsList){
            ArrayList<String> valuesList = evalMeasureMap.get(topicID);
            String ap = valuesList.get(0);
            String p10 = valuesList.get(1);
            String ndcg10 = valuesList.get(2);
            String ndcg1000 = valuesList.get(3);
            System.out.printf("| %-15s | %-15s | %-15s | %-15s | %-15s |%n", "Topic ID: " + topicID, ap, p10, ndcg10, ndcg1000);
        }
        System.out.printf("-------------------------------------------------------------------------------------------%n");
    }

    private static void calculateAveragePrecision(Qrel qrels, Results results){
        //for each topic id loop through list of results and calculate the summation of all precision at rank
        //divide the sum of average precisions

        ArrayList<String> topicsList = new ArrayList<>(results.queryToResults.keySet());
        Collections.sort(topicsList);

        for(String topicId : topicsList){
            ArrayList<Result> listOfResults = results.queryToResults.get(topicId);
//            System.out.println(topicId + ":" + listOfResults.size());

            //algo to find sum of precision at ranks
            double sumOfPrecisionAtRanks = 0;
            double relevantDocs = 0;
            int rank = 1;
            boolean foundRelevantDoc = false;

            for(Result result : listOfResults){
                String docno = result.getDocno();

                if(qrels.isRelevant(topicId, docno)){
                    foundRelevantDoc = true;
                    relevantDocs++;
                    sumOfPrecisionAtRanks +=  relevantDocs / rank;
                }
                rank++;
            }

            //divide sum of precision at ranks by total relevant docs
            double averagePrecision = 0.0;
            if(foundRelevantDoc){
                averagePrecision = sumOfPrecisionAtRanks / qrels.topicToRelevantDocs.get(topicId).size();
            }

            DecimalFormat df = new DecimalFormat("0.000");
            averagePrecision = Double.parseDouble(df.format(averagePrecision));
            String formattedAP = df.format(averagePrecision);
//            System.out.println(formattedAP);
            if(!topicIdToAveragePrecision.containsKey(topicId)){
                topicIdToAveragePrecision.put(topicId, new ArrayList<>());
            }
            topicIdToAveragePrecision.get(topicId).add(averagePrecision);

            if(!topicIdToEvalMeasure.containsKey(topicId)){
                topicIdToEvalMeasure.put(topicId, new ArrayList<>());
            }
            topicIdToEvalMeasure.get(topicId).add(formattedAP);
        }
    }

    private static void calculatePrecisionAt10(Qrel qrels, Results results){
        //for each topic id loop through list of results and calculate the summation of all precision at rank
        //divide the sum of average precisions

        ArrayList<String> topicsList = new ArrayList<>(results.queryToResults.keySet());
        Collections.sort(topicsList);

        for(String topicId : topicsList){
            ArrayList<Result> listOfResults = results.queryToResults.get(topicId);
//            System.out.println(topicId + ":" + listOfResults.size());

            //algo to find sum of precision at ranks
            double relevantDocs = 0;
            int documentCountMax = Math.min(listOfResults.size(), 10);

            for(int i = 0; i < documentCountMax; i++){
                Result result = listOfResults.get(i);
                String docno = result.getDocno();
                if(qrels.isRelevant(topicId, docno)){
                    relevantDocs++;
                }
            }

            //divide sum of precision at ranks by total relevant docs
            double precisionAt10 = relevantDocs / 10;

            DecimalFormat df = new DecimalFormat("0.000");
            String formattedPAt10 = df.format(precisionAt10);
            precisionAt10 = Double.parseDouble(df.format(precisionAt10));

//            System.out.println(formattedPAt10);
            if(!topicIdToPrecisionAt10.containsKey(topicId)){
                topicIdToPrecisionAt10.put(topicId, new ArrayList<>());
            }
            topicIdToPrecisionAt10.get(topicId).add(precisionAt10);

            if(!topicIdToEvalMeasure.containsKey(topicId)){
                topicIdToEvalMeasure.put(topicId, new ArrayList<>());
            }
            topicIdToEvalMeasure.get(topicId).add(formattedPAt10);
        }
    }

    private static void calculateNDCGAt10(Qrel qrels, Results results){
        ArrayList<String> topicsList = new ArrayList<>(results.queryToResults.keySet());
        Collections.sort(topicsList);

        for(String topicId : topicsList){
            ArrayList<Result> listOfResults = results.queryToResults.get(topicId);

            int documentCountMax = Math.min(listOfResults.size(), 10);

            //calculating dcg
            double dcg = 0;
            int dcgRank = 1;
            for(int i = 0; i < documentCountMax; i++){
                Result result = listOfResults.get(i);
                String docno = result.getDocno();
                if(qrels.isRelevant(topicId, docno)){
                    double logBase2 = Math.log(dcgRank + 1)/Math.log(2);
                    dcg += 1/logBase2;
                }
                dcgRank++;
            }

            int relevantDocsMax = Math.min(qrels.topicToRelevantDocs.get(topicId).size(), 10);

            //calculating idcg
            double idcg = 0;
            int idcgRank = 1;
            for(int j = 0; j < relevantDocsMax; j++){
                double logBase2 = Math.log(idcgRank + 1)/Math.log(2);
                idcg += 1/logBase2;
                idcgRank++;
            }

            //calculating ndcg
            double ndcg = dcg/idcg;
            DecimalFormat df = new DecimalFormat("0.000");
            String formattedNDCG10 = df.format(ndcg);
            ndcg = Double.parseDouble(df.format(ndcg));
//            System.out.println(formattedNDCG10);
            if(!topicIdToNDCGAt10.containsKey(topicId)){
                topicIdToNDCGAt10.put(topicId, new ArrayList<>());
            }
            topicIdToNDCGAt10.get(topicId).add(ndcg);

            if(!topicIdToEvalMeasure.containsKey(topicId)){
                topicIdToEvalMeasure.put(topicId, new ArrayList<>());
            }
            topicIdToEvalMeasure.get(topicId).add(formattedNDCG10);

        }
    }

    private static void calculateNDCGAt1000(Qrel qrels, Results results){

        //sort by increasing topic id
        ArrayList<String> topicsList = new ArrayList<>(results.queryToResults.keySet());
        Collections.sort(topicsList);

        for(String topicId : topicsList){
            ArrayList<Result> listOfResults = results.queryToResults.get(topicId);

            int documentCountMax = Math.min(listOfResults.size(), 1000);

            //calculating dcg
            double dcg = 0;
            int dcgRank = 1;
            for(int i = 0; i < documentCountMax; i++){
                Result result = listOfResults.get(i);
                String docno = result.getDocno();
                if(qrels.isRelevant(topicId, docno)){
                    double logBase2 = Math.log(dcgRank + 1)/Math.log(2);
                    dcg += 1/logBase2;
                }
                dcgRank++;
            }

            //since ideal, we assume all relevant so we only want to loop through # of relevant docs
            int relevantDocsMax = qrels.topicToRelevantDocs.get(topicId).size();

            //calculating idcg
            double idcg = 0;
            int idcgRank = 1;
            for(int j = 0; j < relevantDocsMax; j++){
                double logBase2 = Math.log(idcgRank + 1)/Math.log(2);
                idcg += 1/logBase2;
                idcgRank++;
            }

            //calculating ndcg
            double ndcg = dcg/idcg;
            DecimalFormat df = new DecimalFormat("0.000");
            String formattedNDCG1000 = df.format(ndcg);
            ndcg = Double.parseDouble(df.format(ndcg));

//            System.out.println(formattedNDCG1000);
            if(!topicIdToNDCGAt1000.containsKey(topicId)){
                topicIdToNDCGAt1000.put(topicId, new ArrayList<>());
            }
            topicIdToNDCGAt1000.get(topicId).add(ndcg);

            if(!topicIdToEvalMeasure.containsKey(topicId)){
                topicIdToEvalMeasure.put(topicId, new ArrayList<>());
            }
            topicIdToEvalMeasure.get(topicId).add(formattedNDCG1000);
        }
    }


    private static void parsingQrel(String qrelsFilePath, Qrel qrel){
        //code cited from QRels.txt by Professor Mark Smucker

        File qrelFile = new File(qrelsFilePath);
        try {
            //cited from https://stackoverflow.com/questions/5868369/how-can-i-read-a-large-text-file-line-by-line-using-java
            BufferedReader qrelReader = new BufferedReader(new FileReader(qrelFile));
            String line = qrelReader.readLine();

            while (line != null) {
                //cited from https://stackoverflow.com/questions/7899525/how-to-split-a-string-by-space
                String[] qrelLineData = line.split("\\s+");
                if (qrelLineData.length != 4){
                    throw new Exception( "QRel row should have 4 columns" ) ;
                }
                String topicId = qrelLineData[0];
                String docno = qrelLineData[2];
                int judgement = Integer.parseInt(qrelLineData[3]);


                QrelTuple tuple = new QrelTuple(topicId, docno, judgement);
                qrel.addJudgement(tuple);
                line = qrelReader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println("finished parsing qrels file");
    }

    private static void parsingResults(String resultsFilePath, Results results){
        File resultsFile = new File(resultsFilePath);

        try {
            BufferedReader resultsFileReader = new BufferedReader(new FileReader(resultsFile));
            String line = resultsFileReader.readLine();

            while (line != null) {
                String[] resultsLineData = line.split("\\s+");

                if (resultsLineData.length != 6){
                    throw new Exception( "QRel row should have 6 columns" ) ;
                }

                if (!checkBadFormat(resultsLineData[2], resultsLineData[3], resultsLineData[4])){
                       System.out.println("Results file is incorrectly formatted, please refer to TREC file guidelines");
                       System.exit(1);
                }

                String topicId = resultsLineData[0];
                String docno = resultsLineData[2];
                int rank = Integer.parseInt(resultsLineData[3]);
                double score = Double.parseDouble(resultsLineData[4]);
                String runName = resultsLineData[5];

                Result result = new Result(docno, score, rank, runName);
                results.addToResults(topicId, result);
                line = resultsFileReader.readLine();
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("finished parsing results file");
    }

    private static boolean checkBadFormat(String docno, String rank, String score){
        boolean inputsCorrect = true;
        
        if(docno.length() != 13 || docno.equals("null")){
            System.out.println("Format error found in DOCNO column");
            inputsCorrect = false;
        } else if (!rank.matches("\\d+") || rank.equals("null")) {
            System.out.println("Format error found in RANK column");
            inputsCorrect = false;
        } else if (score.equals("null")){
            System.out.println("Format error found in SCORE column");
            inputsCorrect = false;
        }
        return inputsCorrect;
    }


}
