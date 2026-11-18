package servico;

import java.util.Optional;

import dominio.Alerta;
import dominio.TipoSensor;

/**
 * O que o serviço devolve para cada estratégia aplicada a um sensor. Há três
 * desfechos possíveis, e cada um tem o próprio método de criação:
 *
 * - semHistoricoSuficiente: o sistema se cala, porque faltam leituras;
 * - semAlerta: a previsão foi emitida e está abaixo do limite de atenção;
 * - comAlerta: a previsão foi emitida e atingiu um limiar.
 *
 * Em todos os casos o resultado diz o que o valor significa, para que o menu
 * só precise mostrar.
 */
public final class ResultadoPrevisao {

    private final String codigoSensor;
    private final TipoSensor tipoSensor;
    private final String estrategia;
    private final boolean emitida;
    private final double valorPrevisto;
    private final String calculo;
    private final String significado;
    private final Alerta alerta;
    private final boolean alertaNovo;

    private ResultadoPrevisao(String codigoSensor, TipoSensor tipoSensor, String estrategia, boolean emitida,
                              double valorPrevisto, String calculo, String significado,
                              Alerta alerta, boolean alertaNovo) {
        this.codigoSensor = codigoSensor;
        this.tipoSensor = tipoSensor;
        this.estrategia = estrategia;
        this.emitida = emitida;
        this.valorPrevisto = valorPrevisto;
        this.calculo = calculo;
        this.significado = significado;
        this.alerta = alerta;
        this.alertaNovo = alertaNovo;
    }

    /** Não há leituras suficientes: nenhuma previsão é emitida para esta estratégia. */
    public static ResultadoPrevisao semHistoricoSuficiente(String codigoSensor, TipoSensor tipoSensor,
                                                           String estrategia, int leiturasDisponiveis,
                                                           int leiturasNecessarias) {
        String significado = "sem previsão: há " + leiturasDisponiveis + " de " + leiturasNecessarias
                + " leituras necessárias no banco; com tão pouco histórico o sistema prefere se calar";
        return new ResultadoPrevisao(codigoSensor, tipoSensor, estrategia, false, Double.NaN,
                "", significado, null, false);
    }

    /** Previsão emitida e abaixo do limite de atenção: situação normal. */
    public static ResultadoPrevisao semAlerta(String codigoSensor, TipoSensor tipoSensor, String estrategia,
                                              double valorPrevisto, String calculo, double limiteAtencao) {
        String significado = "situação normal: abaixo do limite de atenção ("
                + tipoSensor.formatar(limiteAtencao) + ")";
        return new ResultadoPrevisao(codigoSensor, tipoSensor, estrategia, true, valorPrevisto,
                calculo, significado, null, false);
    }

    /**
     * Previsão emitida que atingiu um limiar.
     *
     * @param alertaNovo true se o alerta acabou de ser gravado; false se ele já
     *                   tinha sido emitido para estas mesmas leituras
     */
    public static ResultadoPrevisao comAlerta(Alerta alerta, String calculo, boolean alertaNovo) {
        return new ResultadoPrevisao(alerta.getCodigoSensor(), alerta.getTipoSensor(), alerta.getEstrategia(),
                true, alerta.getValorPrevisto(), calculo, alerta.explicar(), alerta, alertaNovo);
    }

    public String getCodigoSensor() {
        return codigoSensor;
    }

    public TipoSensor getTipoSensor() {
        return tipoSensor;
    }

    /** Nome da estratégia de previsão que produziu este resultado. */
    public String getEstrategia() {
        return estrategia;
    }

    /** false quando o sistema se calou por falta de histórico. */
    public boolean isEmitida() {
        return emitida;
    }

    /** Valor previsto; só faz sentido quando a previsão foi emitida. */
    public double getValorPrevisto() {
        return valorPrevisto;
    }

    /** Como o valor previsto foi obtido, nas palavras da própria estratégia. */
    public String getCalculo() {
        return calculo;
    }

    /** O que o resultado quer dizer para quem acompanha o território. */
    public String getSignificado() {
        return significado;
    }

    /** O alerta ligado a esta previsão, se ela atingiu algum limiar. */
    public Optional<Alerta> getAlerta() {
        return Optional.ofNullable(alerta);
    }

    /** true se o alerta foi gravado agora; false se não há alerta ou se ele já existia. */
    public boolean isAlertaNovo() {
        return alertaNovo;
    }
}
