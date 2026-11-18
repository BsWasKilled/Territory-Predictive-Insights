package dominio;

import java.util.List;

/**
 * Previsão por comparação com o período anterior: calcula a média do período
 * mais recente e a do período logo antes dele, e projeta a mesma variação para
 * o período seguinte. Enxerga tendência (subindo ou caindo), o que a média
 * móvel não faz, mas precisa do dobro de leituras.
 */
public class PrevisaoComparacao extends EstrategiaPrevisao {

    /** Abaixo desta diferença, em unidades do sensor, a situação é tratada como estável. */
    private static final double VARIACAO_DESPREZIVEL = 0.05;

    private final int periodo;

    /** @param periodo quantas leituras formam cada um dos dois períodos comparados */
    public PrevisaoComparacao(int periodo) {
        super("Comparação com o período anterior", 2 * periodo);
        this.periodo = periodo;
    }

    @Override
    public double prever(List<Medicao> historico) {
        exigirHistoricoSuficiente(historico);
        double mediaRecente = super.prever(recortar(historico, periodo, 0));
        double mediaAnterior = super.prever(recortar(historico, periodo, periodo));
        double projecao = mediaRecente + (mediaRecente - mediaAnterior);

        // a projeção não pode passar do que o sensor consegue medir (ex.: chuva negativa)
        TipoSensor tipo = historico.get(0).getTipo();
        return Math.max(tipo.getMinimo(), Math.min(tipo.getMaximo(), projecao));
    }

    @Override
    public String explicar(List<Medicao> historico) {
        exigirHistoricoSuficiente(historico);
        double mediaRecente = super.prever(recortar(historico, periodo, 0));
        double mediaAnterior = super.prever(recortar(historico, periodo, periodo));
        double variacao = mediaRecente - mediaAnterior;
        TipoSensor tipo = historico.get(0).getTipo();

        String tendencia;
        if (variacao > VARIACAO_DESPREZIVEL) {
            tendencia = "subiu " + tipo.formatar(variacao);
        } else if (variacao < -VARIACAO_DESPREZIVEL) {
            tendencia = "caiu " + tipo.formatar(-variacao);
        } else {
            tendencia = "ficou estável";
        }
        return "as últimas " + periodo + " leituras tiveram média de " + tipo.formatar(mediaRecente)
                + ", contra " + tipo.formatar(mediaAnterior) + " nas " + periodo + " anteriores ("
                + tendencia + "); a previsão repete essa variação no próximo período";
    }
}
