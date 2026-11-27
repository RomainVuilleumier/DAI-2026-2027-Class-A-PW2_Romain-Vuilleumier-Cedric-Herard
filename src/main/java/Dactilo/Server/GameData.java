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
     static protected GameData findAndJoinGame() throws Exception {
        GameData result;
        mutex.lock(); //ensures that only one client tries to join a game at the exact same instant
        result = games.peek();
        if (result == null){
            return null;
        }
        result.clientComes();
        mutex.unlock();
        return result;
    }

    /**
     *
     * @return
     */
    protected boolean Started(){
        return hasStarted.get();
    }
}
