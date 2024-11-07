package com.example.tarefa1_matheusrodrigues;

public class CarData {
    private String name;
    private double x;
    private double y;
    private double speed;
    private double angle;
    private int sensorRange;
    private double distance;
    private int penalty;

    public CarData() { }

    public CarData(String name, double x, double y, double speed, double angle, int sensorRange, double distance, int penalty) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.angle = angle;
        this.sensorRange = sensorRange;
        this.distance = distance;
        this.penalty = penalty;
    }

    public String getName(){
        return this.name;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getSpeed() {
        return this.speed;
    }

    public double getAngle() {
        return this.angle;
    }

    public int getSensorRange() {
        return this.sensorRange;
    }

    public double getDistance(){
        return this.distance;
    }

    public int getPenalty(){
        return this.penalty;
    }

}
