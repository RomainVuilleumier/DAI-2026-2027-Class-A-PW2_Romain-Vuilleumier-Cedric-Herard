package Dactilo.Server;

import java.util.concurrent.atomic.*;
import java.util.concurrent.*;

public class GameData {
    AtomicBoolean hasStarted = new AtomicBoolean(false);
    AtomicInteger nbClients = new AtomicInteger(0);
    AtomicIntegerArray clientsProgression;
    int currentBestProgression = 0;
    Semaphore gate = new Semaphore(0);

    final String goal;

    public GameData(String goal) {
        this.goal = goal;
    }

    protected void ReleaseGate(){
        gate.release(nbClients.get());
    }

    protected void ClientArrives(){
        nbClients.incrementAndGet();
    }

    public boolean Started(){
        return hasStarted.get();
    }
}
