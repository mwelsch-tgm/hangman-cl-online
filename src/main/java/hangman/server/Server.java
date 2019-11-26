package hangman.server;

import hangman.InputOutput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * With this Server you can provide
 * a hangman game to everyone who want to play it!
 * @author Moritz Welsch
 * @date 2019-11-26
 */
public class Server {
    private Integer port = 0;
    private boolean listening = false;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private ArrayList<Game> games = new ArrayList<>();
    private ArrayList<String> words;
    private Random randomGenerator;
    private ServerSocket serverSocket;
    private Lock lock;

    /**
     * Initializes port, generates a random object, a lock and reads with the lock the words from the wordlist
     * When adding or removing something from the wordlist you have to restart the server
     *
     * @param port   Integer for the listening port
     */
    public Server(Integer port) {
        if (port != null) this.port = port;
        InputOutput io = new InputOutput("/mnt/storage/gitclones/hangman-cl-online/src/main/java/hangman/words.txt");
        this.lock = new ReentrantLock();
        this.lock.lock();
        words = io.readFile();
        this.lock.unlock();
        randomGenerator = new Random();

    }

    /**
     * @return True if still listening and online
     */
    public boolean isListening() {
        return listening;
    }

    /**
     * Initiating the ServerSocket with already defined Parameters and starts accepting incoming
     * requests. If client connects to the ServerSocket a new Game will be created and passed
     * to the ExecutorService for immediate concurrent action. A thread is started aswell, waiting
     * for an exit command
     */
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))){
            this.serverSocket = serverSocket;
            listening = true;
            System.out.println("Running on localhost:"+this.getPort());
            System.out.println("Exit with !exit");
            ReadInput ri = new ReadInput(bufferedReader,this);
            executorService.execute(ri);
            Socket clientsocket = null;
            while (listening) {
                System.out.println("Waiting for new player...");
                try{
                    clientsocket = serverSocket.accept();
                }catch (SocketException e){
                    break;
                }

                int index = randomGenerator.nextInt(words.size());
                Game game = new Game(clientsocket,words.get(index),lock);
                executorService.execute(game);
                this.games.add(game);
                System.out.println("Accepted a new player...");
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Start the Server with the given port number
     * @param args the port number on which the server should listen on in the format [port]
     */
    public static void main(String[] args) {
        int port = 0;
        if(args.length!=1){
            System.out.println("Usage: gradle server --args=\"[portNumber]\"");
            System.exit(1);
        }
        try{
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("Error while parsing your port");
            System.exit(1);
        }
        Server miep = new Server(port);
        miep.run();
    }

    /**
     * Return the Port on which the server is listening
     * @return Port on which the server is listening
     */
    private int getPort() {
        return this.serverSocket.getLocalPort();
    }

    /**
     * Clean shutdown of Server
     * <br>
     * Finally we are closing all open resources.
     */
    public void shutdown() {
        listening = false;
        for (Game game:this.games) {
            game.shutdown();
        }
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(executorService!=null)
            executorService.shutdownNow();
    }

}
/**
 * Reads the input, if it is an exit command it shuts the
 * given server down
 * @author Moritz Welsch
 * @date 2019-11-26
 */
class ReadInput implements Runnable{
    private BufferedReader in;
    private Server server;

    /**
     * Initialize the ReadInput
     * @param in the bufferedReader from which will be read
     * @param server the server to shut down if an exit command occurs
     */
    public ReadInput(BufferedReader in, Server server) {
        this.in = in;
        this.server = server;
    }

    /**
     * starts a loop, based on whether the client is still listening and the bufferedReader beeing null
     * if the loop is exited or the exit command gets send the server will receive a shutdown signal
     */
    @Override
    public void run() {
        String s;
            try {
                while(server.isListening()&&(s=in.readLine())!=null){
                    if(s.equals("!exit")){
                        break;
                    }
                    else{
                        System.out.println("Available commands: !exit");
                    }
                }
                server.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }


    }
}
/**
 * Thread for client socket connection.<br>
 * Every client has to be handled by an own Thread.
 */
class Game implements Runnable {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private boolean listening = true;

    private Hangman hangman;
    /**
     * Init of ClientWorker-Thread for socket intercommunication
     *
     * @param socket   Socket got from ServerSocket.accept()
     * @throws IOException will be throwed if the init of Input- or OutputStream fails
     */
    Game(Socket socket, String answer,Lock lock) throws IOException {
        this.socket = socket;

        System.out.println("The answer for the new player is: " + answer);
        out = new PrintWriter(socket.getOutputStream(),true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String username = in.readLine();
        username = username.substring(10);
        this.hangman = new Hangman(answer,10,username,lock);
    }

    /**
     * MessageHandler for incoming Messages on Client Socket
     * <br>
     * Incoming messages first will be checked if they are a single character or a whole word
     */
    @Override
    public void run() {
        String msg;
        try{
            out.println(this.hangman.showObscuredAnswer());
            while(listening && (msg = in.readLine())!= null){
                if(msg.length()==1){
                    char c = msg.charAt(0);
                    this.hangman.guess(c);
                    if(this.hangman.isWon()){
                        out.println("You won!");
                        break;
                    }
                    else if(this.hangman.outOfTries()){
                        out.println("You lost!");
                        break;
                    }
                }
                else if(msg.length()>=1){
                    this.hangman.aufloesen(msg);
                    boolean b = this.hangman.isWon();
                    if(b){
                        out.println("You won!");
                    }
                    else if(!b){
                        out.println("You lost!");
                    }
                    break;
                }
                out.println("Remaining tries: " + this.hangman.getRemainingTries());
                out.println(this.hangman.showObscuredAnswer());

            }
            this.shutdown();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Clean shutdown of Game
     * Finally we are closing all open resources.
     */
    void shutdown() {
        hangman.addToHighscore();
        listening = false;
        out.println("[EXITING NOW]");


        try {
            if(in!=null)
                in.close();
            if(socket!=null){
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
