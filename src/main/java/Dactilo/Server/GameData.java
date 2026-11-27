package Dactilo.Server;

import java.util.concurrent.atomic.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

/**
 *
 */
class GameData {
    static ConcurrentLinkedQueue<GameData> games = new ConcurrentLinkedQueue<>();
    AtomicBoolean hasStarted = new AtomicBoolean(false);
    AtomicInteger nbClients = new AtomicInteger(0);
    private final int  nbSlots;
    AtomicIntegerArray clients; //used as a Boolean atomic array because it does not seem to exist
    AtomicInteger nextFinisherPlace = new AtomicInteger(1);
    static Lock mutex;
    final String goal;

    protected GameData(String goal, int maxNumberClient) {
        nbSlots = maxNumberClient;
        clients = new AtomicIntegerArray(new int[maxNumberClient]);
        this.goal = goal;
        games.add(this);
    }

    /*todo make the game logic

        - game starts if one of the following is true
            - every body is here
            - a minium of 2 persons are in the game AND there is a time out (like 2 minutes)
        - when someone gets his result, he leaves the game by putting his current game at null
        - when every body has left OR if a timeout is called (like 5 minutes) the game kicks every body (set to null) and puts itself back in the queue (or create a new game with a new goal) //todo needs a pointer to every player to kick them

     */


    /**
     *
     * @return
     * @throws Exception
     */
    private int clientComes() throws Exception {
        nbClients.incrementAndGet();
        if (nbClients.get() == nbSlots){
            games.remove(this);
        }
        return reserveFreeSlot();
    }

    /**
     *
     * @return
     * @throws Exception
     */
    private int reserveFreeSlot() throws Exception {
        for (int i = 0; i<nbSlots; i++){
            if (clients.compareAndSet(i,0,1)){
                return i;
            };
        }
        throw new Exception("Trying to join a complete game");
    }



    /**
     *
     * @return
     * @throws Exception
     */
     static protected SimplePair<Integer,GameData> findAndJoinGame() throws Exception {
        GameData result;
        mutex.lock(); //ensures that only one client tries to join a game at the exact same instant
        result = games.peek();
        if (result == null){
            return null;
        }
        int index = result.clientComes();
        mutex.unlock();
        return new SimplePair<>(index,result);
     }

    /**
     *
     * @return
     */
    protected boolean Started(){
        return hasStarted.get();
    }
}
