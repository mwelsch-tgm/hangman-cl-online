package hangman.server;

import hangman.InputOutput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private Integer port = 0;
    private boolean listening = false;
    private ConcurrentHashMap<Game, String> workerList = new ConcurrentHashMap<>();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private ArrayList<String> words;
    private Random randomGenerator;
    private ServerSocket serverSocket;

    /**
     * Initializes host, port and callback for UserInterface interactions.
     *
     * @param port   Integer for the listening port
     */
    public Server(Integer port) {
        if (port != null) this.port = port;
        InputOutput io = new InputOutput("/mnt/storage/gitclones/hangman-cl-online/src/main/java/hangman/words.txt");
        words = io.readFile();
        randomGenerator = new Random();
    }

    /**
     * Initiating the ServerSocket with already defined Parameters and starts accepting incoming
     * requests. If client connects to the ServerSocket a new ClientWorker will be created and passed
     * to the ExecutorService for immediate concurrent action.
     */
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)){
            this.serverSocket = serverSocket;
            listening = true;
            System.out.println(words.toString());
            while (listening) {
                System.out.println("Waiting for new player...");
                Socket clientsocket = serverSocket.accept();
                int index = randomGenerator.nextInt(words.size());
                System.out.println(words.toString());
                Game game = new Game(clientsocket,words.get(index));
                executorService.execute(game);
                System.out.println("Accepted a new player...");
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        System.out.println("Usage: gradle server --args=\"[portNumber]\"");
        int port = 0;
        try{
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        Server miep = new Server(port);
        miep.run();
        System.out.println("Running on localhost:"+miep.getPort());
    }

    private int getPort() {
        return this.serverSocket.getLocalPort();
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
    Game(Socket socket, String answer) throws IOException {
        this.socket = socket;
        this.hangman = new Hangman(answer,10);
        System.out.println(answer);
        out = new PrintWriter(socket.getOutputStream(),true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    /**
     * MessageHandler for incoming Messages on Client Socket
     * <br>
     * The InputSocket will be read synchronous through readLine()
     * Incoming messages first will be checked if they start with any Commands, which will be executed properly.
     * Otherwise text messages will be delegated to the {@link Server#received(String, ClientWorker)} method.
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
                        listening = false;
                        break;
                    }
                    else if(this.hangman.outOfTries()){
                        listening = false;
                        out.println("You lost!");
                        break;
                    }
                }
                else if(msg.length()>=1){
                    boolean b = this.hangman.aufloesen(msg);
                    if(b){
                        out.println("You won!");
                    }
                    else if(!b){
                        out.println("You lost!");
                    }
                    listening = false;
                    break;
                }
                out.println("Remaining tries: " + this.hangman.getRemainingTries());
                out.println(this.hangman.showObscuredAnswer());

            }
            socket.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Clean shutdown of ClientWorker
     * Finally we are closing all open resources.
     */
    void shutdown() {
    }


}
