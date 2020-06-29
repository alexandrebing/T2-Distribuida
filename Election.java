public class Election {

    public Election(){
        
    }

    public void runElection() {
        while (true) {
            System.out.println("Running election");
            Thread.currentThread();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
}