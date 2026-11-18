package dominio;

import java.util.Random;

/**
 * Régua eletrônica: mede o nível do córrego em cm. O córrego demora a subir,
 * por isso este sensor reage à chuva com algumas leituras de atraso.
 */
public class SensorNivelRio extends SensorSimulado implements Sensor {

    public SensorNivelRio(String codigo) {
        this(codigo, new Random());
    }

    public SensorNivelRio(String codigo, Random random) {
        super(codigo, random);
    }

    @Override
    public TipoSensor getTipo() {
        return TipoSensor.NIVEL_RIO;
    }

    @Override
    protected double simularValor(double intensidade, double ruido) {
        // do nível normal (80 cm) à cheia (340 cm), oscilando até 10 cm
        double nivel = 80.0 + 260.0 * intensidade + 10.0 * ruido;
        return Math.max(0.0, nivel);
    }

    @Override
    protected double valorComDefeito() {
        return 999.0; // eco falso do ultrassom: valor acima do que a régua alcança
    }

    @Override
    protected int atrasoEmLeituras() {
        return 3;
    }
}
