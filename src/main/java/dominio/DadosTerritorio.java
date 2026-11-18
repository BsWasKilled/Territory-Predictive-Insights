package dominio;

import java.util.EnumMap;
import java.util.Map;

import excecao.AcessoNegadoException;
import excecao.DadosInvalidosException;

/**
 * Dados do território monitorado e os limiares que valem para ele (cada
 * território alaga com uma chuva diferente, por isso os limiares são dele).
 *
 * Todos os atributos são privados. Os dados descritivos só mudam pelos setters,
 * que validam; os limiares só mudam com o código de acesso, que fica guardado
 * aqui dentro e não tem getter.
 */
public final class DadosTerritorio {

    private String nome;
    private String regiao;
    private int populacao;
    private int pontosVulneraveis;
    private final Map<TipoSensor, Limiares> limiares = new EnumMap<>(TipoSensor.class);
    private final String codigoAcesso;

    public DadosTerritorio(String nome, String regiao, int populacao, int pontosVulneraveis,
                           Map<TipoSensor, Limiares> limiaresIniciais, String codigoAcesso) {
        setNome(nome);
        setRegiao(regiao);
        setPopulacao(populacao);
        setPontosVulneraveis(pontosVulneraveis);

        if (codigoAcesso == null || codigoAcesso.isBlank()) {
            throw new DadosInvalidosException("o território precisa de um código de acesso para proteger os limiares");
        }
        this.codigoAcesso = codigoAcesso;

        for (TipoSensor tipo : TipoSensor.values()) {
            Limiares iniciais = limiaresIniciais == null ? null : limiaresIniciais.get(tipo);
            if (iniciais == null) {
                throw new DadosInvalidosException("faltam os limiares de " + tipo.getDescricao());
            }
            validarLimiaresParaTipo(tipo, iniciais);
            this.limiares.put(tipo, iniciais);
        }
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("o nome do território não pode ficar em branco");
        }
        this.nome = nome.trim();
    }

    public String getRegiao() {
        return regiao;
    }

    public void setRegiao(String regiao) {
        if (regiao == null || regiao.isBlank()) {
            throw new DadosInvalidosException("a região do território não pode ficar em branco");
        }
        this.regiao = regiao.trim();
    }

    public int getPopulacao() {
        return populacao;
    }

    public void setPopulacao(int populacao) {
        if (populacao < 0) {
            throw new DadosInvalidosException("população não pode ser negativa (informado: " + populacao + ")");
        }
        this.populacao = populacao;
    }

    public int getPontosVulneraveis() {
        return pontosVulneraveis;
    }

    public void setPontosVulneraveis(int pontosVulneraveis) {
        if (pontosVulneraveis < 0) {
            throw new DadosInvalidosException(
                    "quantidade de pontos vulneráveis não pode ser negativa (informado: " + pontosVulneraveis + ")");
        }
        this.pontosVulneraveis = pontosVulneraveis;
    }

    public Limiares getLimiares(TipoSensor tipo) {
        return limiares.get(tipo);
    }

    /**
     * Confere o código de acesso sem alterar nada. O menu usa este método para
     * recusar um código errado antes mesmo de pedir os novos limiares.
     *
     * @throws AcessoNegadoException se o código informado não conferir
     */
    public void conferirCodigoAcesso(String codigoInformado) {
        if (!codigoAcesso.equals(codigoInformado)) {
            throw new AcessoNegadoException("código de acesso incorreto");
        }
    }

    /**
     * Troca os limiares de um tipo de sensor. Só funciona com o código de
     * acesso correto; se o código ou os novos limiares forem recusados, nada
     * é alterado.
     *
     * @throws AcessoNegadoException   se o código informado não conferir
     * @throws DadosInvalidosException se os novos limiares não servirem para o sensor
     */
    public void alterarLimiares(String codigoInformado, TipoSensor tipo, Limiares novosLimiares) {
        conferirCodigoAcesso(codigoInformado);
        if (tipo == null || novosLimiares == null) {
            throw new DadosInvalidosException("informe o tipo de sensor e os novos limiares");
        }
        validarLimiaresParaTipo(tipo, novosLimiares);
        limiares.put(tipo, novosLimiares);
    }

    /** Um limiar acima do que o sensor consegue medir nunca dispararia: é configuração inválida. */
    private static void validarLimiaresParaTipo(TipoSensor tipo, Limiares limiares) {
        if (limiares.getCritico() > tipo.getMaximo()) {
            throw new DadosInvalidosException(
                    "limiar inválido: o limite crítico de " + tipo.getDescricao() + " ("
                            + tipo.formatar(limiares.getCritico()) + ") passa do máximo que o sensor mede ("
                            + tipo.formatar(tipo.getMaximo()) + ")");
        }
    }
}
