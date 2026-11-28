package Dactilo.Server;

import java.util.concurrent.atomic.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
class GameData {
    static ConcurrentLinkedQueue<GameData> games = new ConcurrentLinkedQueue<>();
    private AtomicBoolean hasStarted = new AtomicBoolean(false);
    private AtomicInteger nbClients = new AtomicInteger(0);
    private final int  nbSlots;
    private AtomicIntegerArray clients; //used as a Boolean atomic array because it does not seem to exist
    private AtomicInteger nextFinisherPlace = new AtomicInteger(1);
    protected CyclicBarrier gate;//used to ensure that all threads gets notified by the start of the game
    static ReentrantLock mutex = new ReentrantLock();
    final String goal;

    protected GameData(String goal, int maxNumberClient) {
        nbSlots = maxNumberClient;
        gate = new CyclicBarrier(nbSlots);
        clients = new AtomicIntegerArray(new int[maxNumberClient]);
        this.goal = goal;
        games.add(this);
    }

    void leaveGame(int clientId){
        mutex.lock();
        if (clientId < 0 || clientId >= nbSlots) {
            return;
        }

        if (clients.get(clientId) == 0) {
            return;
        }

        clients.set(clientId,0);

        int remainingClients = nbClients.decrementAndGet();

        if (hasStarted.get() && remainingClients == 0){
            resetGame();
        }
        mutex.unlock();
    }

    void resetGame(){
        gate.reset();
        hasStarted.set(false);
        nextFinisherPlace.set(1);
        games.add(this);
    }

    /**
     *
     * @return
     * @throws Exception
     */
    private int clientComes() throws Exception {
        mutex.lock();
        int index = reserveFreeSlot();

        int count = nbClients.incrementAndGet();

        if (count == nbSlots && !hasStarted.get()) {
            games.remove(this);
            hasStarted.set(true);
        }
        mutex.unlock();
        return index;
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
            mutex.unlock();
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
     protected int getPosition(){
         return nextFinisherPlace.getAndIncrement();
     }

    /**
     *
     * @return
     */
    protected boolean Started(){
        return hasStarted.get();
    }
}
