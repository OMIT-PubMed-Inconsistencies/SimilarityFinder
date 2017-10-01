import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;
import edu.cmu.lti.ws4j.RelatednessCalculator;
import edu.cmu.lti.ws4j.impl.WuPalmer;
import net.didion.jwnl.JWNLException;
import net.didion.jwnl.data.*;
import net.didion.jwnl.data.list.PointerTargetNode;
import net.didion.jwnl.data.list.PointerTargetNodeList;
import net.didion.jwnl.dictionary.Dictionary;
import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.URL;
import java.util.*;

public class SimilarityFinder {

    HashMap<String,ArrayList<String[]>> data=new HashMap<String,ArrayList<String[]>>();
    HashMap<String,HashMap<String,SimElement>> dataSims=new HashMap<String,HashMap<String,SimElement>>();


    String[] stopWords=new String[]{ "a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also","although","always","am","among", "amongst", "amoungst", "amount",  "an", "and", "another", "any","anyhow","anyone","anything","anyway", "anywhere", "are", "around", "as",  "at", "back","be","became", "because","become","becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom","but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven","else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ltd", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own","part", "per", "perhaps", "please", "put", "rather", "re", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "within", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the"};
    String[] pronouns=new String[]{"I","me","we","us","you","he","she","him","her","they","them","thou"};
    String stopS=null;
    float yesWeight=1;
    float notWeight=10;
    float sigma=0.1f;
    HashMap<String,Float> frqDictionary=new HashMap<String,Float>();
    HashMap<String,String> lemmaDictionary=new HashMap<String,String>();
    String tripleFolder="../output/04_TripleCreator";
    HashSet<Integer> dateIDs =new HashSet<Integer>();
    HashMap<Integer,Integer[]> idToDates =new HashMap<Integer,Integer[]>();
    ArrayList<String[]> analyzedResult=new ArrayList<String[]>();

    PointerUtils pu=PointerUtils.getInstance();
    Dictionary dictionary = Dictionary.getInstance();

    ILexicalDatabase db = new NictWordNet();
    private RelatednessCalculator rc= new WuPalmer(db);
    String[] type=new String[]{"#n","#v","#other"};

    ArrayList<String> tripleNames =new ArrayList<String>();
    HashMap<String, Integer> monthMap=new HashMap<String, Integer>();
    String[] testStrings=new String[]{"increase","expand","decrease","change","cat"};  //{"increase","decrease","change","measure","cat"}

    class SimElement{
        String name;
        int match=0;
        int nearMatch=0;
        float nearSum=0;
        float max=Float.MIN_VALUE;
        float min=Float.MAX_VALUE;

        public SimElement(String name) {
            this.name = name;
        }

        public void addMatch(){
            match++;
            updateMinMax(1);
        }

        public void addNearrMatch(float val){
            nearMatch+=val;
            nearSum++;
            updateMinMax(val);
        }

        private void updateMinMax(float val){
            max=Math.max(max,val);
            min=Math.min(min,val);
        }

    }






    public static void main(String[] args) {
        SimilarityFinder cc=new SimilarityFinder();



        cc.loadDictionaries();
        cc.readList();
        cc.readFiles();
       cc.analize();
      // cc.grabDates();





      //  cc.testStrings();

     //   cc.writeResults();




        // System.out.println(": "+cc.SimilarityDifference("decrease", 1,"increase",1));

        //cc.printArray(cc.getAntonyms("were"));

  /*      String[][] examples={
                            {"were transfected with","were transfected with"},
                            {"were transfected with","were not transfected with"},
                            {"were transfected with","were transfected without"},
                            {"increase","augment"},
                            {"increase","decrease"},
                            {"also reduced","reduce"},
                            {"also reduced","was significantly upregulated in"},
                            {"reduce","was significantly upregulated in"},
                            {"follicles cyclically degenerate throughout","follicles regenerate throughout"},
                            {"would decrease 34.6 insulin-resistant adults","increase"}

        };

        for (int i = 0; i <examples.length ; i++) {
            System.out.println(examples[i][0]+" , "+examples[i][1]+" : "+cc.checkForNegation(examples[i][0],examples[i][1]));
        }*/

         //System.out.println(cc.checkForNegation("were transfected with","were transfected with"));
    //      System.out.println(cc.checkForNegation("were transfected with","were not transfected with"));
     //     System.out.println(cc.checkForNegation("were transfected with","were transfected without"));
     //     System.out.println(cc.checkForNegation("increase","augment"));
   //     System.out.println(cc.checkForNegation("increase","decrease"));
    //       System.out.println(cc.checkForNegation("also reduced","reduce"));
    //        System.out.println(cc.checkForNegation("also reduced","was significantly upregulated in"));
    //     System.out.println(cc.checkForNegation("reduce","was significantly upregulated in"));
     //     System.out.println(cc.checkForNegation("follicles cyclically degenerate throughout","follicles regenerate throughout"));
      //    System.out.println(cc.checkForNegation("would decrease 34.6 Ãƒ'Ã‚ Â± 0.8 kg/m( 2 ) ) insulin-resistant adults","increase"));
    }

    private void testStrings(){
        StringBuilder sb=new StringBuilder();


        for (int i = 0; i < testStrings.length; i++) {
            sb.append(similarity(testStrings[0],testStrings[i]));
            sb.append("\t\t");
        }
        sb.append("\n");


        sb.append("\t\t\t");
        for (int j = 0; j < testStrings.length ; j++) {
            sb.append((j+1));
            sb.append("\t\t");
        }
        sb.append("\n");
        float[][] dist=new float[testStrings.length][testStrings.length];


        for (int i = 0; i < testStrings.length; i++) {
            sb.append((i+1)+"\t\t");
            for (int j = 0; j < testStrings.length ; j++) {
                dist[i][j]=Math.round(SimilarityDifference(testStrings[i],1,testStrings[j],1)*100);
                dist[i][j]/=500;
                sb.append(dist[i][j]);
                sb.append("\t\t");

            }
            sb.append("\n");
        }

        sb.append("\n");

        sb.append("\t\t");
        for (int j = 0; j < testStrings.length ; j++) {
            sb.append((j/testStrings.length)+1);
            sb.append(",");
            sb.append((j%testStrings.length)+1);
            sb.append("\t\t");
        }
        sb.append("\n");

        for (int i = 0; i < 1; i++) {
            int p1i=(i/testStrings.length);
            int p1j=(i%testStrings.length);


            sb.append(p1i+1);
            sb.append(",");
            sb.append(p1j+1);
            sb.append("\t\t");
            for (int j = 0; j < testStrings.length ; j++) {

                int p2i=(j/testStrings.length);
                int p2j=(j%testStrings.length);
                float diste=Math.abs(dist[p1i][p1j]-dist[p2i][p2j]);
                sb.append(diste);
               sb.append("\t\t");

            }
            sb.append("\n");
        }

        sb.append("\n");

       // checkForNegation

        sb.append("\t\t\t");
        for (int j = 0; j < testStrings.length ; j++) {
            sb.append((j+1));
            sb.append("\t\t");
        }
        sb.append("\n");
        dist=new float[testStrings.length][testStrings.length];


        for (int i = 0; i < testStrings.length; i++) {
            sb.append((i+1)+"\t\t");
            for (int j = 0; j < testStrings.length ; j++) {
                dist[i][j]=Math.round(checkForNegation(testStrings[i],testStrings[j])*100);
                dist[i][j]/=500;
                sb.append(dist[i][j]);
                sb.append("\t\t");

            }
            sb.append("\n");
        }

        sb.append("\n");

        sb.append("\t\t");
        for (int j = 0; j < testStrings.length ; j++) {
            sb.append((j/testStrings.length)+1);
            sb.append(",");
            sb.append((j%testStrings.length)+1);
            sb.append("\t\t");
        }
        sb.append("\n");

        for (int i = 0; i < 1; i++) {
            int p1i=(i/testStrings.length);
            int p1j=(i%testStrings.length);


            sb.append(p1i+1);
            sb.append(",");
            sb.append(p1j+1);
            sb.append("\t\t");
            for (int j = 0; j < testStrings.length ; j++) {

                int p2i=(j/testStrings.length);
                int p2j=(j%testStrings.length);
                float diste=Math.abs(dist[p1i][p1j]-dist[p2i][p2j]);
                sb.append(diste);
                sb.append("\t\t");

            }
            sb.append("\n");
        }

        System.out.println(sb.toString());
    }

    public SimilarityFinder() {
        JWNLConnecter.initializeJWNL();
        pu = PointerUtils.getInstance();
        dictionary = Dictionary.getInstance();

        monthMap.put("Jan", 1);
        monthMap.put("Feb", 2);
        monthMap.put("Mar", 3);
        monthMap.put("Apr", 4);
        monthMap.put("May", 5);
        monthMap.put("Jun", 6);
        monthMap.put("Jul", 7);
        monthMap.put("Aug", 8);
        monthMap.put("Sep", 9);
        monthMap.put("Oct", 10);
        monthMap.put("Nov", 11);
        monthMap.put("Dec", 12);
        monthMap.put("January", 1);
        monthMap.put("February", 2);
        monthMap.put("March", 3);
        monthMap.put("April", 4);
        monthMap.put("May", 5);
        monthMap.put("June", 6);
        monthMap.put("July", 7);
        monthMap.put("August", 8);
        monthMap.put("September", 9);
        monthMap.put("October", 10);
        monthMap.put("November", 11);
        monthMap.put("December", 12);
    }

    private void loadDictionaries(){
        FileReader fileReader = null;
        try {
            fileReader = new FileReader("../output/Dictionary.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = null;


            while ((line = bufferedReader.readLine()) != null) {

                //line=line.substring(line.indexOf("(")+1,line.lastIndexOf(")"));
                // System.out.println(line);
                String[] parts = line.split(" ");
                lemmaDictionary.put(parts[0],parts[1]);
                frqDictionary.put(parts[0],Float.parseFloat(parts[2]));

            }
        }
        catch (Exception e) {

        }
    }

    private void readList(){
        System.out.println("Reading file list");
        File folder = new File(tripleFolder);
        File[] listOfFiles = folder.listFiles();
        for (int i = 0; i < listOfFiles.length; i++) {
            tripleNames.add(listOfFiles[i].getName());
        }
    }

    private void readFiles(){
        System.out.println("Reading files");
        for (int i = 0; i < tripleNames.size() ; i++) {
            FileReader fileReader = null;
            try {
                String fileName=tripleNames.get(i);
                String path=tripleFolder+"/"+fileName;
                fileReader = new FileReader(path);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String line = null;
                fileName=fileName.substring(0,fileName.indexOf("."));



                while ((line = bufferedReader.readLine()) != null) {

                    String conf=line.substring(0,line.indexOf("(")-1); //OLLIE confidence value
                    //System.out.println(conf);

                    String triple=line.substring(line.indexOf("(")+1,line.lastIndexOf(")")); //Triple

                    String[] parts=triple.split(";");
                    parts=trim(parts);
                    String key=parts[0]+";"+parts[2];
                    ArrayList<String[]> val=data.get(key);
                    if(val==null){
                        val=new ArrayList<String[]>();
                    }
                    String num=line.substring(line.lastIndexOf(")")+1);
                    num=num.trim();


                    val.add(new String[]{parts[1],fileName,num,conf});
                    data.remove(key);
                    data.put(key,val);

                }

            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void analize(){
        System.out.println("Analyzing");
        ArrayList<String> printList=new ArrayList<String>();
        Iterator<String> itr=data.keySet().iterator();
        int total=data.size();
        int count=1;
        int cCount=1;
        String confidence;
        int id1,id2,minID,maxID;
        String[] dataString=null;
        // int limit=2000;
        float max=Float.NEGATIVE_INFINITY;
        float res=0;


        while(itr.hasNext()/*&& limit>0*/){
            // limit--;

            String key=itr.next();
            ArrayList<String[]> val=data.get(key);

            HashMap<String,SimElement> smMap=new HashMap<String,SimElement>();

            if(val.size()>1 && !dataSims.keySet().contains(key)){

                for (int i = 0; i <val.size() ; i++) {
                    String[] val1=val.get(i);

                    SimElement sm=new SimElement(key);

                    for (int j = i+1; j <val.size() ; j++) {
                        String[] val2 = val.get(j);

                        if (!val1[1].equalsIgnoreCase(val2[1])) { //No point in comparing in the same abstract

//parts[1],fileName,num,conf

                            if(val1[0].equalsIgnoreCase(val2[0])&& val1[2].equalsIgnoreCase(val2[2])&& val1[3].equalsIgnoreCase(val2[3])){ //ides are different but it is the same triple (Happens when articles are retracted)
                                continue;
                            }


                            if(val1[0].equalsIgnoreCase(val2[0])){
                                sm.addMatch();
                            }
                            else{
                                res=checkForNegation(val1[0],val2[0]);
                                if(res>0){
                                    sm.addNearrMatch(res);
                                }
                            }




//git




                         //   float res=-checkForNegation(val1[0],val2[0]);

                         /*
                            try {

                                if (checkforThreashold(res, 0.05f) > 0) { //0.0005f


                                    float c1=Float.parseFloat(val1[3]);
                                    float c2=Float.parseFloat(val2[3]);


                                    confidence = Math.min(res*c1*c2*10,1.00000000f) + "";


                                    id1 = Integer.parseInt(val1[1]);
                                    id2 = Integer.parseInt(val2[1]);
                                    minID = Math.min(id1, id2);
                                    maxID = Math.max(id1, id2);

                                    dataString = new String[]{confidence, minID + "", maxID + "", key, val1[0], val1[2], val2[0], val2[2]};

                                    dateIDs.add(minID);
                                    dateIDs.add(maxID);
                                    analyzedResult.add(dataString);

                                    System.out.println(".Contradiction number " + cCount + " found at " + count + " out of " + total);
                                    cCount++;
                                } else {
                                    //System.out.println(res);
                                }
                            }
                            catch(Exception e){ //Mostly number format exception for ones with filenames like "11111 (1)"

                            }
                            */


                        }

                    }

                    smMap.put(val1[0],sm);

                }


                dataSims.put(key,smMap);

            }
            count++;
        }



    }

    private void writeResults(){
        System.out.println("Writing file");
        PrintWriter writer = null;
        StringBuilder sb=null;
        try {
            writer = new PrintWriter("../output/results_new.txt", "UTF-8");
            for (int i = 0; i <analyzedResult.size() ; i++) {
                String[] dataString=analyzedResult.get(i);
                Integer[] date1= idToDates.get(Integer.parseInt(dataString[1]));
                Integer[] date2= idToDates.get(Integer.parseInt(dataString[2]));
                sb=new StringBuilder();
                sb.append(dataString[0]); //confidence
                sb.append(";");
                sb.append(dataString[1]); //id 1
                sb.append(";");
                sb.append(date1[0]); //Date 1 : Year
                sb.append("/");
                sb.append(date1[1]); //Date 1 : Month
                sb.append("/");
                sb.append(date1[2]); //Date 1 : Day
                sb.append(";");
                sb.append(dataString[2]); //id 2
                sb.append(";");
                sb.append(date2[0]); //Date 2 : Year
                sb.append("/");
                sb.append(date2[1]); //Date 2 : Month
                sb.append("/");
                sb.append(date2[2]); //Date 2 : Day
                sb.append(";");
                sb.append(dataString[3]); //key: A;B from triplet where a triplet is (A;R;B)
                sb.append(";");
                sb.append(dataString[4]); //R from triplet 1 where a triplet is (A;R;B)
                sb.append(";");
                sb.append(dataString[5]); //Sentence id from triplet 1
                sb.append(";");
                sb.append(dataString[6]); //R from triplet 2 where a triplet is (A;R;B)
                sb.append(";");
                sb.append(dataString[7]); //Sentence id from triplet 2
                writer.println(sb.toString());
            }
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void grabDates() {
        System.out.println("Grabbing dates");
        Iterator<Integer> itr = dateIDs.iterator();
        int pubMedid=0;
        String doc;
        Integer[] date=null;
        int count=1;

        while (itr.hasNext()) {
            pubMedid = itr.next();
            System.out.println(".Grabbing from " + pubMedid + " : " + count + " out of " + dateIDs.size());
            doc = getXMLfromID(pubMedid);
            date=getDate(doc);
            idToDates.put(pubMedid,date);
            count++;
        }
    }

    private Integer[] getDate(String doc) {
        DocumentBuilderFactory dbFactory;
        DocumentBuilder dBuilder;
        Document docu;
        Integer[] date=new Integer[3];

        dbFactory = DocumentBuilderFactory.newInstance();
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            docu = dBuilder.parse(new InputSource(new StringReader(doc)));
            NodeList nList = docu.getElementsByTagName("PubDate");
            NodeList yList = null;
            NodeList mList = null;
            NodeList dList = null;
            NodeList sList = null;

            Element eElement = null;
            Node nNode;

            for (int i = 0; i < nList.getLength(); i++) {
                nNode = nList.item(i);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    eElement = (Element) nNode;
                    yList = eElement.getElementsByTagName("Year");
                    mList = eElement.getElementsByTagName("Month");
                    dList = eElement.getElementsByTagName("Day");


                    //If the month is not there in the pubdate, default to "DateCreated"
                    if (mList == null || mList.getLength() == 0) {
                        sList = docu.getElementsByTagName("DateCreated");

                        for (int j = 0; j < sList.getLength(); j++) {
                            nNode = sList.item(i);
                            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                                eElement = (Element) nNode;
                                yList = eElement.getElementsByTagName("Year");
                                mList = eElement.getElementsByTagName("Month");
                                dList = eElement.getElementsByTagName("Day");
                            }
                        }
                    }

                    try {
                        date[0] = Integer.parseInt(GetStringValueOf(yList));
                    } catch (Exception e) {
                        date[0] = 2015; //Fallback default values
                    }

                    try {
                        date[1] = Integer.parseInt(GetStringValueOf(mList));
                    } catch (Exception e) {
                        date[1] = monthMap.get(GetStringValueOf(mList));

                        if( date[1]==null){
                            date[1]=12;  //Fallback default values
                        }

                    }

                    try {
                        date[2] = Integer.parseInt(GetStringValueOf(dList));
                    } catch (Exception e) {
                        date[2] = 01;   //Fallback default values
                    }


                    return date;
                }
            }

        } catch (Exception e) {
            // e.printStackTrace();
        }
        return date;
    }

    private String GetStringValueOf(NodeList yList) {
        Node nNode;
        Element eElement;//Get year
        for (int j = 0; j <yList.getLength() ; j++) {
            nNode= yList.item(j);
            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                eElement = (Element) nNode;
                return eElement.getTextContent();
            }
        }
        return "";
    }

    private String  getXMLfromID(int pubMedid) {
        boolean collect = false ;
        StringBuilder sb = null;
        URL u;
        BufferedReader in;
        String inputLine;
        String doc="";

        try {

            //Retrieving XML
            u = new URL("https://www.ncbi.nlm.nih.gov/pubmed/"+pubMedid+"?report=xml&format=text");
            in = new BufferedReader(new InputStreamReader(u.openStream()));

            while ((inputLine = in.readLine()) != null) {
                if(inputLine.contains("<pre>")) {
                    collect=true;
                    sb=new StringBuilder();

                }
                else if (inputLine.contains("</pre>")) {
                    doc= StringEscapeUtils.unescapeXml(sb.toString());
                    //System.out.println(doc);
                    return  doc;
                }
                else {
                    if (collect){
                        sb.append(inputLine);
                        sb.append("\n");
                    }
                }
            }


        } catch (Exception e) {

        }
        return doc;
    }






    public float checkForNegation(String s1,String s2){
        ArrayList<String> l1=cleanString(s1);
        //printArray(l1);
        //System.out.println();

        ArrayList<String> l2=cleanString(s2);
        // printArray(l2);
        //System.out.println();

        float simDif=0;

        //if(isIntersect(s1,l2)||isIntersect(s2,l1)){
        simDif=calculateSimilarityDifference(l1,l2);
        // }

        return simDif;
    }

    public float calculateSimilarityDifference(ArrayList<String> l1,ArrayList<String> l2){
        float simDif[]=new float[2];
        int[] count=new int[2];
        float val=0;

        for (int i = 0; i < l1.size(); i++) {
            String w1=l1.get(i);
            for (int j = 0; j <l2.size() ; j++) {
                String w2=l2.get(j);
                float weight=0;
                if(w1.equalsIgnoreCase(w2)){
                    weight=getWeightOf(w1);
                    simDif[0]+=yesWeight*weight*weight; //same word
                   // System.out.println(w1+" "+yesWeight*weight*weight);
                    count[0]++;
                }
                else if(w1.contains("not "+w2)){
                   // System.out.println(w2+"!!!!!");
                    weight=getWeightOf(w2);
                    simDif[1]+=notWeight*weight*weight; //negation of word
                    count[1]++;
                   // System.out.println(w2+" "+notWeight*weight*weight);
                }
                else if(w2.contains("not "+w1)){
                   // System.out.println(w1+"!!!!!");
                    weight=getWeightOf(w1);
                    simDif[1]+=notWeight*weight*weight; //negation of word
                    count[1]++;
                    //System.out.println(w1+" "+notWeight*weight*weight);
                }
                else{


                    val=SimilarityDifference(w1,l1.size(),w2,l2.size());
                   // System.out.println(w1+" "+w2+" "+simDif[0]+" "+simDif[1]+" "+val);
                   // System.out.println(w1+" ? "+w2+" "+val);
                    if(val>0){
                        simDif[0]+=val*yesWeight*getWeightOf(w1)*getWeightOf(w2);
                        count[0]++;
                    }
                    else if(val<0){
                        simDif[1]+=(val*(-notWeight))*getWeightOf(w1)*getWeightOf(w2);
                     //   System.out.println(w1+"  :  "+ getWeightOf(w1));
                      //  System.out.println(w2+"  :  "+ getWeightOf(w2));
                        count[1]++;
                    }
                }
            }

        }

       // System.out.println(": "+simDif[0]+" "+simDif[1]);

        simDif[0]=(simDif[0]*(count[1]+sigma)*yesWeight)/(count[0]+count[1]+2*sigma); //Calculate by inverse
        simDif[1]=(simDif[1]*(count[0]+sigma)*notWeight)/(count[0]+count[1]+2*sigma);
       // simDif[0]=simDif[0]/(count[0]+sigma);
        //simDif[1]=simDif[1]/(count[1]+sigma);


       // System.out.println(": "+simDif[0]+" "+simDif[1]);

        if(simDif[0]<simDif[1]){
            return simDif[1]*(-1);
        }
        else {
            return simDif[0];
        }
    }


    private float SimilarityDifference(String w1,int n1, String w2,int n2){

        if(w1.equalsIgnoreCase(w2)){
            return 0;
        }


        String[] a1=getAntonyms(w1);
        //printArray(a1);
        //System.out.println();
        String[] a2=getAntonyms(w2);
        //printArray(a2);
        //System.out.println();

        //  System.out.println("{ "+w1+" "+w2);

        float diff1=0;
        for (int i = 0; i <a1.length ; i++) {
            diff1=Math.max(diff1,similarity(w2,a1[i]));
            // System.out.println(w2+" "+a1[i]);
        }

        float diff2=0;
        for (int i = 0; i <a2.length ; i++) {
            diff2=Math.max(diff2,similarity(w1,a2[i]));
            // System.out.println(w1+" "+a2[i]);
        }

        // System.out.println("......................");

        float diff=((diff1/n2)+(diff2/n1))/2;
        float sim = similarity(w1, w2)/(n1+n2);

       // System.out.println(w1+" "+w2+" "+diff);

     //  float sum=diff+sim;
     //   diff=diff/sum;
     //  sim=sim/sum;


      //  if(diff>sim){

            return (float)Math.max(Math.pow(Math.min(diff,1),(notWeight/(2*yesWeight))*Math.min(sim,1)+1),0)   *(-1);

          //  return diff*(-1);
   //     }
   //     else if(diff<sim){
  //          return checkforThreashold(sim,0.5f);
  //      }
  //      else{
  //          return 0;
   //     }

    }


    private float similarity(String w1, String w2){

        double max=0;
        double value;

        for (int i = 0; i <type.length; i++) {
            value = rc.calcRelatednessOfWords(w1 + type[i], w2 + type[i]);
            max=Math.max(max,value);
        }


        max=Math.min(10,max); //remove infinity

        //System.out.println(max);
        return (float)max;
    }


    private String[] getAntonyms(String w){
        if(w.contains("not ")){
            String word=w.split(" ")[1];
            String lemma=lemmaDictionary.get(word);
            if(lemma!=null){
                return new String[]{lemma};
            }
            else{
                return new String[]{word};
            }
        }

        Synset[] synsets=getSynsets(w);
        PointerTargetNodeList ptnl=new PointerTargetNodeList();
        if(synsets!=null) {
            for (int i = 0; i < synsets.length; i++) {
                try {
                    ptnl.addAll(pu.getAntonyms(synsets[i]));
                } catch (JWNLException e) {
                    e.printStackTrace();
                }
            }
        }


        HashSet<String> candidates=new HashSet<String>();

        if (ptnl.size() > 0) {
            Object[] resultsAsArray = ptnl.toArray();

            for (int i = 0; i < resultsAsArray.length; i++) {
                Synset synset = ((PointerTargetNode) resultsAsArray[i]).getSynset();
                Word[] words=synset.getWords();
                for (int j = 0; j < words.length; j++) {
                    candidates.add(words[j].getLemma());
                }
            }

        }

        String[] antonyms=new String[candidates.size()];
        int i=0;
        Iterator<String> itr=candidates.iterator();

        while(itr.hasNext()){
            antonyms[i]=itr.next();
            i++;
        }





        return antonyms;
    }

    private Synset[] getSynsets(String sWord){
        try {



            IndexWord word = dictionary.lookupIndexWord(POS.VERB, sWord);

            if(word==null){
                word = dictionary.lookupIndexWord(POS.NOUN, sWord);
            }

            if(word==null){
                word = dictionary.lookupIndexWord(POS.ADJECTIVE, sWord);
            }

            if(word==null){
                word = dictionary.lookupIndexWord(POS.ADVERB, sWord);
            }

            if(word==null){
                return null;
            }

            Synset[] s = word.getSenses();

            if(s.length==0){
                return null;
            }

            return  s;


        } catch (JWNLException e) {
            e.printStackTrace();
        }
        return null;
    }


    private float getWeightOf(String w){
        w=w.toLowerCase();
        Float val=frqDictionary.get(w);
        if(val!=null){
            return val;
        }
        return 0;
    }


    private ArrayList<String> stemAll(ArrayList<String> words){

        ArrayList<String> newWords=new ArrayList<String>();

        if(words.size()==1) { //If size one it is ossible to be already in lemma form
            String lemma=lemmaDictionary.get(words.get(0));
            if(lemma==null) {
                return words;
            }
            else{
                newWords.add(lemma);

                // printArray(newWords);
                // System.out.println();

                return newWords;
            }
        }




        for (int i = 0; i <words.size() ; i++) {
            String word=words.get(i);
            if(word.contains("not ")){
                newWords.add(word);
            }
            else{
                word=word.toLowerCase();
                String lemma=lemmaDictionary.get(word);
                //System.out.println(word+" : "+lemma);
                if(lemma!=null){
                    newWords.add(lemma);
                }
            }
        }


        // printArray(newWords);
        // System.out.println();

        return newWords;
    }

    private float checkforThreashold(float val,float threashold){
        if(val>threashold){
            return val;
        }
        return 0;
    }


    public boolean isIntersect(String s,ArrayList<String> words){
        for (int i = 0; i <words.size() ; i++) {
            if(s.contains(words.get(i))){
                return true;
            }
        }
        return false;
    }


    public ArrayList<String> cleanString(String s){
        s=s.trim();

        s=s.replace("can't","cannot");
        s=s.replace("won't","will not");
        s=s.replace("shan't","shall not");
        s=s.replace("n't"," not");

        String[] parts=s.split(" ");

        if(s.contains(" not ")){
            parts= handleNot(parts);
        }

        // printArray(parts);
        // System.out.println();

        return stemAll(handleStopWords(parts));

    }

    private void printArray(String[] arr){
        for (int i = 0; i <arr.length ; i++) {
            System.out.println(arr[i]);
        }
    }

    private void printArray(ArrayList<String> arr){
        for (int i = 0; i <arr.size() ; i++) {
            System.out.println(arr.get(i));
        }
    }

    private ArrayList<String> handleStopWords(String[] parts){

        ArrayList<String> cleanWords=new ArrayList<String>();

        if(stopS==null){
            StringBuilder sb=new StringBuilder();
            sb.append(" ");
            for (int i = 0; i <stopWords.length ; i++) {
                sb.append(stopWords[i]);
                sb.append(" ");
            }
            for (int i = 0; i < pronouns.length; i++) {
                sb.append(pronouns[i]);
                sb.append(" ");
            }

            stopS=sb.toString();
        }

        for (int i = 0; i <parts.length ; i++) {
            if(!stopS.contains(" "+parts[i]+" ")){
                cleanWords.add(parts[i]);
            }
        }



        return cleanWords;

    }


    public String[] handleNot(String[] parts){
        int notCount=0;
        for (int i = 0; i <parts.length; i++) {
            if(parts[i].equalsIgnoreCase("not")){
                notCount++;
            }
        }

        String[] nParts=new String[parts.length-notCount];
        int index=0;
        for (int i = 0; i < nParts.length; i++) {
            if(!parts[index].equalsIgnoreCase("not")){
                nParts[i]=parts[index];
                index++;
            }
            else{
                nParts[i]="not "+parts[index+1];
                index+=2;
            }
        }
        return nParts;
    }








/*
    public void readCompound(){
        FileReader fileReader = null;
        try {
            fileReader = new FileReader("../output/compound.txt");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line = null;


            while ((line = bufferedReader.readLine()) != null) {
                //SafeAddId(line);
                //
                line=line.substring(line.indexOf("(")+1,line.lastIndexOf(")"));
               // System.out.println(line);

                String[] parts=line.split(";");
                parts=trim(parts);
                String key=parts[0]+";"+parts[2];
                ArrayList<String> val=data.get(key);
                if(val==null){
                    val=new ArrayList<String>();
                }

                val.add(parts[1]);
                data.remove(key);
                data.put(key,val);

               // parts[1]=parts[1].substring(0,parts[1].length()-1);
              //  System.out.println(parts[1]);
               // parts=parts[1].split(";");
                //System.out.println(parts[2]);

            }
        }
        catch (Exception e) {

        }
        //ids=new ArrayList<String>(); //empty the IDs
    }*/

    public String[] trim(String[] arr){
        for (int i = 0; i < arr.length; i++) {
            arr[i]=arr[i].trim();
        }
        return arr;
    }
}
