package servico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import dominio.EstrategiaPrevisao;
import dominio.PrevisaoComparacao;
import dominio.PrevisaoMediaMovel;
import excecao.DadosInvalidosException;

/**
 * Fábrica das estratégias de previsão (padrão Factory).
 *
 * É o único lugar do sistema que sabe quais estratégias existem e como montar
 * cada uma a partir da configuração. O serviço só pede "as estratégias ativas"
 * e trabalha com o tipo EstrategiaPrevisao, sem if nem instanceof.
 *
 * Para incluir uma estratégia nova basta registrar a receita dela aqui e
 * acrescentar o nome na configuração; nenhum outro arquivo muda.
 */
public final class FabricaPrevisao {

    public static final String MEDIA_MOVEL = "MEDIA_MOVEL";
    public static final String COMPARACAO = "COMPARACAO";

    /** Para cada nome de estratégia, a receita que a constrói com os parâmetros da configuração. */
    private static final Map<String, Function<ConfiguracaoSistema, EstrategiaPrevisao>> RECEITAS =
            new LinkedHashMap<>();

    static {
        RECEITAS.put(MEDIA_MOVEL, configuracao -> new PrevisaoMediaMovel(configuracao.getJanelaMediaMovel()));
        RECEITAS.put(COMPARACAO, configuracao -> new PrevisaoComparacao(configuracao.getPeriodoComparacao()));
    }

    private FabricaPrevisao() {
        // só tem métodos estáticos: não deve ser instanciada
    }

    /**
     * Cria a estratégia registrada com o nome informado.
     *
     * @throws DadosInvalidosException se não existir estratégia com esse nome
     */
    public static EstrategiaPrevisao criar(String nome, ConfiguracaoSistema configuracao) {
        String chave = nome == null ? "" : nome.trim().toUpperCase(Locale.ROOT);
        Function<ConfiguracaoSistema, EstrategiaPrevisao> receita = RECEITAS.get(chave);
        if (receita == null) {
            throw new DadosInvalidosException(
                    "estratégia de previsão desconhecida: \"" + nome + "\" (disponíveis: "
                            + String.join(", ", RECEITAS.keySet()) + ")");
        }
        return receita.apply(configuracao);
    }

    /** Cria todas as estratégias marcadas como ativas na configuração, na ordem em que aparecem lá. */
    public static List<EstrategiaPrevisao> criarAtivas(ConfiguracaoSistema configuracao) {
        List<EstrategiaPrevisao> estrategias = new ArrayList<>();
        for (String nome : configuracao.getEstrategiasAtivas()) {
            estrategias.add(criar(nome, configuracao));
        }
        return estrategias;
    }
}
