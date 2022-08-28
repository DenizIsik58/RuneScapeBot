package scripts;

import lombok.SneakyThrows;
import org.tribot.script.sdk.*;
import scripts.api.MyClient;
import scripts.api.utility.StringsUtility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/*public class MultiServerSocket implements Runnable{

    private ServerSocket serverSocket =  new ServerSocket(6667);
    private static List<String> names = new ArrayList<>();
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;

    public MultiServerSocket() throws IOException {
    }


    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    @SneakyThrows
    public void run() {
        while (true){
            Log.info("Accepting connections");
            clientSocket = serverSocket.accept();


            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));


            while ((in.readLine()) != null) {
                Log.info(in.readLine());

                out.println("hello");
                //names.add();
                MulerScript.setState(MulerState.MULING);
                Log.info("I read your message");
            }
            Waiting.wait(50);
        }
    }


    public static List<String> getNames() {
        return names;
    }
*/

public class MultiServerSocket implements Runnable {
    private ServerSocket serverSocket;
    private static List<String> names = new ArrayList<>();
    private static final String tradingRegex = "I want to mule! ([a-zA-Z]+( [a-zA-Z]+)+)";


    @SneakyThrows
    @Override
    public void run() {

        serverSocket = new ServerSocket(6668, 32, InetAddress.getByName("localhost"));


        while (true) {
            Log.info("Accepting connections");
                new EchoClientHandler(serverSocket.accept()).start();

            Waiting.wait(100);
        }
    }

    public void stop() throws IOException {
        serverSocket.close();
    }


    public static class EchoClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;

        public EchoClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @SneakyThrows
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    Log.info(inputLine);

                        try {
                            if (!names.contains(inputLine)) {
                                Log.debug("Adding: " + inputLine);
                                names.add(inputLine);
                                String finalInputLine = inputLine;
                                new java.util.Timer().schedule(new TimerTask() {
                                    @Override
                                    public void run() {
                                        Log.debug("3 minutes passed");
                                    }
                                }, 3 * 60000);
                            }
                            MulerScript.setState(MulerState.MULING);

                            var message = MyPlayer.getTile().getX() + " " + MyPlayer.getTile().getY() + " " + MyPlayer.getTile().getPlane() + " " + MyPlayer.getUsername() + " " + WorldHopper.getCurrentWorld();
                            Log.debug(message);
                            out.println(message);

                        } catch (Exception e) {
                            Log.error(e);
                            Log.debug("Couldn't mule");
                        }
                    }

                in.close();
                out.close();
                clientSocket.close();
            } catch (Exception e) {
                Log.error("Exception occured");
                Log.error(e);
            }
        }
    }

    public static List<String> getNames() {
        return names;
    }
}
