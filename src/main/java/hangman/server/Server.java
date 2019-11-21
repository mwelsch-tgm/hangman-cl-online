package hangman.server;

import hangman.InputOutput;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private Integer port = 5050;
    private boolean listening = false;
    private ConcurrentHashMap<Game, String> workerList = new ConcurrentHashMap<>();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private ArrayList<String> words;

    /**
     * Initializes host, port and callback for UserInterface interactions.
     *
     * @param port   Integer for the listening port
     */
    public Server(Integer port) {
        if (port != null) this.port = port;
        InputOutput io = new InputOutput("words.txt");
        words = io.readFile();
    }

    /**
     * Initiating the ServerSocket with already defined Parameters and starts accepting incoming
     * requests. If client connects to the ServerSocket a new ClientWorker will be created and passed
     * to the ExecutorService for immediate concurrent action.
     */
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port);){
            listening = true;
            while (listening) {
                System.out.println("Waiting for players");
                Socket clientsocket = serverSocket.accept();
                Game game = new Game(clientsocket);
                executorService.execute(game);
                /*
                ClientWorker clientworker = new ClientWorker(clientsocket,this);
                executorService.execute(clientworker);
                workerList.put(clientworker,"");*/
            }
        } catch(IOException e){
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        Server miep = new Server(5050);
        miep.run();
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
        out = new PrintWriter(socket.getOutputStream(),true);
        in = new BufferedReader(new InputStreamReader(socket        .getInputStream()));
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
            System.out.println("Accepted connection,W8 for user input");
            while(listening && (msg = in.readLine())!= null){
                System.out.println("Received: " + msg);
                System.out.println(msg.length()==1);
                if(msg.length()==1){
                    char c = msg.charAt(0);
                    System.out.println(this.hangman);
                    System.out.println(this.hangman.showObscuredAnswer());
                    System.out.println(c);

                    this.hangman.guess(c);
                    out.println(this.hangman.showObscuredAnswer());
                }

            }
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
