package com.militopia;

import java.util.Random;

public class SimpleNoise {
    private int[] p = new int[512];
    private int[] permutation = new int[256];

    public SimpleNoise(long seed) {
        Random rand = new Random(seed);
        for (int i = 0; i < 256; i++) permutation[i] = i;
        for (int i = 0; i < 256; i++) {
            int j = rand.nextInt(256);
            int temp = permutation[i];
            permutation[i] = permutation[j];
            permutation[j] = temp;
        }
        for (int i = 0; i < 256; i++) p[256 + i] = p[i] = permutation[i];
    }

    public double eval(double x, double y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);
        double u = fade(xf);
        double v = fade(yf);
        int aa = p[p[xi] + yi], ab = p[p[xi] + yi + 1];
        int ba = p[p[xi + 1] + yi], bb = p[p[xi + 1] + yi + 1];
        return lerp(v, lerp(u, grad(p[aa], xf, yf), grad(p[ba], xf - 1, yf)),
                       lerp(u, grad(p[ab], xf, yf - 1), grad(p[bb], xf - 1, yf - 1)));
    }

    private double fade(double t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private double lerp(double t, double a, double b) { return a + t * (b - a); }
    private double grad(int hash, double x, double y) {
        return ((hash & 1) == 0 ? x : -x) + ((hash & 2) == 0 ? y : -y);
    }
}