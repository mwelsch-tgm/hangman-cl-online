package hangman;

import java.io.*;
import java.util.ArrayList;

public class InputOutput{

    String filename;


    public InputOutput(String filename) {
        this.filename = filename;
    }

    public ArrayList<String> readFile(){
        ArrayList<String> list = new ArrayList();
        try(BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));){
            String line;
            while ((line = bufferedReader.readLine()) != null){
                list.add(line);
            }
        } catch (FileNotFoundException e) {
            list.add("");
            this.writeToFile(list);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    public void writeToFile(ArrayList<String> content){
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
