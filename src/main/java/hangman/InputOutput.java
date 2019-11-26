package hangman;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * With this Class you can easily
 * read and write stuff to/from files.
 * @author Moritz Welsch
 * @date 2019-11-26
 */
public class InputOutput{

    String filename;


    /**
     * Initialize with the filename
     * @param filename the absolute path from which will be read
     */
    public InputOutput(String filename) {
        this.filename = filename;
    }

    /**
     * Reads the file line by line
     * @return an ArrayList with Strings, where each entry will represent a line
     */
    public ArrayList<String> readFile(){
        ArrayList<String> list = new ArrayList();
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(filename))){
            String line;
            while ((line = bufferedReader.readLine()) != null){
                list.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * Writes given List to the filename
     * @param content a List with Strings, where each String represents a line
     */
    public void writeToFile(List<String> content){
        try ( FileWriter writer = new FileWriter(filename);
              BufferedWriter buffer = new BufferedWriter(writer);
        ){
            for (String s:content) {
                buffer.write(s);
                buffer.newLine();
            }
            buffer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
