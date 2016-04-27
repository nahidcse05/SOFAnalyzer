import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class languageModel {
	private HashMap<String, Double> featureNameIndex;
	private HashMap<String, Double> smoothedFeatureNameIndex;
	private HashMap<String, Double> biFeatureNameIndex;
	private HashMap<String, Double> biFeatureNameIndexProb;
	
	private double uniSum = 0.0;
	private double lambda = 0.9;
	private double miu = 1000;
	
	public languageModel(String[] tokens) {
		featureNameIndex = new HashMap<String, Double>();
		biFeatureNameIndex = new HashMap<String, Double>();
		biFeatureNameIndexProb = new HashMap<String, Double>();
		smoothedFeatureNameIndex = new HashMap<String, Double>();
		
		int inword=1;
		String prevToken="";
		
		double value = 0;
		for (String token : tokens) {
			if(token=="" || token==null)
				continue;
			// Bi-GramStart
			if(inword==1){
				inword=0;
				prevToken=token;
			}
			else{
				String bigram=prevToken+"@"+token;      //Bi-GramSet
				value = 1.0;
				if (biFeatureNameIndex.containsKey(bigram)) {
					value = biFeatureNameIndex.get(bigram) + 1.0;
				} 
				biFeatureNameIndex.put(bigram,value);
				prevToken=token;
			}
			
	
			// Unigram Starts
			value = 1.0;
			if (featureNameIndex.containsKey(token)) {
				value = featureNameIndex.get(token) + 1.0;
			}
			featureNameIndex.put(token, value);
		}
	}
	
	public void unigramModel(){
		Iterator it = featureNameIndex.entrySet().iterator();
		double sum = 0.0;
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        //System.out.println("Key="+pair.getKey()+" ,Value="+pair.getValue());
	        sum += (double)pair.getValue();
	    }
	    
	    System.out.println("Sum:"+ sum);
	    uniSum = sum;
	    //probability assign
	    /*it = featureNameIndex.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			featureNameIndex.put(pair.getKey().toString(), (double)pair.getValue()/sum);
	    }*/
	}
	
	public void smoothedUnigarmModel(HashMap<String, Double> vocabulary, int vocabularyCount){
		Iterator it = vocabulary.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair = (Map.Entry)it.next();
			String key = pair.getKey().toString();
			System.out.println("Key"+key);
			double value = (double)pair.getValue() / vocabularyCount; // p(w|REF) 
			double prob;
			if(featureNameIndex.containsKey(key))
				prob = (featureNameIndex.get(key)+miu*value) / (uniSum+miu);
			
			else
				prob = (miu*value) / (uniSum+miu);
			smoothedFeatureNameIndex.put(key, prob);
		}
	}
	
	public void bigramModel(){
		
		/*Iterator it = biFeatureNameIndex.entrySet().iterator();
		double sum = 0.0;
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        //System.out.println("Key="+pair.getKey()+" ,Value="+pair.getValue());
	        sum += (double)pair.getValue();
	    }
	    */
	    //System.out.println("Bi Sum:"+ sum);
	    //System.out.println("Uni Sum:"+ uniSum);
	    double biSum = 0.0;
	    double biProb = 0.0;
	    String key ="";
	    Iterator it = biFeatureNameIndex.entrySet().iterator();
		while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        key = pair.getKey().toString();
	        String prevToken = key.split("@")[0];
	        double prevTokenCount = featureNameIndex.get(prevToken);
	        biProb = (1-lambda)*(biFeatureNameIndex.get(key)/prevTokenCount) + lambda*(prevTokenCount/uniSum);
	        biFeatureNameIndexProb.put(key, biProb);
	        biSum+=biProb;
	        //System.out.println("Prev Token:"+prevToken+", Count:"+prevTokenCount+", proba:"+(prevTokenCount/uniSum));
	        //System.out.println("Bi Gram:"+key+", count:"+biFeatureNameIndex.get(key));
	        //System.out.println("Key="+key+" ,Value="+biProb);
	        
		}
	    
		System.out.println("Bi Sum:"+ biSum);
	}
	
	
public double estimateUniGram(String[] tokens){

	double likelihood = 1.0;

	for (String token : tokens) {
		if(token=="" || token==null)
			continue;
		if(smoothedFeatureNameIndex.containsKey(token))
			likelihood*=smoothedFeatureNameIndex.get(token);

	}
	if(likelihood==1.0)
		return Double.MIN_VALUE;
	return likelihood;

}

	
	public double estimateBiGram(String[] tokens){
		
		HashMap<String, Double> biGramList = new HashMap<String, Double>();
		
		int inword=1;
		String prevToken="";
		
		double value = 0;
		for (String token : tokens) {
			if(token=="" || token==null)
				continue;
			// Bi-GramStart
			if(inword==1){
				inword=0;
				prevToken=token;
			}
			else{
				String bigram=prevToken+"@"+token;      //Bi-GramSet
				if (biGramList.containsKey(bigram)) {
					value = biGramList.get(bigram) + 1.0;
				} 
				biGramList.put(bigram,value);
				prevToken=token;
			}
		}
		
		Iterator it = biGramList.entrySet().iterator();
		double likelihood = 1.0;
	    while (it.hasNext()) {
	    	 Map.Entry pair = (Map.Entry)it.next();
	    	 String key = pair.getKey().toString();
	    	 if(biFeatureNameIndex.containsKey(key))
	    		 likelihood*=biFeatureNameIndexProb.get(key);
	    	 else{
	    		 prevToken = key.split("@")[0];
	    		 if(featureNameIndex.containsKey(prevToken)){
		 	         double prevTokenCount = featureNameIndex.get(prevToken);
		 	         likelihood*=(prevTokenCount/uniSum);
		    	}
	    	 }
	    }
	    
	    //System.out.println("Likelihood:"+likelihood);
	    if(likelihood==1.0)
	    	return Double.MIN_VALUE;
	    return likelihood;
	    
	}
}
