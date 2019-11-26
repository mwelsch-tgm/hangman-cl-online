package hangman.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * With this Client you can Connect to a server
 * and play hangman on it!
 * @author Moritz Welsch
 * @date 2019-11-26
 */
public class Client extends Thread{

    private String name = "Client";
    private String host = "localhost";
    private Integer port = 5050;

    private InetSocketAddress socketAddress;
    private Socket socket = null;
    private PrintWriter out;
    private BufferedReader in, bufferedReader;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private boolean listening = false;
    private String currentMessage;

    /**
     * Initializes host, port and username
     *
     * @param name   String representation of the username which will be written to the toplist if the player makes it there
     * @param host   String representation of hostname, on which the server should be listening
     * @param port   Integer for the listening port
     */
    public Client(String name, String host, Integer port) {
        if (name != null) this.name = name;
        if (host != null) this.host = host;
        if (port != null) this.port = port;
    }

    /**
     * Initiating the Socket with already defined Parameters (host, port). Also a timeout of 2000 ms is set at connect.
     * <br>
     * After activating {@link #listening} and starting a thread which listens to input from the user, the username will be sent to the Server and the reading loop is started,
     * checking for inputs from the server.
     * <br>
     * In case of an Exception, or an exit command from the server the Thread will be interrupted and
     * the {@link #shutdown()} method will be called.
     */
    public void run() {

        try(Socket socket = new Socket();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
            this.bufferedReader = bufferedReader;
            this.socket = socket;
            socketAddress = new InetSocketAddress(host,port);
            socket.connect(socketAddress,2000);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(),true);
            listening = true;
            String s = null;
            SendInputToClient sitc = new SendInputToClient(this,bufferedReader);
            executorService.execute(sitc);
            while(listening&&(s=in.readLine())!=null){
                if(s.equals("[EXITING NOW]")){
                    break;
                }
                System.out.println(s);
                listening = socket.isConnected();
            }
            this.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Sending message to the server through network
     *
     * @param message Public message for server intercommunication
     */
    public void send(String message) {
        out.println(message);
    }

    /**
     * Clean shutdown of Client
     * <br>
     * Finally we are closing all open resources.
     */
    public void shutdown() {
        listening = false;
        System.out.println("Press enter to exit");
        if(executorService!=null)
            executorService.shutdownNow();
        try {
            if(in!=null)
                in.close();
            if(bufferedReader!=null)
                bufferedReader.close();
            if(socket!=null){
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(out!=null){
            out.close();
        }

    }

    /**
     * @return True if still listening and online
     */
    public boolean isListening() {
        return this.listening;
    }

    /**
     * Parse the username, server ip and port and start the client
     * @param args Define the parameters, in this format: [username,serverip,port]
     */
    public static void main(String[] args) {
        if(args.length!=3){
            System.out.println("Usage: gradle client --args=\"username[without spaces] serverip port\"");
            System.exit(1);
        }
        Client client = new Client(args[0],args[1],Integer.parseInt(args[2]));
        System.out.println("Starting game...");
        client.start();

    }

    /**
     * Return the name of the username. Couldn't name it getName,
     * because I mustn't override that methode
     * @return
     */
    public String getGameName() {
        return name;
    }
}
/**
 * Reads Input from a given bufferedReader and sends it to the Client
 * @author Moritz Welsch
 * @date 2019-11-26
 */
class SendInputToClient implements Runnable{

    private Client client;
    private BufferedReader bufferedReader;

    /**
     * Initialize the Client and the bufferedReader
     * @param cl the client which the message should get sent to
     * @param bufferedReader the bufferedReader from which to read
     */
    public SendInputToClient(Client cl, BufferedReader bufferedReader) {
        this.client = cl;
        this.bufferedReader = bufferedReader;
    }

    /**
     * Sends the username to the server and then starts a loop,
     * based on whether the client is still listening and the bufferedReader beeing null
     */
    @Override
    public void run() {
        try {

            System.out.println("Waiting for input...");
            String input;
            client.send("[USERNAME]"+client.getGameName());
            while (client.isListening()&&(input=bufferedReader.readLine())!=null){
                client.send(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}