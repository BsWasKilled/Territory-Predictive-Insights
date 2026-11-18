package dominio;

import java.util.Random;

/**
 * Pluviômetro: mede a intensidade da chuva em mm/h. É o primeiro a reagir
 * quando a tempestade começa.
 */
public class SensorChuva extends SensorSimulado implements Sensor {

    public SensorChuva(String codigo) {
        this(codigo, new Random());
    }

    public SensorChuva(String codigo, Random random) {
        super(codigo, random);
    }

    @Override
    public TipoSensor getTipo() {
        return TipoSensor.CHUVA;
    }

    @Override
    protected double simularValor(double intensidade, double ruido) {
        // de garoa (2 mm/h) a tempestade (60 mm/h), oscilando até 4 mm/h
        double chuva = 2.0 + 58.0 * intensidade + 4.0 * ruido;
        return Math.max(0.0, chuva);
    }

    @Override
    protected double valorComDefeito() {
        return -1.0; // báscula travada: o equipamento informa chuva negativa
    }
}
