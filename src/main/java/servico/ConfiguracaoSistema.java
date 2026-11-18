package servico;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dominio.DadosTerritorio;
import dominio.Limiares;
import dominio.TipoSensor;

/**
 * Configuração do sistema em instância única (padrão Singleton).
 *
 * Guarda o território monitorado, com os limiares de alerta, e os parâmetros
 * das previsões. Menu, serviço e relatório consultam sempre este mesmo objeto;
 * por isso um limiar alterado no menu vale na hora para o cálculo dos alertas,
 * sem cópias que possam ficar desatualizadas.
 *
 * O construtor é privado: o único jeito de obter a configuração é
 * {@link #getInstancia()}.
 */
public final class ConfiguracaoSistema {

    /**
     * Código de acesso inicial para alterar limiares. Fica no código-fonte
     * porque o projeto é didático; em um sistema real viria de fora do programa.
     */
    public static final String CODIGO_ACESSO_INICIAL = "FIAP2026";

    private static ConfiguracaoSistema instancia;

    private DadosTerritorio territorio;
    private int minimoLeituras;
    private int janelaMediaMovel;
    private int periodoComparacao;
    private List<String> estrategiasAtivas;

    private ConfiguracaoSistema() {
        restaurarPadroes();
    }

    /** Devolve a única instância, criando-a no primeiro uso. */
    public static synchronized ConfiguracaoSistema getInstancia() {
        if (instancia == null) {
            instancia = new ConfiguracaoSistema();
        }
        return instancia;
    }

    /**
     * Volta o território, os limiares e os parâmetros de previsão aos valores
     * iniciais. Os testes usam este método para que um cenário não herde
     * mudanças feitas por outro.
     */
    public void restaurarPadroes() {
        Map<TipoSensor, Limiares> limiaresIniciais = new EnumMap<>(TipoSensor.class);
        limiaresIniciais.put(TipoSensor.CHUVA, new Limiares(10.0, 25.0, 50.0));
        limiaresIniciais.put(TipoSensor.NIVEL_RIO, new Limiares(150.0, 220.0, 300.0));
        limiaresIniciais.put(TipoSensor.DRENAGEM, new Limiares(60.0, 80.0, 95.0));

        territorio = new DadosTerritorio("Comunidade Córrego Azul", "Zona Leste", 18500, 3,
                limiaresIniciais, CODIGO_ACESSO_INICIAL);
        minimoLeituras = 5;
        janelaMediaMovel = 5;
        periodoComparacao = 3;
        estrategiasAtivas = List.of(FabricaPrevisao.MEDIA_MOVEL, FabricaPrevisao.COMPARACAO);
    }

    /** Território monitorado; é por ele que os limiares são alterados, com o código de acesso. */
    public DadosTerritorio getTerritorio() {
        return territorio;
    }

    /** Limiares em vigor para um tipo de sensor. É daqui que o serviço tira os limites dos alertas. */
    public Limiares getLimiares(TipoSensor tipo) {
        return territorio.getLimiares(tipo);
    }

    /** Com menos leituras do que isto no banco, o sistema não emite previsão para o sensor. */
    public int getMinimoLeituras() {
        return minimoLeituras;
    }

    /** Quantas leituras recentes entram na média móvel. */
    public int getJanelaMediaMovel() {
        return janelaMediaMovel;
    }

    /** Quantas leituras formam cada um dos dois períodos da previsão por comparação. */
    public int getPeriodoComparacao() {
        return periodoComparacao;
    }

    /** Nomes das estratégias de previsão em uso; a FabricaPrevisao transforma cada nome em um objeto. */
    public List<String> getEstrategiasAtivas() {
        return estrategiasAtivas;
    }
}
