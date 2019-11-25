package hangman.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * Initializes host, port and callback for UserInterface interactions.
     *
     * @param name   String representation of chatName
     * @param host   String representation of hostname, on which the server should listen
     * @param port   Integer for the listening port
     */
    public Client(String name, String host, Integer port) {
        if (name != null) this.name = name;
        if (host != null) this.host = host;
        if (port != null) this.port = port;
    }

    /**
     * Initiating the Socket with already defined Parameters (host, port). Also a timeout of 2000 ms is set at connect.
     * The {@link java.net.Socket#setKeepAlive(boolean)} is set to true.
     * <br>
     * After activating {@link #listening}, the Chatname will be sent to the Server and the reading loop is started,
     * checking for the {@link BufferedReader#readLine()} and the {@link #listening} flag.
     * <br>
     * In case of an Exception the Thread will be interrupted and if the socket was connected and bound,
     * the {@link #shutdown()} method will be called.
     */
    public void run() {

        try(Socket socket = new Socket();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in))) {
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
        System.out.println("Press enter to exit");
    }

    /**
     * @return True if still listening and online
     */
    public boolean isListening() {
        return this.listening;
    }

    public static void main(String[] args) {
        if(args.length!=3){
            System.out.println("Usage: gradle client --args=\"username[without spaces] serverip port\"");
            System.exit(1);
        }
        Client client = new Client(args[0],args[1],Integer.parseInt(args[2]));
        System.out.println("Starting game...");
        client.start();

    }

    public String getGameName() {
        return name;
    }
}
class SendInputToClient implements Runnable{

    private Client client;
    private BufferedReader bufferedReader;

    public SendInputToClient(Client cl, BufferedReader bufferedReader) {
        this.client = cl;
        this.bufferedReader = bufferedReader;
    }

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