package com.ukkuru.mazemakers.core;

/** Seeded xorshift generator. Integer only, identical on every platform (PRD C-02). */
public final class Rng {
    private int s;

    public Rng(long seed) {
        s = (int) (seed ^ (seed >>> 32));
        if (s == 0) s = 0x9E3779B9;
    }

    public int nextInt(int bound) {
        s ^= s << 13;
        s ^= s >>> 17;
        s ^= s << 5;
        return (s >>> 1) % bound;
    }
}
