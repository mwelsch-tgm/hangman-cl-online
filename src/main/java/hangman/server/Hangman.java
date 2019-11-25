package hangman.server;

import hangman.InputOutput;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

public class Hangman{

    private ArrayList<Character> answer, hit, miss;
    private int maxGuesses;
    private boolean guessedWholeWord = false;
    private String username;
    private Lock lock;
    private HashMap<String, Integer> topList;
    private InputOutput io;

    public Hangman(String answer, int maxGuesses, String username,Lock lock) {
        this.answer = new ArrayList<Character>();
        for (char c: answer.toCharArray()) {
            this.answer.add(c);
        }
        this.maxGuesses = maxGuesses;
        this.hit = new ArrayList<>();
        this.miss = new ArrayList<>();
        this.io = new InputOutput("/mnt/storage/gitclones/hangman-cl-online/src/main/java/hangman/toplist.txt");
        this.username = username;
        this.lock = lock;
        this.topList = new HashMap<>();
    }

    /**
     * Guess the given character and tells you if it is contained within the answer
     * @param c the character you want to guess
     * @return true if it is contained within the answer, otherwise false
     */
    public boolean guess(char c){
        if(answer.contains(c)){
            if(!hit.contains(c))
                hit.add(c);
            return true;
        }
        if(!miss.contains(c))
            miss.add(c);
        return false;
    }
    public int getRemainingTries() {
        return  maxGuesses - miss.size();
    }

    public void aufloesen(String guess){
        guessedWholeWord = true;
        for(char c: guess.toCharArray()){
            this.guess(c);
        }
    }

    public  String showObscuredAnswer(){
        String s = "";
        for (char c:answer) {
            if (hit.contains(c)){
                s+=c;
            }
            else{
                s+="_";
            }
        }
        return s;
    }

    public void readTopList(){
        ArrayList<String> topList = io.readFile();
        for (String s:topList) {
            String[] parts = s.split(" ");
            this.topList.put(parts[1],Integer.parseInt(parts[0]));
        }
    }

    public void addToHighscore(){
        if(isWon()){
            Map.Entry<String, Integer> worstPlayer= null;
            try {
                lock.tryLock(2000,TimeUnit.MILLISECONDS);
                readTopList();
                for(Map.Entry<String, Integer> entry : this.topList.entrySet()) {

                    String key = entry.getKey();
                    Integer value = entry.getValue();
                    if(worstPlayer==null){
                        worstPlayer = new AbstractMap.SimpleEntry<String, Integer>(key,value);
                    }
                    else if(value<worstPlayer.getValue()) {
                        worstPlayer = new AbstractMap.SimpleEntry<String, Integer>(key,value);
                    }
                }
                if(worstPlayer.getValue()<getRemainingTries()){
                    this.topList.remove(worstPlayer.getKey(), worstPlayer.getValue());
                    this.topList.put(this.username,getRemainingTries());
                    writeTopList();
                    System.out.println("The toplist changed!");
                }
                lock.unlock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void writeTopList(){
        ArrayList<String> arrayList = new ArrayList<>();
        for(Map.Entry<String, Integer> entry : this.topList.entrySet()) {
            String key = entry.getKey();
            Integer value = entry.getValue();
            arrayList.add(value + " " + key);
        }
        Collections.sort(arrayList);
        System.out.println(arrayList);
        this.io.writeToFile(arrayList);
    }


    public boolean isWon() {
        return this.showObscuredAnswer().indexOf('_') == -1;
    }

    public boolean outOfTries() {
        return getRemainingTries() <=0;
    }

}
