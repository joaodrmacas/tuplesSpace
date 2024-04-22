package pt.ulisboa.tecnico.tuplespaces.client;

import pt.ulisboa.tecnico.tuplespaces.client.grpc.ClientService;

public class ClientMain {

    private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
    static final int numServers = 3;

    private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}
    public static void main(String[] args) {


        // receive and print arguments
        debug(String.format("Received %d arguments%n", args.length));
        for (int i = 0; i < args.length; i++) {
            debug(String.format("arg[%d] = %s%n", i, args[i]));
        }

        if (args.length > 1){
            System.err.println("Argument(s) wrong!");
            System.err.println("Usage: mvn exec:java -Dexec.args=debug");
            return;
        }

        // check arguments
        if (args.length== 1 && args[0].matches("debug")){
            System.setProperty("debug", "true");
        }
        // use default qualifier "A"
        final String qualifier = "A";

        CommandProcessor parser = new CommandProcessor(new ClientService());
        parser.parseInput();

    }

    // private static boolean isValidArgs(String[] args) {
    //     if (args[0].matches("debug")){
    //         System.setProperty("debug", "true");

    //         if (args.length != 2) {
    //             System.err.println("Argument(s) missing!");
    //             System.err.println("Usage: mvn exec:java -Dexec.args=debug <qualifier>");
    //             return false;
    //         }
    
    //         return 
    //             (args[1].equals("A") || args[1].equals("B") || args[1].equals("C"));
    //     }

    //     if (args.length != 1) {
    //         System.err.println("Argument(s) missing!");
    //         System.err.println("Usage: mvn exec:java -Dexec.args=<qualifier>");
    //         return false;
    //     }

    //     return 
    //         (args[0].equals("A") || args[0].equals("B") || args[0].equals("C"));
    //   }
}
