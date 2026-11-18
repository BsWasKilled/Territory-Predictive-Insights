package dominio;

import java.util.Random;

/**
 * Sensor da galeria pluvial: mede quanto da galeria está ocupado pela água,
 * em %. Galeria cheia significa água voltando para a rua, ou seja, alagamento.
 */
public class SensorDrenagem extends SensorSimulado implements Sensor {

    public SensorDrenagem(String codigo) {
        this(codigo, new Random());
    }

    public SensorDrenagem(String codigo, Random random) {
        super(codigo, random);
    }

    @Override
    public TipoSensor getTipo() {
        return TipoSensor.DRENAGEM;
    }

    @Override
    protected double simularValor(double intensidade, double ruido) {
        // de 20% (tempo seco) a 100% (galeria cheia), oscilando até 6 pontos
        double ocupacao = 20.0 + 80.0 * intensidade + 6.0 * ruido;
        return Math.max(0.0, Math.min(100.0, ocupacao));
    }

    @Override
    protected double valorComDefeito() {
        return 120.0; // sensor descalibrado: ocupação acima de 100%
    }

    @Override
    protected int atrasoEmLeituras() {
        return 1;
    }
}
