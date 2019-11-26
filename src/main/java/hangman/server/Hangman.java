package hangman.server;

import hangman.InputOutput;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;


/**
 * This is the actual Hangman game you're playing
 * @author Moritz Welsch
 * @date 2019-11-26
 */
public class Hangman{

    private ArrayList<Character> answer, hit, miss;
    private int maxGuesses;
    private boolean guessedWholeWord = false;
    private String username;
    private Lock lock;
    private HashMap<String, Integer> topList;
    private InputOutput io;

    /**
     * Initialize hangman
     * @param answer the answer which the client has to guess
     * @param maxGuesses the number of different guesses until the client looses
     * @param username the name of the user if he is added to the toplist
     * @param lock the lock which will be used to read and write to the toplist
     */
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

    /**
     *
     * @return the number of tries remaining
     */
    public int getRemainingTries() {
        return  maxGuesses - miss.size();
    }

    /**
     * Guess a whole word. Game is either won or lost afterwords
     * @param guess the word you think is true
     */
    public void aufloesen(String guess){
        guessedWholeWord = true;
        for(char c: guess.toCharArray()){
            this.guess(c);
        }
    }

    /**
     * Substitues ungessed spots from the answer with underscores
     * @return a String only showing the letters you guesse correctly
     */
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

    /**
     * Read the toplist and set it to the local variable toplist
     */
    public void readTopList(){
        ArrayList<String> topList = io.readFile();
        for (String s:topList) {
            String[] parts = s.split(" ");
            this.topList.put(parts[1],Integer.parseInt(parts[0]));
        }
    }

    /**
     * checks if the game is won and the player is better than
     * the worst player of the toplist. If so the worst player is
     * removed and the current player added and written
     */
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

    /**
     * Write to the toplist from the local variable toplist
     */
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


    /**
     *
     * @return wheter the game is won or not
     */
    public boolean isWon() {
        return this.showObscuredAnswer().indexOf('_') == -1;
    }

    /**
     *
     * @return wheter the player is out of tries
     */

    public boolean outOfTries() {
        return getRemainingTries() <=0;
    }

}
