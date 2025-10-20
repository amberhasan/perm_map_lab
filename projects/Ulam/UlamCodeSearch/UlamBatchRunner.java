public class UlamBatchRunner {
    public static void main(String[] args) {
        // Example: run UlamGreedySearch ten times
        // for (int i = 1; i <= 10; i++) {
        //     System.out.println("=== Run #" + i + " ===");
        //     String[] params = { "-r", "5", "8", "4" };
        //     UlamGreedySearch.main(params);
        //     System.out.println("=== Finished Run #" + i + " ===\n");
        // }

        // Example: infinite loop instead (uncomment if you want it never to stop)
        
        while (true) {
            String[] params = { "-r", "5", "8", "4" };
            UlamGreedySearch.main(params);
        }
        
    }
}
