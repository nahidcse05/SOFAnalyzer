
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.Normalizer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import structures.MyPriorityQueue;
import structures._RankItem;

public class PostAnalyzer {
	
	protected int m_Ngram; 
	protected Tokenizer m_tokenizer;
	protected SnowballStemmer m_stemmer;
	HashSet<String> m_stopwords;
	
	HashMap<String, String> posts;
	//HashMap<String, String [] > postsArray;
	
	HashMap<String, Double> vocabulary;
	int totalVocabularyCount = 0;
	HashMap<String, languageModel> languagModelList;
	
	
	PostAnalyzer(){
		posts = new HashMap<String, String>();
		//postsArray = new HashMap<String, String[]>();
		vocabulary = new HashMap<String, Double>();
		languagModelList = new HashMap <String, languageModel>();
		
		m_Ngram = 1;
		try {
			m_tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream("./data/Model/en-token.bin")));
			m_stemmer = new englishStemmer();
			m_stopwords = new HashSet<String>();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void LoadStopwords(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;

			while ((line = reader.readLine()) != null) {
				line = SnowballStemming(Normalize(line));
				if (!line.isEmpty())
					m_stopwords.add(line);
			}
			reader.close();
			System.out.format("Loading %d stopwords from %s\n", m_stopwords.size(), filename);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
	}
	
	//Tokenizer.
	protected String[] Tokenizer(String source){
		String[] tokens = m_tokenizer.tokenize(source);
		return tokens;
	}
	
	//Snowball Stemmer.
	protected String SnowballStemming(String token){
		m_stemmer.setCurrent(token);
		if(m_stemmer.stem())
			return m_stemmer.getCurrent();
		else
			return token;
	}
	
	//Normalize.
	protected String Normalize(String token){
		token = Normalizer.normalize(token, Normalizer.Form.NFKC);
		token = token.replaceAll("\\W+", "");
		token = token.toLowerCase();
		
		if (isNumber(token))
			return "NUM";
		else
			return token;
	}
	
	public boolean isNumber(String token) {
		return token.matches("\\d+");
	}	
	
	protected boolean isLegit(String token) {
		return !token.isEmpty() 
			&& !m_stopwords.contains(token)
			&& token.length()>1
			&& token.length()<20;
	}
	
	protected boolean isBoundary(String token) {
		return token.isEmpty();//is this a good checking condition?
	}
	
	//Given a long string, tokenize it, normalie it and stem it, return back the string array.
	protected String[] TokenizerNormalizeStemmer(String source){
		String[] tokens = Tokenizer(source); //Original tokens.
		//Normalize them and stem them.		
		for(int i = 0; i < tokens.length; i++)
			tokens[i] = SnowballStemming(Normalize(tokens[i]));
		
		LinkedList<String> Ngrams = new LinkedList<String>();
		int tokenLength = tokens.length, N = m_Ngram;		
		for(int i=0; i<tokenLength; i++) {
			String token = tokens[i];
			boolean legit = isLegit(token);
			if (legit)
				Ngrams.add(token);//unigram
			
			//N to 2 grams
			if (!isBoundary(token)) {
				for(int j=i-1; j>=Math.max(0, i-N+1); j--) {	
					if (isBoundary(tokens[j]))
						break;//touch the boundary
					
					token = tokens[j] + "-" + token;
					legit |= isLegit(tokens[j]);
					if (legit)//at least one of them is legitimate
						Ngrams.add(token);
				}
			}
		}
		
		return Ngrams.toArray(new String[Ngrams.size()]);
	}
	
	
	public void loadQuestionFile(File file) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsoluteFile()), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;

			while((line=reader.readLine())!=null) {
				buffer.append(line);
			}
			reader.close();
			//System.out.println(Jsoup.parse(buffer.toString()).text());
			
			String key = questionNumberExtractor(file.getName());
			if(!posts.containsKey(key))
				posts.put(key, Jsoup.parse(buffer.toString()).text()+" #####"); // ##### end of question
			else{
				String tmp = posts.get(key) + " "+ Jsoup.parse(buffer.toString()).text();
				posts.put(key, tmp);
			}
		} catch (IOException e) {
			System.err.format("[Error]Failed to open file %s!", file.getName());
			e.printStackTrace();
		} 
	}
	
	
	public void loadAnswerFile(File file) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsoluteFile()), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;

			while((line=reader.readLine())!=null) {
				buffer.append(line);
			}
			reader.close();
			//System.out.println(Jsoup.parse(buffer.toString()).text());
			
			String key = answerNumberExtractor(file.getName());
			if(!posts.containsKey(key))
				posts.put(key, Jsoup.parse(buffer.toString()).text());
			else{
				//System.out.println("Found");
				String tmp = posts.get(key) + " "+ Jsoup.parse(buffer.toString()).text();
				posts.put(key, tmp);
			}
		} catch (IOException e) {
			System.err.format("[Error]Failed to open file %s!", file.getName());
			e.printStackTrace();
		} 
	}
	
	public String questionNumberExtractor(String fileName){
		int startIndex = fileName.indexOf("n") + 1;
		int endIndex = fileName.indexOf(".");
		
		return fileName.substring(startIndex, endIndex);
	}
	
	public String answerNumberExtractor(String fileName){
		int startIndex = fileName.indexOf("r") + 1;
		int endIndex = fileName.indexOf("_");
		
		return fileName.substring(startIndex, endIndex);
	}
	
	public void LoadQuestionDirectory(String folder, String suffix) {
		int postCounter = 0;
		File dir = new File(folder);
		for (File f : dir.listFiles()) {
			if (postCounter>10000)
				break;
			if (f.isFile() && f.getName().endsWith(suffix)){
				//analyzeDocumentDemo(LoadJson(f.getAbsolutePath()));
				//System.out.println(f.getName());
				//System.out.println(questionNumberExtractor(f.getName()));
				loadQuestionFile(f);
				postCounter++;
			}
			else if (f.isDirectory())
				LoadQuestionDirectory(f.getAbsolutePath(), suffix);
		}
		System.out.println("Loading " + postCounter + " post documents from " + folder);
	}
	
	public void LoadAnswerDirectory(String folder, String suffix) {
		int postCounter = 0;
		File dir = new File(folder);
		for (File f : dir.listFiles()) {
			if (postCounter>10000)
				break;
			if (f.isFile() && f.getName().endsWith(suffix)){
				//analyzeDocumentDemo(LoadJson(f.getAbsolutePath()));
				//System.out.println(f.getName());
				//System.out.println(answerNumberExtractor(f.getName()));
				loadAnswerFile(f);
				postCounter++;
			}
			else if (f.isDirectory())
				LoadAnswerDirectory(f.getAbsolutePath(), suffix);
		}
		System.out.println("Loading " + postCounter + " answer documents from " + folder);
	}
	
	
	public void generatePosts(){
		Iterator it = posts.entrySet().iterator();
		int fileIndex = 0;
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        //System.out.println(pair.getKey() + " = " + pair.getValue());
	        //String[] tmp = TokenizerNormalizeStemmer(pair.getValue().toString());
	        /*for(String str:tmp){
	        	System.out.print(str+" ");
	        }
	        System.out.println();*/
	        if(fileIndex%100!=0){ // for training
		      
	        	if(pair.getValue().toString().split("#####").length<=1)
	        		continue;
	        	String[] tokens = TokenizerNormalizeStemmer(pair.getValue().toString());
	        	if(tokens.length<20)
	        		continue;
	        	
	        	for (String token : tokens) {
	        		if(token=="" || token==null)
	        			continue;
	        		double value = 1.0;
	        		if (vocabulary.containsKey(token)) {
	        			value = vocabulary.get(token) + 1.0;
	        		}

	        		vocabulary.put(token, value);
	        		totalVocabularyCount++;
	        	}
		        languageModel langModel = new languageModel(tokens);
		        langModel.unigramModel();
		        
		        //langModel.bigramModel();
		        
		        languagModelList.put(pair.getKey().toString(), langModel);
		        
		    }
	        fileIndex++;
	    }
	    
	    //smoothing starts
	    Iterator langModelit = languagModelList.entrySet().iterator();
        while (langModelit.hasNext()) {
        	 Map.Entry langpair = (Map.Entry)langModelit.next();
        	 languageModel langModel = (languageModel) langpair.getValue();
        	 langModel.smoothedUnigarmModel(vocabulary, totalVocabularyCount);
        }
        System.out.println("\n\n Ranked Results\n\n");
        
	    
	}
	
	public void test(){
		
		Iterator it = posts.entrySet().iterator();
		int fileIndex = 0;
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        //System.out.println(pair.getKey() + " = " + pair.getValue());
	        //String[] tmp = TokenizerNormalizeStemmer(pair.getValue().toString());
	        /*for(String str:tmp){
	        	System.out.print(str+" ");
	        }
	        System.out.println();*/
	        if(fileIndex%100==0){ // for testing
	        	String question = pair.getValue().toString().split("#####")[0];
	        	System.out.println("Query::"+question);
		        String [] testSentence = TokenizerNormalizeStemmer(question);
		        MyPriorityQueue<_RankItem> fVector = new MyPriorityQueue<_RankItem>(5);
				
		        Iterator langModelit = languagModelList.entrySet().iterator();
		        while (langModelit.hasNext()) {
		        	 Map.Entry langpair = (Map.Entry)langModelit.next();
		        	 languageModel langModel = (languageModel) langpair.getValue();
		        	 double likelihood = langModel.estimateUniGram(testSentence);
		        	 fVector.add(new _RankItem(langpair.getKey().toString(), likelihood));
				}
		        System.out.println("\n\n Ranked Results\n\n");
		        for(_RankItem rt:fVector){
					System.out.print(rt.m_name+"("+rt.m_value+")");
					System.out.println(posts.get(rt.m_name));
		        }
				System.out.println("\n\n");
			
		        
		    }
	        fileIndex++;
	    }
		
		
	}
	
	public static void main(String[] args){
		PostAnalyzer post = new PostAnalyzer();
		post.LoadQuestionDirectory("/media/nahid/502C45162C44F90C/askUbuntu/outputFiles/question/", ".txt");
		post.LoadAnswerDirectory("/media/nahid/502C45162C44F90C/askUbuntu/outputFiles/answer/", ".txt");
		post.generatePosts();
		post.test();
	}
	
}
