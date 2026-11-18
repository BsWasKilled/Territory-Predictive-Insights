package dominio;

import java.util.List;

import excecao.DadosInvalidosException;

/**
 * Base de toda estratégia de previsão. Guarda o que as estratégias têm em
 * comum: um nome, a quantidade de leituras de que precisam e o cálculo mais
 * simples possível (a média das leituras recebidas).
 *
 * Cada estratégia concreta sobrescreve prever() para escolher QUAIS leituras
 * do histórico entram na conta e como combiná-las, reaproveitando o cálculo
 * daqui com super.prever(...). Quem usa uma estratégia só chama prever() e
 * explicar(), sem precisar saber qual delas recebeu (polimorfismo).
 *
 * O histórico deve vir em ordem cronológica: a leitura mais antiga primeiro.
 */
public abstract class EstrategiaPrevisao {

    private final String nome;
    private final int leiturasNecessarias;

    protected EstrategiaPrevisao(String nome, int leiturasNecessarias) {
        if (leiturasNecessarias < 2) {
            throw new DadosInvalidosException(
                    "uma previsão precisa de pelo menos 2 leituras (informado: " + leiturasNecessarias + ")");
        }
        this.nome = nome;
        this.leiturasNecessarias = leiturasNecessarias;
    }

    /**
     * Previsão base: a média das leituras recebidas.
     *
     * @throws DadosInvalidosException se não houver leitura alguma
     */
    public double prever(List<Medicao> leituras) {
        if (leituras == null || leituras.isEmpty()) {
            throw new DadosInvalidosException("não há leituras para calcular a previsão");
        }
        double soma = 0.0;
        for (Medicao leitura : leituras) {
            soma += leitura.getValor();
        }
        return soma / leituras.size();
    }

    /** Conta, em linguagem simples, como o valor previsto foi obtido e o que ele quer dizer. */
    public abstract String explicar(List<Medicao> historico);

    /**
     * Sem histórico suficiente a estratégia se recusa a prever: é melhor o
     * sistema se calar do que alarmar (ou tranquilizar) sem base.
     */
    protected void exigirHistoricoSuficiente(List<Medicao> historico) {
        int disponiveis = historico == null ? 0 : historico.size();
        if (disponiveis < leiturasNecessarias) {
            throw new DadosInvalidosException(
                    "histórico insuficiente para a estratégia \"" + nome + "\": há " + disponiveis
                            + " de " + leiturasNecessarias + " leituras necessárias");
        }
    }

    /** As 'quantidade' leituras que terminam 'recuo' posições antes do fim do histórico. */
    protected List<Medicao> recortar(List<Medicao> historico, int quantidade, int recuo) {
        int fim = historico.size() - recuo;
        return historico.subList(fim - quantidade, fim);
    }

    public String getNome() {
        return nome;
    }

    public int getLeiturasNecessarias() {
        return leiturasNecessarias;
    }
}
