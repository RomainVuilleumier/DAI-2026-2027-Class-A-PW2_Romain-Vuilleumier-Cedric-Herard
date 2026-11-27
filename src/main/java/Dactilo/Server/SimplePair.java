package Dactilo.Server;

public class SimplePair <T,U>{
    private T first;
    private U second;
    public SimplePair(){}
    public SimplePair(T first,U second){
        this.first = first;
        this.second = second;
    }
    public void setFirst(T newFirst){
        first = newFirst;
    }

    public void setSecond(U second) {
        this.second = second;
    }

    T getFirst(){
        return first;
    }
    U getSecond(){
        return second;
    }
}
