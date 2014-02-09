package com.worldcretornica.lilylistme;

public class SortedPlayer implements Comparable<SortedPlayer> {

    String name;
    int rank;
    String color;
    boolean invisible;
    
    public SortedPlayer(String name, int rank, String color, boolean invisible) {
        this.name = name;
        this.rank = rank;
        this.color = color;
        this.invisible = invisible;
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
