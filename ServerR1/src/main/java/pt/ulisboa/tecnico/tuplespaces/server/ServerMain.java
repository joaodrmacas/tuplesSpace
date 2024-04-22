package pt.ulisboa.tecnico.tuplespaces.server;

public class ServerMain {

    public static void main(String[] args) {

        int index = 0;
        // check arguments
        if (!isValidArgs(args)) {
            System.err.println("Invalid args");
            return;
        }

        if (System.getProperty("debug")!=null) index++;

        // get the port
        String port = args[index];
        index++;
        String qualifier = args[index];
        // create and execute the server
        new ServerService().execute(port, qualifier);
    }

    private static boolean isValidArgs(String[] args) {
        int port;

        if (args[0].matches("debug")){

            System.setProperty("debug", "true");

            try {
                port = Integer.parseInt(args[1]);
            }catch (NumberFormatException e) {
                System.err.println("Second argument is not an integer");
                return false;
            }

            return (args.length==3 && port>=0 && port<=65535 
            && args[2].length()==1 && (args[2].equals("A") ||
             args[2].equals("B") || args[2].equals("C")));
        }
        
        try {
            port = Integer.parseInt(args[0]);
        }catch (NumberFormatException e) {
            System.err.println("First argument is not an integer");
            return false;
        }
        return (args.length==2 && port>=0 && port<=65535 && 
        args[1].length()==1 && (args[1].equals("A") || 
        args[1].equals("B") || args[1].equals("C")));
    }
}

