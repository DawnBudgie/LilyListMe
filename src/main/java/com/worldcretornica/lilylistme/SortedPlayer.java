package com.worldcretornica.lilylistme;

public class SortedPlayer implements Comparable<SortedPlayer> {

    String name;
    int rank;
    String color;
    
    public SortedPlayer(String name, int rank, String color) {
        this.name = name;
        this.rank = rank;
        this.color = color;
    }

    @Override
    public int compareTo(SortedPlayer o) {
        if(this.rank == o.rank) {
            return this.name.compareTo(o.name);
        } else {
            return this.rank - o.rank;
        }
    }
    
    
}
