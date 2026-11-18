package dominio;

import java.util.List;

/**
 * Previsão por média móvel: o próximo valor deve ficar perto da média das
 * últimas N leituras. Suaviza oscilações, mas reage devagar a mudanças bruscas.
 */
public class PrevisaoMediaMovel extends EstrategiaPrevisao {

    private final int janela;

    /** @param janela quantas leituras mais recentes entram na média */
    public PrevisaoMediaMovel(int janela) {
        super("Média móvel", janela);
        this.janela = janela;
    }

    @Override
    public double prever(List<Medicao> historico) {
        exigirHistoricoSuficiente(historico);
        return super.prever(recortar(historico, janela, 0));
    }

    @Override
    public String explicar(List<Medicao> historico) {
        exigirHistoricoSuficiente(historico);
        List<Medicao> ultimas = recortar(historico, janela, 0);
        TipoSensor tipo = ultimas.get(0).getTipo();

        double menor = ultimas.get(0).getValor();
        double maior = ultimas.get(0).getValor();
        for (Medicao leitura : ultimas) {
            menor = Math.min(menor, leitura.getValor());
            maior = Math.max(maior, leitura.getValor());
        }
        return "média das últimas " + janela + " leituras, que ficaram entre " + tipo.formatar(menor)
                + " e " + tipo.formatar(maior) + "; é o valor esperado se o ritmo recente continuar";
    }
}
