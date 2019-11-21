package hangman.server;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Hangman{

    private ArrayList<Character> answer, hit, miss;
    private int maxGuesses;

    public Hangman(String answer, int maxGuesses) {
        this.answer = new ArrayList<Character>();
        for (char c: answer.toCharArray()) {
            this.answer.add(c);
        }
        this.maxGuesses = maxGuesses;
        this.hit = new ArrayList<>();
        this.miss = new ArrayList<>();
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

    public boolean isWon() {
        return this.showObscuredAnswer().indexOf('_') == -1;
    }

}
