package dominio;

import java.util.Random;

import excecao.DadosInvalidosException;
import excecao.FonteIndisponivelException;

/**
 * Parte comum dos sensores simulados. Todos acompanham a mesma "tempestade":
 * a intensidade sai da calmaria, chega ao pico e volta à calmaria a cada ciclo
 * de leituras. O que muda de um sensor para outro é como essa intensidade vira
 * a grandeza medida, e isso cada classe filha define.
 *
 * Como um equipamento de verdade, o sensor simulado também falha: às vezes não
 * responde (FonteIndisponivelException) e às vezes devolve um valor absurdo,
 * que a validação da Medicao barra antes de chegar ao banco.
 */
public abstract class SensorSimulado implements Sensor {

    /** Quantas leituras dura uma tempestade completa: calmaria, pico e calmaria. */
    private static final int LEITURAS_POR_TEMPESTADE = 16;
    /** Chance de o sensor não responder a uma leitura. */
    private static final double CHANCE_DE_FALHA = 0.08;
    /** Chance de o sensor responder com um valor impossível. */
    private static final double CHANCE_DE_DEFEITO = 0.04;

    private final String codigo;
    private final Random random;
    private int leiturasFeitas;
    private boolean respondeu = true;

    /**
     * @param random gerador de números aleatórios; vem de fora para que os
     *               testes possam controlar quando o sensor falha
     */
    protected SensorSimulado(String codigo, Random random) {
        if (codigo == null || codigo.isBlank()) {
            throw new DadosInvalidosException("sensor sem código de identificação");
        }
        if (random == null) {
            throw new DadosInvalidosException("sensor " + codigo + " sem gerador de valores");
        }
        this.codigo = codigo;
        this.random = random;
    }

    @Override
    public String getCodigo() {
        return codigo;
    }

    @Override
    public final double lerValor() throws FonteIndisponivelException {
        respondeu = random.nextDouble() >= CHANCE_DE_FALHA;
        if (!respondeu) {
            throw new FonteIndisponivelException(
                    "o sensor " + codigo + " (" + getTipo().getDescricao() + ") não respondeu à leitura");
        }
        leiturasFeitas++;
        if (random.nextDouble() < CHANCE_DE_DEFEITO) {
            return valorComDefeito();
        }
        double ruido = random.nextDouble() * 2.0 - 1.0; // entre -1 e 1
        double valor = simularValor(intensidadeDaTempestade(), ruido);
        return Math.round(valor * 10.0) / 10.0; // uma casa decimal, como um instrumento real
    }

    @Override
    public boolean verificarFuncionamento() {
        return respondeu;
    }

    /** Intensidade da tempestade neste momento: 0 na calmaria, 1 no pico. */
    private double intensidadeDaTempestade() {
        int passo = Math.floorMod(leiturasFeitas - atrasoEmLeituras(), LEITURAS_POR_TEMPESTADE);
        double seno = Math.sin(Math.PI * passo / LEITURAS_POR_TEMPESTADE);
        return seno * seno;
    }

    /**
     * Transforma a intensidade da tempestade na grandeza que o sensor mede.
     *
     * @param intensidade de 0 (calmaria) a 1 (pico da tempestade)
     * @param ruido       variação aleatória entre -1 e 1
     */
    protected abstract double simularValor(double intensidade, double ruido);

    /** Valor impossível que o equipamento devolve quando está com defeito. */
    protected abstract double valorComDefeito();

    /** Quantas leituras este sensor demora para reagir à chuva (0 = reage na hora). */
    protected int atrasoEmLeituras() {
        return 0;
    }
}
