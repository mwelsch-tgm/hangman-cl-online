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
     * Initializes host, port and callback for UserInterface interactions.
     *
     * @param port   Integer for the listening port
     */
    public Server(Integer port) {
        if (port != null) this.port = port;
        InputOutput io = new InputOutput("/mnt/storage/gitclones/hangman-cl-online/src/main/java/hangman/words.txt");
        words = io.readFile();
        randomGenerator = new Random();
        this.lock = new ReentrantLock();
    }

    public boolean isListening() {
        return listening;
    }

    /**
     * Initiating the ServerSocket with already defined Parameters and starts accepting incoming
     * requests. If client connects to the ServerSocket a new ClientWorker will be created and passed
     * to the ExecutorService for immediate concurrent action.
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
class ReadInput implements Runnable{
    private BufferedReader in;
    private Server server;

    public ReadInput(BufferedReader in, Server server) {
        this.in = in;
        this.server = server;
    }

    @Override
    public void run() {
        String s;
            try {
                while(server.isListening()&&(s=in.readLine())!=null){
                    if(s.equals("!exit")){
                        break;
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
     * Clean shutdown of ClientWorker
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
